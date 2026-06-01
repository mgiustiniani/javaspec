package org.javaspec.doubles;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a method call observed by a javaspec interface double.
 */
public final class Call {
    private final String methodName;
    private final ArgumentList arguments;

    Call(String methodName, Object[] arguments) {
        this.methodName = Objects.requireNonNull(methodName, "methodName must not be null");
        this.arguments = ArgumentList.from(arguments);
    }

    /**
     * Creates a call snapshot useful for assertions against call history.
     */
    public static Call of(String methodName, Object... arguments) {
        return new Call(methodName, arguments);
    }

    /**
     * Returns the called method name.
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
     * Returns an immutable snapshot of the call arguments.
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
     * Returns the call arguments as a defensive array copy.
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
     * Returns the number of arguments supplied to the call.
     */
    public int argumentCount() {
        return arguments.size();
    }

    /**
     * Returns true when this call has exactly the supplied arguments.
     */
    public boolean hasArguments(Object... expectedArguments) {
        return arguments.matches(expectedArguments);
    }

    /**
     * Returns true when this call has the supplied method name, regardless of arguments.
     */
    public boolean matches(String expectedMethodName) {
        return methodName.equals(expectedMethodName);
    }

    /**
     * Returns true when this call has the supplied method name and exact arguments.
     */
    public boolean matches(String expectedMethodName, Object... expectedArguments) {
        return matches(expectedMethodName) && hasArguments(expectedArguments);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Call)) {
            return false;
        }
        Call call = (Call) other;
        return methodName.equals(call.methodName) && arguments.equals(call.arguments);
    }

    @Override
    public int hashCode() {
        return methodName.hashCode() * 31 + arguments.hashCode();
    }

    @Override
    public String toString() {
        return methodName + arguments.toString();
    }
}
