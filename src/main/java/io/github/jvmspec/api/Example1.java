package io.github.jvmspec.api;

/**
 * One-column PHPSpec-style example-data callback.
 *
 * @param <A> first row value type
 */
public interface Example1<A> {
    void run(A first) throws Exception;
}
