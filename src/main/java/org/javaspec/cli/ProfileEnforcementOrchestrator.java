package org.javaspec.cli;

import org.javaspec.compatibility.ProfileEnforcement;
import org.javaspec.compatibility.ProfileEnforcementReport;
import org.javaspec.compatibility.ProfileViolation;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.profile.TargetProfile;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates target-profile compatibility enforcement for discovered specs.
 */
final class ProfileEnforcementOrchestrator {

    /**
     * Checks that all discovered specs and their related types are compatible
     * with the given target profile.
     *
     * @param targetProfile the target Java LTS profile
     * @param specs         the discovered specs to check
     * @param err           the error stream for diagnostics
     * @return {@code EXIT_OK} if all compatible, or {@code EXIT_USAGE} with diagnostics
     */
    int enforceProfileCompatibility(
            TargetProfile targetProfile,
            List<DiscoveredSpec> specs,
            PrintStream err
    ) {
        ProfileEnforcement enforcement = ProfileEnforcement.defaultEnforcement();
        List<ProfileEnforcementFinding> deniedFindings = new ArrayList<ProfileEnforcementFinding>();
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            DescribedType describedType = spec.describedType();
            addDeniedProfileFinding(
                    deniedFindings,
                    enforcement,
                    targetProfile,
                    spec.specQualifiedName() + " -> " + promptTarget(describedType),
                    describedType
            );
            List<DescribedType> relatedTypes = relatedTypesOf(describedType);
            for (int ri = 0; ri < relatedTypes.size(); ri++) {
                DescribedType relatedType = relatedTypes.get(ri);
                addDeniedProfileFinding(
                        deniedFindings,
                        enforcement,
                        targetProfile,
                        "related to " + spec.specQualifiedName() + " -> " + promptTarget(relatedType),
                        relatedType
                );
            }
        }
        if (deniedFindings.isEmpty()) {
            return Main.EXIT_OK;
        }
        printProfileCompatibilityError(deniedFindings, err);
        return Main.EXIT_USAGE;
    }

    private static void addDeniedProfileFinding(
            List<ProfileEnforcementFinding> deniedFindings,
            ProfileEnforcement enforcement,
            TargetProfile targetProfile,
            String sourceDescription,
            DescribedType describedType
    ) {
        ProfileEnforcementReport report = enforcement.enforce(targetProfile, describedType);
        if (report.isDenied()) {
            deniedFindings.add(ProfileEnforcementFinding.of(sourceDescription, report));
        }
    }

    private static void printProfileCompatibilityError(
            List<ProfileEnforcementFinding> deniedFindings,
            PrintStream err
    ) {
        ProfileEnforcementFinding firstFinding = deniedFindings.get(0);
        ProfileEnforcementReport firstReport = firstFinding.report();
        ProfileViolation firstViolation = firstReport.violations().get(0);
        err.println("Profile compatibility error: " + firstViolation.message());
        err.println("Selected profile: " + firstReport.targetProfile().key()
                + " (" + firstReport.targetProfile().displayLabel() + ")");
        err.println("Spec/type: " + firstFinding.sourceDescription());
        if (deniedFindings.size() == 1 && firstReport.violations().size() == 1) {
            return;
        }
        err.println("Violations:");
        for (int i = 0; i < deniedFindings.size(); i++) {
            ProfileEnforcementFinding finding = deniedFindings.get(i);
            List<ProfileViolation> violations = finding.report().violations();
            for (int vi = 0; vi < violations.size(); vi++) {
                err.println("  - " + finding.sourceDescription() + ": " + violations.get(vi).summaryLine());
            }
        }
    }

    private static String promptTarget(DescribedType describedType) {
        if (JavaTypeKind.CLASS.equals(describedType.kind())) {
            return describedType.qualifiedName();
        }
        return describedType.kind().displayName() + " " + describedType.qualifiedName();
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

    private static final class ProfileEnforcementFinding {
        private final String sourceDescription;
        private final ProfileEnforcementReport report;

        private ProfileEnforcementFinding(String sourceDescription, ProfileEnforcementReport report) {
            this.sourceDescription = sourceDescription;
            this.report = report;
        }

        static ProfileEnforcementFinding of(String sourceDescription, ProfileEnforcementReport report) {
            return new ProfileEnforcementFinding(sourceDescription, report);
        }

        String sourceDescription() {
            return sourceDescription;
        }

        ProfileEnforcementReport report() {
            return report;
        }
    }
}
