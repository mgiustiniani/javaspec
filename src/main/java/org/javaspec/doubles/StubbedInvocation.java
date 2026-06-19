package org.javaspec.doubles;

import java.util.Objects;

final class StubbedInvocation {
    private static final int RETURN_VALUE = 1;
    private static final int THROWABLE = 2;
    private static final int ANSWER = 3;
    private static final int RETURN_SEQUENCE = 4;

    private final MethodPattern pattern;
    private final int kind;
    private final Object returnValue;
    private final Object[] returnSequence;
    private int returnSequenceIndex;
    private final Throwable throwable;
    private final StubAnswer answer;

    private StubbedInvocation(
            MethodPattern pattern,
            int kind,
            Object returnValue,
            Object[] returnSequence,
            Throwable throwable,
            StubAnswer answer
    ) {
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
        this.kind = kind;
        this.returnValue = returnValue;
        this.returnSequence = returnSequence == null ? null : Arguments.copy(returnSequence);
        this.returnSequenceIndex = 0;
        this.throwable = throwable;
        this.answer = answer;
    }

    static StubbedInvocation returning(MethodPattern pattern, Object returnValue) {
        return new StubbedInvocation(pattern, RETURN_VALUE, returnValue, null, null, null);
    }

    static StubbedInvocation returningSequence(MethodPattern pattern, Object[] returnValues) {
        if (returnValues == null || returnValues.length == 0) {
            throw new IllegalArgumentException("returnValues must contain at least one value");
        }
        return new StubbedInvocation(pattern, RETURN_SEQUENCE, null, returnValues, null, null);
    }

    static StubbedInvocation throwing(MethodPattern pattern, Throwable throwable) {
        return new StubbedInvocation(
                pattern,
                THROWABLE,
                null,
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
        if (kind == RETURN_SEQUENCE) {
            return nextReturnValue();
        }
        return returnValue;
    }

    private synchronized Object nextReturnValue() {
        int index = returnSequenceIndex;
        if (returnSequenceIndex < returnSequence.length - 1) {
            returnSequenceIndex++;
        }
        return Arguments.copyValue(returnSequence[index]);
    }
}
