package io.github.jvmspec.doubles.prophecy;

import io.github.jvmspec.doubles.ArgumentMatcher;

/**
 * Prophecy-style custom argument token.
 * <p>
 * Implement this interface when built-in tokens such as {@link Argument#any()},
 * {@link Argument#eq(Object)}, or {@link Argument#matching(java.util.function.Predicate, String)}
 * are not expressive enough. Custom tokens can be passed anywhere an argument token is accepted,
 * including generated typed {@code *Prophecy} wrapper token overloads.
 * </p>
 */
@FunctionalInterface
public interface ArgumentToken extends ArgumentMatcher {
    /**
     * Returns true when the actual argument satisfies this token.
     */
    @Override
    boolean matches(Object actual);

    /**
     * Returns a concise diagnostic description for assertion messages.
     */
    @Override
    default String describe() {
        return "customToken()";
    }
}
