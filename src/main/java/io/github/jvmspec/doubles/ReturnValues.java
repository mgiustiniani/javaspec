package io.github.jvmspec.doubles;

import java.lang.reflect.Method;

final class ReturnValues {
    private ReturnValues() {
    }

    static Object valueFor(Method method, Object stubbedValue, boolean stubbed) {
        Class<?> returnType = method.getReturnType();
        if (void.class.equals(returnType)) {
            return null;
        }
        if (!stubbed) {
            return DefaultValues.valueFor(returnType);
        }
        if (stubbedValue == null) {
            if (returnType.isPrimitive()) {
                throw new IllegalStateException("Stubbed value for method '" + method.getName()
                        + "' is null but return type is primitive " + returnType.getName());
            }
            return null;
        }
        if (returnType.isPrimitive()) {
            Class<?> wrapperType = wrapperType(returnType);
            if (!wrapperType.isInstance(stubbedValue)) {
                throw incompatibleValue(method, stubbedValue, wrapperType);
            }
            return stubbedValue;
        }
        if (!returnType.isInstance(stubbedValue)) {
            throw incompatibleValue(method, stubbedValue, returnType);
        }
        return stubbedValue;
    }

    private static IllegalStateException incompatibleValue(Method method, Object value, Class<?> expectedType) {
        return new IllegalStateException("Stubbed value for method '" + method.getName()
                + "' has type " + value.getClass().getName()
                + " but return type is " + expectedType.getName());
    }

    private static Class<?> wrapperType(Class<?> primitiveType) {
        if (boolean.class.equals(primitiveType)) return Boolean.class;
        if (byte.class.equals(primitiveType)) return Byte.class;
        if (short.class.equals(primitiveType)) return Short.class;
        if (int.class.equals(primitiveType)) return Integer.class;
        if (long.class.equals(primitiveType)) return Long.class;
        if (float.class.equals(primitiveType)) return Float.class;
        if (double.class.equals(primitiveType)) return Double.class;
        if (char.class.equals(primitiveType)) return Character.class;
        if (void.class.equals(primitiveType)) return Void.class;
        return primitiveType;
    }
}
