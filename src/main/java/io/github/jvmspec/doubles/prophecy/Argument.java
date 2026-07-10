package io.github.jvmspec.doubles.prophecy;

import io.github.jvmspec.doubles.ArgumentMatcher;
import io.github.jvmspec.doubles.ArgumentMatchers;

import java.util.function.Predicate;

/**
 * Fluent argument-matching DSL inspired by Prophecy's {@code Argument} utility.
 * <p>
 * Each method delegates to the corresponding {@link ArgumentMatchers} factory, wrapping
 * the result in a static import-friendly {@code Argument} type.
 * </p>
 *
 * <pre>{@code
 * import static io.github.jvmspec.doubles.prophecy.Argument.*;
 *
 * mailerProphecy.send(any(), eq("hello")).willReturn(true);
 * }</pre>
 */
public final class Argument {

    private Argument() {
    }

    /**
     * Matches any argument value, including null.
     */
    public static ArgumentMatcher any() {
        return ArgumentMatchers.any();
    }

    /**
     * Matches null or an argument assignable to the supplied type.
     */
    public static ArgumentMatcher any(Class<?> type) {
        return ArgumentMatchers.any(type);
    }

    /**
     * Alias for {@link #any(Class)}.
     */
    public static ArgumentMatcher anyType(Class<?> type) {
        return ArgumentMatchers.anyType(type);
    }

    /**
     * Matches only null.
     */
    public static ArgumentMatcher isNull() {
        return ArgumentMatchers.isNull();
    }

    /**
     * Matches any non-null argument.
     */
    public static ArgumentMatcher notNull() {
        return ArgumentMatchers.notNull();
    }

    /**
     * Matches an argument equal to the expected value using javaspec's array-aware equality.
     */
    public static ArgumentMatcher eq(Object expected) {
        return ArgumentMatchers.eq(expected);
    }

    /**
     * Alias for {@link #eq(Object)}.
     */
    public static ArgumentMatcher equalTo(Object expected) {
        return ArgumentMatchers.equalTo(expected);
    }

    /**
     * Matches a string argument that contains the expected substring.
     * <p>
     * If the actual argument is not a {@link CharSequence}, the matcher returns false.
     * </p>
     *
     * @param expectedSubstring the substring that the actual argument must contain
     * @return an {@link ArgumentMatcher} that checks containment
     */
    public static ArgumentMatcher containingString(String expectedSubstring) {
        return new ContainsStringMatcher(expectedSubstring);
    }

    /**
     * Matches the exact same object reference using {@code ==} semantics.
     */
    public static ArgumentMatcher same(Object expected) {
        return ArgumentMatchers.same(expected);
    }

    /**
     * Alias for {@link #same(Object)}.
     */
    public static ArgumentMatcher identicalTo(Object expected) {
        return ArgumentMatchers.identicalTo(expected);
    }

    /**
     * Matches when the actual value equals one of the supplied candidates.
     */
    public static ArgumentMatcher in(Object... candidates) {
        return ArgumentMatchers.in(candidates);
    }

    /**
     * Matches when the actual value does not equal any supplied candidates.
     */
    public static ArgumentMatcher notIn(Object... candidates) {
        return ArgumentMatchers.notIn(candidates);
    }

    /**
     * Matches through a custom predicate.
     */
    public static ArgumentMatcher matching(Predicate<Object> predicate) {
        return ArgumentMatchers.matching(predicate);
    }

    /**
     * Matches through a custom predicate with a diagnostic description.
     */
    public static ArgumentMatcher matching(Predicate<Object> predicate, String description) {
        return ArgumentMatchers.matching(predicate, description);
    }

    private static final class ContainsStringMatcher implements ArgumentMatcher {
        private final String expected;

        ContainsStringMatcher(String expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object actual) {
            if (actual == null) {
                return false;
            }
            if (actual instanceof CharSequence) {
                return actual.toString().contains(expected);
            }
            return false;
        }

        @Override
        public String describe() {
            return "containingString(\"" + expected + "\")";
        }
    }
}
