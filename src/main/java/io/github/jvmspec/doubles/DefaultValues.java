package io.github.jvmspec.doubles;

final class DefaultValues {
    private DefaultValues() {
    }

    static Object valueFor(Class<?> returnType) {
        if (void.class.equals(returnType)) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return Boolean.FALSE;
        }
        if (byte.class.equals(returnType)) {
            return Byte.valueOf((byte) 0);
        }
        if (short.class.equals(returnType)) {
            return Short.valueOf((short) 0);
        }
        if (int.class.equals(returnType)) {
            return Integer.valueOf(0);
        }
        if (long.class.equals(returnType)) {
            return Long.valueOf(0L);
        }
        if (float.class.equals(returnType)) {
            return Float.valueOf(0.0f);
        }
        if (double.class.equals(returnType)) {
            return Double.valueOf(0.0d);
        }
        if (char.class.equals(returnType)) {
            return Character.valueOf('\0');
        }
        return null;
    }
}
