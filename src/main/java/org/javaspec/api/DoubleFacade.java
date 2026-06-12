package org.javaspec.api;

import org.javaspec.doubles.Call;
import org.javaspec.doubles.DoubleControl;
import org.javaspec.doubles.Doubles;
import org.javaspec.doubles.InterfaceDouble;

import java.util.List;

/**
 * Facade over the javaspec doubles API ({@link Doubles}).
 * <p>Extracted from {@link ObjectBehavior} to reduce responsibility concentration.
 * Provides double creation, control, and verification methods.</p>
 */
public class DoubleFacade {

    /**
     * Creates a zero-dependency proxy double for an interface type.
     */
    public <D> D doubleFor(Class<D> interfaceType) {
        return Doubles.create(interfaceType);
    }

    /**
     * Creates a typed double handle containing both proxy and control API.
     */
    public <D> InterfaceDouble<D> interfaceDouble(Class<D> interfaceType) {
        return Doubles.interfaceDouble(interfaceType);
    }

    /**
     * Returns the stubbing, verification, and inspection API for a javaspec double.
     */
    public DoubleControl doubleControl(Object doubleInstance) {
        return Doubles.control(doubleInstance);
    }

    /**
     * Alias for {@link #doubleControl(Object)}.
     */
    public DoubleControl inspectDouble(Object doubleInstance) {
        return doubleControl(doubleInstance);
    }

    /**
     * Returns all recorded calls for a javaspec double.
     */
    public List<Call> doubleCalls(Object doubleInstance) {
        return doubleControl(doubleInstance).calls();
    }

    /**
     * Returns recorded calls for a method on a javaspec double.
     */
    public List<Call> doubleCalls(Object doubleInstance, String methodName) {
        return doubleControl(doubleInstance).calls(methodName);
    }

    /**
     * Returns recorded calls for a method and exact arguments on a javaspec double.
     */
    public List<Call> doubleCalls(Object doubleInstance, String methodName, Object... exactArguments) {
        return doubleControl(doubleInstance).calls(methodName, exactArguments);
    }

    /**
     * Returns the number of calls for a method on a javaspec double.
     */
    public int doubleCallCount(Object doubleInstance, String methodName) {
        return doubleControl(doubleInstance).callCount(methodName);
    }

    /**
     * Returns the number of calls for a method and exact arguments on a javaspec double.
     */
    public int doubleCallCount(Object doubleInstance, String methodName, Object... exactArguments) {
        return doubleControl(doubleInstance).callCount(methodName, exactArguments);
    }

    /**
     * Returns the number of calls for a method and exact arguments on a javaspec double.
     */
    public int doubleCallCountFor(Object doubleInstance, String methodName, Object... exactArguments) {
        return doubleControl(doubleInstance).callCountFor(methodName, exactArguments);
    }

    /**
     * Asserts that a method on a javaspec double was called at least once.
     */
    public void shouldHaveBeenCalled(Object doubleInstance, String methodName) {
        doubleControl(doubleInstance).verifyCalled(methodName);
    }

    /**
     * Asserts that a method on a javaspec double was called at least once with exact arguments.
     */
    public void shouldHaveBeenCalled(Object doubleInstance, String methodName, Object... exactArguments) {
        doubleControl(doubleInstance).verifyCalled(methodName, exactArguments);
    }

    /**
     * Asserts that a method on a javaspec double was called at least once with exact arguments.
     */
    public void shouldHaveBeenCalledWith(Object doubleInstance, String methodName, Object... exactArguments) {
        doubleControl(doubleInstance).verifyCalledWith(methodName, exactArguments);
    }

    /**
     * Asserts that a method on a javaspec double was not called.
     */
    public void shouldNotHaveBeenCalled(Object doubleInstance, String methodName) {
        doubleControl(doubleInstance).verifyNotCalled(methodName);
    }

    /**
     * Asserts that a method on a javaspec double was not called with exact arguments.
     */
    public void shouldNotHaveBeenCalled(Object doubleInstance, String methodName, Object... exactArguments) {
        doubleControl(doubleInstance).verifyNotCalled(methodName, exactArguments);
    }

    /**
     * Asserts that a method on a javaspec double was not called with exact arguments.
     */
    public void shouldNotHaveBeenCalledWith(Object doubleInstance, String methodName, Object... exactArguments) {
        doubleControl(doubleInstance).verifyNotCalledWith(methodName, exactArguments);
    }

    /**
     * Asserts an exact call count for a method on a javaspec double.
     */
    public void shouldHaveBeenCalledTimes(Object doubleInstance, String methodName, int expectedCount) {
        doubleControl(doubleInstance).verifyCallCount(methodName, expectedCount);
    }

    /**
     * Asserts an exact call count for a method and exact arguments on a javaspec double.
     */
    public void shouldHaveBeenCalledTimes(
            Object doubleInstance,
            String methodName,
            int expectedCount,
            Object... exactArguments
    ) {
        doubleControl(doubleInstance).verifyCallCountFor(methodName, expectedCount, exactArguments);
    }
}
