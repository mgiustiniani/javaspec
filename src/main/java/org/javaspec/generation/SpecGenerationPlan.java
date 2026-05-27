package org.javaspec.generation;

import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;

import java.io.File;
import java.util.Objects;

/**
 * Immutable plan for writing a generated Java specification skeleton.
 */
public final class SpecGenerationPlan {
    private final DescribedType describedType;
    private final String specQualifiedName;
    private final String specSimpleName;
    private final File specRoot;
    private final File targetFile;
    private final String sourceContent;

    private SpecGenerationPlan(
            DescribedType describedType,
            String specQualifiedName,
            String specSimpleName,
            File specRoot,
            File targetFile,
            String sourceContent
    ) {
        this.describedType = describedType;
        this.specQualifiedName = specQualifiedName;
        this.specSimpleName = specSimpleName;
        this.specRoot = specRoot;
        this.targetFile = targetFile;
        this.sourceContent = sourceContent;
    }

    public static SpecGenerationPlan of(
            DescribedClass describedClass,
            String specQualifiedName,
            String specSimpleName,
            File specRoot,
            File targetFile,
            String sourceContent
    ) {
        return of(DescribedType.of(describedClass), specQualifiedName, specSimpleName, specRoot, targetFile, sourceContent);
    }

    public static SpecGenerationPlan of(
            DescribedType describedType,
            String specQualifiedName,
            String specSimpleName,
            File specRoot,
            File targetFile,
            String sourceContent
    ) {
        return new SpecGenerationPlan(
                Objects.requireNonNull(describedType, "describedType must not be null"),
                Objects.requireNonNull(specQualifiedName, "specQualifiedName must not be null"),
                Objects.requireNonNull(specSimpleName, "specSimpleName must not be null"),
                Objects.requireNonNull(specRoot, "specRoot must not be null"),
                Objects.requireNonNull(targetFile, "targetFile must not be null"),
                Objects.requireNonNull(sourceContent, "sourceContent must not be null")
        );
    }

    public DescribedClass describedClass() {
        return describedType.describedClass();
    }

    public DescribedType describedType() {
        return describedType;
    }

    public String specQualifiedName() {
        return specQualifiedName;
    }

    public String specSimpleName() {
        return specSimpleName;
    }

    public File specRoot() {
        return specRoot;
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
        if (!(other instanceof SpecGenerationPlan)) {
            return false;
        }
        SpecGenerationPlan that = (SpecGenerationPlan) other;
        return describedType.equals(that.describedType)
                && specQualifiedName.equals(that.specQualifiedName)
                && specSimpleName.equals(that.specSimpleName)
                && specRoot.equals(that.specRoot)
                && targetFile.equals(that.targetFile)
                && sourceContent.equals(that.sourceContent);
    }

    @Override
    public int hashCode() {
        int result = describedType.hashCode();
        result = 31 * result + specQualifiedName.hashCode();
        result = 31 * result + specSimpleName.hashCode();
        result = 31 * result + specRoot.hashCode();
        result = 31 * result + targetFile.hashCode();
        result = 31 * result + sourceContent.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SpecGenerationPlan{" +
                "describedType=" + describedType +
                ", specQualifiedName='" + specQualifiedName + '\'' +
                ", specRoot=" + specRoot +
                ", targetFile=" + targetFile +
                '}';
    }
}
