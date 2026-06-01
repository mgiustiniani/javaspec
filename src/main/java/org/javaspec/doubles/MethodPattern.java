package org.javaspec.doubles;

final class MethodPattern {
    private final String methodName;
    private final ArgumentList arguments;

    private MethodPattern(String methodName, ArgumentList arguments) {
        this.methodName = validMethodName(methodName);
        this.arguments = arguments;
    }

    static MethodPattern anyArguments(String methodName) {
        return new MethodPattern(methodName, null);
    }

    static MethodPattern exactArguments(String methodName, Object[] arguments) {
        return new MethodPattern(methodName, ArgumentList.from(arguments));
    }

    boolean exactArguments() {
        return arguments != null;
    }

    boolean matches(String actualMethodName, Object[] actualArguments) {
        if (!methodName.equals(actualMethodName)) {
            return false;
        }
        return arguments == null || arguments.matches(actualArguments);
    }

    boolean matches(Call call) {
        if (!call.matches(methodName)) {
            return false;
        }
        return arguments == null || call.hasArguments(arguments.toArray());
    }

    String describe() {
        if (arguments == null) {
            return "method '" + methodName + "'";
        }
        return "method '" + methodName + "' with arguments " + arguments.toString();
    }

    private static String validMethodName(String methodName) {
        if (methodName == null) {
            throw new IllegalArgumentException("Method name must not be null");
        }
        if (methodName.trim().length() == 0) {
            throw new IllegalArgumentException("Method name must not be empty");
        }
        return methodName;
    }
}
