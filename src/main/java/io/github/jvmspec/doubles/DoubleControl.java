package io.github.jvmspec.doubles;

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
     * Starts an argument-constrained stub for the named method.
     * Ordinary values are matched exactly; {@link ArgumentMatcher} values are matched by callback.
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
     * Creates a verifier for calls to the named method with constrained arguments.
     * Ordinary values are matched exactly; {@link ArgumentMatcher} values are matched by callback.
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
     * Verifies that the named methods were called in the given order (by their first occurrence
     * in the call history).  All named methods must have been called; the check compares the
     * index of the first observed call for each method name.
     *
     * @param methodNames the method names to verify in order
     * @throws AssertionError if any method was not called, or if the order is wrong
     */
    public void verifyInOrder(String... methodNames) {
        if (methodNames == null || methodNames.length == 0) {
            return;
        }
        List<Call> history = handler.calls();
        int[] firstCallIndex = new int[methodNames.length];
        java.util.Arrays.fill(firstCallIndex, -1);
        for (int ci = 0; ci < history.size(); ci++) {
            String calledName = history.get(ci).methodName();
            for (int mi = 0; mi < methodNames.length; mi++) {
                if (firstCallIndex[mi] == -1 && methodNames[mi].equals(calledName)) {
                    firstCallIndex[mi] = ci;
                }
            }
        }
        for (int mi = 0; mi < methodNames.length; mi++) {
            if (firstCallIndex[mi] < 0) {
                throw new AssertionError("Expected method '" + methodNames[mi]
                        + "' to have been called (verifyInOrder), but it was not called. Recorded calls: "
                        + describeCalls(history));
            }
        }
        for (int mi = 1; mi < methodNames.length; mi++) {
            if (firstCallIndex[mi] <= firstCallIndex[mi - 1]) {
                throw new AssertionError("Expected '" + methodNames[mi - 1]
                        + "' to be called before '" + methodNames[mi]
                        + "' (verifyInOrder), but the recorded order was wrong. Recorded calls: "
                        + describeCalls(history));
            }
        }
    }

    /**
     * Verifies that {@code firstMethod} was called (at least once) before {@code secondMethod}
     * (at least once), comparing the first occurrence of each.
     *
     * @param firstMethod  the method that must have been called first
     * @param secondMethod the method that must have been called after
     * @throws AssertionError if either method was not called, or if the order is wrong
     */
    public void verifyCalledBefore(String firstMethod, String secondMethod) {
        verifyInOrder(firstMethod, secondMethod);
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

    void addSequentialStub(MethodPattern pattern, Object[] returnValues) {
        handler.addSequentialStub(pattern, returnValues);
    }

    void addThrowingStub(MethodPattern pattern, Throwable throwable) {
        handler.addThrowingStub(pattern, throwable);
    }

    void addAnswerStub(MethodPattern pattern, StubAnswer answer) {
        handler.addAnswerStub(pattern, answer);
    }

    void addAnswerSequenceStub(MethodPattern pattern, StubAnswer[] answers) {
        handler.addAnswerSequenceStub(pattern, answers);
    }

    void addReturningSequenceThenThrowingStub(
            MethodPattern pattern, Object[] returnValues, Throwable onExhaust) {
        handler.addReturningSequenceThenThrowingStub(pattern, returnValues, onExhaust);
    }

    int count(MethodPattern pattern) {
        return handler.count(pattern);
    }

    void verifyCalled(MethodPattern pattern) {
        int count = count(pattern);
        if (count == 0) {
            throw new AssertionError("Expected " + pattern.describe()
                    + " to have been called, but it was not called"
                    + argumentMismatchHint(pattern)
                    + ". Recorded calls: " + describeCalls(handler.calls()));
        }
    }

    void verifyNotCalled(MethodPattern pattern) {
        int count = count(pattern);
        if (count != 0) {
            throw new AssertionError("Expected " + pattern.describe() + " not to have been called, but it was called "
                    + count + " time(s). Matching calls: " + describeCalls(handler.calls(pattern)));
        }
    }

    void verifyCallCount(MethodPattern pattern, int expectedCount) {
        if (expectedCount < 0) {
            throw new IllegalArgumentException("Expected call count must not be negative: " + expectedCount);
        }
        int actualCount = count(pattern);
        if (actualCount != expectedCount) {
            throw new AssertionError("Expected " + pattern.describe() + " to be called " + expectedCount
                    + " time(s), but it was called " + actualCount + " time(s). Matching calls: "
                    + describeCalls(handler.calls(pattern)) + ". Recorded calls: " + describeCalls(handler.calls()));
        }
    }

    private String argumentMismatchHint(MethodPattern pattern) {
        if (!pattern.argumentConstrained()) {
            return "";
        }
        List<Call> sameMethodCalls = handler.calls(MethodPattern.anyArguments(pattern.methodName()));
        if (sameMethodCalls.isEmpty()) {
            return "";
        }
        return "; method was called with different arguments: " + describeCalls(sameMethodCalls);
    }

    private static String describeCalls(List<Call> calls) {
        if (calls == null || calls.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        int limit = Math.min(calls.size(), 5);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(calls.get(i));
        }
        if (calls.size() > limit) {
            builder.append(", ... ").append(calls.size() - limit).append(" more");
        }
        builder.append(']');
        return builder.toString();
    }
}
