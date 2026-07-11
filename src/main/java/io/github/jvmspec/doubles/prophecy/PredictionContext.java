package io.github.jvmspec.doubles.prophecy;

import io.github.jvmspec.doubles.Call;
import io.github.jvmspec.doubles.DoubleControl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable context supplied to custom Prophecy-style prediction callbacks.
 */
public final class PredictionContext {
    private final DoubleControl control;
    private final String methodName;
    private final Object[] arguments;
    private final List<Call> matchingCalls;
    private final List<Call> allCalls;

    PredictionContext(DoubleControl control, String methodName, Object[] arguments,
            List<Call> matchingCalls, List<Call> allCalls) {
        this.control = Objects.requireNonNull(control, "control must not be null");
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
        this.matchingCalls = Collections.unmodifiableList(matchingCalls);
        this.allCalls = Collections.unmodifiableList(allCalls);
    }

    /**
     * Returns the control API for advanced custom checks.
     */
    public DoubleControl control() {
        return control;
    }

    /**
     * JavaBean-style alias for {@link #control()}.
     */
    public DoubleControl getControl() {
        return control();
    }

    /**
     * Returns the predicted method name.
     */
    public String methodName() {
        return methodName;
    }

    /**
     * JavaBean-style alias for {@link #methodName()}.
     */
    public String getMethodName() {
        return methodName();
    }

    /**
     * Returns the predicted argument pattern as a defensive copy.
     */
    public Object[] arguments() {
        return arguments.clone();
    }

    /**
     * JavaBean-style alias for {@link #arguments()}.
     */
    public Object[] getArguments() {
        return arguments();
    }

    /**
     * Returns calls matching the predicted method and argument pattern.
     */
    public List<Call> calls() {
        return matchingCalls;
    }

    /**
     * JavaBean-style alias for {@link #calls()}.
     */
    public List<Call> getCalls() {
        return calls();
    }

    /**
     * Returns all calls recorded on the prophecy's double.
     */
    public List<Call> allCalls() {
        return allCalls;
    }

    /**
     * JavaBean-style alias for {@link #allCalls()}.
     */
    public List<Call> getAllCalls() {
        return allCalls();
    }

    /**
     * Returns how many calls matched the predicted method and argument pattern.
     */
    public int callCount() {
        return matchingCalls.size();
    }

    /**
     * JavaBean-style alias for {@link #callCount()}.
     */
    public int getCallCount() {
        return callCount();
    }
}
