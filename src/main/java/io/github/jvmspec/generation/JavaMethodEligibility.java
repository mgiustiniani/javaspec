package io.github.jvmspec.generation;

import io.github.jvmspec.internal.type.JavaIdentifiers;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.util.ArrayList;
import java.util.List;

/** Selects behavior methods that can legally be synchronized for each Java type kind. */
final class JavaMethodEligibility {
    private JavaMethodEligibility() {
    }

    static List<MethodDescriptor> eligibleMethods(DescribedType describedType) {
        JavaTypeKind kind = describedType.kind();
        if (JavaTypeMethodCapabilities.supportsBodies(kind)) {
            return JavaTypeKind.ENUM.equals(kind)
                    ? nonImplicitEnumMethods(describedType.methods())
                    : describedType.methods();
        }
        if (JavaTypeMethodCapabilities.supportsInterfaceDeclarations(kind)) {
            return interfaceMethods(describedType);
        }
        if (JavaTypeMethodCapabilities.supportsAnnotationElements(kind)) {
            return annotationElementMethods(describedType);
        }
        return new ArrayList<MethodDescriptor>();
    }

    static List<MethodDescriptor> interfaceMethods(DescribedType describedType) {
        List<MethodDescriptor> result = new ArrayList<MethodDescriptor>();
        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (!method.isStatic()) result.add(method);
        }
        return result;
    }

    static List<MethodDescriptor> annotationElementMethods(DescribedType describedType) {
        List<MethodDescriptor> result = new ArrayList<MethodDescriptor>();
        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (isCompatibleAnnotationElement(method)) result.add(method);
        }
        return result;
    }

    private static List<MethodDescriptor> nonImplicitEnumMethods(List<MethodDescriptor> methods) {
        List<MethodDescriptor> result = new ArrayList<MethodDescriptor>();
        for (int i = 0; i < methods.size(); i++) {
            if (!isImplicitEnumMethod(methods.get(i))) result.add(methods.get(i));
        }
        return result;
    }

    private static boolean isImplicitEnumMethod(MethodDescriptor method) {
        if ("valueOf".equals(method.methodName()) && method.parameterTypes().size() == 1
                && "String".equals(method.parameterTypes().get(0))) {
            return true;
        }
        if ("values".equals(method.methodName()) && method.parameterTypes().isEmpty()) return true;
        return ("name".equals(method.methodName()) || "ordinal".equals(method.methodName()))
                && method.parameterTypes().isEmpty();
    }

    private static boolean isCompatibleAnnotationElement(MethodDescriptor method) {
        return !method.isStatic()
                && !method.hasParameters()
                && isCompatibleAnnotationElementReturnType(method.returnType());
    }

    private static boolean isCompatibleAnnotationElementReturnType(String returnType) {
        String normalized = returnType.trim().replace(" ", "");
        int arrayDimensions = 0;
        while (normalized.endsWith("[]")) {
            arrayDimensions++;
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        if (arrayDimensions > 1
                || "void".equals(normalized)
                || isKnownInvalidAnnotationElementType(normalized)) {
            return false;
        }
        if (isPrimitiveType(normalized)
                || "String".equals(normalized)
                || "java.lang.String".equals(normalized)
                || "Class".equals(normalized)
                || "java.lang.Class".equals(normalized)
                || isClassElementType(normalized)) {
            return true;
        }
        if (normalized.indexOf('<') >= 0 || normalized.indexOf('?') >= 0) return false;
        return isQualifiedTypeName(normalized);
    }

    private static boolean isPrimitiveType(String typeName) {
        return "boolean".equals(typeName)
                || "byte".equals(typeName)
                || "short".equals(typeName)
                || "int".equals(typeName)
                || "long".equals(typeName)
                || "float".equals(typeName)
                || "double".equals(typeName)
                || "char".equals(typeName);
    }

    private static boolean isKnownInvalidAnnotationElementType(String typeName) {
        return "Object".equals(typeName)
                || "java.lang.Object".equals(typeName)
                || "Void".equals(typeName)
                || "java.lang.Void".equals(typeName)
                || "Boolean".equals(typeName)
                || "java.lang.Boolean".equals(typeName)
                || "Byte".equals(typeName)
                || "java.lang.Byte".equals(typeName)
                || "Short".equals(typeName)
                || "java.lang.Short".equals(typeName)
                || "Integer".equals(typeName)
                || "java.lang.Integer".equals(typeName)
                || "Long".equals(typeName)
                || "java.lang.Long".equals(typeName)
                || "Float".equals(typeName)
                || "java.lang.Float".equals(typeName)
                || "Double".equals(typeName)
                || "java.lang.Double".equals(typeName)
                || "Character".equals(typeName)
                || "java.lang.Character".equals(typeName);
    }

    private static boolean isClassElementType(String typeName) {
        return (typeName.startsWith("Class<") || typeName.startsWith("java.lang.Class<"))
                && typeName.endsWith(">");
    }

    private static boolean isQualifiedTypeName(String typeName) {
        if (typeName.length() == 0) return false;
        String[] parts = typeName.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if (!JavaIdentifiers.isIdentifier(parts[i])) return false;
        }
        return true;
    }
}
