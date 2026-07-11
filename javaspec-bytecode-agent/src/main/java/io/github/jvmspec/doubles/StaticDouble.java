package io.github.jvmspec.doubles;

import io.github.jvmspec.doubles.agent.AgentDoubleRegistry;

/**
 * Control handle for static-method interception of a single class.
 *
 * <p>While active, matching static methods are routed through javaspec's double handler. Stubbed
 * calls return configured values; unstubbed calls return normal javaspec default values (for
 * example {@code null}, {@code 0}, {@code false}) rather than executing the original static method.
 * Call {@link #close()} to restore original static behavior for subsequent calls.</p>
 */
public final class StaticDouble<T> implements AutoCloseable {
    private final Class<T> type;
    private final DoubleControl control;
    private boolean closed;

    StaticDouble(Class<T> type, DoubleControl control) {
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
            AgentDoubleRegistry.unregisterStatic(type);
            closed = true;
        }
    }
}
