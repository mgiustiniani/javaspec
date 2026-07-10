package io.github.jvmspec.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable descriptor of a method's source-level signature and return type.
 */
public final class MethodDescriptor {
    private final String methodName;
    private final String returnType;
    private final List<String> parameterTypes;
    private final List<String> parameterNames;
    private final boolean staticMethod;

    private MethodDescriptor(
            String methodName,
            String returnType,
            List<String> parameterTypes,
            List<String> parameterNames,
            boolean staticMethod
    ) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterTypes = Collections.unmodifiableList(parameterTypes);
        this.parameterNames = Collections.unmodifiableList(parameterNames);
        this.staticMethod = staticMethod;
    }

    public static MethodDescriptor of(String methodName, String returnType) {
        return of(methodName, returnType, Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    public static MethodDescriptor of(
            String methodName,
            String returnType,
            List<String> parameterTypes,
            List<String> parameterNames
    ) {
        String validatedMethodName = validateMethodName(methodName);
        String validatedReturnType = validateTypeName(returnType, "returnType");
        List<String> types = new ArrayList<String>(Objects.requireNonNull(parameterTypes, "parameterTypes must not be null"));
        List<String> names = new ArrayList<String>(Objects.requireNonNull(parameterNames, "parameterNames must not be null"));
        if (types.size() != names.size()) {
            throw new IllegalArgumentException("parameterTypes size (" + types.size()
                    + ") must equal parameterNames size (" + names.size() + ")");
        }
        for (int i = 0; i < types.size(); i++) {
            types.set(i, validateTypeName(types.get(i), "parameterTypes[" + i + "]"));
            names.set(i, validateParameterName(names.get(i), "parameterNames[" + i + "]"));
        }
        return create(validatedMethodName, validatedReturnType, types, names, false);
    }

    public static MethodDescriptor staticMethod(String methodName, String returnType) {
        return staticMethod(methodName, returnType, Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    public static MethodDescriptor staticMethod(
            String methodName,
            String returnType,
            List<String> parameterTypes,
            List<String> parameterNames
    ) {
        String validatedMethodName = validateMethodName(methodName);
        String validatedReturnType = validateTypeName(returnType, "returnType");
        List<String> types = new ArrayList<String>(Objects.requireNonNull(parameterTypes, "parameterTypes must not be null"));
        List<String> names = new ArrayList<String>(Objects.requireNonNull(parameterNames, "parameterNames must not be null"));
        if (types.size() != names.size()) {
            throw new IllegalArgumentException("parameterTypes size (" + types.size()
                    + ") must equal parameterNames size (" + names.size() + ")");
        }
        for (int i = 0; i < types.size(); i++) {
            types.set(i, validateTypeName(types.get(i), "parameterTypes[" + i + "]"));
            names.set(i, validateParameterName(names.get(i), "parameterNames[" + i + "]"));
        }
        return create(validatedMethodName, validatedReturnType, types, names, true);
    }

    private static MethodDescriptor create(
            String methodName,
            String returnType,
            List<String> parameterTypes,
            List<String> parameterNames,
            boolean staticMethod
    ) {
        return new MethodDescriptor(methodName, returnType, parameterTypes, parameterNames, staticMethod);
    }

    public static MethodDescriptor voidMethod(String methodName) {
        return of(methodName, "void");
    }

    public static MethodDescriptor voidMethod(
            String methodName,
            List<String> parameterTypes,
            List<String> parameterNames
    ) {
        return of(methodName, "void", parameterTypes, parameterNames);
    }

    public String methodName() {
        return methodName;
    }

    public String returnType() {
        return returnType;
    }

    public List<String> parameterTypes() {
        return parameterTypes;
    }

    public List<String> parameterNames() {
        return parameterNames;
    }

    public boolean hasParameters() {
        return !parameterTypes.isEmpty();
    }

    public boolean isStatic() {
        return staticMethod;
    }

    public boolean isVoid() {
        return "void".equals(returnType);
    }

    public String normalizedSignatureKey() {
        StringBuilder builder = new StringBuilder();
        builder.append(staticMethod ? "static" : "instance");
        builder.append('#').append(methodName).append('(');
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(normalizedTypeName(parameterTypes.get(i)));
        }
        builder.append(')');
        return builder.toString();
    }

    public boolean hasCompatibleSignature(MethodDescriptor other) {
        Objects.requireNonNull(other, "other must not be null");
        if (staticMethod != other.staticMethod
                || !methodName.equals(other.methodName)
                || parameterTypes.size() != other.parameterTypes.size()) {
            return false;
        }
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (!compatibleParameterType(parameterTypes.get(i), other.parameterTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static String normalizedTypeName(String typeName) {
        Objects.requireNonNull(typeName, "typeName must not be null");
        String normalized = typeName.trim();
        if (normalized.endsWith("...")) {
            normalized = normalized.substring(0, normalized.length() - 3) + "[]";
        }
        while (normalized.startsWith("java.lang.")) {
            normalized = normalized.substring("java.lang.".length());
        }
        return normalized;
    }

    private static boolean compatibleParameterType(String left, String right) {
        String normalizedLeft = normalizedTypeName(left);
        String normalizedRight = normalizedTypeName(right);
        return normalizedLeft.equals(normalizedRight)
                || "Object".equals(normalizedLeft)
                || "Object".equals(normalizedRight);
    }

    public boolean hasDefaultReturnValue() {
        return !isVoid();
    }

    public String defaultReturnExpression() {
        if (isVoid()) {
            return "";
        }
        if ("boolean".equals(returnType)) {
            return "false";
        }
        if ("long".equals(returnType)) {
            return "0L";
        }
        if ("float".equals(returnType)) {
            return "0.0f";
        }
        if ("double".equals(returnType)) {
            return "0.0d";
        }
        if ("char".equals(returnType)) {
            return "'\\0'";
        }
        if ("byte".equals(returnType) || "short".equals(returnType) || "int".equals(returnType)) {
            return "0";
        }
        return "null";
    }

    public String defaultReturnStatement() {
        if (isVoid()) {
            return "";
        }
        return "return " + defaultReturnExpression() + ";";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MethodDescriptor)) {
            return false;
        }
        MethodDescriptor that = (MethodDescriptor) other;
        return methodName.equals(that.methodName)
                && returnType.equals(that.returnType)
                && parameterTypes.equals(that.parameterTypes)
                && parameterNames.equals(that.parameterNames)
                && staticMethod == that.staticMethod;
    }

    @Override
    public int hashCode() {
        int result = methodName.hashCode();
        result = 31 * result + returnType.hashCode();
        result = 31 * result + parameterTypes.hashCode();
        result = 31 * result + parameterNames.hashCode();
        result = 31 * result + (staticMethod ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MethodDescriptor{");
        if (staticMethod) {
            sb.append("static ");
        }
        sb.append(returnType).append(" ").append(methodName).append("(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameterTypes.get(i)).append(" ").append(parameterNames.get(i));
        }
        sb.append(")}");
        return sb.toString();
    }

    private static String validateMethodName(String methodName) {
        Objects.requireNonNull(methodName, "methodName must not be null");
        if (!isJavaIdentifier(methodName)) {
            throw new IllegalArgumentException("methodName must be a Java identifier: " + methodName);
        }
        return methodName;
    }

    private static String validateParameterName(String parameterName, String fieldName) {
        Objects.requireNonNull(parameterName, fieldName + " must not be null");
        if (!isJavaIdentifier(parameterName)) {
            throw new IllegalArgumentException(fieldName + " must be a Java identifier: " + parameterName);
        }
        return parameterName;
    }

    private static String validateTypeName(String typeName, String fieldName) {
        Objects.requireNonNull(typeName, fieldName + " must not be null");
        String trimmed = typeName.trim();
        if (trimmed.length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return trimmed;
    }

    private static boolean isJavaIdentifier(String value) {
        if (value.length() == 0) {
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
