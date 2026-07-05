package io.github.jvmspec.doubles;

final class ArgumentPattern {
    private final Object[] expectedArguments;

    private ArgumentPattern(Object[] expectedArguments) {
        this.expectedArguments = copyExpectedArguments(expectedArguments);
    }

    static ArgumentPattern from(Object[] expectedArguments) {
        return new ArgumentPattern(expectedArguments);
    }

    boolean matches(Object[] actualArguments) {
        Object[] actual = actualArguments == null ? new Object[0] : actualArguments;
        if (expectedArguments.length != actual.length) {
            return false;
        }
        for (int i = 0; i < expectedArguments.length; i++) {
            if (!matchesExpected(expectedArguments[i], actual[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return Arguments.describePattern(expectedArguments);
    }

    private static boolean matchesExpected(Object expected, Object actual) {
        if (expected instanceof ArgumentMatcher) {
            return ((ArgumentMatcher) expected).matches(actual);
        }
        return Arguments.equalValues(expected, actual);
    }

    private static Object[] copyExpectedArguments(Object[] expectedArguments) {
        if (expectedArguments == null || expectedArguments.length == 0) {
            return new Object[0];
        }
        Object[] copy = new Object[expectedArguments.length];
        for (int i = 0; i < expectedArguments.length; i++) {
            Object argument = expectedArguments[i];
            copy[i] = argument instanceof ArgumentMatcher ? argument : Arguments.copyValue(argument);
        }
        return copy;
    }
}
