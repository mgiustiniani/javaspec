package org.javaspec.api;

/**
 * Lifecycle signals for example specifications: skip and pending.
 * <p>Extracted from {@link ObjectBehavior} to reduce responsibility concentration.</p>
 */
public class SpecLifecycleSignals {

    /**
     * Stops the current example and reports it as skipped with the default reason.
     * <p>
     * The runner still executes {@code letGo()} when the example lifecycle has already started. If
     * {@code letGo()} fails after this signal, the example is reported as broken because teardown failed.
     * </p>
     *
     * @throws SkipExampleException always
     */
    protected void skip() {
        throw new SkipExampleException();
    }

    /**
     * Stops the current example and reports it as skipped with the supplied reason.
     * <p>
     * The runner still executes {@code letGo()} when the example lifecycle has already started. If
     * {@code letGo()} fails after this signal, the example is reported as broken because teardown failed.
     * </p>
     *
     * @param reason human-readable skip reason
     * @throws SkipExampleException always
     */
    protected void skip(String reason) {
        throw new SkipExampleException(reason);
    }

    /**
     * Stops the current example and reports it as pending with the default reason.
     * <p>
     * The runner still executes {@code letGo()} when the example lifecycle has already started. If
     * {@code letGo()} fails after this signal, the example is reported as broken because teardown failed.
     * </p>
     *
     * @throws PendingExampleException always
     */
    protected void pending() {
        throw new PendingExampleException();
    }

    /**
     * Stops the current example and reports it as pending with the supplied reason.
     * <p>
     * The runner still executes {@code letGo()} when the example lifecycle has already started. If
     * {@code letGo()} fails after this signal, the example is reported as broken because teardown failed.
     * </p>
     *
     * @param reason human-readable pending reason
     * @throws PendingExampleException always
     */
    protected void pending(String reason) {
        throw new PendingExampleException(reason);
    }
}
