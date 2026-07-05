package io.github.jvmspec.discovery;

import io.github.jvmspec.model.DescribedClass;

import java.io.File;
import java.util.Objects;

/**
 * Immutable result of checking whether a described class already exists.
 */
public final class ClassCheckResult {
    private final DescribedClass describedClass;
    private final File sourceRoot;
    private final File sourceFile;
    private final boolean classpathPresent;
    private final boolean sourceFilePresent;

    private ClassCheckResult(
            DescribedClass describedClass,
            File sourceRoot,
            File sourceFile,
            boolean classpathPresent,
            boolean sourceFilePresent
    ) {
        this.describedClass = describedClass;
        this.sourceRoot = sourceRoot;
        this.sourceFile = sourceFile;
        this.classpathPresent = classpathPresent;
        this.sourceFilePresent = sourceFilePresent;
    }

    public static ClassCheckResult of(
            DescribedClass describedClass,
            File sourceRoot,
            File sourceFile,
            boolean classpathPresent,
            boolean sourceFilePresent
    ) {
        return new ClassCheckResult(
                Objects.requireNonNull(describedClass, "describedClass must not be null"),
                Objects.requireNonNull(sourceRoot, "sourceRoot must not be null"),
                Objects.requireNonNull(sourceFile, "sourceFile must not be null"),
                classpathPresent,
                sourceFilePresent
        );
    }

    public DescribedClass describedClass() {
        return describedClass;
    }

    public File sourceRoot() {
        return sourceRoot;
    }

    public File sourceFile() {
        return sourceFile;
    }

    public boolean classpathPresent() {
        return classpathPresent;
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
        if (!(other instanceof ClassCheckResult)) {
            return false;
        }
        ClassCheckResult that = (ClassCheckResult) other;
        return classpathPresent == that.classpathPresent
                && sourceFilePresent == that.sourceFilePresent
                && describedClass.equals(that.describedClass)
                && sourceRoot.equals(that.sourceRoot)
                && sourceFile.equals(that.sourceFile);
    }

    @Override
    public int hashCode() {
        int result = describedClass.hashCode();
        result = 31 * result + sourceRoot.hashCode();
        result = 31 * result + sourceFile.hashCode();
        result = 31 * result + (classpathPresent ? 1 : 0);
        result = 31 * result + (sourceFilePresent ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ClassCheckResult{" +
                "describedClass=" + describedClass +
                ", sourceRoot=" + sourceRoot +
                ", sourceFile=" + sourceFile +
                ", classpathPresent=" + classpathPresent +
                ", sourceFilePresent=" + sourceFilePresent +
                '}';
    }
}
