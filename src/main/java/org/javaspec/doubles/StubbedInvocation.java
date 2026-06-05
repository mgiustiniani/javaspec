package org.javaspec.doubles;

import java.util.Objects;

final class StubbedInvocation {
    private static final int RETURN_VALUE = 1;
    private static final int THROWABLE = 2;
    private static final int ANSWER = 3;

    private final MethodPattern pattern;
    private final int kind;
    private final Object returnValue;
    private final Throwable throwable;
    private final StubAnswer answer;

    private StubbedInvocation(
            MethodPattern pattern,
            int kind,
            Object returnValue,
            Throwable throwable,
            StubAnswer answer
    ) {
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
        this.kind = kind;
        this.returnValue = returnValue;
        this.throwable = throwable;
        this.answer = answer;
    }

    static StubbedInvocation returning(MethodPattern pattern, Object returnValue) {
        return new StubbedInvocation(pattern, RETURN_VALUE, returnValue, null, null);
    }

    static StubbedInvocation throwing(MethodPattern pattern, Throwable throwable) {
        return new StubbedInvocation(
                pattern,
                THROWABLE,
                null,
                Objects.requireNonNull(throwable, "throwable must not be null"),
                null
        );
    }

    static StubbedInvocation answering(MethodPattern pattern, StubAnswer answer) {
        return new StubbedInvocation(
                pattern,
                ANSWER,
                null,
                null,
                Objects.requireNonNull(answer, "answer must not be null")
        );
    }

    boolean argumentConstrained() {
        return pattern.argumentConstrained();
    }

    boolean matches(String methodName, Object[] arguments) {
        return pattern.matches(methodName, arguments);
    }

    Object invoke(DoubleInvocation invocation) throws Throwable {
        if (kind == THROWABLE) {
            throw throwable;
        }
        if (kind == ANSWER) {
            return answer.answer(invocation);
        }
        return returnValue;
    }
}
