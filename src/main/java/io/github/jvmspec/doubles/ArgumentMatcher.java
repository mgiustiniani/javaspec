package io.github.jvmspec.doubles;

/**
 * Matches one argument in a javaspec interface double stub, verifier, or call query.
 * <p>
 * Implementations should be side-effect free. The description is used in assertion messages and
 * call-pattern descriptions, so it should be concise and human-readable.
 * </p>
 */
@FunctionalInterface
public interface ArgumentMatcher {
    /**
     * Returns true when the actual argument satisfies this matcher.
     */
    boolean matches(Object actual);

    /**
     * Returns a human-readable description of this matcher.
     */
    default String describe() {
        return "customMatcher()";
    }
}
