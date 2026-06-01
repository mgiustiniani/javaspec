package org.javaspec.doubles;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stubbing, verification, and inspection API for a javaspec interface double.
 */
public final class DoubleControl {
    private final DoubleInvocationHandler handler;

    DoubleControl(DoubleInvocationHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
    }

    /**
     * Starts a method-name stub. The stub matches calls to the named method with any arguments.
     */
    public MethodStub when(String methodName) {
        return new MethodStub(this, MethodPattern.anyArguments(methodName));
    }

    /**
     * Starts an exact-arguments stub for the named method.
     */
    public MethodStub when(String methodName, Object... exactArguments) {
        return new MethodStub(this, MethodPattern.exactArguments(methodName, exactArguments));
    }

    /**
     * Stubs the named method, with any arguments, to return the supplied value.
     */
    public DoubleControl returns(String methodName, Object returnValue) {
        return when(methodName).thenReturn(returnValue);
    }

    /**
     * Stubs the named method and exact arguments to return the supplied value.
     */
    public DoubleControl returnsFor(String methodName, Object returnValue, Object... exactArguments) {
        return when(methodName, exactArguments).thenReturn(returnValue);
    }

    /**
     * Creates a verifier for calls to the named method with any arguments.
     */
    public CallVerifier verify(String methodName) {
        return new CallVerifier(this, MethodPattern.anyArguments(methodName));
    }

    /**
     * Creates a verifier for calls to the named method with exact arguments.
     */
    public CallVerifier verify(String methodName, Object... exactArguments) {
        return new CallVerifier(this, MethodPattern.exactArguments(methodName, exactArguments));
    }

    /**
     * Creates a verifier for calls to the named method with exact arguments.
     */
    public CallVerifier verifyFor(String methodName, Object... exactArguments) {
        return verify(methodName, exactArguments);
    }

    /**
     * Returns the number of calls to the named method, regardless of arguments.
     */
    public int callCount(String methodName) {
        return count(MethodPattern.anyArguments(methodName));
    }

    /**
     * Returns the number of calls to the named method with exact arguments.
     */
    public int callCount(String methodName, Object... exactArguments) {
        return count(MethodPattern.exactArguments(methodName, exactArguments));
    }

    /**
     * Returns the number of calls to the named method with exact arguments.
     */
    public int callCountFor(String methodName, Object... exactArguments) {
        return callCount(methodName, exactArguments);
    }

    /**
     * Returns true when the named method was called at least once, regardless of arguments.
     */
    public boolean wasCalled(String methodName) {
        return callCount(methodName) > 0;
    }

    /**
     * Returns true when the named method was called at least once with exact arguments.
     */
    public boolean wasCalledWith(String methodName, Object... exactArguments) {
        return callCountFor(methodName, exactArguments) > 0;
    }

    /**
     * Verifies that the named method was called at least once, regardless of arguments.
     */
    public void verifyCalled(String methodName) {
        verifyCalled(MethodPattern.anyArguments(methodName));
    }

    /**
     * Verifies that the named method was called at least once with exact arguments.
     */
    public void verifyCalled(String methodName, Object... exactArguments) {
        verifyCalled(MethodPattern.exactArguments(methodName, exactArguments));
    }

    /**
     * Verifies that the named method was called at least once with exact arguments.
     */
    public void verifyCalledWith(String methodName, Object... exactArguments) {
        verifyCalled(methodName, exactArguments);
    }

    /**
     * Verifies that the named method was not called, regardless of arguments.
     */
    public void verifyNotCalled(String methodName) {
        verifyNotCalled(MethodPattern.anyArguments(methodName));
    }

    /**
     * Verifies that the named method was not called with exact arguments.
     */
    public void verifyNotCalled(String methodName, Object... exactArguments) {
        verifyNotCalled(MethodPattern.exactArguments(methodName, exactArguments));
    }

    /**
     * Verifies that the named method was not called with exact arguments.
     */
    public void verifyNotCalledWith(String methodName, Object... exactArguments) {
        verifyNotCalled(methodName, exactArguments);
    }

    /**
     * Verifies the exact number of calls to the named method, regardless of arguments.
     */
    public void verifyCallCount(String methodName, int expectedCount) {
        verifyCallCount(MethodPattern.anyArguments(methodName), expectedCount);
    }

    /**
     * Verifies the exact number of calls to the named method with exact arguments.
     */
    public void verifyCallCount(String methodName, int expectedCount, Object... exactArguments) {
        verifyCallCount(MethodPattern.exactArguments(methodName, exactArguments), expectedCount);
    }

    /**
     * Verifies the exact number of calls to the named method with exact arguments.
     */
    public void verifyCallCountFor(String methodName, int expectedCount, Object... exactArguments) {
        verifyCallCount(methodName, expectedCount, exactArguments);
    }

    /**
     * Returns all recorded calls as an immutable snapshot.
     */
    public List<Call> calls() {
        return Collections.unmodifiableList(handler.calls());
    }

    /**
     * Returns calls to the named method, regardless of arguments, as an immutable snapshot.
     */
    public List<Call> calls(String methodName) {
        return Collections.unmodifiableList(handler.calls(MethodPattern.anyArguments(methodName)));
    }

    /**
     * Returns calls to the named method with exact arguments as an immutable snapshot.
     */
    public List<Call> calls(String methodName, Object... exactArguments) {
        return Collections.unmodifiableList(handler.calls(MethodPattern.exactArguments(methodName, exactArguments)));
    }

    /**
     * Returns calls to the named method with exact arguments as an immutable snapshot.
     */
    public List<Call> callsFor(String methodName, Object... exactArguments) {
        return calls(methodName, exactArguments);
    }

    /**
     * Clears recorded calls and keeps stubs.
     */
    public void clearCalls() {
        handler.clearCalls();
    }

    /**
     * Alias for {@link #clearCalls()}.
     */
    public void resetCalls() {
        clearCalls();
    }

    /**
     * Clears configured stubs and keeps call history.
     */
    public void clearStubs() {
        handler.clearStubs();
    }

    /**
     * Alias for {@link #clearStubs()}.
     */
    public void resetStubs() {
        clearStubs();
    }

    /**
     * Clears recorded calls and configured stubs.
     */
    public void reset() {
        clearCalls();
        clearStubs();
    }

    void addStub(MethodPattern pattern, Object returnValue) {
        handler.addStub(pattern, returnValue);
    }

    int count(MethodPattern pattern) {
        return handler.count(pattern);
    }

    void verifyCalled(MethodPattern pattern) {
        int count = count(pattern);
        if (count == 0) {
            throw new AssertionError("Expected " + pattern.describe() + " to have been called, but it was not called");
        }
    }

    void verifyNotCalled(MethodPattern pattern) {
        int count = count(pattern);
        if (count != 0) {
            throw new AssertionError("Expected " + pattern.describe() + " not to have been called, but it was called "
                    + count + " time(s)");
        }
    }

    void verifyCallCount(MethodPattern pattern, int expectedCount) {
        if (expectedCount < 0) {
            throw new IllegalArgumentException("Expected call count must not be negative: " + expectedCount);
        }
        int actualCount = count(pattern);
        if (actualCount != expectedCount) {
            throw new AssertionError("Expected " + pattern.describe() + " to be called " + expectedCount
                    + " time(s), but it was called " + actualCount + " time(s)");
        }
    }
}
