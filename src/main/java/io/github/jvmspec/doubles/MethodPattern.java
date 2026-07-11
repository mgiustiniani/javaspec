package io.github.jvmspec.doubles;

final class MethodPattern {
    private final String methodName;
    private final ArgumentPattern arguments;

    private MethodPattern(String methodName, ArgumentPattern arguments) {
        this.methodName = validMethodName(methodName);
        this.arguments = arguments;
    }

    static MethodPattern anyArguments(String methodName) {
        return new MethodPattern(methodName, null);
    }

    static MethodPattern exactArguments(String methodName, Object[] arguments) {
        return new MethodPattern(methodName, ArgumentPattern.from(arguments));
    }

    boolean argumentConstrained() {
        return arguments != null;
    }

    String methodName() {
        return methodName;
    }

    boolean matches(String actualMethodName, Object[] actualArguments) {
        if (!methodName.equals(actualMethodName)) {
            return false;
        }
        return arguments == null || arguments.matches(actualArguments);
    }

    boolean matches(Call call) {
        return call.matches(methodName) && (arguments == null || arguments.matches(call.argumentsArray()));
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
