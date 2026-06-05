package org.javaspec.compatibility;

import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.model.MethodDescriptor;
import org.javaspec.profile.TargetProfile;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ProfileEnforcementTest {
    private final ProfileEnforcement enforcement = ProfileEnforcement.defaultEnforcement();

    @Test
    public void java8CompatibleKindsAreAllowedUnderJava8() {
        JavaTypeKind[] java8Kinds = new JavaTypeKind[] {
                JavaTypeKind.CLASS,
                JavaTypeKind.FINAL_CLASS,
                JavaTypeKind.INTERFACE,
                JavaTypeKind.ENUM,
                JavaTypeKind.ANNOTATION
        };

        for (int i = 0; i < java8Kinds.length; i++) {
            JavaTypeKind kind = java8Kinds[i];
            DescribedType describedType = DescribedType.of("com.example." + kind.name() + "Type", kind);

            ProfileEnforcementReport report = enforcement.enforce(TargetProfile.JAVA8, describedType);

            assertTrue(kind.displayName() + " should be allowed under java8", report.isAllowed());
            assertFalse(report.hasViolations());
            assertNull(report.firstViolation());
        }
    }

    @Test
    public void recordsAndSealedTypesAreDeniedBeforeJava17AndAllowedFromJava17() {
        JavaTypeKind[] postJava8Kinds = new JavaTypeKind[] {
                JavaTypeKind.RECORD,
                JavaTypeKind.SEALED_CLASS,
                JavaTypeKind.SEALED_INTERFACE
        };
        TargetProfile[] deniedProfiles = new TargetProfile[] {TargetProfile.JAVA8, TargetProfile.JAVA11};
        TargetProfile[] allowedProfiles = new TargetProfile[] {TargetProfile.JAVA17, TargetProfile.JAVA21, TargetProfile.JAVA25};

        for (int kindIndex = 0; kindIndex < postJava8Kinds.length; kindIndex++) {
            JavaTypeKind kind = postJava8Kinds[kindIndex];
            DescribedType describedType = DescribedType.of("com.example." + kind.name() + "Type", kind);

            for (int profileIndex = 0; profileIndex < deniedProfiles.length; profileIndex++) {
                ProfileEnforcementReport report = enforcement.enforce(deniedProfiles[profileIndex], describedType);

                assertTrue(kind.displayName() + " should be denied under " + deniedProfiles[profileIndex].key(), report.isDenied());
                assertEquals(TargetProfile.JAVA17, report.firstViolation().requiredProfile());
                assertEquals("type kind", report.firstViolation().location());
            }

            for (int profileIndex = 0; profileIndex < allowedProfiles.length; profileIndex++) {
                assertTrue(kind.displayName() + " should be allowed under " + allowedProfiles[profileIndex].key(),
                        enforcement.isAllowed(allowedProfiles[profileIndex], describedType));
            }
        }
    }

    @Test
    public void methodSignaturesDenyJava21ApiOwnersUnderJava17AndAllowThemUnderJava21() {
        DescribedType describedType = DescribedType.of("com.example.SequencedService", JavaTypeKind.CLASS)
                .withMethods(Arrays.asList(
                        MethodDescriptor.of("items", "java.util.SequencedCollection"),
                        MethodDescriptor.voidMethod(
                                "replaceItems",
                                Arrays.asList("java.util.SequencedCollection"),
                                Arrays.asList("items")
                        )
                ));

        ProfileEnforcementReport denied = enforcement.enforce(TargetProfile.JAVA17, describedType);

        assertTrue(denied.isDenied());
        assertEquals(2, denied.violations().size());
        assertEquals("method items return type", denied.violations().get(0).location());
        assertEquals(TargetProfile.JAVA21, denied.violations().get(0).requiredProfile());
        assertEquals("java.util.SequencedCollection", denied.violations().get(0).subject());
        assertEquals("method replaceItems parameter 1 type", denied.violations().get(1).location());
        assertEquals(TargetProfile.JAVA21, denied.violations().get(1).requiredProfile());

        assertTrue(enforcement.enforce(TargetProfile.JAVA21, describedType).isAllowed());
    }

    @Test
    public void methodSignaturesDenyJava25GathererUnderJava21AndAllowItUnderJava25() {
        DescribedType describedType = DescribedType.of("com.example.GatheringService", JavaTypeKind.CLASS)
                .withMethods(Arrays.asList(
                        MethodDescriptor.of("gatherer", "java.util.stream.Gatherer"),
                        MethodDescriptor.voidMethod(
                                "install",
                                Arrays.asList("java.util.stream.Gatherer"),
                                Arrays.asList("gatherer")
                        )
                ));

        ProfileEnforcementReport denied = enforcement.enforce(TargetProfile.JAVA21, describedType);

        assertTrue(denied.isDenied());
        assertEquals(2, denied.violations().size());
        assertEquals(TargetProfile.JAVA25, denied.violations().get(0).requiredProfile());
        assertEquals("java.util.stream.Gatherer", denied.violations().get(0).subject());
        assertEquals(TargetProfile.JAVA25, denied.violations().get(1).requiredProfile());

        assertTrue(enforcement.enforce(TargetProfile.JAVA25, describedType).isAllowed());
    }

    @Test
    public void methodSignatureChecksIgnoreUnknownProjectTypesPrimitivesAndVoid() {
        DescribedType describedType = DescribedType.of("com.example.ProjectService", JavaTypeKind.CLASS)
                .withMethods(Arrays.asList(
                        MethodDescriptor.voidMethod(
                                "acceptProjectValues",
                                Arrays.asList("int", "com.example.ProjectType", "ProjectAlias[]"),
                                Arrays.asList("amount", "projectType", "aliases")
                        ),
                        MethodDescriptor.of("projectType", "com.example.ProjectType")
                ));

        ProfileEnforcementReport report = enforcement.enforce(TargetProfile.JAVA8, describedType);

        assertTrue(report.isAllowed());
        assertTrue(report.violations().isEmpty());
    }

    @Test
    public void methodSignatureNormalizationInspectsSafeGenericsArraysVarargsAndWildcards() {
        DescribedType describedType = DescribedType.of("com.example.NormalizedService", JavaTypeKind.CLASS)
                .withMethods(Arrays.asList(
                        MethodDescriptor.of(
                                "nestedSequencedCollections",
                                "java.util.List<? extends java.util.SequencedCollection[]>"
                        ),
                        MethodDescriptor.voidMethod(
                                "acceptAll",
                                Arrays.asList(
                                        "java.util.SequencedCollection...",
                                        "java.util.List<? super java.util.stream.Gatherer>"
                                ),
                                Arrays.asList("collections", "gatherers")
                        )
                ));

        ProfileEnforcementReport denied = enforcement.enforce(TargetProfile.JAVA17, describedType);

        assertTrue(denied.isDenied());
        assertEquals(3, denied.violations().size());
        assertEquals("method nestedSequencedCollections return type", denied.violations().get(0).location());
        assertEquals("java.util.SequencedCollection", denied.violations().get(0).subject());
        assertEquals("method acceptAll parameter 1 type", denied.violations().get(1).location());
        assertEquals("java.util.SequencedCollection", denied.violations().get(1).subject());
        assertEquals("method acceptAll parameter 2 type", denied.violations().get(2).location());
        assertEquals("java.util.stream.Gatherer", denied.violations().get(2).subject());

        assertTrue(enforcement.enforce(TargetProfile.JAVA25, describedType).isAllowed());
    }

    @Test
    public void allowedReportsExposeAccessorsMessagesAndImmutableSummaries() {
        DescribedType describedType = DescribedType.of("com.example.LegacyService", JavaTypeKind.CLASS);

        ProfileEnforcementReport report = enforcement.enforce(TargetProfile.JAVA8, describedType);

        assertSame(TargetProfile.JAVA8, report.targetProfile());
        assertEquals(describedType, report.describedType());
        assertTrue(report.isAllowed());
        assertFalse(report.isDenied());
        assertFalse(report.hasViolations());
        assertNull(report.firstViolation());
        assertEquals("class com.example.LegacyService is compatible with Java 8", report.message());
        assertEquals(Arrays.asList("class com.example.LegacyService is compatible with Java 8"), report.summaryLines());
        assertEquals(report.message(), report.toString());
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                report.summaryLines().add("mutated");
            }
        });
    }

    @Test
    public void violationsAndDeniedReportsHaveValueSemanticsAndDefensiveImmutableCopies() {
        DescribedType describedType = DescribedType.of("com.example.User", JavaTypeKind.RECORD);
        CompatibilityResult deniedResult = CompatibilityResult.denied(
                TargetProfile.JAVA8,
                TargetProfile.JAVA17,
                "record",
                "record requires Java 17 but target profile is Java 8"
        );
        ProfileViolation violation = ProfileViolation.of(describedType, "type kind", deniedResult);
        List<ProfileViolation> sourceViolations = new ArrayList<ProfileViolation>();
        sourceViolations.add(violation);

        ProfileEnforcementReport report = ProfileEnforcementReport.of(TargetProfile.JAVA8, describedType, sourceViolations);
        sourceViolations.clear();
        ProfileEnforcementReport sameReport = ProfileEnforcementReport.of(
                TargetProfile.JAVA8,
                describedType,
                Arrays.asList(ProfileViolation.of(describedType, "type kind", deniedResult))
        );

        assertEquals(1, report.violations().size());
        assertEquals(violation, report.firstViolation());
        assertEquals("type kind", violation.location());
        assertEquals(describedType, violation.describedType());
        assertSame(TargetProfile.JAVA8, violation.targetProfile());
        assertTrue(violation.hasRequiredProfile());
        assertSame(TargetProfile.JAVA17, violation.requiredProfile());
        assertEquals("record", violation.subject());
        assertEquals("record requires Java 17 but target profile is Java 8", violation.message());
        assertEquals("type kind: record requires Java 17 but target profile is Java 8", violation.summaryLine());
        assertEquals(violation.summaryLine(), violation.toString());
        assertEquals("Profile compatibility error: record requires Java 17 but target profile is Java 8", report.message());
        assertEquals(Arrays.asList("type kind: record requires Java 17 but target profile is Java 8"), report.summaryLines());
        assertEquals(sameReport, report);
        assertEquals(sameReport.hashCode(), report.hashCode());
        assertEquals(report.message(), report.toString());
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                report.violations().add(violation);
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                report.summaryLines().add("mutated");
            }
        });
    }

    @Test
    public void violationRequiresDeniedCompatibilityResult() {
        final DescribedType describedType = DescribedType.of("com.example.LegacyService", JavaTypeKind.CLASS);
        final CompatibilityResult allowedResult = CompatibilityResult.allowed(
                TargetProfile.JAVA8,
                TargetProfile.JAVA8,
                "class",
                "class is allowed"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ProfileViolation.of(describedType, "type kind", allowedResult);
            }
        });
        assertTrue(exception.getMessage().contains("compatibilityResult must be denied"));
    }
}
