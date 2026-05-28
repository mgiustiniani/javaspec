package org.javaspec.matcher;

import java.util.Objects;

/**
 * Concrete custom matcher implementation that uses a simple predicate.
 * <p>
 * This is the default matcher implementation. It can be replaced by
 * a JUnit-based implementation later without changing the {@link Matcher}
 * interface contract.
 * </p>
 *
 * @param <T> the type of value this matcher evaluates
 */
public final class CustomMatcher<T> implements Matcher<T> {
    private final String name;
    private final MatchPredicate<T> predicate;

    public CustomMatcher(String name, MatchPredicate<T> predicate) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
    }

    @Override
    public MatchResult evaluate(T subject, Object... expected) {
        Objects.requireNonNull(expected, "expected must not be null");
        try {
            if (predicate.test(subject, expected)) {
                return MatchResult.passed();
            }
            return MatchResult.failure(failureMessage(subject, expected));
        } catch (Exception ex) {
            return MatchResult.failure(
                    name + " threw " + ex.getClass().getSimpleName()
                    + ": " + (ex.getMessage() != null ? ex.getMessage() : "")
            );
        }
    }

    @Override
    public String name() {
        return name;
    }

    private String failureMessage(T subject, Object... expected) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected ").append(name).append("(");
        appendExpected(sb, expected);
        sb.append(") but ");
        if (subject instanceof Object[]) {
            sb.append("got [");
            Object[] arr = (Object[]) subject;
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(arr[i]);
            }
            sb.append("]");
        } else {
            sb.append("got ").append(subject);
        }
        return sb.toString();
    }

    private static void appendExpected(StringBuilder sb, Object... expected) {
        for (int i = 0; i < expected.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(expected[i]);
        }
    }

    @FunctionalInterface
    public interface MatchPredicate<T> {
        boolean test(T subject, Object... expected);
    }
}
