package org.javaspec.matcher;

/**
 * Contract for a javaspec matcher.
 * <p>
 * A matcher evaluates whether a subject value meets an expectation.
 * Implementations can be swapped: the default {@link CustomMatcher}
 * uses a simple predicate; a JUnit-based implementation could wrap
 * JUnit assertions while implementing the same interface.
 * </p>
 *
 * @param <T> the type of value this matcher can evaluate
 */
public interface Matcher<T> {
    /**
     * Evaluates whether the subject value matches the expectation.
     *
     * @param subject   the actual value to evaluate
     * @param expected  the expected value or expectation arguments
     * @return a {@link MatchResult} with pass/fail status and a descriptive message
     */
    MatchResult evaluate(T subject, Object... expected);

    /**
     * Returns a descriptive name for this matcher (e.g. "shouldBe", "shouldEqual").
     * Used in failure messages.
     */
    String name();
}
