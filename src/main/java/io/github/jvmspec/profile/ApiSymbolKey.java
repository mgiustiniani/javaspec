package io.github.jvmspec.profile;

import java.util.Objects;

/**
 * Immutable lookup key for an API owner and optional member name.
 */
public final class ApiSymbolKey {
    private final String ownerQualifiedName;
    private final String memberName;

    private ApiSymbolKey(String ownerQualifiedName, String memberName) {
        this.ownerQualifiedName = ownerQualifiedName;
        this.memberName = memberName;
    }

    public static ApiSymbolKey of(String ownerQualifiedName) {
        return of(ownerQualifiedName, null);
    }

    public static ApiSymbolKey of(String ownerQualifiedName, String memberName) {
        return new ApiSymbolKey(
                validateRequired(ownerQualifiedName, "ownerQualifiedName"),
                validateOptional(memberName, "memberName")
        );
    }

    public String ownerQualifiedName() {
        return ownerQualifiedName;
    }

    public boolean hasMemberName() {
        return memberName != null;
    }

    public String memberName() {
        return memberName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ApiSymbolKey)) {
            return false;
        }
        ApiSymbolKey that = (ApiSymbolKey) other;
        return ownerQualifiedName.equals(that.ownerQualifiedName)
                && Objects.equals(memberName, that.memberName);
    }

    @Override
    public int hashCode() {
        int result = ownerQualifiedName.hashCode();
        result = 31 * result + (memberName == null ? 0 : memberName.hashCode());
        return result;
    }

    @Override
    public String toString() {
        if (memberName == null) {
            return ownerQualifiedName;
        }
        return ownerQualifiedName + "#" + memberName;
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

    private static String validateOptional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        return validateRequired(value, fieldName);
    }
}
