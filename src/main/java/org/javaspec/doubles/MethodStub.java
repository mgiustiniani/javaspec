package org.javaspec.doubles;

import java.util.Objects;

/**
 * Fluent stub for a method on an interface double.
 */
public final class MethodStub {
    private final DoubleControl control;
    private final MethodPattern pattern;

    MethodStub(DoubleControl control, MethodPattern pattern) {
        this.control = control;
        this.pattern = pattern;
    }

    /**
     * Stubs matching calls to return the supplied value.
     *
     * @return the owning control object for optional chaining
     */
    public DoubleControl thenReturn(Object returnValue) {
        control.addStub(pattern, returnValue);
        return control;
    }

    /**
     * Alias for {@link #thenReturn(Object)}.
     */
    public DoubleControl returns(Object returnValue) {
        return thenReturn(returnValue);
    }

    /**
     * Stubs matching calls to throw the supplied throwable.
     * The call is recorded before the throwable is thrown.
     *
     * @return the owning control object for optional chaining
     */
    public DoubleControl thenThrow(Throwable throwable) {
        control.addThrowingStub(pattern, Objects.requireNonNull(throwable, "throwable must not be null"));
        return control;
    }

    /**
     * Alias for {@link #thenThrow(Throwable)}.
     */
    public DoubleControl throwsException(Throwable throwable) {
        return thenThrow(throwable);
    }

    /**
     * Stubs matching calls by invoking the supplied answer callback.
     * Return values are validated using the same rules as {@link #thenReturn(Object)}.
     *
     * @return the owning control object for optional chaining
     */
    public DoubleControl thenAnswer(StubAnswer answer) {
        control.addAnswerStub(pattern, Objects.requireNonNull(answer, "answer must not be null"));
        return control;
    }

    /**
     * Alias for {@link #thenAnswer(StubAnswer)}.
     */
    public DoubleControl answers(StubAnswer answer) {
        return thenAnswer(answer);
    }
}
