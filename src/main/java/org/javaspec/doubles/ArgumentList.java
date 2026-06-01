package org.javaspec.doubles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ArgumentList {
    private final Object[] arguments;
    private final int hashCode;

    private ArgumentList(Object[] arguments) {
        this.arguments = arguments;
        this.hashCode = Arguments.hashValues(arguments);
    }

    static ArgumentList from(Object[] arguments) {
        return new ArgumentList(Arguments.copy(arguments));
    }

    int size() {
        return arguments.length;
    }

    Object get(int index) {
        return Arguments.copyValue(arguments[index]);
    }

    Object[] toArray() {
        return Arguments.copy(arguments);
    }

    List<Object> toList() {
        List<Object> values = new ArrayList<Object>(arguments.length);
        for (int i = 0; i < arguments.length; i++) {
            values.add(Arguments.copyValue(arguments[i]));
        }
        return Collections.unmodifiableList(values);
    }

    boolean matches(Object[] actualArguments) {
        Object[] actual = actualArguments == null ? new Object[0] : actualArguments;
        if (arguments.length != actual.length) {
            return false;
        }
        for (int i = 0; i < arguments.length; i++) {
            if (!Arguments.equalValues(arguments[i], actual[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArgumentList)) {
            return false;
        }
        return matches(((ArgumentList) other).arguments);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return Arguments.describe(arguments);
    }
}
