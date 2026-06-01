package org.javaspec.doubles;

final class StubbedInvocation {
    private final MethodPattern pattern;
    private final Object returnValue;

    StubbedInvocation(MethodPattern pattern, Object returnValue) {
        this.pattern = pattern;
        this.returnValue = returnValue;
    }

    boolean exactArguments() {
        return pattern.exactArguments();
    }

    boolean matches(String methodName, Object[] arguments) {
        return pattern.matches(methodName, arguments);
    }

    Object returnValue() {
        return returnValue;
    }
}
