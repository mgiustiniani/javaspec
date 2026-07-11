package io.github.jvmspec.matcher;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MatchResultTest {
    @Test
    public void passedResultHasPassedTrue() {
        MatchResult result = MatchResult.passed();
        assertTrue(result.isPassed());
        assertFalse(result.isFailed());
        assertEquals("", result.failureMessage());
    }

    @Test
    public void failureResultHasMessage() {
        MatchResult result = MatchResult.failure("expected A but got B");
        assertFalse(result.isPassed());
        assertTrue(result.isFailed());
        assertEquals("expected A but got B", result.failureMessage());
    }

    @Test(expected = NullPointerException.class)
    public void failureMessageMustNotBeNull() {
        MatchResult.failure(null);
    }

    @Test
    public void equalsAndHashCode() {
        MatchResult a = MatchResult.passed();
        MatchResult b = MatchResult.passed();
        MatchResult c = MatchResult.failure("error");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("string"));
    }

    @Test
    public void toStringContainsStatus() {
        assertTrue(MatchResult.passed().toString().contains("passed"));
        assertTrue(MatchResult.failure("error").toString().contains("error"));
    }
}
