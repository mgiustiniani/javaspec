package org.javaspec.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable aggregate result for a runner invocation.
 */
public final class RunResult {
    private static final List<SpecResult> EMPTY_SPEC_RESULTS = Collections.unmodifiableList(new ArrayList<SpecResult>());
    private static final List<ExampleResult> EMPTY_EXAMPLE_RESULTS = Collections.unmodifiableList(new ArrayList<ExampleResult>());

    private final List<SpecResult> specResults;
    private final List<ExampleResult> exampleResults;
    private final int totalCount;
    private final int passedCount;
    private final int failedCount;
    private final int brokenCount;
    private final int skippedCount;
    private final int pendingCount;

    private RunResult(List<SpecResult> specResults) {
        this.specResults = specResults;
        this.exampleResults = flatten(specResults);
        this.totalCount = exampleResults.size();
        this.passedCount = count(exampleResults, ExampleStatus.PASSED);
        this.failedCount = count(exampleResults, ExampleStatus.FAILED);
        this.brokenCount = count(exampleResults, ExampleStatus.BROKEN);
        this.skippedCount = count(exampleResults, ExampleStatus.SKIPPED);
        this.pendingCount = count(exampleResults, ExampleStatus.PENDING);
    }

    public static RunResult of(List<SpecResult> specResults) {
        return new RunResult(immutableSpecResults(specResults));
    }

    public List<SpecResult> specResults() {
        return specResults;
    }

    public List<SpecResult> results() {
        return specResults;
    }

    public List<ExampleResult> exampleResults() {
        return exampleResults;
    }

    public List<ExampleResult> examples() {
        return exampleResults;
    }

    public int totalCount() {
        return totalCount;
    }

    public int totalExamples() {
        return totalCount;
    }

    public int total() {
        return totalCount;
    }

    public int passedCount() {
        return passedCount;
    }

    public int failedCount() {
        return failedCount;
    }

    public int brokenCount() {
        return brokenCount;
    }

    public int skippedCount() {
        return skippedCount;
    }

    public int pendingCount() {
        return pendingCount;
    }

    public int skippedOrPendingCount() {
        return skippedCount + pendingCount;
    }

    public int nonExecutedCount() {
        return skippedOrPendingCount();
    }

    public boolean isSuccessful() {
        return failedCount == 0 && brokenCount == 0;
    }

    public boolean hasFailures() {
        return failedCount > 0 || brokenCount > 0;
    }

    public List<ExampleResult> failures() {
        List<ExampleResult> failures = new ArrayList<ExampleResult>();
        for (int i = 0; i < exampleResults.size(); i++) {
            ExampleResult result = exampleResults.get(i);
            if (result.isFailed() || result.isBroken()) {
                failures.add(result);
            }
        }
        if (failures.isEmpty()) {
            return EMPTY_EXAMPLE_RESULTS;
        }
        return Collections.unmodifiableList(failures);
    }

    public List<ExampleResult> failedExamples() {
        return examplesWithStatus(ExampleStatus.FAILED);
    }

    public List<ExampleResult> brokenExamples() {
        return examplesWithStatus(ExampleStatus.BROKEN);
    }

    public List<ExampleResult> skippedExamples() {
        return examplesWithStatus(ExampleStatus.SKIPPED);
    }

    public List<ExampleResult> pendingExamples() {
        return examplesWithStatus(ExampleStatus.PENDING);
    }

    public List<ExampleResult> skippedOrPendingExamples() {
        List<ExampleResult> results = new ArrayList<ExampleResult>();
        for (int i = 0; i < exampleResults.size(); i++) {
            ExampleResult result = exampleResults.get(i);
            if (result.isSkippedOrPending()) {
                results.add(result);
            }
        }
        if (results.isEmpty()) {
            return EMPTY_EXAMPLE_RESULTS;
        }
        return Collections.unmodifiableList(results);
    }

    public List<ExampleResult> nonExecutedExamples() {
        return skippedOrPendingExamples();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RunResult)) {
            return false;
        }
        RunResult that = (RunResult) other;
        return specResults.equals(that.specResults);
    }

    @Override
    public int hashCode() {
        return specResults.hashCode();
    }

    @Override
    public String toString() {
        return "RunResult{" +
                "specResults=" + specResults +
                ", totalCount=" + totalCount +
                ", passedCount=" + passedCount +
                ", failedCount=" + failedCount +
                ", brokenCount=" + brokenCount +
                ", skippedCount=" + skippedCount +
                ", pendingCount=" + pendingCount +
                '}';
    }

    private List<ExampleResult> examplesWithStatus(ExampleStatus status) {
        List<ExampleResult> results = new ArrayList<ExampleResult>();
        for (int i = 0; i < exampleResults.size(); i++) {
            ExampleResult result = exampleResults.get(i);
            if (status.equals(result.status())) {
                results.add(result);
            }
        }
        if (results.isEmpty()) {
            return EMPTY_EXAMPLE_RESULTS;
        }
        return Collections.unmodifiableList(results);
    }

    private static List<SpecResult> immutableSpecResults(List<SpecResult> specResults) {
        Objects.requireNonNull(specResults, "specResults must not be null");
        if (specResults.isEmpty()) {
            return EMPTY_SPEC_RESULTS;
        }
        List<SpecResult> copy = new ArrayList<SpecResult>();
        for (int i = 0; i < specResults.size(); i++) {
            copy.add(Objects.requireNonNull(specResults.get(i), "specResults[" + i + "] must not be null"));
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<ExampleResult> flatten(List<SpecResult> specResults) {
        List<ExampleResult> results = new ArrayList<ExampleResult>();
        for (int i = 0; i < specResults.size(); i++) {
            results.addAll(specResults.get(i).exampleResults());
        }
        if (results.isEmpty()) {
            return EMPTY_EXAMPLE_RESULTS;
        }
        return Collections.unmodifiableList(results);
    }

    private static int count(List<ExampleResult> results, ExampleStatus status) {
        int count = 0;
        for (int i = 0; i < results.size(); i++) {
            if (status.equals(results.get(i).status())) {
                count++;
            }
        }
        return count;
    }
}
