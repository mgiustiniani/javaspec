package org.javaspec.doubles;

/**
 * Fluent call verifier for a method on an interface double.
 */
public final class CallVerifier {
    private final DoubleControl control;
    private final MethodPattern pattern;

    CallVerifier(DoubleControl control, MethodPattern pattern) {
        this.control = control;
        this.pattern = pattern;
    }

    /**
     * Returns the number of matching calls.
     */
    public int count() {
        return control.count(pattern);
    }

    /**
     * Verifies that at least one matching call was recorded.
     */
    public void called() {
        control.verifyCalled(pattern);
    }

    /**
     * Verifies that no matching call was recorded.
     */
    public void notCalled() {
        control.verifyNotCalled(pattern);
    }

    /**
     * Verifies that exactly one matching call was recorded.
     */
    public void calledOnce() {
        times(1);
    }

    /**
     * Verifies that exactly the expected number of matching calls was recorded.
     */
    public void times(int expectedCount) {
        control.verifyCallCount(pattern, expectedCount);
    }
}
