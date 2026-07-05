package io.github.jvmspec.profile;

/**
 * Kind of Java API symbol represented as metadata.
 */
public enum ApiSymbolKind {
    TYPE("type"),
    ARRAY_TYPE("array type"),
    NESTED_TYPE("nested type"),
    METHOD("method"),
    STATIC_METHOD("static method"),
    FIELD("field"),
    LANGUAGE_FEATURE("language feature");

    private final String displayName;

    ApiSymbolKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean hasMemberName() {
        return METHOD.equals(this) || STATIC_METHOD.equals(this) || FIELD.equals(this);
    }
}
