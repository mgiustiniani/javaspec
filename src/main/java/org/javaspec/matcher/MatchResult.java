package org.javaspec.matcher;

import java.util.Objects;

/**
 * Immutable result of a matcher evaluation.
 * Carries pass/fail status and a descriptive failure message.
 */
public final class MatchResult {
    private final boolean passed;
    private final String failureMessage;

    private MatchResult(boolean passed, String failureMessage) {
        this.passed = passed;
        this.failureMessage = Objects.requireNonNull(failureMessage, "failureMessage must not be null");
    }

    public static MatchResult passed() {
        return new MatchResult(true, "");
    }

    public static MatchResult failure(String failureMessage) {
        return new MatchResult(false, Objects.requireNonNull(failureMessage, "failureMessage must not be null"));
    }

    public boolean isPassed() {
        return passed;
    }

    public boolean isFailed() {
        return !passed;
    }

    public String failureMessage() {
        return failureMessage;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MatchResult)) return false;
        MatchResult that = (MatchResult) other;
        return passed == that.passed && failureMessage.equals(that.failureMessage);
    }

    @Override
    public int hashCode() {
        int result = (passed ? 1 : 0);
        result = 31 * result + failureMessage.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (passed) {
            return "MatchResult{passed}";
        }
        return "MatchResult{failed: '" + failureMessage + "'}";
    }
}
