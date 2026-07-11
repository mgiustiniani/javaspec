package io.github.jvmspec.compatibility;

import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.profile.TargetProfile;

import java.util.Objects;

/**
 * Immutable profile enforcement violation for one described type location.
 */
public final class ProfileViolation {
    private final DescribedType describedType;
    private final String location;
    private final CompatibilityResult compatibilityResult;

    private ProfileViolation(
            DescribedType describedType,
            String location,
            CompatibilityResult compatibilityResult
    ) {
        this.describedType = describedType;
        this.location = location;
        this.compatibilityResult = compatibilityResult;
    }

    public static ProfileViolation of(
            DescribedType describedType,
            String location,
            CompatibilityResult compatibilityResult
    ) {
        Objects.requireNonNull(compatibilityResult, "compatibilityResult must not be null");
        if (!compatibilityResult.isDenied()) {
            throw new IllegalArgumentException("compatibilityResult must be denied");
        }
        return new ProfileViolation(
                Objects.requireNonNull(describedType, "describedType must not be null"),
                validateRequired(location, "location"),
                compatibilityResult
        );
    }

    public DescribedType describedType() {
        return describedType;
    }

    public String location() {
        return location;
    }

    public CompatibilityResult compatibilityResult() {
        return compatibilityResult;
    }

    public TargetProfile targetProfile() {
        return compatibilityResult.targetProfile();
    }

    public boolean hasRequiredProfile() {
        return compatibilityResult.hasRequiredProfile();
    }

    public TargetProfile requiredProfile() {
        return compatibilityResult.requiredProfile();
    }

    public String subject() {
        return compatibilityResult.subject();
    }

    public String message() {
        return compatibilityResult.message();
    }

    public String summaryLine() {
        return location + ": " + message();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProfileViolation)) {
            return false;
        }
        ProfileViolation that = (ProfileViolation) other;
        return describedType.equals(that.describedType)
                && location.equals(that.location)
                && compatibilityResult.equals(that.compatibilityResult);
    }

    @Override
    public int hashCode() {
        int result = describedType.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + compatibilityResult.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return summaryLine();
    }

    private static String validateRequired(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        if (value.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not contain leading or trailing whitespace: " + value);
        }
        return value;
    }
}
