package io.github.jvmspec.doubles.prophecy;

import io.github.jvmspec.doubles.DoubleControl;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of all predictions made during a specification example.
 * <p>
 * Each {@link ObjectProphecy} registers its predictions here. At the end of an
 * example, {@link #checkAll()} verifies all registered predictions against
 * their respective double controls.
 * </p>
 */
public final class PredictionRegistry {
    private final List<PredictionEntry> entries = new ArrayList<PredictionEntry>();

    /**
     * Registers a prediction to be checked later.
     *
     * @param control    the double control to check against
     * @param prediction the prediction to check
     */
    public void register(DoubleControl control, Prediction prediction) {
        entries.add(new PredictionEntry(control, prediction));
    }

    /**
     * Checks all registered predictions. Clears the registry after checking.
     *
     * @throws AssertionError if any prediction is not met
     */
    public void checkAll() {
        List<AssertionError> failures = new ArrayList<AssertionError>();
        for (PredictionEntry entry : entries) {
            try {
                entry.prediction.check(entry.control);
            } catch (AssertionError e) {
                failures.add(e);
            }
        }
        entries.clear();
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Prophecy predictions failed (").append(failures.size()).append("):");
            for (AssertionError failure : failures) {
                sb.append("\n  - ").append(failure.getMessage());
            }
            AssertionError aggregate = new AssertionError(sb.toString());
            for (AssertionError failure : failures) {
                aggregate.addSuppressed(failure);
            }
            throw aggregate;
        }
    }

    /**
     * Returns the number of registered predictions.
     */
    public int count() {
        return entries.size();
    }

    private static final class PredictionEntry {
        final DoubleControl control;
        final Prediction prediction;

        PredictionEntry(DoubleControl control, Prediction prediction) {
            this.control = control;
            this.prediction = prediction;
        }
    }
}
