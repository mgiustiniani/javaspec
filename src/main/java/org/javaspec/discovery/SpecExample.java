package org.javaspec.discovery;

import java.util.Objects;

/**
 * Immutable metadata for one executable example method in a specification.
 */
public final class SpecExample {
    public static final int UNKNOWN_SOURCE_LINE = 0;

    private final String methodName;
    private final String displayName;
    private final int sourceOrderIndex;
    private final int sourceLine;

    private SpecExample(String methodName, String displayName, int sourceOrderIndex, int sourceLine) {
        this.methodName = methodName;
        this.displayName = displayName;
        this.sourceOrderIndex = sourceOrderIndex;
        this.sourceLine = sourceLine;
    }

    public static SpecExample of(String methodName, int sourceOrderIndex) {
        return of(methodName, displayNameFor(methodName), sourceOrderIndex, UNKNOWN_SOURCE_LINE);
    }

    public static SpecExample of(String methodName, int sourceOrderIndex, int sourceLine) {
        return of(methodName, displayNameFor(methodName), sourceOrderIndex, sourceLine);
    }

    public static SpecExample of(String methodName, String displayName, int sourceOrderIndex) {
        return of(methodName, displayName, sourceOrderIndex, UNKNOWN_SOURCE_LINE);
    }

    public static SpecExample of(String methodName, String displayName, int sourceOrderIndex, int sourceLine) {
        String validatedMethodName = validateMethodName(methodName);
        String validatedDisplayName = validateDisplayName(displayName);
        if (sourceOrderIndex < 0) {
            throw new IllegalArgumentException("sourceOrderIndex must not be negative: " + sourceOrderIndex);
        }
        if (sourceLine < UNKNOWN_SOURCE_LINE) {
            throw new IllegalArgumentException("sourceLine must not be negative: " + sourceLine);
        }
        return new SpecExample(validatedMethodName, validatedDisplayName, sourceOrderIndex, sourceLine);
    }

    public static boolean isExampleMethodName(String methodName) {
        return methodName != null
                && isJavaIdentifier(methodName)
                && (methodName.startsWith("it_") || methodName.startsWith("its_"))
                && !"let".equals(methodName)
                && !"letGo".equals(methodName);
    }

    public static String displayNameFor(String methodName) {
        return validateMethodName(methodName).replace('_', ' ');
    }

    public String methodName() {
        return methodName;
    }

    public String name() {
        return methodName;
    }

    public String displayName() {
        return displayName;
    }

    public int sourceOrderIndex() {
        return sourceOrderIndex;
    }

    public int orderIndex() {
        return sourceOrderIndex;
    }

    public int sourceOrder() {
        return sourceOrderIndex;
    }

    public int sourceLine() {
        return sourceLine;
    }

    public int sourceLineNumber() {
        return sourceLine;
    }

    public int lineNumber() {
        return sourceLine;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public int getLineNumber() {
        return sourceLine;
    }

    public boolean hasSourceLine() {
        return sourceLine > UNKNOWN_SOURCE_LINE;
    }

    public String stableId(String specQualifiedName) {
        return validateSpecQualifiedName(specQualifiedName) + "#" + methodName;
    }

    public String fullName(String specQualifiedName) {
        return stableId(specQualifiedName);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SpecExample)) {
            return false;
        }
        SpecExample that = (SpecExample) other;
        return sourceOrderIndex == that.sourceOrderIndex
                && methodName.equals(that.methodName)
                && displayName.equals(that.displayName);
    }

    @Override
    public int hashCode() {
        int result = methodName.hashCode();
        result = 31 * result + displayName.hashCode();
        result = 31 * result + sourceOrderIndex;
        return result;
    }

    @Override
    public String toString() {
        return "SpecExample{" +
                "methodName='" + methodName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", sourceOrderIndex=" + sourceOrderIndex +
                ", sourceLine=" + sourceLine +
                '}';
    }

    private static String validateSpecQualifiedName(String specQualifiedName) {
        Objects.requireNonNull(specQualifiedName, "specQualifiedName must not be null");
        if (specQualifiedName.trim().length() == 0) {
            throw new IllegalArgumentException("specQualifiedName must not be blank");
        }
        return specQualifiedName;
    }

    private static String validateMethodName(String methodName) {
        Objects.requireNonNull(methodName, "methodName must not be null");
        if (methodName.length() == 0) {
            throw new IllegalArgumentException("methodName must not be empty");
        }
        if (methodName.trim().length() == 0) {
            throw new IllegalArgumentException("methodName must not be blank");
        }
        if (!methodName.equals(methodName.trim())) {
            throw new IllegalArgumentException("methodName must not contain leading or trailing whitespace: " + methodName);
        }
        if (!isExampleMethodName(methodName)) {
            throw new IllegalArgumentException("methodName must start with it_ or its_ and be a Java identifier: " + methodName);
        }
        return methodName;
    }

    private static String validateDisplayName(String displayName) {
        Objects.requireNonNull(displayName, "displayName must not be null");
        if (displayName.length() == 0) {
            throw new IllegalArgumentException("displayName must not be empty");
        }
        if (displayName.trim().length() == 0) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        return displayName;
    }

    private static boolean isJavaIdentifier(String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        int index = 0;
        int firstCodePoint = value.codePointAt(index);
        if (!Character.isJavaIdentifierStart(firstCodePoint)) {
            return false;
        }
        index += Character.charCount(firstCodePoint);
        while (index < value.length()) {
            int currentCodePoint = value.codePointAt(index);
            if (!Character.isJavaIdentifierPart(currentCodePoint)) {
                return false;
            }
            index += Character.charCount(currentCodePoint);
        }
        return true;
    }
}
