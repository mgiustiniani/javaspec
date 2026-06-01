package org.javaspec.doubles;

import java.util.List;
import java.util.Objects;

/**
 * A typed handle for an interface proxy plus its stubbing and verification API.
 *
 * @param <T> the doubled interface type
 */
public final class InterfaceDouble<T> {
    private final Class<T> interfaceType;
    private final T instance;
    private final DoubleControl control;

    InterfaceDouble(Class<T> interfaceType, T instance, DoubleControl control) {
        this.interfaceType = Objects.requireNonNull(interfaceType, "interfaceType must not be null");
        this.instance = Objects.requireNonNull(instance, "instance must not be null");
        this.control = Objects.requireNonNull(control, "control must not be null");
    }

    /**
     * Returns the doubled interface type.
     */
    public Class<T> interfaceType() {
        return interfaceType;
    }

    /**
     * Returns the proxy instance to pass to the subject under specification.
     */
    public T instance() {
        return instance;
    }

    /**
     * Alias for {@link #instance()}.
     */
    public T proxy() {
        return instance();
    }

    /**
     * Returns the stubbing, verification, and inspection API for this double.
     */
    public DoubleControl control() {
        return control;
    }

    public MethodStub when(String methodName) {
        return control.when(methodName);
    }

    public MethodStub when(String methodName, Object... exactArguments) {
        return control.when(methodName, exactArguments);
    }

    public InterfaceDouble<T> returns(String methodName, Object returnValue) {
        control.returns(methodName, returnValue);
        return this;
    }

    public InterfaceDouble<T> returnsFor(String methodName, Object returnValue, Object... exactArguments) {
        control.returnsFor(methodName, returnValue, exactArguments);
        return this;
    }

    public CallVerifier verify(String methodName) {
        return control.verify(methodName);
    }

    public CallVerifier verify(String methodName, Object... exactArguments) {
        return control.verify(methodName, exactArguments);
    }

    public int callCount(String methodName) {
        return control.callCount(methodName);
    }

    public int callCount(String methodName, Object... exactArguments) {
        return control.callCount(methodName, exactArguments);
    }

    public int callCountFor(String methodName, Object... exactArguments) {
        return control.callCountFor(methodName, exactArguments);
    }

    public boolean wasCalled(String methodName) {
        return control.wasCalled(methodName);
    }

    public boolean wasCalledWith(String methodName, Object... exactArguments) {
        return control.wasCalledWith(methodName, exactArguments);
    }

    public void verifyCalled(String methodName) {
        control.verifyCalled(methodName);
    }

    public void verifyCalled(String methodName, Object... exactArguments) {
        control.verifyCalled(methodName, exactArguments);
    }

    public void verifyCalledWith(String methodName, Object... exactArguments) {
        control.verifyCalledWith(methodName, exactArguments);
    }

    public void verifyNotCalled(String methodName) {
        control.verifyNotCalled(methodName);
    }

    public void verifyNotCalled(String methodName, Object... exactArguments) {
        control.verifyNotCalled(methodName, exactArguments);
    }

    public void verifyNotCalledWith(String methodName, Object... exactArguments) {
        control.verifyNotCalledWith(methodName, exactArguments);
    }

    public void verifyCallCount(String methodName, int expectedCount) {
        control.verifyCallCount(methodName, expectedCount);
    }

    public void verifyCallCount(String methodName, int expectedCount, Object... exactArguments) {
        control.verifyCallCount(methodName, expectedCount, exactArguments);
    }

    public void verifyCallCountFor(String methodName, int expectedCount, Object... exactArguments) {
        control.verifyCallCountFor(methodName, expectedCount, exactArguments);
    }

    public List<Call> calls() {
        return control.calls();
    }

    public List<Call> calls(String methodName) {
        return control.calls(methodName);
    }

    public List<Call> calls(String methodName, Object... exactArguments) {
        return control.calls(methodName, exactArguments);
    }

    public List<Call> callsFor(String methodName, Object... exactArguments) {
        return control.callsFor(methodName, exactArguments);
    }

    public void clearCalls() {
        control.clearCalls();
    }

    public void clearStubs() {
        control.clearStubs();
    }

    public void reset() {
        control.reset();
    }

    public void resetCalls() {
        control.resetCalls();
    }

    public void resetStubs() {
        control.resetStubs();
    }
}
