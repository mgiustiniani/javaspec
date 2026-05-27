package org.javaspec.model;

/**
 * Java 8 class-like type kinds supported by the first javaspec generator slice.
 */
public enum JavaTypeKind {
    CLASS("class", "class", 8),
    FINAL_CLASS("final class", "final class", 8),
    INTERFACE("interface", "interface", 8),
    ENUM("enum", "enum", 8),
    ANNOTATION("annotation", "@interface", 8),
    RECORD("record", "record", 16),
    SEALED_CLASS("sealed class", "sealed class", 17),
    SEALED_INTERFACE("sealed interface", "sealed interface", 17);

    private final String displayName;
    private final String sourceKeyword;
    private final int minimumJavaVersion;

    JavaTypeKind(String displayName, String sourceKeyword, int minimumJavaVersion) {
        this.displayName = displayName;
        this.sourceKeyword = sourceKeyword;
        this.minimumJavaVersion = minimumJavaVersion;
    }

    public String displayName() {
        return displayName;
    }

    public String sourceKeyword() {
        return sourceKeyword;
    }

    public int minimumJavaVersion() {
        return minimumJavaVersion;
    }

    public boolean isJava8Compatible() {
        return minimumJavaVersion <= 8;
    }
}
