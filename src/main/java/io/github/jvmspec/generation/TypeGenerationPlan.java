package io.github.jvmspec.generation;

import io.github.jvmspec.model.DescribedType;

import java.io.File;
import java.util.Objects;

/**
 * Immutable plan for writing a generated Java class-like type skeleton.
 */
public final class TypeGenerationPlan {
    private final DescribedType describedType;
    private final File sourceRoot;
    private final File targetFile;
    private final String sourceContent;

    private TypeGenerationPlan(
            DescribedType describedType,
            File sourceRoot,
            File targetFile,
            String sourceContent
    ) {
        this.describedType = describedType;
        this.sourceRoot = sourceRoot;
        this.targetFile = targetFile;
        this.sourceContent = sourceContent;
    }

    public static TypeGenerationPlan of(
            DescribedType describedType,
            File sourceRoot,
            File targetFile,
            String sourceContent
    ) {
        return new TypeGenerationPlan(
                Objects.requireNonNull(describedType, "describedType must not be null"),
                Objects.requireNonNull(sourceRoot, "sourceRoot must not be null"),
                Objects.requireNonNull(targetFile, "targetFile must not be null"),
                Objects.requireNonNull(sourceContent, "sourceContent must not be null")
        );
    }

    public DescribedType describedType() {
        return describedType;
    }

    public File sourceRoot() {
        return sourceRoot;
    }

    public File targetFile() {
        return targetFile;
    }

    public String sourceContent() {
        return sourceContent;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TypeGenerationPlan)) {
            return false;
        }
        TypeGenerationPlan that = (TypeGenerationPlan) other;
        return describedType.equals(that.describedType)
                && sourceRoot.equals(that.sourceRoot)
                && targetFile.equals(that.targetFile)
                && sourceContent.equals(that.sourceContent);
    }

    @Override
    public int hashCode() {
        int result = describedType.hashCode();
        result = 31 * result + sourceRoot.hashCode();
        result = 31 * result + targetFile.hashCode();
        result = 31 * result + sourceContent.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TypeGenerationPlan{" +
                "describedType=" + describedType +
                ", sourceRoot=" + sourceRoot +
                ", targetFile=" + targetFile +
                '}';
    }
}
