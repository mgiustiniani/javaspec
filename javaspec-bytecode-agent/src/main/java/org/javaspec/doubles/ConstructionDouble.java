package org.javaspec.doubles;

import org.javaspec.doubles.agent.AgentDoubleRegistry;

/**
 * Control handle for construction-aware doubles.
 *
 * <p>While open, every newly constructed instance of {@link #type()} is registered with this
 * handle's control. Intercepted instance methods on those constructed objects use normal javaspec
 * stubbing and verification semantics.</p>
 */
public final class ConstructionDouble<T> implements AutoCloseable {
    private final Class<T> type;
    private final DoubleControl control;
    private boolean closed;

    ConstructionDouble(Class<T> type, DoubleControl control) {
        this.type = type;
        this.control = control;
    }

    public Class<T> type() {
        return type;
    }

    public DoubleControl control() {
        return control;
    }

    public MethodStub when(String methodName) {
        return control.when(methodName);
    }

    public MethodStub when(String methodName, Object... exactArguments) {
        return control.when(methodName, exactArguments);
    }

    public void close() {
        if (!closed) {
            AgentDoubleRegistry.unregisterConstruction(type);
            closed = true;
        }
    }
}
