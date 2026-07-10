package io.github.jvmspec.api;

/**
 * Two-column PHPSpec-style example-data callback.
 *
 * @param <A> first row value type
 * @param <B> second row value type
 */
public interface Example2<A, B> {
    void run(A first, B second) throws Exception;
}
