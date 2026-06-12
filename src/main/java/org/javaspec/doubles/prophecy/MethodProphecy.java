package org.javaspec.doubles.prophecy;

import org.javaspec.doubles.DoubleControl;
import org.javaspec.doubles.StubAnswer;

import java.util.Objects;

/**
 * A prophecy about a specific method call on a prophesized object.
 * <p>
 * Provides stub setup ({@code willReturn}, {@code willThrow}, {@code will}) and
 * prediction setup ({@code shouldBeCalled}, {@code shouldNotBeCalled},
 * {@code shouldBeCalledTimes}) for a named method with specific arguments.
 * </p>
 *
 * @param <R> the return type of the method
 */
public final class MethodProphecy<R> {
    private final DoubleControl control;
    private final String methodName;
    private final Object[] arguments;
    private final PredictionRegistry registry;
    private Promise<R> promise;
    private Prediction prediction;

    /**
     * Creates a method prophecy for the given control, method name, and arguments.
     *
     * @param control    the double control for stubbing/verification
     * @param methodName the method name
     * @param arguments  the method arguments (may contain {@code ArgumentMatcher} instances)
     */
    public MethodProphecy(DoubleControl control, String methodName, Object... arguments) {
        this(control, null, methodName, arguments);
    }

    /**
     * Creates a method prophecy with an optional prediction registry.
     *
     * @param control    the double control for stubbing/verification
     * @param registry   the prediction registry (may be null)
     * @param methodName the method name
     * @param arguments  the method arguments (may contain {@code ArgumentMatcher} instances)
     */
    public MethodProphecy(DoubleControl control, PredictionRegistry registry, String methodName, Object... arguments) {
        this.control = Objects.requireNonNull(control, "control must not be null");
        this.registry = registry;
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.arguments = arguments.clone();
    }

    /**
     * Configures this method prophecy to return the supplied value when invoked.
     *
     * @param value the return value
     * @return this method prophecy (for chaining)
     */
    public MethodProphecy<R> willReturn(R value) {
        this.promise = Promise.willReturn(value);
        applyPromise();
        return this;
    }

    /**
     * Configures this method prophecy to throw the supplied throwable when invoked.
     *
     * @param throwable the throwable to throw
     * @return this method prophecy (for chaining)
     */
    public MethodProphecy<R> willThrow(Throwable throwable) {
        this.promise = Promise.willThrow(throwable);
        applyPromise();
        return this;
    }

    /**
     * Configures this method prophecy to use a custom answer when invoked.
     *
     * @param answer the stub answer
     * @return this method prophecy (for chaining)
     */
    public MethodProphecy<R> will(StubAnswer answer) {
        this.promise = Promise.will(answer);
        applyPromise();
        return this;
    }

    /**
     * Sets a prediction that this method should have been called at least once.
     *
     * @return this method prophecy (for chaining)
     */
    public MethodProphecy<R> shouldBeCalled() {
        this.prediction = new Prediction(methodName, arguments, PredictionMode.CALLED);
        registerPrediction();
        return this;
    }

    /**
     * Sets a prediction that this method should not have been called.
     *
     * @return this method prophecy (for chaining)
     */
    public MethodProphecy<R> shouldNotBeCalled() {
        this.prediction = new Prediction(methodName, arguments, PredictionMode.NOT_CALLED);
        registerPrediction();
        return this;
    }

    /**
     * Sets a prediction that this method should have been called exactly the specified number of times.
     *
     * @param times the expected call count
     * @return this method prophecy (for chaining)
     */
    public MethodProphecy<R> shouldBeCalledTimes(int times) {
        this.prediction = new Prediction(methodName, arguments, PredictionMode.CALLED_TIMES, times);
        registerPrediction();
        return this;
    }

    /**
     * Returns the configured promise, or {@code null} if no promise was set.
     */
    Promise<R> promise() {
        return promise;
    }

    /**
     * Returns the configured prediction, or {@code null} if no prediction was set.
     */
    Prediction prediction() {
        return prediction;
    }

    /**
     * Returns the method name.
     */
    String methodName() {
        return methodName;
    }

    /**
     * Returns the method arguments.
     */
    Object[] arguments() {
        return arguments.clone();
    }

    private void registerPrediction() {
        if (registry != null && prediction != null) {
            registry.register(control, prediction);
        }
    }

    private void applyPromise() {
        if (promise.isReturn()) {
            control.when(methodName, arguments).thenReturn(promise.value());
        } else if (promise.isThrow()) {
            control.when(methodName, arguments).thenThrow(promise.throwable());
        } else if (promise.isAnswer()) {
            control.when(methodName, arguments).thenAnswer(promise.answer());
        }
    }
}
