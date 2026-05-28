package org.javaspec.matcher;

import java.lang.reflect.Array;
import java.util.Collection;
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
        boolean equal = value == null ? unexpected == null : value.equals(unexpected);
        if (equal) {
            throw new AssertionError("Expected value not to equal " + unexpected + " but it did");
        }
    }

    /**
     * Alias for {@link #shouldNotEqual(Object)} using PHPSpec return terminology.
     */
    public void shouldNotReturn(T unexpected) {
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

    private static String typeName(Object actual) {
        if (actual == null) {
            return "null";
        }
        return actual.getClass().getName();
    }
}
