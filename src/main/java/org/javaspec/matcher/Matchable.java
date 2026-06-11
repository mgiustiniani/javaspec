package org.javaspec.matcher;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Wrapper around a value that provides phpspec-style matcher chaining.
 * <p>
 * Enables the pattern: {@code methodReturnValue.shouldReturn(expected)}.
 * Each registry-backed {@code should*} method delegates to a {@link Matcher} obtained
 * from the {@link MatcherRegistry}; lightweight string and collection matchers are implemented
 * directly to keep the runtime dependency-free.
 * </p>
 *
 * @param <T> the type of the wrapped value
 */
public final class Matchable<T> {
    private final T value;
    private final MatcherRegistry registry;

    public Matchable(T value, MatcherRegistry registry) {
        this.value = value;
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * The wrapped value.
     */
    public T value() {
        return value;
    }

    /**
     * Asserts that the wrapped value is identical to the expected value (by {@code ==} semantics).
     */
    public void shouldBe(T expected) {
        Matcher<T> matcher = registry.matcherFor("identity");
        MatchResult result = matcher.evaluate(value, expected);
        assertPassed(result);
    }

    /**
     * Asserts that the wrapped value is equal to the expected value (by {@code equals} semantics).
     */
    public void shouldEqual(T expected) {
        Matcher<T> matcher = registry.matcherFor("equality");
        MatchResult result = matcher.evaluate(value, expected);
        assertPassed(result);
    }

    /**
     * Alias for {@link #shouldEqual(Object)} using PHPSpec return terminology.
     */
    public void shouldReturn(T expected) {
        shouldEqual(expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object)}.
     */
    public void shouldBeLike(T expected) {
        shouldEqual(expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object)}.
     */
    public void shouldBeEqualTo(T expected) {
        shouldEqual(expected);
    }

    /**
     * Asserts that the wrapped value is NOT identical to the unexpected value.
     */
    public void shouldNotBe(T unexpected) {
        Matcher<T> matcher = registry.matcherFor("negated-identity");
        MatchResult result = matcher.evaluate(value, unexpected);
        assertPassed(result);
    }

    /**
     * Asserts that the wrapped value is NOT equal to the unexpected value.
     */
    public void shouldNotEqual(T unexpected) {
        Matcher<T> matcher = registry.matcherFor("negated-equality");
        MatchResult result = matcher.evaluate(value, unexpected);
        assertPassed(result);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object)} using PHPSpec return terminology.
     */
    public void shouldNotReturn(T unexpected) {
        shouldNotEqual(unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object)}.
     */
    public void shouldNotBeLike(T unexpected) {
        shouldNotEqual(unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object)}.
     */
    public void shouldNotBeEqualTo(T unexpected) {
        shouldNotEqual(unexpected);
    }

    /**
     * Asserts that the wrapped value has the given type.
     */
    public void shouldHaveType(Class<?> expectedType) {
        if (expectedType == null) {
            throw new AssertionError("Expected type must not be null");
        }
        if (value == null) {
            throw new AssertionError("Expected an instance of " + expectedType.getName() + " but got null");
        }
        Class<?> actualType = value.getClass();
        if (!expectedType.isAssignableFrom(actualType)) {
            throw new AssertionError(
                    "Expected an instance of " + expectedType.getName()
                    + " but got " + actualType.getName()
            );
        }
    }

    /**
     * Alias for {@link #shouldHaveType(Class)}.
     */
    public void shouldBeAnInstanceOf(Class<?> expectedType) {
        shouldHaveType(expectedType);
    }

    /**
     * Alias for {@link #shouldHaveType(Class)} using PHPSpec return terminology.
     */
    public void shouldReturnAnInstanceOf(Class<?> expectedType) {
        shouldHaveType(expectedType);
    }

    /**
     * Asserts that the wrapped object, or wrapped class object, implements or extends the expected type.
     */
    public void shouldImplement(Class<?> expectedType) {
        if (expectedType == null) {
            throw new AssertionError("Expected implemented type must not be null");
        }
        if (value == null) {
            throw new AssertionError("Expected an implementation of " + expectedType.getName() + " but got null");
        }
        Class<?> actualType = value instanceof Class<?> ? (Class<?>) value : value.getClass();
        if (!expectedType.isAssignableFrom(actualType)) {
            throw new AssertionError(
                    "Expected " + actualType.getName() + " to implement " + expectedType.getName()
            );
        }
    }

    /**
     * Asserts that strings, collections, maps, arrays, or iterables contain the expected value.
     */
    public void shouldContain(Object expected) {
        if (!contains(value, expected)) {
            throw new AssertionError("Expected " + value + " to contain " + expected);
        }
    }

    /**
     * Asserts that strings, collections, maps, arrays, or iterables do not contain the unexpected value.
     */
    public void shouldNotContain(Object unexpected) {
        if (contains(value, unexpected)) {
            throw new AssertionError("Expected " + value + " not to contain " + unexpected);
        }
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables have the expected count.
     * <p>
     * For a generic {@link Iterable} that is not a {@link Collection}, the check is bounded: it
     * iterates at most {@code expectedCount + 1} elements and fails fast when the bound is exceeded,
     * so it is safe on infinite iterables.
     * </p>
     */
    public void shouldHaveCount(int expectedCount) {
        if (expectedCount < 0) {
            throw new AssertionError("Expected count must not be negative: " + expectedCount);
        }
        if (isGenericIterable(value)) {
            int limit = expectedCount < Integer.MAX_VALUE ? expectedCount + 1 : Integer.MAX_VALUE;
            int boundedCount = boundedCountOf((Iterable<?>) value, limit);
            if (boundedCount > expectedCount) {
                throw new AssertionError(
                        "Expected " + value + " to have count " + expectedCount
                        + " but it has more than " + expectedCount + " elements"
                );
            }
            if (boundedCount != expectedCount) {
                throw new AssertionError("Expected " + value + " to have count " + expectedCount + " but got " + boundedCount);
            }
            return;
        }
        int actualCount = countOf(value);
        if (actualCount != expectedCount) {
            throw new AssertionError("Expected " + value + " to have count " + expectedCount + " but got " + actualCount);
        }
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables are empty.
     * <p>
     * For a generic {@link Iterable} that is not a {@link Collection}, emptiness is determined via
     * {@code iterator().hasNext()} without consuming the iterable, so the check is bounded and safe
     * on infinite iterables.
     * </p>
     */
    public void shouldBeEmpty() {
        if (!isEmptyValue(value)) {
            if (isGenericIterable(value)) {
                throw new AssertionError("Expected " + value + " to be empty but it has at least one element");
            }
            throw new AssertionError("Expected " + value + " to be empty but had count " + countOf(value));
        }
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables are not empty.
     * <p>
     * For a generic {@link Iterable} that is not a {@link Collection}, emptiness is determined via
     * {@code iterator().hasNext()} without consuming the iterable, so the check is bounded and safe
     * on infinite iterables.
     * </p>
     */
    public void shouldNotBeEmpty() {
        if (isEmptyValue(value)) {
            throw new AssertionError("Expected " + value + " not to be empty");
        }
    }

    /**
     * Asserts that the wrapped map contains the expected key.
     */
    public void shouldHaveKey(Object expectedKey) {
        Map<?, ?> map = mapOf(value);
        if (!map.containsKey(expectedKey)) {
            throw new AssertionError("Expected " + value + " to have key " + expectedKey);
        }
    }

    /**
     * Asserts that the wrapped map does not contain the unexpected key.
     */
    public void shouldNotHaveKey(Object unexpectedKey) {
        Map<?, ?> map = mapOf(value);
        if (map.containsKey(unexpectedKey)) {
            throw new AssertionError("Expected " + value + " not to have key " + unexpectedKey);
        }
    }

    /**
     * Asserts that the wrapped map contains the expected value.
     */
    public void shouldHaveValue(Object expectedValue) {
        Map<?, ?> map = mapOf(value);
        if (!map.containsValue(expectedValue)) {
            throw new AssertionError("Expected " + value + " to have value " + expectedValue);
        }
    }

    /**
     * Asserts that the wrapped map does not contain the unexpected value.
     */
    public void shouldNotHaveValue(Object unexpectedValue) {
        Map<?, ?> map = mapOf(value);
        if (map.containsValue(unexpectedValue)) {
            throw new AssertionError("Expected " + value + " not to have value " + unexpectedValue);
        }
    }

    /**
     * Asserts that the wrapped character sequence starts with the expected prefix.
     */
    public void shouldStartWith(String expectedPrefix) {
        if (!(value instanceof CharSequence)) {
            throw new AssertionError("Expected a character sequence but got " + typeName(value));
        }
        if (expectedPrefix == null) {
            throw new AssertionError("Expected prefix must not be null");
        }
        if (!value.toString().startsWith(expectedPrefix)) {
            throw new AssertionError("Expected " + value + " to start with " + expectedPrefix);
        }
    }

    /**
     * Asserts that the wrapped character sequence does not start with the unexpected prefix.
     */
    public void shouldNotStartWith(String unexpectedPrefix) {
        if (!(value instanceof CharSequence)) {
            throw new AssertionError("Expected a character sequence but got " + typeName(value));
        }
        if (unexpectedPrefix == null) {
            throw new AssertionError("Expected prefix must not be null");
        }
        if (value.toString().startsWith(unexpectedPrefix)) {
            throw new AssertionError("Expected " + value + " not to start with " + unexpectedPrefix);
        }
    }

    /**
     * Asserts that the wrapped character sequence ends with the expected suffix.
     */
    public void shouldEndWith(String expectedSuffix) {
        if (!(value instanceof CharSequence)) {
            throw new AssertionError("Expected a character sequence but got " + typeName(value));
        }
        if (expectedSuffix == null) {
            throw new AssertionError("Expected suffix must not be null");
        }
        if (!value.toString().endsWith(expectedSuffix)) {
            throw new AssertionError("Expected " + value + " to end with " + expectedSuffix);
        }
    }

    /**
     * Asserts that the wrapped character sequence does not end with the unexpected suffix.
     */
    public void shouldNotEndWith(String unexpectedSuffix) {
        if (!(value instanceof CharSequence)) {
            throw new AssertionError("Expected a character sequence but got " + typeName(value));
        }
        if (unexpectedSuffix == null) {
            throw new AssertionError("Expected suffix must not be null");
        }
        if (value.toString().endsWith(unexpectedSuffix)) {
            throw new AssertionError("Expected " + value + " not to end with " + unexpectedSuffix);
        }
    }

    /**
     * Asserts that the wrapped character sequence matches the supplied regular expression.
     */
    public void shouldMatchPattern(String pattern) {
        if (!(value instanceof CharSequence)) {
            throw new AssertionError("Expected a character sequence but got " + typeName(value));
        }
        if (pattern == null) {
            throw new AssertionError("Expected pattern must not be null");
        }
        try {
            if (!Pattern.compile(pattern).matcher(value.toString()).find()) {
                throw new AssertionError("Expected " + value + " to match pattern " + pattern);
            }
        } catch (PatternSyntaxException ex) {
            AssertionError error = new AssertionError("Invalid pattern: " + pattern);
            error.initCause(ex);
            throw error;
        }
    }

    /**
     * Asserts that the wrapped character sequence does not match the supplied regular expression.
     */
    public void shouldNotMatchPattern(String pattern) {
        if (!(value instanceof CharSequence)) {
            throw new AssertionError("Expected a character sequence but got " + typeName(value));
        }
        if (pattern == null) {
            throw new AssertionError("Expected pattern must not be null");
        }
        try {
            if (Pattern.compile(pattern).matcher(value.toString()).find()) {
                throw new AssertionError("Expected " + value + " not to match pattern " + pattern);
            }
        } catch (PatternSyntaxException ex) {
            AssertionError error = new AssertionError("Invalid pattern: " + pattern);
            error.initCause(ex);
            throw error;
        }
    }

    /**
     * Evaluates a custom matcher by name from the registry against the wrapped value.
     */
    public void shouldMatch(String matcherName, Object... args) {
        Matcher<T> matcher = registry.matcherFor(matcherName);
        MatchResult result = matcher.evaluate(value, args);
        assertPassed(result);
    }

    private static void assertPassed(MatchResult result) {
        if (result.isFailed()) {
            throw new AssertionError(result.failureMessage());
        }
    }

    private static boolean contains(Object actual, Object expected) {
        if (actual == null) {
            return false;
        }
        if (actual instanceof CharSequence) {
            return expected != null && actual.toString().contains(String.valueOf(expected));
        }
        if (actual instanceof Collection<?>) {
            return ((Collection<?>) actual).contains(expected);
        }
        if (actual instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) actual;
            return map.containsKey(expected) || map.containsValue(expected);
        }
        Class<?> actualClass = actual.getClass();
        if (actualClass.isArray()) {
            int length = Array.getLength(actual);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(actual, i);
                if (element == null ? expected == null : element.equals(expected)) {
                    return true;
                }
            }
            return false;
        }
        if (actual instanceof Iterable<?>) {
            for (Object element : (Iterable<?>) actual) {
                if (element == null ? expected == null : element.equals(expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int countOf(Object actual) {
        if (actual == null) {
            throw new AssertionError("Expected a countable value but got null");
        }
        if (actual instanceof CharSequence) {
            return ((CharSequence) actual).length();
        }
        if (actual instanceof Collection<?>) {
            return ((Collection<?>) actual).size();
        }
        if (actual instanceof Map<?, ?>) {
            return ((Map<?, ?>) actual).size();
        }
        Class<?> actualClass = actual.getClass();
        if (actualClass.isArray()) {
            return Array.getLength(actual);
        }
        if (actual instanceof Iterable<?>) {
            int count = 0;
            for (Object ignored : (Iterable<?>) actual) {
                count++;
            }
            return count;
        }
        throw new AssertionError("Expected a countable value but got " + typeName(actual));
    }

    /**
     * Whether the value is a generic {@link Iterable} without a constant-time size, i.e. not a
     * {@link Collection}. Such iterables may be infinite and must only be consumed with a bounded
     * number of iteration steps.
     */
    private static boolean isGenericIterable(Object actual) {
        return actual instanceof Iterable<?> && !(actual instanceof Collection<?>);
    }

    /**
     * Determines whether a countable value is empty. Generic iterables are probed with a single
     * {@code iterator().hasNext()} call instead of being fully consumed; all other countable types
     * use their constant-time {@link #countOf(Object)} result. A {@code null} value fails with the
     * standard "Expected a countable value but got null" error from {@link #countOf(Object)}.
     */
    private static boolean isEmptyValue(Object actual) {
        if (isGenericIterable(actual)) {
            return !((Iterable<?>) actual).iterator().hasNext();
        }
        return countOf(actual) == 0;
    }

    /**
     * Counts the elements of an iterable, consuming at most {@code limit} elements. Returns
     * {@code limit} when the iterable has {@code limit} or more elements, making the count safe
     * on infinite iterables.
     */
    private static int boundedCountOf(Iterable<?> actual, int limit) {
        int count = 0;
        Iterator<?> iterator = actual.iterator();
        while (count < limit && iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    private static Map<?, ?> mapOf(Object actual) {
        if (!(actual instanceof Map<?, ?>)) {
            throw new AssertionError("Expected a map but got " + typeName(actual));
        }
        return (Map<?, ?>) actual;
    }

    private static String typeName(Object actual) {
        if (actual == null) {
            return "null";
        }
        return actual.getClass().getName();
    }
}
