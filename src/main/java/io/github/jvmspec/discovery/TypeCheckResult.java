package io.github.jvmspec.discovery;

import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;

import java.io.File;
import java.util.Objects;

/**
 * Immutable result of checking whether a described Java type already exists.
 */
public final class TypeCheckResult {
    private final DescribedType describedType;
    private final File sourceRoot;
    private final File sourceFile;
    private final JavaTypeKind classpathKind;
    private final boolean classpathPresent;
    private final boolean sourceFilePresent;

    private TypeCheckResult(
            DescribedType describedType,
            File sourceRoot,
            File sourceFile,
            JavaTypeKind classpathKind,
            boolean classpathPresent,
            boolean sourceFilePresent
    ) {
        this.describedType = describedType;
        this.sourceRoot = sourceRoot;
        this.sourceFile = sourceFile;
        this.classpathKind = classpathKind;
        this.classpathPresent = classpathPresent;
        this.sourceFilePresent = sourceFilePresent;
    }

    public static TypeCheckResult of(
            DescribedType describedType,
            File sourceRoot,
            File sourceFile,
            JavaTypeKind classpathKind,
            boolean classpathPresent,
            boolean sourceFilePresent
    ) {
        return new TypeCheckResult(
                Objects.requireNonNull(describedType, "describedType must not be null"),
                Objects.requireNonNull(sourceRoot, "sourceRoot must not be null"),
                Objects.requireNonNull(sourceFile, "sourceFile must not be null"),
                classpathKind,
                classpathPresent,
                sourceFilePresent
        );
    }

    public DescribedType describedType() {
        return describedType;
    }

    public File sourceRoot() {
        return sourceRoot;
    }

    public File sourceFile() {
        return sourceFile;
    }

    public JavaTypeKind classpathKind() {
        return classpathKind;
    }

    public boolean classpathPresent() {
        return classpathPresent;
    }

    public boolean classpathKindMatches() {
        return !classpathPresent || describedType.kind().equals(classpathKind);
    }

    public boolean sourceFilePresent() {
        return sourceFilePresent;
    }

    public boolean isPresent() {
        return classpathPresent || sourceFilePresent;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TypeCheckResult)) {
            return false;
        }
        TypeCheckResult that = (TypeCheckResult) other;
        return classpathPresent == that.classpathPresent
                && sourceFilePresent == that.sourceFilePresent
                && describedType.equals(that.describedType)
                && sourceRoot.equals(that.sourceRoot)
                && sourceFile.equals(that.sourceFile)
                && nullSafeEquals(classpathKind, that.classpathKind);
    }

    @Override
    public int hashCode() {
        int result = describedType.hashCode();
        result = 31 * result + sourceRoot.hashCode();
        result = 31 * result + sourceFile.hashCode();
        result = 31 * result + (classpathKind == null ? 0 : classpathKind.hashCode());
        result = 31 * result + (classpathPresent ? 1 : 0);
        result = 31 * result + (sourceFilePresent ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TypeCheckResult{" +
                "describedType=" + describedType +
                ", sourceRoot=" + sourceRoot +
                ", sourceFile=" + sourceFile +
                ", classpathKind=" + classpathKind +
                ", classpathPresent=" + classpathPresent +
                ", sourceFilePresent=" + sourceFilePresent +
                '}';
    }

    private static boolean nullSafeEquals(Object left, Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }
}
