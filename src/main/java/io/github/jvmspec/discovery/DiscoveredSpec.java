package io.github.jvmspec.discovery;

import io.github.jvmspec.model.DescribedClass;
import io.github.jvmspec.model.DescribedType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A Java specification source file discovered under the configured spec root.
 */
public final class DiscoveredSpec {
    private static final List<SpecExample> EMPTY_EXAMPLES = Collections.unmodifiableList(new ArrayList<SpecExample>());

    private final File specFile;
    private final String specQualifiedName;
    private final DescribedType describedType;
    private final List<SpecExample> examples;

    private DiscoveredSpec(File specFile, String specQualifiedName, DescribedType describedType, List<SpecExample> examples) {
        this.specFile = specFile;
        this.specQualifiedName = specQualifiedName;
        this.describedType = describedType;
        this.examples = examples;
    }

    public static DiscoveredSpec of(File specFile, String specQualifiedName, DescribedClass describedClass) {
        return of(specFile, specQualifiedName, DescribedType.of(describedClass));
    }

    public static DiscoveredSpec of(
            File specFile,
            String specQualifiedName,
            DescribedClass describedClass,
            List<SpecExample> examples
    ) {
        return of(specFile, specQualifiedName, DescribedType.of(describedClass), examples);
    }

    public static DiscoveredSpec of(File specFile, String specQualifiedName, DescribedType describedType) {
        return of(specFile, specQualifiedName, describedType, EMPTY_EXAMPLES);
    }

    public static DiscoveredSpec of(File specFile, String specQualifiedName, DescribedType describedType, List<SpecExample> examples) {
        return new DiscoveredSpec(
                Objects.requireNonNull(specFile, "specFile must not be null"),
                Objects.requireNonNull(specQualifiedName, "specQualifiedName must not be null"),
                Objects.requireNonNull(describedType, "describedType must not be null"),
                immutableExamples(examples)
        );
    }

    public File specFile() {
        return specFile;
    }

    public String sourceFilePath() {
        return specFile.getPath();
    }

    public String sourceFile() {
        return sourceFilePath();
    }

    public String getSourceFilePath() {
        return sourceFilePath();
    }

    public String getSourceFile() {
        return sourceFilePath();
    }

    public boolean hasSourceFile() {
        return sourceFilePath().length() > 0;
    }

    public String specQualifiedName() {
        return specQualifiedName;
    }

    public String id() {
        return specQualifiedName;
    }

    public String stableId() {
        return specQualifiedName;
    }

    public String getId() {
        return specQualifiedName;
    }

    public String getStableId() {
        return specQualifiedName;
    }

    public DescribedClass describedClass() {
        return describedType.describedClass();
    }

    public DescribedType describedType() {
        return describedType;
    }

    public List<SpecExample> examples() {
        return examples;
    }

    public List<SpecExample> exampleMetadata() {
        return examples;
    }

    public boolean hasExamples() {
        return !examples.isEmpty();
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
                ", examples=" + examples +
                '}';
    }

    private static List<SpecExample> immutableExamples(List<SpecExample> examples) {
        Objects.requireNonNull(examples, "examples must not be null");
        if (examples.isEmpty()) {
            return EMPTY_EXAMPLES;
        }
        List<SpecExample> copy = new ArrayList<SpecExample>(examples);
        for (int i = 0; i < copy.size(); i++) {
            Objects.requireNonNull(copy.get(i), "examples[" + i + "] must not be null");
        }
        return Collections.unmodifiableList(copy);
    }
}
