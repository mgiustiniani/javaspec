package io.github.jvmspec.api;

import io.github.jvmspec.matcher.Matchable;
import io.github.jvmspec.matcher.MatcherRegistry;

/**
 * Dispatches assertion methods to a {@link MatcherRegistry}.
 * <p>Extracted from {@link ObjectBehavior} to reduce responsibility concentration.
 * Every assertion method delegates to {@code match(value).shouldXxx(...)}.</p>
 */
public class AssertionDispatcher {
    private MatcherRegistry matcherRegistry;

    /**
     * Creates a new dispatcher backed by the given registry.
     *
     * @param matcherRegistry the matcher registry to use
     */
    public AssertionDispatcher(MatcherRegistry matcherRegistry) {
        this.matcherRegistry = matcherRegistry;
    }

    /**
     * Returns the underlying matcher registry.
     */
    public MatcherRegistry matcherRegistry() {
        return matcherRegistry;
    }

    /**
     * Replaces the matcher registry. Used when the registry is updated at runtime.
     */
    public void setMatcherRegistry(MatcherRegistry registry) {
        this.matcherRegistry = registry;
    }

    // --- Matchable wrapper ---

    /**
     * Wraps a value in a {@link Matchable} so that matcher methods can be chained.
     *
     * @param value the value to wrap
     * @param <R>   the type of the value
     * @return a Matchable that delegates to the registered matchers
     */
    public <R> Matchable<R> match(R value) {
        return new Matchable<R>(value, matcherRegistry);
    }

    // --- Equality assertions ---

    /**
     * Asserts that the subject value is identical to the expected value (by == semantics).
     */
    public void shouldBe(Object actual, Object expected) {
        match(actual).shouldBe(expected);
    }

    /**
     * Asserts that the subject value is equal to the expected value (by equals semantics).
     */
    public void shouldEqual(Object actual, Object expected) {
        match(actual).shouldEqual(expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object, Object)} using PHPSpec return terminology.
     */
    public void shouldReturn(Object actual, Object expected) {
        match(actual).shouldReturn(expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object, Object)}.
     */
    public void shouldBeLike(Object actual, Object expected) {
        match(actual).shouldBeLike(expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object, Object)}.
     */
    public void shouldBeEqualTo(Object actual, Object expected) {
        match(actual).shouldBeEqualTo(expected);
    }

    /**
     * Asserts that the subject value is NOT identical to the unexpected value.
     */
    public void shouldNotBe(Object actual, Object unexpected) {
        match(actual).shouldNotBe(unexpected);
    }

    /**
     * Asserts that the subject value is NOT equal to the unexpected value.
     */
    public void shouldNotEqual(Object actual, Object unexpected) {
        match(actual).shouldNotEqual(unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object, Object)} using PHPSpec return terminology.
     */
    public void shouldNotReturn(Object actual, Object unexpected) {
        match(actual).shouldNotReturn(unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object, Object)}.
     */
    public void shouldNotBeLike(Object actual, Object unexpected) {
        match(actual).shouldNotBeLike(unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object, Object)}.
     */
    public void shouldNotBeEqualTo(Object actual, Object unexpected) {
        match(actual).shouldNotBeEqualTo(unexpected);
    }

    // --- Type assertions ---

    /**
     * Asserts that the subject value has the given type.
     */
    public void shouldHaveType(Object actual, Class<?> expectedType) {
        match(actual).shouldHaveType(expectedType);
    }

    /**
     * Alias for {@link #shouldHaveType(Object, Class)}.
     */
    public void shouldBeAnInstanceOf(Object actual, Class<?> expectedType) {
        match(actual).shouldBeAnInstanceOf(expectedType);
    }

    /**
     * Alias for {@link #shouldHaveType(Object, Class)} using PHPSpec return terminology.
     */
    public void shouldReturnAnInstanceOf(Object actual, Class<?> expectedType) {
        match(actual).shouldReturnAnInstanceOf(expectedType);
    }

    /**
     * Asserts that the subject value implements or extends the expected type.
     */
    public void shouldImplement(Object actual, Class<?> expectedType) {
        match(actual).shouldImplement(expectedType);
    }

    // --- Numeric assertions ---

    /**
     * Asserts that the numeric subject value is within the inclusive tolerance of the expected value.
     */
    public void shouldBeApproximately(Object actual, Number expected, Number tolerance) {
        match(actual).shouldBeApproximately(expected, tolerance);
    }

    /**
     * Alias for {@link #shouldBeApproximately(Object, Number, Number)} using PHPSpec return terminology.
     */
    public void shouldReturnApproximately(Object actual, Number expected, Number tolerance) {
        match(actual).shouldReturnApproximately(expected, tolerance);
    }

    /**
     * Asserts that the numeric subject value is outside the inclusive tolerance of the unexpected value.
     */
    public void shouldNotBeApproximately(Object actual, Number unexpected, Number tolerance) {
        match(actual).shouldNotBeApproximately(unexpected, tolerance);
    }

    /**
     * Alias for {@link #shouldNotBeApproximately(Object, Number, Number)} using PHPSpec return terminology.
     */
    public void shouldNotReturnApproximately(Object actual, Number unexpected, Number tolerance) {
        match(actual).shouldNotReturnApproximately(unexpected, tolerance);
    }

    // --- Collection/content assertions ---

    /**
     * Asserts that strings, collections, maps, arrays, or iterables contain the expected value.
     */
    public void shouldContain(Object actual, Object expected) {
        match(actual).shouldContain(expected);
    }

    /**
     * Asserts that strings, collections, maps, arrays, or iterables do not contain the unexpected value.
     */
    public void shouldNotContain(Object actual, Object unexpected) {
        match(actual).shouldNotContain(unexpected);
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables have the expected count.
     */
    public void shouldHaveCount(Object actual, int expectedCount) {
        match(actual).shouldHaveCount(expectedCount);
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables are empty.
     */
    public void shouldBeEmpty(Object actual) {
        match(actual).shouldBeEmpty();
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables are not empty.
     */
    public void shouldNotBeEmpty(Object actual) {
        match(actual).shouldNotBeEmpty();
    }

    // --- Map assertions ---

    /**
     * Asserts that the map contains the expected key.
     */
    public void shouldHaveKey(Object actual, Object key) {
        match(actual).shouldHaveKey(key);
    }

    /**
     * Asserts that the map does not contain the unexpected key.
     */
    public void shouldNotHaveKey(Object actual, Object key) {
        match(actual).shouldNotHaveKey(key);
    }

    /**
     * Asserts that the map contains the expected value.
     */
    public void shouldHaveValue(Object actual, Object value) {
        match(actual).shouldHaveValue(value);
    }

    /**
     * Asserts that the map does not contain the unexpected value.
     */
    public void shouldNotHaveValue(Object actual, Object value) {
        match(actual).shouldNotHaveValue(value);
    }

    // --- String pattern assertions ---

    /**
     * Asserts that the character sequence does not start with the unexpected prefix.
     */
    public void shouldNotStartWith(Object actual, String prefix) {
        match(actual).shouldNotStartWith(prefix);
    }

    /**
     * Asserts that the character sequence does not end with the unexpected suffix.
     */
    public void shouldNotEndWith(Object actual, String suffix) {
        match(actual).shouldNotEndWith(suffix);
    }

    /**
     * Asserts that the character sequence does not match the supplied regular expression.
     */
    public void shouldNotMatchPattern(Object actual, String pattern) {
        match(actual).shouldNotMatchPattern(pattern);
    }
}
