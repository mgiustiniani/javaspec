package io.github.jvmspec.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal recorder used by the runner to attach example-data row results to the containing example.
 */
public final class ExampleDataRowRecorder {
    private static final ThreadLocal<List<ExampleDataRowResult>> CURRENT = new ThreadLocal<List<ExampleDataRowResult>>();

    private ExampleDataRowRecorder() {
    }

    public static void start() {
        CURRENT.set(new ArrayList<ExampleDataRowResult>());
    }

    public static void record(ExampleDataRowResult result) {
        List<ExampleDataRowResult> results = CURRENT.get();
        if (results != null) {
            results.add(result);
        }
    }

    public static List<ExampleDataRowResult> finish() {
        List<ExampleDataRowResult> results = CURRENT.get();
        CURRENT.remove();
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<ExampleDataRowResult>(results));
    }
}
