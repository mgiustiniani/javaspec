package io.github.jvmspec.doubles.prophecy;

import io.github.jvmspec.doubles.ArgumentMatcher;

import java.util.function.Predicate;

/**
 * Shorter alias for {@link Argument}. Allows import-static style:
 * <pre>{@code
 * import static io.github.jvmspec.doubles.prophecy.Arg.*;
 *
 * mailerProphecy.send(any(), eq("hello")).willReturn(true);
 * }</pre>
 */
public final class Arg {

    private Arg() {
    }

    /**
     * Matches any argument value, including null.
     */
    public static ArgumentMatcher any() {
        return Argument.any();
    }

    /**
     * Matches null or an argument assignable to the supplied type.
     */
    public static ArgumentMatcher any(Class<?> type) {
        return Argument.any(type);
    }

    /**
     * Matches only null.
     */
    public static ArgumentMatcher isNull() {
        return Argument.isNull();
    }

    /**
     * Matches any non-null argument.
     */
    public static ArgumentMatcher notNull() {
        return Argument.notNull();
    }

    /**
     * Matches an argument equal to the expected value.
     */
    public static ArgumentMatcher eq(Object expected) {
        return Argument.eq(expected);
    }

    /**
     * Matches a string argument that contains the expected substring.
     */
    public static ArgumentMatcher containingString(String expectedSubstring) {
        return Argument.containingString(expectedSubstring);
    }

    /**
     * Matches the exact same object reference using {@code ==} semantics.
     */
    public static ArgumentMatcher same(Object expected) {
        return Argument.same(expected);
    }

    /**
     * Alias for {@link #same(Object)}.
     */
    public static ArgumentMatcher identicalTo(Object expected) {
        return Argument.identicalTo(expected);
    }

    /**
     * Matches when the actual value equals one of the supplied candidates.
     */
    public static ArgumentMatcher in(Object... candidates) {
        return Argument.in(candidates);
    }

    /**
     * Matches when the actual value does not equal any supplied candidates.
     */
    public static ArgumentMatcher notIn(Object... candidates) {
        return Argument.notIn(candidates);
    }

    /**
     * Matches through a custom predicate.
     */
    public static ArgumentMatcher matching(Predicate<Object> predicate) {
        return Argument.matching(predicate);
    }

    /**
     * Matches through a custom predicate with a diagnostic description.
     */
    public static ArgumentMatcher matching(Predicate<Object> predicate, String description) {
        return Argument.matching(predicate, description);
    }
}
