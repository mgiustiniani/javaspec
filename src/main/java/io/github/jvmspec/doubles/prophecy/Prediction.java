package io.github.jvmspec.doubles.prophecy;

import io.github.jvmspec.doubles.DoubleControl;

import java.util.Objects;

/**
 * A prediction that a specific method call should or should not have occurred.
 * <p>
 * Created by {@code shouldBeCalled()}, {@code shouldNotBeCalled()}, or
 * {@code shouldBeCalledTimes(n)} on a {@link MethodProphecy}. Verified when
 * {@link PredictionRegistry#checkAll()} is invoked.
 * </p>
 */
final class Prediction {
    private final String methodName;
    private final Object[] arguments;
    private final PredictionMode mode;
    private final int expectedCount;
    private final PredictionCallback callback;

    Prediction(String methodName, Object[] arguments, PredictionMode mode) {
        this(methodName, arguments, mode, mode == PredictionMode.CALLED ? 1 : 0);
    }

    Prediction(String methodName, Object[] arguments, PredictionMode mode, int expectedCount) {
        this(methodName, arguments, mode, expectedCount, null);
    }

    Prediction(String methodName, Object[] arguments, PredictionCallback callback) {
        this(methodName, arguments, PredictionMode.CUSTOM, 0,
                Objects.requireNonNull(callback, "callback must not be null"));
    }

    private Prediction(String methodName, Object[] arguments, PredictionMode mode, int expectedCount,
            PredictionCallback callback) {
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.arguments = arguments.clone();
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.expectedCount = expectedCount;
        this.callback = callback;
    }

    /**
     * Checks this prediction against the supplied double control.
     *
     * @param control the double control to check against
     * @throws AssertionError if the prediction is not met
     */
    void check(DoubleControl control) {
        switch (mode) {
            case CALLED:
                control.verifyCalled(methodName, arguments);
                break;
            case NOT_CALLED:
                control.verifyNotCalled(methodName, arguments);
                break;
            case CALLED_TIMES:
                control.verifyCallCount(methodName, expectedCount, arguments);
                break;
            case CUSTOM:
                checkCustom(control);
                break;
        }
    }

    /**
     * Returns the method name for this prediction.
     */
    String methodName() {
        return methodName;
    }

    /**
     * Returns the prediction mode.
     */
    PredictionMode mode() {
        return mode;
    }

    /**
     * Returns the expected call count (meaningful only for CALLED_TIMES mode).
     */
    int expectedCount() {
        return expectedCount;
    }

    private void checkCustom(DoubleControl control) {
        PredictionContext context = new PredictionContext(control, methodName, arguments,
                control.calls(methodName, arguments), control.calls());
        try {
            callback.check(context);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            AssertionError failure = new AssertionError("Custom prediction for method '" + methodName
                    + "' failed: " + e.getMessage());
            failure.initCause(e);
            throw failure;
        }
    }
}
