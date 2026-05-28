package org.javaspec.matcher;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CustomMatcherTest {
    @Test
    public void passesWhenPredicateReturnsTrue() {
        CustomMatcher<Integer> matcher = new CustomMatcher<Integer>("isPositive", new CustomMatcher.MatchPredicate<Integer>() {
            @Override
            public boolean test(Integer subject, Object... expected) {
                return subject > 0;
            }
        });

        MatchResult result = matcher.evaluate(5);
        assertTrue(result.isPassed());
    }

    @Test
    public void failsWhenPredicateReturnsFalse() {
        CustomMatcher<Integer> matcher = new CustomMatcher<Integer>("isPositive", new CustomMatcher.MatchPredicate<Integer>() {
            @Override
            public boolean test(Integer subject, Object... expected) {
                return subject > 0;
            }
        });

        MatchResult result = matcher.evaluate(-1);
        assertFalse(result.isPassed());
        assertTrue(result.failureMessage().contains("isPositive"));
    }

    @Test
    public void handlesExpectedArguments() {
        CustomMatcher<Integer> matcher = new CustomMatcher<Integer>("greaterThan", new CustomMatcher.MatchPredicate<Integer>() {
            @Override
            public boolean test(Integer subject, Object... expected) {
                if (expected.length == 0) return false;
                return subject > (Integer) expected[0];
            }
        });

        assertTrue(matcher.evaluate(10, 5).isPassed());
        assertFalse(matcher.evaluate(3, 5).isPassed());
    }

    @Test
    public void acceptsNullSubjectAndDelegatesToPredicate() {
        CustomMatcher<String> matcher = new CustomMatcher<String>("isNullWithMarker", new CustomMatcher.MatchPredicate<String>() {
            @Override
            public boolean test(String subject, Object... expected) {
                return subject == null && expected.length == 1 && "marker".equals(expected[0]);
            }
        });

        MatchResult result = matcher.evaluate(null, "marker");

        assertTrue(result.isPassed());
    }

    @Test
    public void catchesExceptionsAndReportsFailure() {
        CustomMatcher<Integer> matcher = new CustomMatcher<Integer>("crash", new CustomMatcher.MatchPredicate<Integer>() {
            @Override
            public boolean test(Integer subject, Object... expected) {
                throw new RuntimeException("boom");
            }
        });

        MatchResult result = matcher.evaluate(1);
        assertFalse(result.isPassed());
        assertTrue(result.failureMessage().contains("crash"));
        assertTrue(result.failureMessage().contains("RuntimeException"));
    }

    @Test
    public void returnsName() {
        CustomMatcher<Integer> matcher = new CustomMatcher<Integer>("myMatcher", new CustomMatcher.MatchPredicate<Integer>() {
            @Override
            public boolean test(Integer subject, Object... expected) {
                return true;
            }
        });

        assertEquals("myMatcher", matcher.name());
    }
}
