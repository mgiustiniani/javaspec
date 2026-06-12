package org.javaspec.doubles.prophecy;

import org.javaspec.doubles.ArgumentMatcher;
import org.javaspec.doubles.ArgumentMatchers;

/**
 * Shorter alias for {@link Argument}. Allows import-static style:
 * <pre>{@code
 * import static org.javaspec.doubles.prophecy.Arg.*;
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
}
