package io.github.jvmspec.cli.run;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.ProductionSignatureReader;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.discovery.TypeCheckResult;
import io.github.jvmspec.discovery.TypeExistenceChecker;
import io.github.jvmspec.generation.AtomicFileWriter;
import io.github.jvmspec.generation.ClassConstructorUpdater;
import io.github.jvmspec.generation.ClassMethodUpdater;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.generation.SpecFileGenerator;
import io.github.jvmspec.generation.SpecGenerationPlan;
import io.github.jvmspec.generation.SpecSkeletonGenerator;
import io.github.jvmspec.generation.SpecSupportFileGenerator;
import io.github.jvmspec.generation.TypeFileGenerator;
import io.github.jvmspec.generation.TypeGenerationPlan;
import io.github.jvmspec.generation.TypeSkeletonGenerator;
import io.github.jvmspec.generation.ProphecyExistenceChecker;
import io.github.jvmspec.generation.ProphecyFileGenerator;
import io.github.jvmspec.generation.ProphecyGenerationPlan;
import io.github.jvmspec.generation.ProphecySkeletonGenerator;
import io.github.jvmspec.generation.SpecSupportFileGenerator;
import io.github.jvmspec.generation.parser.JavaSourceParserLoader;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Orchestrates the generation/update loop over discovered specs: type-existence
 * checking, constructor/method skeleton updates, type skeleton generation, and
 * related-spec generation.
 * <p>Extracted from {@link io.github.jvmspec.cli.Main Main} to isolate the generation
 * workflow and enable unit testing.</p>
 */
public final class GenerationOrchestrator {
    private static final String UNRESOLVED_FUNCTIONAL_PARAMETER_PREFIX = "__javaspecFunctionalArg";
    private static final int EXIT_OK = 0;
    private static final int EXIT_MISSING_NOT_GENERATED = 1;
    private static final int EXIT_IO_ERROR = 70;

    private GenerationOrchestrator() {
    }

    /**
     * Runs the generation/update loop over the given specs.
     *
     * @param specs              the discovered specs (may be extended with related specs)
     * @param specRoot           spec root directory
     * @param sourceRoot         source root directory
     * @param input              buffered reader for interactive prompting
     * @param out                output stream
     * @param err                error stream
     * @param generate           whether to auto-generate without prompting
     * @param dryRun             whether to run in dry-run mode
     * @param namingConvention   the spec naming convention
     * @param classLoader        the class loader for type existence checks
     * @param constructorPolicy  the constructor handling policy
     * @return a {@link GenerationOrchestratorResult} indicating exit code and
     *         whether execution should proceed
     */
    public static GenerationOrchestratorResult execute(
            List<DiscoveredSpec> specs,
            File specRoot,
            File sourceRoot,
            BufferedReader input,
            PrintStream out,
            PrintStream err,
            boolean generate,
            boolean dryRun,
            SpecNamingConvention namingConvention,
            ClassLoader classLoader,
            ConstructorPolicy constructorPolicy
    ) {
        return execute(specs, specRoot, sourceRoot, input, out, err, generate, dryRun,
                namingConvention, classLoader, constructorPolicy,
                new File("target/generated-sources/javaspec"));
    }

    /**
     * Runs the generation/update loop over the given specs.
     *
     * @param specs              the discovered specs (may be extended with related specs)
     * @param specRoot           spec root directory
     * @param sourceRoot         source root directory
     * @param input              buffered reader for interactive prompting
     * @param out                output stream
     * @param err                error stream
     * @param generate           whether to auto-generate without prompting
     * @param dryRun             whether to run in dry-run mode
     * @param namingConvention   the spec naming convention
     * @param classLoader        the class loader for type existence checks
     * @param constructorPolicy  the constructor handling policy
     * @param generatedSourcesRoot root directory for generated source files (support, prophecy)
     * @return a {@link GenerationOrchestratorResult} indicating exit code and
     *         whether execution should proceed
     */
    public static GenerationOrchestratorResult execute(
            List<DiscoveredSpec> specs,
            File specRoot,
            File sourceRoot,
            BufferedReader input,
            PrintStream out,
            PrintStream err,
            boolean generate,
            boolean dryRun,
            SpecNamingConvention namingConvention,
            ClassLoader classLoader,
            ConstructorPolicy constructorPolicy,
            File generatedSourcesRoot
    ) {
        return execute(specs, specRoot, sourceRoot, input, out, err, generate, dryRun,
                namingConvention, classLoader, constructorPolicy, generatedSourcesRoot,
                new GenerationActivity());
    }

    public static GenerationOrchestratorResult execute(
            List<DiscoveredSpec> specs,
            File specRoot,
            File sourceRoot,
            BufferedReader input,
            PrintStream out,
            PrintStream err,
            boolean generate,
            boolean dryRun,
            SpecNamingConvention namingConvention,
            ClassLoader classLoader,
            ConstructorPolicy constructorPolicy,
            File generatedSourcesRoot,
            GenerationActivity activity
    ) {
        int missingWithoutGeneration = 0;
        int dryRunPendingChanges = 0;

        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            // Production truth wins over spec inference: when the production source already
            // exists, adopt its declared signatures before generating support or skeletons.
            DescribedType describedType = ProductionSignatureReader.refine(spec.describedType(), sourceRoot);
            GenerationOrchestratorResult functionalTargetFailure = validateFunctionalTargets(
                    describedType, spec, err);
            if (functionalTargetFailure != null) {
                return functionalTargetFailure;
            }
            GenerationOrchestratorResult recordComponentFailure = validateRecordComponentNames(
                    describedType, sourceRoot, spec, err);
            if (recordComponentFailure != null) {
                return recordComponentFailure;
            }
            try {
                RelatedSpecCheckResult relatedSpecResult = ensureRelatedSpecs(
                        describedType,
                        specs,
                        specRoot,
                        sourceRoot,
                        input,
                        out,
                        generate,
                        dryRun,
                        namingConvention,
                        classLoader,
                        generatedSourcesRoot,
                        activity
                );
                if (!relatedSpecResult.allAccepted()) {
                    missingWithoutGeneration++;
                }
                if (relatedSpecResult.hasPendingChanges()) {
                    dryRunPendingChanges++;
                }
            } catch (IOException ex) {
                err.println("I/O error while generating related specification: " + messageOf(ex));
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            } catch (SecurityException ex) {
                err.println("I/O error while generating related specification: " + messageOf(ex));
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            }

            GenerationOrchestratorResult sourceShapeFailure = validateExistingSourceShape(
                    describedType, sourceRoot, err);
            if (sourceShapeFailure != null) {
                return sourceShapeFailure;
            }

            if (generate || dryRun) {
                SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(
                        describedType, specRoot, generatedSourcesRoot, namingConvention);
                try {
                    if (needsSupportUpdate(describedType) || declaresGeneratedSupport(spec, supportPlan)) {
                        if (dryRun) {
                            if (reportSupportDryRun(supportPlan, out, "specification support")) {
                                dryRunPendingChanges++;
                                activity.proposed("SPEC_SUPPORT", supportPlan.targetFile());
                            }
                        } else {
                            SpecSupportFileGenerator.SupportWriteResult supportResult =
                                    SpecSupportFileGenerator.writeOrUpdateResult(supportPlan);
                            if (supportResult.changed()) {
                                activity.applied("SPEC_SUPPORT", supportResult.file());
                                out.println("Updated specification support: " + supportResult.file().getPath());
                            }
                        }
                    }
                } catch (IOException ex) {
                    err.println("I/O error while updating specification support: " + messageOf(ex));
                    err.println("Target path: " + supportPlan.targetFile().getPath());
                    return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
                } catch (SecurityException ex) {
                    err.println("I/O error while updating specification support: " + messageOf(ex));
                    err.println("Target path: " + supportPlan.targetFile().getPath());
                    return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
                }
            }

            TypeCheckResult checkResult;
            try {
                checkResult = TypeExistenceChecker.check(describedType, sourceRoot, classLoader);
            } catch (SecurityException ex) {
                err.println("I/O error while checking type existence: " + messageOf(ex));
                err.println("Described type: " + describedType.qualifiedName());
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            }

            if (checkResult.isPresent()) {
                out.println(presentMessage(spec, checkResult));
                if (checkResult.sourceFilePresent()) {
                    out.println("Source file: " + checkResult.sourceFile().getPath());
                }
                if (checkResult.classpathPresent()) {
                    out.println("Classpath: present");
                    if (checkResult.classpathKind() != null && !checkResult.classpathKindMatches()) {
                        out.println("Classpath type kind: " + checkResult.classpathKind().displayName()
                                + " (expected " + describedType.kind().displayName() + ")");
                    }
                }
                if (checkResult.sourceFilePresent()
                        && (describedType.hasConstructors() || describedType.hasMethods())) {
                    File sourceFile = checkResult.sourceFile();
                    try {
                        String existingSource = new String(
                                Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
                        String proposedSource = existingSource;
                        boolean constructorChange = false;
                        boolean methodChange = false;
                        if (describedType.hasConstructors()) {
                            String constructorSource = ClassConstructorUpdater.updateSource(
                                    proposedSource, describedType, constructorPolicy);
                            constructorChange = !proposedSource.equals(constructorSource);
                            proposedSource = constructorSource;
                        }
                        if (describedType.hasMethods()) {
                            String methodSource = ClassMethodUpdater.updateSource(
                                    proposedSource, describedType);
                            methodChange = !proposedSource.equals(methodSource);
                            proposedSource = methodSource;
                        }
                        int proposedChanges = (constructorChange ? 1 : 0) + (methodChange ? 1 : 0);
                        if (proposedChanges > 0) {
                            if (dryRun) {
                                if (constructorChange) activity.proposed("CONSTRUCTOR_SYNCHRONIZATION", sourceFile);
                                if (methodChange) activity.proposed("METHOD_SYNCHRONIZATION", sourceFile);
                                dryRunPendingChanges += proposedChanges;
                                if (constructorChange) {
                                    out.println("Would update constructors in " + sourceFile.getPath()
                                            + " (policy: " + policyOptionName(constructorPolicy) + ")");
                                }
                                if (methodChange) {
                                    out.println("Would update methods in " + sourceFile.getPath());
                                }
                            } else if (authorizeSourceSynchronization(
                                    generate, input, out, sourceFile, describedType,
                                    constructorChange, methodChange)) {
                                applyAuthorizedSourceUpdate(
                                        sourceFile, proposedSource, activity);
                                if (constructorChange) {
                                    out.println("Updated constructors in " + sourceFile.getPath()
                                            + " (policy: " + policyOptionName(constructorPolicy) + ")");
                                }
                                if (methodChange) {
                                    out.println("Updated methods in " + sourceFile.getPath());
                                }
                            } else {
                                if (constructorChange) activity.proposed("CONSTRUCTOR_SYNCHRONIZATION", sourceFile);
                                if (methodChange) activity.proposed("METHOD_SYNCHRONIZATION", sourceFile);
                                missingWithoutGeneration += proposedChanges;
                            }
                        }
                    } catch (IOException ex) {
                        err.println("I/O error while synchronizing source: " + messageOf(ex));
                        err.println("Target path: " + sourceFile.getPath());
                        return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
                    }
                }
                continue;
            }

            TypeGenerationPlan plan = TypeSkeletonGenerator.plan(describedType, sourceRoot);
            out.println(spec.specQualifiedName() + " describes missing " + describedType.kind().displayName()
                    + " " + describedType.qualifiedName() + ".");
            out.println("Spec file: " + spec.specFile().getPath());
            out.println("Target path: " + plan.targetFile().getPath());

            if (dryRun) {
                dryRunPendingChanges++;
                activity.proposed("TYPE_SKELETON", plan.targetFile());
                out.println("Would generate " + plan.describedType().kind().displayName()
                        + " skeleton: " + plan.targetFile().getPath());
                continue;
            }

            if (!generate) {
                boolean accepted;
                try {
                    accepted = askToGenerate(input, out, plan);
                } catch (IOException ex) {
                    err.println("I/O error while reading generation confirmation: " + messageOf(ex));
                    return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
                }
                if (!accepted) {
                    activity.proposed("TYPE_SKELETON", plan.targetFile());
                    missingWithoutGeneration++;
                    continue;
                }
            }

            try {
                File generatedFile = TypeFileGenerator.write(plan);
                activity.applied("TYPE_SKELETON", generatedFile);
                out.println("Generated " + plan.describedType().kind().displayName()
                        + " skeleton: " + generatedFile.getPath());
            } catch (IOException ex) {
                err.println("I/O error while generating " + plan.describedType().kind().displayName()
                        + " skeleton: " + messageOf(ex));
                err.println("Target path: " + plan.targetFile().getPath());
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            } catch (SecurityException ex) {
                err.println("I/O error while generating " + plan.describedType().kind().displayName()
                        + " skeleton: " + messageOf(ex));
                err.println("Target path: " + plan.targetFile().getPath());
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            }
        }

        // --- Prophecy wrapper generation (C3) ---
        boolean prophecyWrappersGenerated;
        try {
            prophecyWrappersGenerated = ensureProphecyWrappers(
                    specs, specRoot, sourceRoot, input, out, err, generate, dryRun, classLoader,
                    generatedSourcesRoot, namingConvention, activity
            );
        } catch (IOException ex) {
            err.println("I/O error while checking prophecy wrappers: " + messageOf(ex));
            return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
        }
        if (!prophecyWrappersGenerated) {
            missingWithoutGeneration++;
        }

        if (dryRun) {
            if (dryRunPendingChanges > 0) {
                out.println("Dry-run found pending generation/update work; no files were written.");
                return GenerationOrchestratorResult.missingNotGenerated(dryRunPendingChanges);
            }
            out.println("Dry-run found no pending generation/update work.");
        }

        if (missingWithoutGeneration > 0) {
            out.println("No production files were written.");
            return GenerationOrchestratorResult.missingNotGenerated(missingWithoutGeneration);
        }

        return GenerationOrchestratorResult.proceed();
    }

    /**
     * Ensures that related types (extended, implemented, permitted) have
     * corresponding spec files.
     */
    private static boolean needsSupportUpdate(DescribedType describedType) {
        return describedType.hasMethods()
                || describedType.hasEnumConstants()
                || (JavaTypeKind.RECORD.equals(describedType.kind()) && describedType.hasConstructors());
    }

    private static boolean declaresGeneratedSupport(
            DiscoveredSpec spec,
            SpecGenerationPlan supportPlan
    ) throws IOException {
        String supportFileName = supportPlan.targetFile().getName();
        String supportClassName = supportFileName.substring(
                0, supportFileName.length() - SpecNamingConvention.JAVA_SUFFIX.length());
        String source = new String(Files.readAllBytes(spec.specFile().toPath()), StandardCharsets.UTF_8);
        return Pattern.compile("\\bextends\\s+" + Pattern.quote(supportClassName) + "\\b")
                .matcher(source)
                .find();
    }

    private static RelatedSpecCheckResult ensureRelatedSpecs(
            DescribedType owner,
            List<DiscoveredSpec> specs,
            File specRoot,
            File sourceRoot,
            BufferedReader input,
            PrintStream out,
            boolean generate,
            boolean dryRun,
            SpecNamingConvention namingConvention,
            ClassLoader classLoader,
            File generatedSourcesRoot,
            GenerationActivity activity
    ) throws IOException {
        boolean allAccepted = true;
        boolean pendingChanges = false;
        List<DescribedType> relatedTypes = relatedTypesOf(owner);
        for (int i = 0; i < relatedTypes.size(); i++) {
            DescribedType relatedType = relatedTypes.get(i);
            TypeCheckResult checkResult = TypeExistenceChecker.check(relatedType, sourceRoot, classLoader);
            if (checkResult.isPresent()) {
                continue;
            }

            SpecGenerationPlan specPlan = SpecSkeletonGenerator.plan(relatedType, specRoot, namingConvention);
            if (specPlan.targetFile().exists() || isSpecKnown(specs, specPlan.specQualifiedName())) {
                continue;
            }

            out.println("Related " + relatedType.kind().displayName() + " " + relatedType.qualifiedName()
                    + " is missing.");
            out.println("Spec target path: " + specPlan.targetFile().getPath());
            if (dryRun) {
                SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(relatedType, specRoot, generatedSourcesRoot, namingConvention);
                if (reportSupportDryRun(supportPlan, out, "related specification support")) {
                    pendingChanges = true;
                    activity.proposed("RELATED_SPEC_SUPPORT", supportPlan.targetFile());
                }
                activity.proposed("RELATED_SPEC", specPlan.targetFile());
                out.println("Would generate related specification: " + specPlan.targetFile().getPath());
                pendingChanges = true;
                specs.add(DiscoveredSpec.of(specPlan.targetFile(), specPlan.specQualifiedName(), relatedType));
                continue;
            }

            boolean accepted = generate || askToGenerateSpec(input, out, specPlan);
            if (!accepted) {
                activity.proposed("RELATED_SPEC", specPlan.targetFile());
                allAccepted = false;
                continue;
            }

            SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(relatedType, specRoot, generatedSourcesRoot, namingConvention);
            File generatedSupport = SpecSupportFileGenerator.writeOrUpdate(supportPlan);
            File generatedSpec = SpecFileGenerator.write(specPlan);
            activity.applied("RELATED_SPEC_SUPPORT", generatedSupport);
            activity.applied("RELATED_SPEC", generatedSpec);
            out.println("Generated related specification support: " + generatedSupport.getPath());
            out.println("Generated related specification: " + generatedSpec.getPath());
            specs.add(DiscoveredSpec.of(generatedSpec, specPlan.specQualifiedName(), relatedType));
        }
        return RelatedSpecCheckResult.of(allAccepted, pendingChanges);
    }

    private static List<DescribedType> relatedTypesOf(DescribedType owner) {
        List<DescribedType> relatedTypes = new ArrayList<DescribedType>();
        addExtendedRelatedTypes(relatedTypes, owner);
        addImplementedRelatedTypes(relatedTypes, owner);
        addPermittedRelatedTypes(relatedTypes, owner);
        return relatedTypes;
    }

    private static void addExtendedRelatedTypes(List<DescribedType> relatedTypes, DescribedType owner) {
        JavaTypeKind relatedKind = (JavaTypeKind.INTERFACE.equals(owner.kind())
                || JavaTypeKind.SEALED_INTERFACE.equals(owner.kind()))
                ? JavaTypeKind.INTERFACE
                : JavaTypeKind.CLASS;
        for (int i = 0; i < owner.extendedTypeNames().size(); i++) {
            addRelatedType(relatedTypes, DescribedType.of(owner.extendedTypeNames().get(i), relatedKind));
        }
    }

    private static void addImplementedRelatedTypes(List<DescribedType> relatedTypes, DescribedType owner) {
        for (int i = 0; i < owner.implementedTypeNames().size(); i++) {
            addRelatedType(relatedTypes, DescribedType.of(owner.implementedTypeNames().get(i), JavaTypeKind.INTERFACE));
        }
    }

    private static void addPermittedRelatedTypes(List<DescribedType> relatedTypes, DescribedType owner) {
        if (JavaTypeKind.SEALED_INTERFACE.equals(owner.kind())) {
            return;
        }
        for (int i = 0; i < owner.permittedTypeNames().size(); i++) {
            String permittedTypeName = owner.permittedTypeNames().get(i);
            addRelatedType(relatedTypes, DescribedType.of(
                    permittedTypeName,
                    JavaTypeKind.FINAL_CLASS,
                    singleList(owner.qualifiedName()),
                    emptyStringList(),
                    emptyStringList()
            ));
        }
    }

    private static List<String> singleList(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    private static List<String> emptyStringList() {
        return new ArrayList<String>();
    }

    private static void addRelatedType(List<DescribedType> relatedTypes, DescribedType candidate) {
        for (int i = 0; i < relatedTypes.size(); i++) {
            if (relatedTypes.get(i).qualifiedName().equals(candidate.qualifiedName())) {
                return;
            }
        }
        relatedTypes.add(candidate);
    }

    private static boolean isSpecKnown(List<DiscoveredSpec> specs, String specQualifiedName) {
        for (int i = 0; i < specs.size(); i++) {
            if (specQualifiedName.equals(specs.get(i).specQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean reportSupportDryRun(
            SpecGenerationPlan supportPlan,
            PrintStream out,
            String artifactName
    ) throws IOException {
        File targetFile = supportPlan.targetFile();
        if (!targetFile.exists()) {
            out.println("Would generate " + artifactName + ": " + targetFile.getPath());
            return true;
        }

        String existingSource = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
        if (!existingSource.equals(supportPlan.sourceContent())) {
            out.println("Would update " + artifactName + ": " + targetFile.getPath());
            return true;
        }
        return false;
    }

    private static String presentMessage(DiscoveredSpec spec, TypeCheckResult checkResult) {
        DescribedType describedType = spec.describedType();
        if (JavaTypeKind.CLASS.equals(describedType.kind())) {
            return spec.specQualifiedName() + " describes " + describedType.qualifiedName() + "; class exists.";
        }
        return spec.specQualifiedName() + " describes " + describedType.kind().displayName() + " "
                + describedType.qualifiedName() + "; type exists.";
    }

    private static String promptTarget(DescribedType describedType) {
        if (JavaTypeKind.CLASS.equals(describedType.kind())) {
            return describedType.qualifiedName();
        }
        return describedType.kind().displayName() + " " + describedType.qualifiedName();
    }

    private static String policyOptionName(ConstructorPolicy policy) {
        return policy.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }

    private static boolean askToGenerateSpec(
            BufferedReader input,
            PrintStream out,
            SpecGenerationPlan plan
    ) throws IOException {
        while (true) {
            out.println("Do you want me to create specification " + plan.specQualifiedName()
                    + " for " + promptTarget(plan.describedType()) + "? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }

    private static boolean askToGenerate(
            BufferedReader input,
            PrintStream out,
            TypeGenerationPlan plan
    ) throws IOException {
        while (true) {
            out.println("Do you want me to create " + promptTarget(plan.describedType()) + " for you? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }

    private static boolean authorizeSourceSynchronization(
            boolean generate,
            BufferedReader input,
            PrintStream out,
            File sourceFile,
            DescribedType describedType,
            boolean constructorChange,
            boolean methodChange
    ) throws IOException {
        if (generate) return true;
        if (methodChange && !constructorChange) {
            return askToUpdateMethods(input, out, sourceFile, describedType);
        }
        String scope = methodChange ? "constructors and methods" : "constructors";
        while (true) {
            out.println("Do you want me to update " + scope + " for "
                    + promptTarget(describedType) + " in " + sourceFile.getPath() + "? [Y/n]");
            String answer = input.readLine();
            if (answer == null) return false;
            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized)
                    || "yes".equalsIgnoreCase(normalized)) return true;
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) return false;
            out.println("Please answer y or n.");
        }
    }

    private static void applyAuthorizedSourceUpdate(
            File sourceFile,
            String proposedSource,
            GenerationActivity activity
    ) throws IOException {
        AtomicFileWriter.writeUtf8(sourceFile, proposedSource);
        activity.applied("SOURCE_SYNCHRONIZATION", sourceFile);
    }

    private static boolean askToUpdateMethods(
            BufferedReader input,
            PrintStream out,
            File sourceFile,
            DescribedType describedType
    ) throws IOException {
        while (true) {
            out.println("Do you want me to add missing method skeletons to "
                    + promptTarget(describedType) + " in " + sourceFile.getPath() + "? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }

    private static final class RelatedSpecCheckResult {
        private final boolean allAccepted;
        private final boolean pendingChanges;

        private RelatedSpecCheckResult(boolean allAccepted, boolean pendingChanges) {
            this.allAccepted = allAccepted;
            this.pendingChanges = pendingChanges;
        }

        static RelatedSpecCheckResult of(boolean allAccepted, boolean pendingChanges) {
            return new RelatedSpecCheckResult(allAccepted, pendingChanges);
        }

        boolean allAccepted() {
            return allAccepted;
        }

        boolean hasPendingChanges() {
            return pendingChanges;
        }
    }

    private static boolean ensureProphecyWrappers(
            List<DiscoveredSpec> specs,
            File specRoot,
            File sourceRoot,
            BufferedReader input,
            PrintStream out,
            PrintStream err,
            boolean generate,
            boolean dryRun,
            ClassLoader classLoader,
            File generatedSourcesRoot,
            SpecNamingConvention namingConvention,
            GenerationActivity activity
    ) throws IOException {
        // Collect spec files
        List<File> specFiles = new ArrayList<File>();
        for (int i = 0; i < specs.size(); i++) {
            File specFile = specs.get(i).specFile();
            if (specFile != null && specFile.exists()) {
                specFiles.add(specFile);
            }
        }

        if (specFiles.isEmpty()) {
            return true;
        }

        // Find prophesize/prophecy calls in spec files
        List<String> prophesizedTypes;
        try {
            prophesizedTypes = ProphecyExistenceChecker.findProphesizedInterfaces(specFiles);
        } catch (IOException ex) {
            err.println("I/O error while scanning for prophecy calls: " + messageOf(ex));
            return false;
        }

        if (prophesizedTypes.isEmpty()) {
            return true;
        }

        boolean allGenerated = true;

        for (int i = 0; i < prophesizedTypes.size(); i++) {
            String typeFqcn = prophesizedTypes.get(i);

            // Check if wrapper already exists
            if (ProphecyExistenceChecker.wrapperExists(typeFqcn, classLoader)) {
                continue;
            }

            // Resolve the prophesized type
            Class<?> prophesizedType = ProphecyExistenceChecker.resolveProphesizedType(typeFqcn, classLoader);
            if (prophesizedType == null) {
                err.println("Warning: cannot resolve prophesized type " + typeFqcn
                        + " referenced by prophesize() call; skipping wrapper generation.");
                continue;
            }

            String packageName = ProphecyExistenceChecker.defaultPackage(typeFqcn);
            String wrapperSimpleName = prophesizedType.getSimpleName() + "Prophecy";
            String packagePath = packageName.replace('.', '/');
            File targetFile;
            if (packagePath.length() > 0) {
                targetFile = new File(generatedSourcesRoot, packagePath + "/" + wrapperSimpleName + ".java");
            } else {
                targetFile = new File(generatedSourcesRoot, wrapperSimpleName + ".java");
            }

            String sourceCode = ProphecySkeletonGenerator.render(prophesizedType, packageName);

            out.println("Missing prophecy wrapper " + wrapperSimpleName
                    + " for " + typeFqcn + ".");
            out.println("Target path: " + targetFile.getPath());

            if (dryRun) {
                activity.proposed("PROPHECY_WRAPPER", targetFile);
                out.println("Would generate prophecy wrapper: " + targetFile.getPath());
                continue;
            }

            boolean accepted = generate || askToGenerateProphecy(input, out, typeFqcn);
            if (!accepted) {
                activity.proposed("PROPHECY_WRAPPER", targetFile);
                allGenerated = false;
                continue;
            }

            ProphecyGenerationPlan plan = ProphecyGenerationPlan.of(
                    prophesizedType, packageName, targetFile, sourceCode
            );
            try {
                File writtenFile = ProphecyFileGenerator.write(plan);
                activity.applied("PROPHECY_WRAPPER", writtenFile);
                out.println("Generated prophecy wrapper: " + writtenFile.getPath());
            } catch (IOException ex) {
                err.println("I/O error while generating prophecy wrapper: " + messageOf(ex));
                err.println("Target path: " + targetFile.getPath());
                return false;
            } catch (SecurityException ex) {
                err.println("I/O error while generating prophecy wrapper: " + messageOf(ex));
                err.println("Target path: " + targetFile.getPath());
                return false;
            }
        }

        // --- Update support class helpers in generatedSourcesRoot, never in src ---
        updateSupportProphecyHelpers(
                specs, specRoot, generatedSourcesRoot, namingConvention, prophesizedTypes,
                dryRun, generate, out, err, activity);

        return allGenerated;
    }

    private static boolean askToGenerateProphecy(
            BufferedReader input,
            PrintStream out,
            String interfaceFqcn
    ) throws IOException {
        while (true) {
            out.println("Do you want me to create prophecy wrapper " + interfaceFqcn + "Prophecy? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }

    private static void updateSupportProphecyHelpers(
            List<DiscoveredSpec> specs,
            File specRoot,
            File generatedSourcesRoot,
            SpecNamingConvention namingConvention,
            List<String> prophesizedTypeNames,
            boolean dryRun,
            boolean generate,
            PrintStream out,
            PrintStream err,
            GenerationActivity activity
    ) {
        if (prophesizedTypeNames.isEmpty()) {
            return;
        }
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            // Collect only the types prophesized by this spec's source file
            File specFile = spec.specFile();
            if (specFile == null || !specFile.exists()) {
                continue;
            }
            List<String> typesForThisSpec = new ArrayList<String>();
            try {
                List<String> found = ProphecyExistenceChecker.findProphesizedInterfaces(specFile);
                for (int j = 0; j < prophesizedTypeNames.size(); j++) {
                    if (found.contains(prophesizedTypeNames.get(j))) {
                        typesForThisSpec.add(prophesizedTypeNames.get(j));
                    }
                }
            } catch (IOException ex) {
                err.println("I/O error scanning spec for prophecy helpers: " + messageOf(ex));
                continue;
            }
            if (typesForThisSpec.isEmpty()) {
                continue;
            }
            // The support file lives only in generatedSourcesRoot, never in src
            io.github.jvmspec.model.DescribedClass describedClass = spec.describedClass();
            SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(
                    spec.describedType(), specRoot, generatedSourcesRoot, namingConvention);
            File supportFile = supportPlan.targetFile();
            if (!supportFile.isFile()) {
                continue;
            }
            try {
                String source = new String(
                        java.nio.file.Files.readAllBytes(supportFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                String updated = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                        source, spec.describedType(), typesForThisSpec);
                if (!source.equals(updated)) {
                    if (dryRun || !generate) {
                        activity.proposed("PROPHECY_SUPPORT_HELPERS", supportFile);
                        out.println("Would add prophecy helpers to support: " + supportFile.getPath());
                    } else {
                        AtomicFileWriter.writeUtf8(supportFile, updated);
                        activity.applied("PROPHECY_SUPPORT_HELPERS", supportFile);
                        out.println("Updated prophecy helpers in support: " + supportFile.getPath());
                    }
                }
            } catch (IOException ex) {
                err.println("I/O error updating prophecy helpers in support: " + messageOf(ex));
            }
        }
    }

    private static GenerationOrchestratorResult validateRecordComponentNames(
            DescribedType describedType,
            File sourceRoot,
            DiscoveredSpec spec,
            PrintStream err
    ) {
        if (!JavaTypeKind.RECORD.equals(describedType.kind()) || !describedType.hasConstructors()) {
            return null;
        }
        try {
            File sourceFile = new File(sourceRoot,
                    describedType.qualifiedName().replace('.', File.separatorChar) + ".java");
            if (sourceFile.isFile()) {
                String source = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
                ClassConstructorUpdater.updateSource(source, describedType, ConstructorPolicy.PRESERVE);
            } else {
                TypeSkeletonGenerator.plan(describedType, sourceRoot);
            }
            return null;
        } catch (IllegalArgumentException ex) {
            err.println(messageOf(ex));
            err.println("Spec file: " + spec.specFile().getPath());
            err.println("Production source was not written. Add one unambiguous zero-argument accessor "
                    + "expectation for each constructor value or use a reliably named local/formal parameter.");
            return GenerationOrchestratorResult.missingNotGenerated();
        } catch (IOException ex) {
            err.println("I/O error while validating record component names: " + messageOf(ex));
            err.println("Spec file: " + spec.specFile().getPath());
            return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
        }
    }

    private static GenerationOrchestratorResult validateFunctionalTargets(
            DescribedType describedType,
            DiscoveredSpec spec,
            PrintStream err
    ) {
        for (int methodIndex = 0; methodIndex < describedType.methods().size(); methodIndex++) {
            MethodDescriptor method = describedType.methods().get(methodIndex);
            for (int parameterIndex = 0; parameterIndex < method.parameterNames().size(); parameterIndex++) {
                String parameterName = method.parameterNames().get(parameterIndex);
                if (!method.isParameterTypeUnknown(parameterIndex)
                        || !parameterName.startsWith(UNRESOLVED_FUNCTIONAL_PARAMETER_PREFIX)) {
                    continue;
                }
                err.println("Cannot infer functional-interface target for " + describedType.qualifiedName()
                        + "#" + method.methodName() + " parameter " + (parameterIndex + 1) + ".");
                err.println("Spec file: " + spec.specFile().getPath());
                err.println("Assign the lambda or method reference to an explicitly typed variable, add an explicit "
                        + "functional-interface cast, or provide one unambiguous production signature.");
                return GenerationOrchestratorResult.missingNotGenerated();
            }
        }
        return null;
    }

    private static GenerationOrchestratorResult validateExistingSourceShape(
            DescribedType describedType,
            File sourceRoot,
            PrintStream err
    ) {
        if (!describedType.hasMethods() && !describedType.hasConstructors()) {
            return null;
        }
        File sourceFile = new File(
                sourceRoot,
                describedType.qualifiedName().replace('.', File.separatorChar) + ".java"
        );
        if (!sourceFile.isFile()) {
            return null;
        }
        try {
            String source = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
            int closingBrace = JavaSourceParserLoader.defaultParser()
                    .parse(source)
                    .typeClosingBraceOffset(describedType.simpleName());
            if (closingBrace >= 0) {
                return null;
            }
            err.println("Unsupported source update: no named class-like declaration for "
                    + describedType.qualifiedName() + " was found in " + sourceFile.getPath() + ".");
            err.println("Compact source files and implicit classes are not supported as javaspec subjects; "
                    + "use a named class, record, interface, enum, or annotation.");
            return GenerationOrchestratorResult.missingNotGenerated();
        } catch (IOException ex) {
            err.println("I/O error while validating source shape: " + messageOf(ex));
            err.println("Target path: " + sourceFile.getPath());
            return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
        }
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
