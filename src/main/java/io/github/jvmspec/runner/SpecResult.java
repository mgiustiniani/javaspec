package io.github.jvmspec.runner;

import io.github.jvmspec.discovery.DiscoveredSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result for one discovered specification class.
 */
public final class SpecResult {
    private static final List<ExampleResult> EMPTY_RESULTS = Collections.unmodifiableList(new ArrayList<ExampleResult>());

    private final String specQualifiedName;
    private final String sourceFilePath;
    private final boolean executable;
    private final String notExecutableReason;
    private final List<ExampleResult> exampleResults;
    private final int totalCount;
    private final int passedCount;
    private final int failedCount;
    private final int brokenCount;
    private final int skippedCount;
    private final int pendingCount;

    private SpecResult(
            String specQualifiedName,
            String sourceFilePath,
            boolean executable,
            String notExecutableReason,
            List<ExampleResult> exampleResults
    ) {
        this.specQualifiedName = specQualifiedName;
        this.sourceFilePath = sourceFilePath;
        this.executable = executable;
        this.notExecutableReason = notExecutableReason;
        this.exampleResults = exampleResults;
        this.totalCount = exampleResults.size();
        this.passedCount = count(exampleResults, ExampleStatus.PASSED);
        this.failedCount = count(exampleResults, ExampleStatus.FAILED);
        this.brokenCount = count(exampleResults, ExampleStatus.BROKEN);
        this.skippedCount = count(exampleResults, ExampleStatus.SKIPPED);
        this.pendingCount = count(exampleResults, ExampleStatus.PENDING);
    }

    public static SpecResult executable(DiscoveredSpec spec, List<ExampleResult> exampleResults) {
        Objects.requireNonNull(spec, "spec must not be null");
        return of(spec.specQualifiedName(), spec.sourceFilePath(), true, "", exampleResults);
    }

    public static SpecResult notExecutable(DiscoveredSpec spec, String reason, List<ExampleResult> exampleResults) {
        Objects.requireNonNull(spec, "spec must not be null");
        return of(spec.specQualifiedName(), spec.sourceFilePath(), false, reason, exampleResults);
    }

    public static SpecResult of(String specQualifiedName, List<ExampleResult> exampleResults) {
        return of(specQualifiedName, true, "", exampleResults);
    }

    public static SpecResult of(String specQualifiedName, String sourceFilePath, List<ExampleResult> exampleResults) {
        return of(specQualifiedName, sourceFilePath, true, "", exampleResults);
    }

    public static SpecResult of(String specQualifiedName, boolean executable, String notExecutableReason, List<ExampleResult> exampleResults) {
        return of(specQualifiedName, "", executable, notExecutableReason, exampleResults);
    }

    public static SpecResult of(
            String specQualifiedName,
            String sourceFilePath,
            boolean executable,
            String notExecutableReason,
            List<ExampleResult> exampleResults
    ) {
        String validatedReason = safeReason(notExecutableReason);
        if (!executable && validatedReason.length() == 0) {
            throw new IllegalArgumentException("notExecutableReason is required when executable is false");
        }
        return new SpecResult(
                validateText("specQualifiedName", specQualifiedName),
                safeSourceFilePath(sourceFilePath),
                executable,
                validatedReason,
                immutableResults(exampleResults)
        );
    }

    public String specQualifiedName() {
        return specQualifiedName;
    }

    public String getSpecQualifiedName() {
        return specQualifiedName;
    }

    public String id() {
        return specQualifiedName;
    }

    public String stableId() {
        return specQualifiedName;
    }

    public String getId() {
        return specQualifiedName;
    }

    public String getStableId() {
        return specQualifiedName;
    }

    public String sourceFilePath() {
        return sourceFilePath;
    }

    public String sourceFile() {
        return sourceFilePath;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public String getSourceFile() {
        return sourceFilePath;
    }

    public boolean hasSourceFile() {
        return sourceFilePath.length() > 0;
    }

    public boolean isExecutable() {
        return executable;
    }

    public boolean executable() {
        return executable;
    }

    public String notExecutableReason() {
        return notExecutableReason;
    }

    public String skipReason() {
        return notExecutableReason;
    }

    public List<ExampleResult> exampleResults() {
        return exampleResults;
    }

    public List<ExampleResult> results() {
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
            return EMPTY_RESULTS;
        }
        return Collections.unmodifiableList(failures);
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
            return EMPTY_RESULTS;
        }
        return Collections.unmodifiableList(results);
    }

    public List<ExampleResult> nonExecutedExamples() {
        return skippedOrPendingExamples();
    }

    public List<ExampleResult> failedExamples() {
        return examplesWithStatus(ExampleStatus.FAILED);
    }

    public List<ExampleResult> brokenExamples() {
        return examplesWithStatus(ExampleStatus.BROKEN);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SpecResult)) {
            return false;
        }
        SpecResult that = (SpecResult) other;
        return executable == that.executable
                && specQualifiedName.equals(that.specQualifiedName)
                && notExecutableReason.equals(that.notExecutableReason)
                && exampleResults.equals(that.exampleResults);
    }

    @Override
    public int hashCode() {
        int result = specQualifiedName.hashCode();
        result = 31 * result + (executable ? 1 : 0);
        result = 31 * result + notExecutableReason.hashCode();
        result = 31 * result + exampleResults.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SpecResult{" +
                "specQualifiedName='" + specQualifiedName + '\'' +
                ", sourceFilePath='" + sourceFilePath + '\'' +
                ", executable=" + executable +
                ", notExecutableReason='" + notExecutableReason + '\'' +
                ", exampleResults=" + exampleResults +
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
            return EMPTY_RESULTS;
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

    private static List<ExampleResult> immutableResults(List<ExampleResult> results) {
        Objects.requireNonNull(results, "exampleResults must not be null");
        if (results.isEmpty()) {
            return EMPTY_RESULTS;
        }
        List<ExampleResult> copy = new ArrayList<ExampleResult>();
        for (int i = 0; i < results.size(); i++) {
            copy.add(Objects.requireNonNull(results.get(i), "exampleResults[" + i + "] must not be null"));
        }
        return Collections.unmodifiableList(copy);
    }

    private static String validateText(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String safeReason(String reason) {
        if (reason == null) {
            return "";
        }
        return reason;
    }

    private static String safeSourceFilePath(String sourceFilePath) {
        if (sourceFilePath == null || sourceFilePath.trim().length() == 0) {
            return "";
        }
        return sourceFilePath;
    }
}
