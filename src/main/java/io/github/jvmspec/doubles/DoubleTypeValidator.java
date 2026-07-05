package io.github.jvmspec.doubles;

import java.lang.reflect.Modifier;

final class DoubleTypeValidator {
    private DoubleTypeValidator() {
    }

    static void requireSupportedInterface(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Cannot create a double for null; only interfaces are supported.");
        }
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot create a double for primitive type " + typeName(type)
                    + "; only interfaces are supported.");
        }
        if (type.isArray()) {
            throw new IllegalArgumentException("Cannot create a double for array type " + typeName(type)
                    + "; only interfaces are supported.");
        }
        if (type.isAnnotation()) {
            throw new IllegalArgumentException("Cannot create a double for annotation type " + typeName(type)
                    + "; only ordinary interfaces are supported.");
        }
        if (type.isEnum()) {
            throw new IllegalArgumentException("Cannot create a double for enum type " + typeName(type)
                    + "; only interfaces are supported.");
        }
        if (!type.isInterface()) {
            if (Modifier.isFinal(type.getModifiers())) {
                throw new IllegalArgumentException("Cannot create a double for final class " + typeName(type)
                        + "; only interfaces are supported.");
            }
            throw new IllegalArgumentException("Cannot create a double for concrete class " + typeName(type)
                    + "; only interfaces are supported.");
        }
    }

    private static String typeName(Class<?> type) {
        String canonicalName = type.getCanonicalName();
        return canonicalName == null ? type.getName() : canonicalName;
    }
}
