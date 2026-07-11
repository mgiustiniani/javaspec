package io.github.jvmspec.matcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of named {@link Matcher} instances.
 * <p>
 * Built-in matchers (identity, equality, negated-equality, negated-identity) are available by default.
 * Custom matchers can be added via {@link #register(String, Matcher)}.
 * A JUnit-based implementation could swap the entire registry.
 * </p>
 */
public final class MatcherRegistry {
    private static final Matcher<Object> NEGATED_EQUALITY = new Matcher<Object>() {
        @Override
        public MatchResult evaluate(Object subject, Object... expected) {
            Objects.requireNonNull(expected, "expected must not be null");
            if (expected.length == 0) {
                return MatchResult.failure("Expected a value to compare against but none was provided");
            }
            boolean equal = subject == null ? expected[0] == null : subject.equals(expected[0]);
            if (!equal) {
                return MatchResult.passed();
            }
            return MatchResult.failure("Expected value not to equal " + expected[0] + " but it did");
        }

        @Override
        public String name() {
            return "negated-equality";
        }
    };

    private final Map<String, Matcher<?>> matchers;

    private MatcherRegistry(Map<String, Matcher<?>> matchers) {
        this.matchers = Objects.requireNonNull(matchers, "matchers must not be null");
    }

    public static MatcherRegistry createWithDefaults() {
        Map<String, Matcher<?>> map = new LinkedHashMap<String, Matcher<?>>();
        map.put("identity", new CustomMatcher<Object>("identity", new CustomMatcher.MatchPredicate<Object>() {
            @Override
            public boolean test(Object subject, Object... expected) {
                if (expected.length == 0) return false;
                return subject == expected[0];
            }
        }));
        map.put("equality", new CustomMatcher<Object>("equality", new CustomMatcher.MatchPredicate<Object>() {
            @Override
            public boolean test(Object subject, Object... expected) {
                if (expected.length == 0) return false;
                if (subject == null) return expected[0] == null;
                return subject.equals(expected[0]);
            }
        }));
        map.put("negated-identity", new CustomMatcher<Object>("negated-identity", new CustomMatcher.MatchPredicate<Object>() {
            @Override
            public boolean test(Object subject, Object... expected) {
                if (expected.length == 0) return false;
                return subject != expected[0];
            }
        }));
        return new MatcherRegistry(map);
    }

    /**
     * Register a custom matcher by name.
     */
    public void register(String name, Matcher<?> matcher) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(matcher, "matcher must not be null");
        matchers.put(name, matcher);
    }

    /**
     * Retrieve a matcher by name.
     *
     * @throws IllegalArgumentException if no matcher is registered for the given name
     */
    @SuppressWarnings("unchecked")
    public <T> Matcher<T> matcherFor(String name) {
        Objects.requireNonNull(name, "name must not be null");
        Matcher<?> matcher = matchers.get(name);
        if (matcher == null && "negated-equality".equals(name)) {
            matcher = NEGATED_EQUALITY;
        }
        if (matcher == null) {
            throw new IllegalArgumentException("No matcher registered for '" + name + "'");
        }
        return (Matcher<T>) matcher;
    }

    /**
     * Returns the number of explicitly registered matchers.
     */
    public int size() {
        return matchers.size();
    }

    /**
     * Returns true if a matcher is available for the given name.
     */
    public boolean contains(String name) {
        return matchers.containsKey(name) || "negated-equality".equals(name);
    }
}
