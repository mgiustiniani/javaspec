package org.javaspec.doubles;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * Immutable invocation context passed to answer callbacks for interface doubles.
 */
public final class DoubleInvocation {
    private final String methodName;
    private final Method method;
    private final ArgumentList arguments;

    DoubleInvocation(Method method, Object[] arguments) {
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.methodName = method.getName();
        this.arguments = ArgumentList.from(arguments);
    }

    /**
     * Returns the invoked method name.
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
     * Returns the invoked reflective method.
     */
    public Method method() {
        return method;
    }

    /**
     * JavaBean-style alias for {@link #method()}.
     */
    public Method getMethod() {
        return method();
    }

    /**
     * Returns an immutable snapshot of invocation arguments.
     */
    public List<Object> arguments() {
        return arguments.toList();
    }

    /**
     * JavaBean-style alias for {@link #arguments()}.
     */
    public List<Object> getArguments() {
        return arguments();
    }

    /**
     * Returns invocation arguments as a defensive array copy.
     */
    public Object[] argumentsArray() {
        return arguments.toArray();
    }

    /**
     * Returns a defensive copy of the argument at the supplied index when it is an array.
     */
    public Object argument(int index) {
        return arguments.get(index);
    }

    /**
     * Returns the number of arguments supplied to the invocation.
     */
    public int argumentCount() {
        return arguments.size();
    }

    @Override
    public String toString() {
        return methodName + arguments.toString();
    }
}
