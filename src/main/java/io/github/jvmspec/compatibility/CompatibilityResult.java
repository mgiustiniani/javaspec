package io.github.jvmspec.compatibility;

import io.github.jvmspec.profile.TargetProfile;

import java.util.Objects;

/**
 * Immutable result of a target-profile compatibility check.
 */
public final class CompatibilityResult {
    private final boolean allowed;
    private final TargetProfile targetProfile;
    private final TargetProfile requiredProfile;
    private final String subject;
    private final String message;

    private CompatibilityResult(
            boolean allowed,
            TargetProfile targetProfile,
            TargetProfile requiredProfile,
            String subject,
            String message
    ) {
        this.allowed = allowed;
        this.targetProfile = targetProfile;
        this.requiredProfile = requiredProfile;
        this.subject = subject;
        this.message = message;
    }

    public static CompatibilityResult allowed(TargetProfile targetProfile, TargetProfile requiredProfile, String subject, String message) {
        return new CompatibilityResult(
                true,
                Objects.requireNonNull(targetProfile, "targetProfile must not be null"),
                requiredProfile,
                validateRequired(subject, "subject"),
                Objects.requireNonNull(message, "message must not be null")
        );
    }

    public static CompatibilityResult denied(TargetProfile targetProfile, TargetProfile requiredProfile, String subject, String message) {
        return new CompatibilityResult(
                false,
                Objects.requireNonNull(targetProfile, "targetProfile must not be null"),
                requiredProfile,
                validateRequired(subject, "subject"),
                Objects.requireNonNull(message, "message must not be null")
        );
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isDenied() {
        return !allowed;
    }

    public TargetProfile targetProfile() {
        return targetProfile;
    }

    public boolean hasRequiredProfile() {
        return requiredProfile != null;
    }

    public TargetProfile requiredProfile() {
        return requiredProfile;
    }

    public String subject() {
        return subject;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CompatibilityResult)) {
            return false;
        }
        CompatibilityResult that = (CompatibilityResult) other;
        return allowed == that.allowed
                && targetProfile == that.targetProfile
                && requiredProfile == that.requiredProfile
                && subject.equals(that.subject)
                && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        int result = allowed ? 1 : 0;
        result = 31 * result + targetProfile.hashCode();
        result = 31 * result + (requiredProfile == null ? 0 : requiredProfile.hashCode());
        result = 31 * result + subject.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return message;
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
