package io.github.jvmspec.doubles;

import java.util.Objects;

final class StubbedInvocation {
    private static final int RETURN_VALUE = 1;
    private static final int THROWABLE = 2;
    private static final int ANSWER = 3;
    private static final int RETURN_SEQUENCE = 4;
    private static final int ANSWER_SEQUENCE = 5;

    private final MethodPattern pattern;
    private final int kind;
    private final Object returnValue;
    private final Object[] returnSequence;
    private int returnSequenceIndex;
    private final Throwable throwable;
    /** On-exhaustion throwable for RETURN_SEQUENCE with an exhaustion policy. */
    private final Throwable exhaustionThrowable;
    private final StubAnswer answer;
    private final StubAnswer[] answerSequence;
    private int answerSequenceIndex;

    private StubbedInvocation(
            MethodPattern pattern,
            int kind,
            Object returnValue,
            Object[] returnSequence,
            Throwable throwable,
            Throwable exhaustionThrowable,
            StubAnswer answer,
            StubAnswer[] answerSequence
    ) {
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
        this.kind = kind;
        this.returnValue = returnValue;
        this.returnSequence = returnSequence == null ? null : Arguments.copy(returnSequence);
        this.returnSequenceIndex = 0;
        this.throwable = throwable;
        this.exhaustionThrowable = exhaustionThrowable;
        this.answer = answer;
        this.answerSequence = answerSequence == null ? null : answerSequence.clone();
        this.answerSequenceIndex = 0;
    }

    static StubbedInvocation returning(MethodPattern pattern, Object returnValue) {
        return new StubbedInvocation(pattern, RETURN_VALUE, returnValue, null, null, null, null, null);
    }

    static StubbedInvocation returningSequence(MethodPattern pattern, Object[] returnValues) {
        if (returnValues == null || returnValues.length == 0) {
            throw new IllegalArgumentException("returnValues must contain at least one value");
        }
        return new StubbedInvocation(pattern, RETURN_SEQUENCE, null, returnValues, null, null, null, null);
    }

    static StubbedInvocation returningSequenceThenThrowing(
            MethodPattern pattern, Object[] returnValues, Throwable onExhaust) {
        if (returnValues == null || returnValues.length == 0) {
            throw new IllegalArgumentException("returnValues must contain at least one value");
        }
        Objects.requireNonNull(onExhaust, "onExhaust must not be null");
        return new StubbedInvocation(pattern, RETURN_SEQUENCE, null, returnValues, null, onExhaust, null, null);
    }

    static StubbedInvocation throwing(MethodPattern pattern, Throwable throwable) {
        return new StubbedInvocation(
                pattern, THROWABLE, null, null,
                Objects.requireNonNull(throwable, "throwable must not be null"),
                null, null, null);
    }

    static StubbedInvocation answering(MethodPattern pattern, StubAnswer answer) {
        return new StubbedInvocation(
                pattern, ANSWER, null, null, null, null,
                Objects.requireNonNull(answer, "answer must not be null"),
                null);
    }

    static StubbedInvocation answeringSequence(MethodPattern pattern, StubAnswer[] answers) {
        if (answers == null || answers.length == 0) {
            throw new IllegalArgumentException("answers must contain at least one value");
        }
        return new StubbedInvocation(pattern, ANSWER_SEQUENCE, null, null, null, null, null, answers);
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
        if (kind == ANSWER_SEQUENCE) {
            return nextAnswer(invocation);
        }
        return returnValue;
    }

    private synchronized Object nextReturnValue() throws Throwable {
        int index = returnSequenceIndex;
        boolean exhausted = index >= returnSequence.length;
        if (!exhausted && returnSequenceIndex < returnSequence.length - 1) {
            returnSequenceIndex++;
        } else if (!exhausted) {
            // At the last element: check exhaustion policy.
            if (exhaustionThrowable != null && returnSequenceIndex == returnSequence.length - 1) {
                // Advance index past end to mark exhausted for next call.
                returnSequenceIndex++;
            }
        }
        if (index >= returnSequence.length) {
            // Already exhausted: throw exhaustion throwable if set, else repeat last value.
            if (exhaustionThrowable != null) {
                throw exhaustionThrowable;
            }
            return Arguments.copyValue(returnSequence[returnSequence.length - 1]);
        }
        Object value = Arguments.copyValue(returnSequence[index]);
        if (exhaustionThrowable != null && returnSequenceIndex >= returnSequence.length) {
            // Value delivered; next call will throw.
        }
        return value;
    }

    private synchronized Object nextAnswer(DoubleInvocation invocation) throws Throwable {
        int index = answerSequenceIndex;
        if (answerSequenceIndex < answerSequence.length - 1) {
            answerSequenceIndex++;
        }
        return answerSequence[index].answer(invocation);
    }
}
