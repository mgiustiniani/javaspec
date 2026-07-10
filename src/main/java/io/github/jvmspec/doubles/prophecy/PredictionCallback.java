package io.github.jvmspec.doubles.prophecy;

/**
 * Custom Prophecy-style prediction callback.
 * <p>
 * Use with {@link MethodProphecy#should(PredictionCallback)} when built-in predictions such as
 * {@code shouldBeCalled()} or {@code shouldBeCalledTimes(int)} are not expressive enough. Throw
 * {@link AssertionError} from the callback to fail the prediction with your own diagnostic.
 * </p>
 */
@FunctionalInterface
public interface PredictionCallback {
    /**
     * Checks the prediction against the supplied context.
     *
     * @param context immutable call-history context for the predicted method
     * @throws Exception if the callback cannot complete; assertion failures are reported directly
     */
    void check(PredictionContext context) throws Exception;
}
