package io.github.jvmspec.generation;

import io.github.jvmspec.model.DescribedClass;

import java.io.File;
import java.util.Objects;

/**
 * Immutable plan for writing a generated Java class skeleton.
 */
public final class ClassGenerationPlan {
    private final DescribedClass describedClass;
    private final File sourceRoot;
    private final File targetFile;
    private final String sourceContent;

    private ClassGenerationPlan(
            DescribedClass describedClass,
            File sourceRoot,
            File targetFile,
            String sourceContent
    ) {
        this.describedClass = describedClass;
        this.sourceRoot = sourceRoot;
        this.targetFile = targetFile;
        this.sourceContent = sourceContent;
    }

    public static ClassGenerationPlan of(
            DescribedClass describedClass,
            File sourceRoot,
            File targetFile,
            String sourceContent
    ) {
        return new ClassGenerationPlan(
                Objects.requireNonNull(describedClass, "describedClass must not be null"),
                Objects.requireNonNull(sourceRoot, "sourceRoot must not be null"),
                Objects.requireNonNull(targetFile, "targetFile must not be null"),
                Objects.requireNonNull(sourceContent, "sourceContent must not be null")
        );
    }

    public DescribedClass describedClass() {
        return describedClass;
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
        if (!(other instanceof ClassGenerationPlan)) {
            return false;
        }
        ClassGenerationPlan that = (ClassGenerationPlan) other;
        return describedClass.equals(that.describedClass)
                && sourceRoot.equals(that.sourceRoot)
                && targetFile.equals(that.targetFile)
                && sourceContent.equals(that.sourceContent);
    }

    @Override
    public int hashCode() {
        int result = describedClass.hashCode();
        result = 31 * result + sourceRoot.hashCode();
        result = 31 * result + targetFile.hashCode();
        result = 31 * result + sourceContent.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ClassGenerationPlan{" +
                "describedClass=" + describedClass +
                ", sourceRoot=" + sourceRoot +
                ", targetFile=" + targetFile +
                '}';
    }
}
