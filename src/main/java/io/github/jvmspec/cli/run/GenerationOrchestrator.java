package io.github.jvmspec.cli.run;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.discovery.TypeCheckResult;
import io.github.jvmspec.discovery.TypeExistenceChecker;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.generation.SpecGenerationPlan;
import io.github.jvmspec.generation.SpecSkeletonGenerator;
import io.github.jvmspec.generation.SpecSupportFileGenerator;
import io.github.jvmspec.generation.TypeFileGenerator;
import io.github.jvmspec.generation.TypeGenerationPlan;
import io.github.jvmspec.generation.TypeSkeletonGenerator;
import io.github.jvmspec.internal.language.BehaviorContract;
import io.github.jvmspec.internal.language.LanguageRuntime;
import io.github.jvmspec.internal.language.ProductionLanguageBackend;
import io.github.jvmspec.internal.language.SourceSynchronizationPlan;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        ProductionLanguageBackend productionBackend =
                LanguageRuntime.javaProductionBackend();

        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            // Production truth wins over spec inference: when the production source already
            // exists, adopt its declared signatures before generating support or skeletons.
            BehaviorContract behaviorContract = productionBackend.refine(
                    BehaviorContract.from(spec.describedType()), sourceRoot);
            DescribedType describedType = behaviorContract.describedType();
            GenerationOrchestratorResult functionalTargetFailure = GenerationPreflightValidator.validateFunctionalTargets(
                    describedType, spec, err);
            if (functionalTargetFailure != null) {
                return functionalTargetFailure;
            }
            GenerationOrchestratorResult recordComponentFailure = GenerationPreflightValidator.validateRecordComponentNames(
                    describedType, sourceRoot, spec, err);
            if (recordComponentFailure != null) {
                return recordComponentFailure;
            }
            try {
                RelatedSpecGenerationOrchestrator.Result relatedSpecResult =
                        RelatedSpecGenerationOrchestrator.ensure(
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
                err.println("I/O error while generating related specification: " + GenerationErrorMessages.messageOf(ex));
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            } catch (SecurityException ex) {
                err.println("I/O error while generating related specification: " + GenerationErrorMessages.messageOf(ex));
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            }

            GenerationOrchestratorResult sourceShapeFailure = GenerationPreflightValidator.validateExistingSourceShape(
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
                            if (GenerationDryRunReporter.reportSupport(supportPlan, out, "specification support")) {
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
                    err.println("I/O error while updating specification support: " + GenerationErrorMessages.messageOf(ex));
                    err.println("Target path: " + supportPlan.targetFile().getPath());
                    return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
                } catch (SecurityException ex) {
                    err.println("I/O error while updating specification support: " + GenerationErrorMessages.messageOf(ex));
                    err.println("Target path: " + supportPlan.targetFile().getPath());
                    return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
                }
            }

            TypeCheckResult checkResult;
            try {
                checkResult = TypeExistenceChecker.check(describedType, sourceRoot, classLoader);
            } catch (SecurityException ex) {
                err.println("I/O error while checking type existence: " + GenerationErrorMessages.messageOf(ex));
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
                        SourceSynchronizationPlan synchronization =
                                productionBackend.planSynchronization(
                                        existingSource, behaviorContract, constructorPolicy);
                        String proposedSource = synchronization.proposedSource();
                        boolean constructorChange = synchronization.constructorChange();
                        boolean methodChange = synchronization.methodChange();
                        int proposedChanges = synchronization.proposedChangeCount();
                        if (proposedChanges > 0) {
                            if (dryRun) {
                                if (constructorChange) activity.proposed("CONSTRUCTOR_SYNCHRONIZATION", sourceFile);
                                if (methodChange) activity.proposed("METHOD_SYNCHRONIZATION", sourceFile);
                                dryRunPendingChanges += proposedChanges;
                                if (constructorChange) {
                                    out.println("Would update constructors in " + sourceFile.getPath()
                                            + " (policy: " + GenerationAuthorization.policyOptionName(constructorPolicy) + ")");
                                }
                                if (methodChange) {
                                    out.println("Would update methods in " + sourceFile.getPath());
                                }
                            } else if (GenerationAuthorization.authorizeSourceSynchronization(
                                    generate, input, out, sourceFile, describedType,
                                    constructorChange, methodChange)) {
                                GenerationAuthorization.applyAuthorizedSourceUpdate(
                                        sourceFile, proposedSource, activity);
                                if (constructorChange) {
                                    out.println("Updated constructors in " + sourceFile.getPath()
                                            + " (policy: " + GenerationAuthorization.policyOptionName(constructorPolicy) + ")");
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
                        err.println("I/O error while synchronizing source: " + GenerationErrorMessages.messageOf(ex));
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
                    accepted = GenerationAuthorization.askToGenerate(input, out, plan);
                } catch (IOException ex) {
                    err.println("I/O error while reading generation confirmation: " + GenerationErrorMessages.messageOf(ex));
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
                        + " skeleton: " + GenerationErrorMessages.messageOf(ex));
                err.println("Target path: " + plan.targetFile().getPath());
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            } catch (SecurityException ex) {
                err.println("I/O error while generating " + plan.describedType().kind().displayName()
                        + " skeleton: " + GenerationErrorMessages.messageOf(ex));
                err.println("Target path: " + plan.targetFile().getPath());
                return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
            }
        }

        // --- Prophecy wrapper generation (C3) ---
        boolean prophecyWrappersGenerated;
        try {
            prophecyWrappersGenerated = ProphecyGenerationOrchestrator.ensureWrappers(
                    specs, specRoot, sourceRoot, input, out, err, generate, dryRun, classLoader,
                    generatedSourcesRoot, namingConvention, activity
            );
        } catch (IOException ex) {
            err.println("I/O error while checking prophecy wrappers: " + GenerationErrorMessages.messageOf(ex));
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


    private static String presentMessage(DiscoveredSpec spec, TypeCheckResult checkResult) {
        DescribedType describedType = spec.describedType();
        if (JavaTypeKind.CLASS.equals(describedType.kind())) {
            return spec.specQualifiedName() + " describes " + describedType.qualifiedName() + "; class exists.";
        }
        return spec.specQualifiedName() + " describes " + describedType.kind().displayName() + " "
                + describedType.qualifiedName() + "; type exists.";
    }


}
