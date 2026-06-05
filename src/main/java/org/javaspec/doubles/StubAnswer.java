package org.javaspec.doubles;

/**
 * Computes a stubbed result from an immutable double invocation context.
 */
public interface StubAnswer {
    /**
     * Returns the stubbed result for the supplied invocation or throws to make the double throw.
     */
    Object answer(DoubleInvocation invocation) throws Throwable;
}
