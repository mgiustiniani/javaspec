package org.javaspec.doubles.prophecy;

/**
 * Modes for a {@link Prediction}.
 */
enum PredictionMode {
    /** The method should have been called at least once. */
    CALLED,
    /** The method should not have been called. */
    NOT_CALLED,
    /** The method should have been called exactly a specific number of times. */
    CALLED_TIMES
}
