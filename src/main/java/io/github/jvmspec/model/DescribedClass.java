package io.github.jvmspec.model;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable description of a Java class by its source-level qualified name.
 */
public final class DescribedClass {
    private static final Set<String> RESERVED_WORDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null"
    )));

    private final String qualifiedName;
    private final String packageName;
    private final String simpleName;

    private DescribedClass(String qualifiedName, String packageName, String simpleName) {
        this.qualifiedName = qualifiedName;
        this.packageName = packageName;
        this.simpleName = simpleName;
    }

    public static DescribedClass of(String className) {
        String validatedName = validateQualifiedName(className);
        int lastDot = validatedName.lastIndexOf('.');
        if (lastDot < 0) {
            return new DescribedClass(validatedName, "", validatedName);
        }
        return new DescribedClass(
                validatedName,
                validatedName.substring(0, lastDot),
                validatedName.substring(lastDot + 1)
        );
    }

    public static DescribedClass from(String className) {
        return of(className);
    }

    public static DescribedClass fromName(String className) {
        return of(className);
    }

    public static DescribedClass named(String className) {
        return of(className);
    }

    public String qualifiedName() {
        return qualifiedName;
    }

    public String packageName() {
        return packageName;
    }

    public String simpleName() {
        return simpleName;
    }

    public boolean hasPackage() {
        return !packageName.isEmpty();
    }

    public String sourceRelativePath() {
        if (!hasPackage()) {
            return simpleName + ".java";
        }
        return packageName.replace('.', File.separatorChar) + File.separator + simpleName + ".java";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DescribedClass)) {
            return false;
        }
        DescribedClass that = (DescribedClass) other;
        return qualifiedName.equals(that.qualifiedName);
    }

    @Override
    public int hashCode() {
        return qualifiedName.hashCode();
    }

    @Override
    public String toString() {
        return qualifiedName;
    }

    private static String validateQualifiedName(String className) {
        Objects.requireNonNull(className, "className must not be null");
        if (className.length() == 0) {
            throw new IllegalArgumentException("Class name must not be empty");
        }
        if (className.trim().length() == 0) {
            throw new IllegalArgumentException("Class name must not be blank");
        }
        if (!className.equals(className.trim())) {
            throw new IllegalArgumentException("Class name must not contain leading or trailing whitespace: " + className);
        }
        if (className.charAt(className.length() - 1) == '.') {
            throw new IllegalArgumentException("Class name contains an empty segment: " + className);
        }

        int segmentStart = 0;
        while (segmentStart < className.length()) {
            int dot = className.indexOf('.', segmentStart);
            int segmentEnd = dot < 0 ? className.length() : dot;
            validateSegment(className, segmentStart, segmentEnd);
            if (dot < 0) {
                break;
            }
            segmentStart = dot + 1;
        }
        return className;
    }

    private static void validateSegment(String className, int start, int end) {
        if (start == end) {
            throw new IllegalArgumentException("Class name contains an empty segment: " + className);
        }

        String segment = className.substring(start, end);
        if (RESERVED_WORDS.contains(segment)) {
            throw new IllegalArgumentException("Class name segment is a reserved Java word: " + segment);
        }

        int index = start;
        int firstCodePoint = className.codePointAt(index);
        if (!Character.isJavaIdentifierStart(firstCodePoint)) {
            throw new IllegalArgumentException("Class name segment starts with an invalid Java identifier character: " + segment);
        }
        index += Character.charCount(firstCodePoint);
        while (index < end) {
            int currentCodePoint = className.codePointAt(index);
            if (!Character.isJavaIdentifierPart(currentCodePoint)) {
                throw new IllegalArgumentException("Class name segment contains an invalid Java identifier character: " + segment);
            }
            index += Character.charCount(currentCodePoint);
        }
    }
}
