package io.github.jvmspec.api;

/**
 * One-column example-data row.
 *
 * @param <A> first row value type
 */
public final class ExampleRow1<A> {
    private final A first;

    ExampleRow1(A first) {
        this.first = first;
    }

    public A first() {
        return first;
    }

    String describe() {
        return "[" + String.valueOf(first) + "]";
    }
}
