package org.javaspec.doubles;

import java.util.Objects;

/**
 * Zero-dependency argument matcher factories for interface doubles.
 */
public final class ArgumentMatchers {
    private static final ArgumentMatcher ANY = new ArgumentMatcher() {
        @Override
        public boolean matches(Object actual) {
            return true;
        }

        @Override
        public String describe() {
            return "any()";
        }
    };

    private static final ArgumentMatcher IS_NULL = new ArgumentMatcher() {
        @Override
        public boolean matches(Object actual) {
            return actual == null;
        }

        @Override
        public String describe() {
            return "isNull()";
        }
    };

    private static final ArgumentMatcher NOT_NULL = new ArgumentMatcher() {
        @Override
        public boolean matches(Object actual) {
            return actual != null;
        }

        @Override
        public String describe() {
            return "notNull()";
        }
    };

    private ArgumentMatchers() {
    }

    /**
     * Matches any argument value, including null.
     */
    public static ArgumentMatcher any() {
        return ANY;
    }

    /**
     * Alias for {@link #any()}.
     */
    public static ArgumentMatcher anyArgument() {
        return any();
    }

    /**
     * Matches null or an argument assignable to the supplied type.
     * <p>
     * This matcher is intentionally nullable because interface methods often accept reference
     * arguments that may be null. Primitive parameter invocations are observed as boxed values by
     * the JDK proxy, so primitive class tokens such as {@code int.class} match their wrapper type.
     * </p>
     */
    public static ArgumentMatcher any(Class<?> type) {
        return anyType(type);
    }

    /**
     * Alias for {@link #any(Class)}.
     */
    public static ArgumentMatcher anyType(Class<?> type) {
        return new TypeArgumentMatcher(type);
    }

    /**
     * Matches only null.
     */
    public static ArgumentMatcher isNull() {
        return IS_NULL;
    }

    /**
     * Matches any non-null argument.
     */
    public static ArgumentMatcher notNull() {
        return NOT_NULL;
    }

    /**
     * Matches an argument equal to the expected value using javaspec's array-aware equality.
     */
    public static ArgumentMatcher eq(Object expected) {
        return equalTo(expected);
    }

    /**
     * Alias for {@link #eq(Object)}.
     */
    public static ArgumentMatcher equalTo(Object expected) {
        return new EqualToArgumentMatcher(expected);
    }

    private static final class TypeArgumentMatcher implements ArgumentMatcher {
        private final Class<?> requestedType;
        private final Class<?> matchingType;

        TypeArgumentMatcher(Class<?> type) {
            this.requestedType = Objects.requireNonNull(type, "type must not be null");
            this.matchingType = type.isPrimitive() ? wrapperType(type) : type;
        }

        @Override
        public boolean matches(Object actual) {
            return actual == null || matchingType.isInstance(actual);
        }

        @Override
        public String describe() {
            return "any(" + requestedType.getName() + ")";
        }
    }

    private static final class EqualToArgumentMatcher implements ArgumentMatcher {
        private final Object expected;

        EqualToArgumentMatcher(Object expected) {
            this.expected = Arguments.copyValue(expected);
        }

        @Override
        public boolean matches(Object actual) {
            return Arguments.equalValues(expected, actual);
        }

        @Override
        public String describe() {
            return "eq(" + Arguments.describeValue(expected) + ")";
        }
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
