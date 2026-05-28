package org.javaspec.matcher;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MatcherRegistryTest {
    @Test
    public void hasDefaultMatchers() {
        MatcherRegistry registry = MatcherRegistry.createWithDefaults();

        assertTrue(registry.contains("identity"));
        assertTrue(registry.contains("equality"));
        assertTrue(registry.contains("negated-identity"));
    }

    @Test
    public void registersCustomMatcher() {
        MatcherRegistry registry = MatcherRegistry.createWithDefaults();
        CustomMatcher<String> matcher = new CustomMatcher<String>("length", new CustomMatcher.MatchPredicate<String>() {
            @Override
            public boolean test(String subject, Object... expected) {
                if (expected.length == 0) return false;
                return subject.length() == (Integer) expected[0];
            }
        });

        registry.register("length", matcher);
        assertTrue(registry.contains("length"));
        assertEquals(matcher, registry.matcherFor("length"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsForUnknownMatcher() {
        MatcherRegistry registry = MatcherRegistry.createWithDefaults();
        registry.matcherFor("nonexistent");
    }

    @Test
    public void identityMatcherPassesForSameReference() {
        MatcherRegistry registry = MatcherRegistry.createWithDefaults();
        Object obj = new Object();
        Object sameRef = obj;

        Matcher<Object> identity = registry.matcherFor("identity");
        assertTrue(identity.evaluate(obj, sameRef).isPassed());
        assertFalse(identity.evaluate(obj, new Object()).isPassed());
    }

    @Test
    public void equalityMatcherPassesForEqualValues() {
        MatcherRegistry registry = MatcherRegistry.createWithDefaults();
        String a = "hello";
        String b = "hello";

        Matcher<Object> equality = registry.matcherFor("equality");
        assertTrue(equality.evaluate(a, b).isPassed());
        assertFalse(equality.evaluate(a, "world").isPassed());
    }

    @Test
    public void negatedIdentityMatcherPassesForDifferentReferences() {
        MatcherRegistry registry = MatcherRegistry.createWithDefaults();
        String a = "hello";
        String b = "world";

        Matcher<Object> negated = registry.matcherFor("negated-identity");
        assertTrue(negated.evaluate(a, b).isPassed());
        assertFalse(negated.evaluate(a, a).isPassed());
    }

    @Test
    public void reportsSize() {
        MatcherRegistry registry = MatcherRegistry.createWithDefaults();
        assertEquals(3, registry.size());
        registry.register("custom", new CustomMatcher<Object>("custom", new CustomMatcher.MatchPredicate<Object>() {
            @Override
            public boolean test(Object subject, Object... expected) {
                return true;
            }
        }));
        assertEquals(4, registry.size());
    }
}
