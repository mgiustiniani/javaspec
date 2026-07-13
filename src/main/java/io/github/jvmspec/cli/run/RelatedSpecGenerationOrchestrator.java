package io.github.jvmspec.cli.run;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.discovery.TypeCheckResult;
import io.github.jvmspec.discovery.TypeExistenceChecker;
import io.github.jvmspec.generation.SpecFileGenerator;
import io.github.jvmspec.generation.SpecGenerationPlan;
import io.github.jvmspec.generation.SpecSkeletonGenerator;
import io.github.jvmspec.generation.SpecSupportFileGenerator;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** Generates missing specifications and support for related Java types. */
final class RelatedSpecGenerationOrchestrator {
    private RelatedSpecGenerationOrchestrator() {
    }

    static Result ensure(
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
                if (GenerationDryRunReporter.reportSupport(
                        supportPlan, out, "related specification support")) {
                    pendingChanges = true;
                    activity.proposed("RELATED_SPEC_SUPPORT", supportPlan.targetFile());
                }
                activity.proposed("RELATED_SPEC", specPlan.targetFile());
                out.println("Would generate related specification: " + specPlan.targetFile().getPath());
                pendingChanges = true;
                specs.add(DiscoveredSpec.of(specPlan.targetFile(), specPlan.specQualifiedName(), relatedType));
                continue;
            }

            boolean accepted = generate || GenerationAuthorization.askToGenerateSpec(input, out, specPlan);
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
        return Result.of(allAccepted, pendingChanges);
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

    static final class Result {
        private final boolean allAccepted;
        private final boolean pendingChanges;

        private Result(boolean allAccepted, boolean pendingChanges) {
            this.allAccepted = allAccepted;
            this.pendingChanges = pendingChanges;
        }

        static Result of(boolean allAccepted, boolean pendingChanges) {
            return new Result(allAccepted, pendingChanges);
        }

        boolean allAccepted() {
            return allAccepted;
        }

        boolean hasPendingChanges() {
            return pendingChanges;
        }
    }
}
