package org.javaspec.runner;

/**
 * Outcome for one discovered specification example.
 */
public enum ExampleStatus {
    PASSED,
    FAILED,
    BROKEN,
    SKIPPED,
    PENDING;

    public boolean isPassed() {
        return PASSED.equals(this);
    }

    public boolean isFailed() {
        return FAILED.equals(this);
    }

    public boolean isBroken() {
        return BROKEN.equals(this);
    }

    public boolean isSkipped() {
        return SKIPPED.equals(this);
    }

    public boolean isPending() {
        return PENDING.equals(this);
    }

    public boolean isSkippedOrPending() {
        return isSkipped() || isPending();
    }

    public boolean isNonExecuted() {
        return isSkippedOrPending();
    }

    public boolean isFailure() {
        return FAILED.equals(this) || BROKEN.equals(this);
    }
}
