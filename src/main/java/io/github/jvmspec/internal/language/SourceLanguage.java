package io.github.jvmspec.internal.language;

import java.util.Objects;

/** Internal source-language identity; only Java is registered before 1.0. */
public final class SourceLanguage {
    public static final SourceLanguage JAVA = new SourceLanguage("java", ".java");

    private final String id;
    private final String sourceExtension;

    private SourceLanguage(String id, String sourceExtension) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sourceExtension = Objects.requireNonNull(
                sourceExtension, "sourceExtension must not be null");
    }

    public String id() {
        return id;
    }

    public String sourceExtension() {
        return sourceExtension;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SourceLanguage)) return false;
        SourceLanguage that = (SourceLanguage) other;
        return id.equals(that.id) && sourceExtension.equals(that.sourceExtension);
    }

    public int hashCode() {
        return 31 * id.hashCode() + sourceExtension.hashCode();
    }

    public String toString() {
        return id;
    }
}
