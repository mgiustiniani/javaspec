package io.github.jvmspec.api;

/**
 * Two-column example-data row.
 *
 * @param <A> first row value type
 * @param <B> second row value type
 */
public final class ExampleRow2<A, B> {
    private final A first;
    private final B second;

    ExampleRow2(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A first() {
        return first;
    }

    public B second() {
        return second;
    }

    String describe() {
        return "[" + String.valueOf(first) + ", " + String.valueOf(second) + "]";
    }
}
