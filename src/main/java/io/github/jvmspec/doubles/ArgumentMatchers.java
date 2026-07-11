package io.github.jvmspec.doubles;

import java.util.Objects;
import java.util.function.Predicate;

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

    /**
     * Matches the exact same object reference using {@code ==} semantics.
     */
    public static ArgumentMatcher same(Object expected) {
        return identicalTo(expected);
    }

    /**
     * Alias for {@link #same(Object)}.
     */
    public static ArgumentMatcher identicalTo(Object expected) {
        return new SameArgumentMatcher(expected);
    }

    /**
     * Matches when the actual value equals one of the supplied candidates.
     */
    public static ArgumentMatcher in(Object... candidates) {
        return new InArgumentMatcher(false, candidates);
    }

    /**
     * Matches when the actual value does not equal any supplied candidate.
     */
    public static ArgumentMatcher notIn(Object... candidates) {
        return new InArgumentMatcher(true, candidates);
    }

    /**
     * Matches through a custom predicate.
     */
    public static ArgumentMatcher matching(Predicate<Object> predicate) {
        return matching(predicate, "matching(callback)");
    }

    /**
     * Matches through a custom predicate with a diagnostic description.
     */
    public static ArgumentMatcher matching(Predicate<Object> predicate, String description) {
        return new PredicateArgumentMatcher(predicate, description);
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

    private static final class SameArgumentMatcher implements ArgumentMatcher {
        private final Object expected;

        SameArgumentMatcher(Object expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object actual) {
            return actual == expected;
        }

        @Override
        public String describe() {
            return "same(" + Arguments.describeValue(expected) + ")";
        }
    }

    private static final class InArgumentMatcher implements ArgumentMatcher {
        private final boolean negated;
        private final Object[] candidates;

        InArgumentMatcher(boolean negated, Object[] candidates) {
            this.negated = negated;
            this.candidates = Arguments.copy(candidates == null ? new Object[0] : candidates);
        }

        @Override
        public boolean matches(Object actual) {
            for (Object candidate : candidates) {
                if (Arguments.equalValues(candidate, actual)) {
                    return !negated;
                }
            }
            return negated;
        }

        @Override
        public String describe() {
            return (negated ? "notIn" : "in") + Arguments.describe(candidates);
        }
    }

    private static final class PredicateArgumentMatcher implements ArgumentMatcher {
        private final Predicate<Object> predicate;
        private final String description;

        PredicateArgumentMatcher(Predicate<Object> predicate, String description) {
            this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
            this.description = description == null || description.trim().isEmpty()
                    ? "matching(callback)" : description;
        }

        @Override
        public boolean matches(Object actual) {
            return predicate.test(actual);
        }

        @Override
        public String describe() {
            return description;
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
