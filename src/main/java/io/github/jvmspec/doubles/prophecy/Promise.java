package io.github.jvmspec.doubles.prophecy;

import io.github.jvmspec.doubles.StubAnswer;

/**
 * Represents a promised return value or side-effect for a prophesized method call.
 * <p>
 * Inspired by Prophecy's {@code Promise} concept. A {@code Promise} is created by
 * {@code willReturn(value)} or {@code willThrow(exception)} on a {@link MethodProphecy}.
 * </p>
 *
 * @param <R> the type of the promised return value
 */
public final class Promise<R> {
    private final Object value;
    private final Throwable throwable;
    private final StubAnswer answer;

    private Promise(Object value, Throwable throwable, StubAnswer answer) {
        this.value = value;
        this.throwable = throwable;
        this.answer = answer;
    }

    /**
     * Creates a promise that returns the supplied value.
     *
     * @param <R>   the return type
     * @param value the return value
     * @return a new promise
     */
    @SuppressWarnings("unchecked")
    public static <R> Promise<R> willReturn(R value) {
        return new Promise<R>(value, null, null);
    }

    /**
     * Creates a promise that throws the supplied throwable.
     *
     * @param <R>       the return type (inferred)
     * @param throwable the throwable to throw
     * @return a new promise
     */
    @SuppressWarnings("unchecked")
    public static <R> Promise<R> willThrow(Throwable throwable) {
        return new Promise<R>(null, throwable, null);
    }

    /**
     * Creates a promise that delegates to a custom answer.
     *
     * @param <R>    the return type
     * @param answer the stub answer
     * @return a new promise
     */
    @SuppressWarnings("unchecked")
    public static <R> Promise<R> will(StubAnswer answer) {
        return new Promise<R>(null, null, answer);
    }

    /**
     * Returns the promised value, or {@code null} if this promise throws or uses an answer.
     */
    Object value() {
        return value;
    }

    /**
     * Returns the promised throwable, or {@code null} if this promise returns a value or uses an answer.
     */
    Throwable throwable() {
        return throwable;
    }

    /**
     * Returns the promised answer, or {@code null} if this promise returns a value or throws.
     */
    StubAnswer answer() {
        return answer;
    }

    /**
     * Returns true when this promise returns a value.
     */
    boolean isReturn() {
        return value != null || (throwable == null && answer == null);
    }

    /**
     * Returns true when this promise throws.
     */
    boolean isThrow() {
        return throwable != null;
    }

    /**
     * Returns true when this promise uses a custom answer.
     */
    boolean isAnswer() {
        return answer != null;
    }
}
