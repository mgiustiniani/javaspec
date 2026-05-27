package org.javaspec.discovery;

import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;

import java.io.File;
import java.util.Objects;

/**
 * A Java specification source file discovered under the configured spec root.
 */
public final class DiscoveredSpec {
    private final File specFile;
    private final String specQualifiedName;
    private final DescribedType describedType;

    private DiscoveredSpec(File specFile, String specQualifiedName, DescribedType describedType) {
        this.specFile = specFile;
        this.specQualifiedName = specQualifiedName;
        this.describedType = describedType;
    }

    public static DiscoveredSpec of(File specFile, String specQualifiedName, DescribedClass describedClass) {
        return of(specFile, specQualifiedName, DescribedType.of(describedClass));
    }

    public static DiscoveredSpec of(File specFile, String specQualifiedName, DescribedType describedType) {
        return new DiscoveredSpec(
                Objects.requireNonNull(specFile, "specFile must not be null"),
                Objects.requireNonNull(specQualifiedName, "specQualifiedName must not be null"),
                Objects.requireNonNull(describedType, "describedType must not be null")
        );
    }

    public File specFile() {
        return specFile;
    }

    public String specQualifiedName() {
        return specQualifiedName;
    }

    public DescribedClass describedClass() {
        return describedType.describedClass();
    }

    public DescribedType describedType() {
        return describedType;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DiscoveredSpec)) {
            return false;
        }
        DiscoveredSpec that = (DiscoveredSpec) other;
        return specFile.equals(that.specFile)
                && specQualifiedName.equals(that.specQualifiedName)
                && describedType.equals(that.describedType);
    }

    @Override
    public int hashCode() {
        int result = specFile.hashCode();
        result = 31 * result + specQualifiedName.hashCode();
        result = 31 * result + describedType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DiscoveredSpec{" +
                "specFile=" + specFile +
                ", specQualifiedName='" + specQualifiedName + '\'' +
                ", describedType=" + describedType +
                '}';
    }
}
