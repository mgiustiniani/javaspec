package io.github.jvmspec.profile;

import java.util.Objects;

/**
 * Immutable metadata descriptor for a public Java API symbol.
 */
public final class ApiSymbol {
    private final String ownerQualifiedName;
    private final String memberName;
    private final ApiSymbolKind kind;
    private final ApiSymbolCategory category;
    private final TargetProfile introducedProfile;
    private final String notes;

    private ApiSymbol(
            String ownerQualifiedName,
            String memberName,
            ApiSymbolKind kind,
            ApiSymbolCategory category,
            TargetProfile introducedProfile,
            String notes
    ) {
        this.ownerQualifiedName = ownerQualifiedName;
        this.memberName = memberName;
        this.kind = kind;
        this.category = category;
        this.introducedProfile = introducedProfile;
        this.notes = notes;
    }

    public static ApiSymbol type(
            String ownerQualifiedName,
            ApiSymbolCategory category,
            TargetProfile introducedProfile,
            String notes
    ) {
        return of(ownerQualifiedName, null, ApiSymbolKind.TYPE, category, introducedProfile, notes);
    }

    public static ApiSymbol nestedType(
            String ownerQualifiedName,
            ApiSymbolCategory category,
            TargetProfile introducedProfile,
            String notes
    ) {
        return of(ownerQualifiedName, null, ApiSymbolKind.NESTED_TYPE, category, introducedProfile, notes);
    }

    public static ApiSymbol arrayType(
            String ownerQualifiedName,
            ApiSymbolCategory category,
            TargetProfile introducedProfile,
            String notes
    ) {
        return of(ownerQualifiedName, null, ApiSymbolKind.ARRAY_TYPE, category, introducedProfile, notes);
    }

    public static ApiSymbol method(
            String ownerQualifiedName,
            String memberName,
            ApiSymbolCategory category,
            TargetProfile introducedProfile,
            String notes
    ) {
        return of(ownerQualifiedName, memberName, ApiSymbolKind.METHOD, category, introducedProfile, notes);
    }

    public static ApiSymbol staticMethod(
            String ownerQualifiedName,
            String memberName,
            ApiSymbolCategory category,
            TargetProfile introducedProfile,
            String notes
    ) {
        return of(ownerQualifiedName, memberName, ApiSymbolKind.STATIC_METHOD, category, introducedProfile, notes);
    }

    public static ApiSymbol field(
            String ownerQualifiedName,
            String memberName,
            ApiSymbolCategory category,
            TargetProfile introducedProfile,
            String notes
    ) {
        return of(ownerQualifiedName, memberName, ApiSymbolKind.FIELD, category, introducedProfile, notes);
    }

    public static ApiSymbol languageFeature(
            String featureName,
            ApiSymbolCategory category,
            TargetProfile introducedProfile,
            String notes
    ) {
        return of(featureName, null, ApiSymbolKind.LANGUAGE_FEATURE, category, introducedProfile, notes);
    }

    public static ApiSymbol of(
            String ownerQualifiedName,
            String memberName,
            ApiSymbolKind kind,
            ApiSymbolCategory category,
            TargetProfile introducedProfile,
            String notes
    ) {
        ApiSymbolKind validatedKind = Objects.requireNonNull(kind, "kind must not be null");
        String validatedMemberName = validateOptional(memberName, "memberName");
        if (validatedKind.hasMemberName() && validatedMemberName == null) {
            throw new IllegalArgumentException("memberName must be present for symbol kind: " + validatedKind.displayName());
        }
        if (!validatedKind.hasMemberName() && validatedMemberName != null) {
            throw new IllegalArgumentException("memberName must be absent for symbol kind: " + validatedKind.displayName());
        }
        return new ApiSymbol(
                validateRequired(ownerQualifiedName, "ownerQualifiedName"),
                validatedMemberName,
                validatedKind,
                Objects.requireNonNull(category, "category must not be null"),
                Objects.requireNonNull(introducedProfile, "introducedProfile must not be null"),
                Objects.requireNonNull(notes, "notes must not be null")
        );
    }

    public ApiSymbolKey key() {
        return ApiSymbolKey.of(ownerQualifiedName, memberName);
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

    public ApiSymbolKind kind() {
        return kind;
    }

    public ApiSymbolCategory category() {
        return category;
    }

    public TargetProfile introducedProfile() {
        return introducedProfile;
    }

    public String notes() {
        return notes;
    }

    public boolean isAvailableIn(TargetProfile targetProfile) {
        Objects.requireNonNull(targetProfile, "targetProfile must not be null");
        return targetProfile.supports(introducedProfile);
    }

    public String displayName() {
        if (memberName == null) {
            return ownerQualifiedName;
        }
        return ownerQualifiedName + "." + memberName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ApiSymbol)) {
            return false;
        }
        ApiSymbol that = (ApiSymbol) other;
        return ownerQualifiedName.equals(that.ownerQualifiedName)
                && Objects.equals(memberName, that.memberName)
                && kind == that.kind
                && category == that.category
                && introducedProfile == that.introducedProfile
                && notes.equals(that.notes);
    }

    @Override
    public int hashCode() {
        int result = ownerQualifiedName.hashCode();
        result = 31 * result + (memberName == null ? 0 : memberName.hashCode());
        result = 31 * result + kind.hashCode();
        result = 31 * result + category.hashCode();
        result = 31 * result + introducedProfile.hashCode();
        result = 31 * result + notes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return displayName() + " [" + kind.displayName() + ", " + introducedProfile.key() + "]";
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
