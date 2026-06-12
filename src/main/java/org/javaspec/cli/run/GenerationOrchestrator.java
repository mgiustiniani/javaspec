package org.javaspec.cli.run;

import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecNamingConvention;
import org.javaspec.discovery.TypeCheckResult;
import org.javaspec.discovery.TypeExistenceChecker;
import org.javaspec.generation.ClassConstructorUpdater;
import org.javaspec.generation.ClassMethodUpdater;
import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.generation.SpecFileGenerator;
import org.javaspec.generation.SpecGenerationPlan;
import org.javaspec.generation.SpecSkeletonGenerator;
import org.javaspec.generation.SpecSupportFileGenerator;
import org.javaspec.generation.TypeFileGenerator;
import org.javaspec.generation.TypeGenerationPlan;
import org.javaspec.generation.TypeSkeletonGenerator;
import org.javaspec.generation.ProphecyExistenceChecker;
import org.javaspec.generation.ProphecyFileGenerator;
import org.javaspec.generation.ProphecyGenerationPlan;
import org.javaspec.generation.ProphecySkeletonGenerator;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the generation/update loop over discovered specs: type-existence
 * checking, constructor/method skeleton updates, type skeleton generation, and
 * related-spec generation.
 * <p>Extracted from {@link org.javaspec.cli.Main Main} to isolate the generation
 * workflow and enable unit testing.</p>
 */
public final class GenerationOrchestrator {
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
        boolean missingWithoutGeneration = false;
        boolean dryRunPendingChanges = false;

        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            DescribedType describedType = spec.describedType();
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
                        generatedSourcesRoot
                );
                if (!relatedSpecResult.allAccepted()) {
                    missingWithoutGeneration = true;
                }
                if (relatedSpecResult.hasPendingChanges()) {
                    dryRunPendingChanges = true;
                }
            } catch (IOException ex) {
                err.println("I/O error while generating related specification: " + messageOf(ex));
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            } catch (SecurityException ex) {
                err.println("I/O error while generating related specification: " + messageOf(ex));
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            }

            if ((generate || dryRun) && describedType.hasMethods()) {
                SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(describedType, specRoot, generatedSourcesRoot, namingConvention);
                try {
                    if (dryRun) {
                        if (reportSupportDryRun(supportPlan, out, "specification support")) {
                            dryRunPendingChanges = true;
                        }
                    } else {
                        File supportFile = SpecSupportFileGenerator.writeOrUpdate(supportPlan);
                        out.println("Updated specification support: " + supportFile.getPath());
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
                String dryRunSource = null;
                if (describedType.hasConstructors() && checkResult.sourceFilePresent()) {
                    File sourceFile = checkResult.sourceFile();
                    try {
                        if (dryRun) {
                            dryRunSource = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
                            String updatedSource = ClassConstructorUpdater.updateSource(dryRunSource, describedType, constructorPolicy);
                            if (!dryRunSource.equals(updatedSource)) {
                                dryRunPendingChanges = true;
                                out.println("Would update constructors in " + sourceFile.getPath()
                                        + " (policy: " + policyOptionName(constructorPolicy) + ")");
                            }
                            dryRunSource = updatedSource;
                        } else {
                            ClassConstructorUpdater.updateFile(sourceFile, describedType, constructorPolicy);
                            out.println("Updated constructors in " + sourceFile.getPath()
                                    + " (policy: " + policyOptionName(constructorPolicy) + ")");
                        }
                    } catch (IOException ex) {
                        err.println("I/O error while updating constructors: " + messageOf(ex));
                        err.println("Target path: " + sourceFile.getPath());
                        return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
                    }
                }
                if (describedType.hasMethods() && checkResult.sourceFilePresent()) {
                    File sourceFile = checkResult.sourceFile();
                    try {
                        String existingSource = dryRun && dryRunSource != null
                                ? dryRunSource
                                : new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
                        String updatedSource = ClassMethodUpdater.updateSource(existingSource, describedType);
                        if (!existingSource.equals(updatedSource)) {
                            if (dryRun) {
                                dryRunPendingChanges = true;
                                out.println("Would update methods in " + sourceFile.getPath());
                            } else {
                                boolean accepted = generate || askToUpdateMethods(input, out, sourceFile, describedType);
                                if (!accepted) {
                                    missingWithoutGeneration = true;
                                    continue;
                                }
                                Files.write(sourceFile.toPath(), updatedSource.getBytes(StandardCharsets.UTF_8));
                                out.println("Updated methods in " + sourceFile.getPath());
                            }
                        }
                    } catch (IOException ex) {
                        err.println("I/O error while updating methods: " + messageOf(ex));
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
                dryRunPendingChanges = true;
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
                    missingWithoutGeneration = true;
                    continue;
                }
            }

            try {
                File generatedFile = TypeFileGenerator.write(plan);
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
                    specs, specRoot, sourceRoot, input, out, err, generate, dryRun, classLoader, generatedSourcesRoot
            );
        } catch (IOException ex) {
            err.println("I/O error while checking prophecy wrappers: " + messageOf(ex));
            return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
        }
        if (!prophecyWrappersGenerated) {
            missingWithoutGeneration = true;
        }

        if (dryRun) {
            if (dryRunPendingChanges) {
                out.println("Dry-run found pending generation/update work; no files were written.");
                return GenerationOrchestratorResult.missingNotGenerated();
            }
            out.println("Dry-run found no pending generation/update work.");
        }

        if (missingWithoutGeneration) {
            out.println("No production files were written.");
            return GenerationOrchestratorResult.missingNotGenerated();
        }

        return GenerationOrchestratorResult.proceed();
    }

    /**
     * Ensures that related types (extended, implemented, permitted) have
     * corresponding spec files.
     */
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
            File generatedSourcesRoot
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
                }
                out.println("Would generate related specification: " + specPlan.targetFile().getPath());
                pendingChanges = true;
                specs.add(DiscoveredSpec.of(specPlan.targetFile(), specPlan.specQualifiedName(), relatedType));
                continue;
            }

            boolean accepted = generate || askToGenerateSpec(input, out, specPlan);
            if (!accepted) {
                allAccepted = false;
                continue;
            }

            SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(relatedType, specRoot, generatedSourcesRoot, namingConvention);
            File generatedSupport = SpecSupportFileGenerator.writeOrUpdate(supportPlan);
            File generatedSpec = SpecFileGenerator.write(specPlan);
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
        String updatedSource = SpecSupportFileGenerator.updateSource(existingSource, supportPlan.describedType());
        if (!existingSource.equals(updatedSource)) {
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
            File generatedSourcesRoot
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
        List<String> prophesizedInterfaces;
        try {
            prophesizedInterfaces = ProphecyExistenceChecker.findProphesizedInterfaces(specFiles);
        } catch (IOException ex) {
            err.println("I/O error while scanning for prophecy calls: " + messageOf(ex));
            return false;
        }

        if (prophesizedInterfaces.isEmpty()) {
            return true;
        }

        boolean allGenerated = true;

        for (int i = 0; i < prophesizedInterfaces.size(); i++) {
            String interfaceFqcn = prophesizedInterfaces.get(i);

            // Check if wrapper already exists
            if (ProphecyExistenceChecker.wrapperExists(interfaceFqcn, classLoader)) {
                continue;
            }

            // Resolve the interface
            Class<?> interfaceType = ProphecyExistenceChecker.resolveInterface(interfaceFqcn, classLoader);
            if (interfaceType == null) {
                err.println("Warning: cannot resolve interface " + interfaceFqcn
                        + " referenced by prophesize() call; skipping wrapper generation.");
                continue;
            }

            String packageName = ProphecyExistenceChecker.defaultPackage(interfaceFqcn);
            String wrapperSimpleName = interfaceType.getSimpleName() + "Prophecy";
            String packagePath = packageName.replace('.', '/');
            File targetFile;
            if (packagePath.length() > 0) {
                targetFile = new File(generatedSourcesRoot, packagePath + "/" + wrapperSimpleName + ".java");
            } else {
                targetFile = new File(generatedSourcesRoot, wrapperSimpleName + ".java");
            }

            String sourceCode = ProphecySkeletonGenerator.render(interfaceType, packageName);

            out.println("Missing prophecy wrapper " + wrapperSimpleName
                    + " for " + interfaceFqcn + ".");
            out.println("Target path: " + targetFile.getPath());

            if (dryRun) {
                out.println("Would generate prophecy wrapper: " + targetFile.getPath());
                continue;
            }

            boolean accepted = generate || askToGenerateProphecy(input, out, interfaceFqcn);
            if (!accepted) {
                allGenerated = false;
                continue;
            }

            ProphecyGenerationPlan plan = ProphecyGenerationPlan.of(
                    interfaceType, packageName, targetFile, sourceCode
            );
            try {
                File writtenFile = ProphecyFileGenerator.write(plan);
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

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
