package io.github.jvmspec.compatibility;

import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.profile.TargetProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable report produced by enforcing a target profile against one described type.
 */
public final class ProfileEnforcementReport {
    private final TargetProfile targetProfile;
    private final DescribedType describedType;
    private final List<ProfileViolation> violations;

    private ProfileEnforcementReport(
            TargetProfile targetProfile,
            DescribedType describedType,
            List<ProfileViolation> violations
    ) {
        this.targetProfile = targetProfile;
        this.describedType = describedType;
        this.violations = violations;
    }

    public static ProfileEnforcementReport of(
            TargetProfile targetProfile,
            DescribedType describedType,
            List<ProfileViolation> violations
    ) {
        TargetProfile validatedTargetProfile = Objects.requireNonNull(targetProfile, "targetProfile must not be null");
        DescribedType validatedDescribedType = Objects.requireNonNull(describedType, "describedType must not be null");
        return new ProfileEnforcementReport(
                validatedTargetProfile,
                validatedDescribedType,
                immutableViolations(validatedTargetProfile, validatedDescribedType, violations)
        );
    }

    public TargetProfile targetProfile() {
        return targetProfile;
    }

    public DescribedType describedType() {
        return describedType;
    }

    public List<ProfileViolation> violations() {
        return violations;
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    public ProfileViolation firstViolation() {
        if (violations.isEmpty()) {
            return null;
        }
        return violations.get(0);
    }

    public boolean isAllowed() {
        return violations.isEmpty();
    }

    public boolean isDenied() {
        return !isAllowed();
    }

    public String message() {
        if (isAllowed()) {
            return describedType.toString() + " is compatible with " + targetProfile.displayLabel();
        }
        return "Profile compatibility error: " + violations.get(0).message();
    }

    public List<String> summaryLines() {
        List<String> lines = new ArrayList<String>();
        if (isAllowed()) {
            lines.add(message());
            return Collections.unmodifiableList(lines);
        }
        for (int i = 0; i < violations.size(); i++) {
            lines.add(violations.get(i).summaryLine());
        }
        return Collections.unmodifiableList(lines);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProfileEnforcementReport)) {
            return false;
        }
        ProfileEnforcementReport that = (ProfileEnforcementReport) other;
        return targetProfile == that.targetProfile
                && describedType.equals(that.describedType)
                && violations.equals(that.violations);
    }

    @Override
    public int hashCode() {
        int result = targetProfile.hashCode();
        result = 31 * result + describedType.hashCode();
        result = 31 * result + violations.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return message();
    }

    private static List<ProfileViolation> immutableViolations(
            TargetProfile targetProfile,
            DescribedType describedType,
            List<ProfileViolation> violations
    ) {
        Objects.requireNonNull(violations, "violations must not be null");
        if (violations.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProfileViolation> copy = new ArrayList<ProfileViolation>(violations);
        for (int i = 0; i < copy.size(); i++) {
            ProfileViolation violation = Objects.requireNonNull(copy.get(i), "violations[" + i + "] must not be null");
            if (violation.targetProfile() != targetProfile) {
                throw new IllegalArgumentException("violations[" + i + "] targetProfile must match report targetProfile");
            }
            if (!violation.describedType().equals(describedType)) {
                throw new IllegalArgumentException("violations[" + i + "] describedType must match report describedType");
            }
        }
        return Collections.unmodifiableList(copy);
    }
}
