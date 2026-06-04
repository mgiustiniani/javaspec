package org.javaspec.runner;

import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecExample;

import java.util.Objects;

/**
 * Immutable result for one discovered example method.
 */
public final class ExampleResult {
    public static final int UNKNOWN_SOURCE_LINE = 0;

    private final String specQualifiedName;
    private final String methodName;
    private final String displayName;
    private final int sourceOrderIndex;
    private final String sourceFilePath;
    private final int sourceLine;
    private final ExampleStatus status;
    private final String detail;
    private final FailureDetail failureDetail;

    private ExampleResult(
            String specQualifiedName,
            String methodName,
            String displayName,
            int sourceOrderIndex,
            String sourceFilePath,
            int sourceLine,
            ExampleStatus status,
            String detail,
            FailureDetail failureDetail
    ) {
        this.specQualifiedName = specQualifiedName;
        this.methodName = methodName;
        this.displayName = displayName;
        this.sourceOrderIndex = sourceOrderIndex;
        this.sourceFilePath = sourceFilePath;
        this.sourceLine = sourceLine;
        this.status = status;
        this.detail = detail;
        this.failureDetail = failureDetail;
    }

    public static ExampleResult passed(DiscoveredSpec spec, SpecExample example) {
        return of(spec, example, ExampleStatus.PASSED, "", null);
    }

    public static ExampleResult failed(DiscoveredSpec spec, SpecExample example, Throwable failure) {
        return of(spec, example, ExampleStatus.FAILED, "Assertion failed", FailureDetail.of(failure));
    }

    public static ExampleResult failed(DiscoveredSpec spec, SpecExample example, String detail, Throwable failure) {
        return of(spec, example, ExampleStatus.FAILED, detail, FailureDetail.of(failure));
    }

    public static ExampleResult broken(DiscoveredSpec spec, SpecExample example, String detail, Throwable failure) {
        return of(spec, example, ExampleStatus.BROKEN, detail, FailureDetail.of(failure));
    }

    public static ExampleResult skipped(DiscoveredSpec spec, SpecExample example, String detail) {
        return of(spec, example, ExampleStatus.SKIPPED, detail, null);
    }

    public static ExampleResult pending(DiscoveredSpec spec, SpecExample example, String detail) {
        return of(spec, example, ExampleStatus.PENDING, detail, null);
    }

    public static ExampleResult of(
            DiscoveredSpec spec,
            SpecExample example,
            ExampleStatus status,
            String detail,
            FailureDetail failureDetail
    ) {
        Objects.requireNonNull(spec, "spec must not be null");
        Objects.requireNonNull(example, "example must not be null");
        return of(
                spec.specQualifiedName(),
                example.methodName(),
                example.displayName(),
                example.sourceOrderIndex(),
                status,
                detail,
                failureDetail,
                spec.sourceFilePath(),
                example.sourceLine()
        );
    }

    public static ExampleResult of(
            String specQualifiedName,
            String methodName,
            String displayName,
            int sourceOrderIndex,
            ExampleStatus status,
            String detail,
            FailureDetail failureDetail
    ) {
        return of(
                specQualifiedName,
                methodName,
                displayName,
                sourceOrderIndex,
                status,
                detail,
                failureDetail,
                "",
                UNKNOWN_SOURCE_LINE
        );
    }

    public static ExampleResult of(
            String specQualifiedName,
            String methodName,
            String displayName,
            int sourceOrderIndex,
            ExampleStatus status,
            String detail,
            FailureDetail failureDetail,
            String sourceFilePath,
            int sourceLine
    ) {
        if (sourceOrderIndex < 0) {
            throw new IllegalArgumentException("sourceOrderIndex must not be negative: " + sourceOrderIndex);
        }
        if (sourceLine < UNKNOWN_SOURCE_LINE) {
            throw new IllegalArgumentException("sourceLine must not be negative: " + sourceLine);
        }
        ExampleStatus validatedStatus = Objects.requireNonNull(status, "status must not be null");
        if ((ExampleStatus.FAILED.equals(validatedStatus) || ExampleStatus.BROKEN.equals(validatedStatus))
                && failureDetail == null) {
            throw new IllegalArgumentException("failureDetail is required for " + validatedStatus + " examples");
        }
        return new ExampleResult(
                validateText("specQualifiedName", specQualifiedName),
                validateText("methodName", methodName),
                validateText("displayName", displayName),
                sourceOrderIndex,
                safeSourceFilePath(sourceFilePath),
                sourceLine,
                validatedStatus,
                safeDetail(detail),
                failureDetail
        );
    }

    public String specQualifiedName() {
        return specQualifiedName;
    }

    public String getSpecQualifiedName() {
        return specQualifiedName;
    }

    public String methodName() {
        return methodName;
    }

    public String exampleMethodName() {
        return methodName;
    }

    public String name() {
        return methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String displayName() {
        return displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int sourceOrderIndex() {
        return sourceOrderIndex;
    }

    public int orderIndex() {
        return sourceOrderIndex;
    }

    public int getSourceOrderIndex() {
        return sourceOrderIndex;
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

    public int sourceLine() {
        return sourceLine;
    }

    public int sourceLineNumber() {
        return sourceLine;
    }

    public int lineNumber() {
        return sourceLine;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public int getLineNumber() {
        return sourceLine;
    }

    public boolean hasSourceLine() {
        return sourceLine > UNKNOWN_SOURCE_LINE;
    }

    public boolean hasSourceLocation() {
        return hasSourceFile() || hasSourceLine();
    }

    public ExampleStatus status() {
        return status;
    }

    public ExampleStatus getStatus() {
        return status;
    }

    public String detail() {
        return detail;
    }

    public String message() {
        return detail;
    }

    public String getDetail() {
        return detail;
    }

    public FailureDetail failureDetail() {
        return failureDetail;
    }

    public FailureDetail failure() {
        return failureDetail;
    }

    public FailureDetail getFailureDetail() {
        return failureDetail;
    }

    public boolean hasFailureDetail() {
        return failureDetail != null;
    }

    public boolean isPassed() {
        return ExampleStatus.PASSED.equals(status);
    }

    public boolean isFailed() {
        return ExampleStatus.FAILED.equals(status);
    }

    public boolean isBroken() {
        return ExampleStatus.BROKEN.equals(status);
    }

    public boolean isSkipped() {
        return ExampleStatus.SKIPPED.equals(status);
    }

    public boolean isPending() {
        return ExampleStatus.PENDING.equals(status);
    }

    public boolean isSkippedOrPending() {
        return isSkipped() || isPending();
    }

    public boolean isNonExecuted() {
        return isSkippedOrPending();
    }

    public boolean isFailure() {
        return ExampleStatus.FAILED.equals(status) || ExampleStatus.BROKEN.equals(status);
    }

    public String fullName() {
        return specQualifiedName + "#" + methodName;
    }

    public String id() {
        return fullName();
    }

    public String stableId() {
        return fullName();
    }

    public String getId() {
        return fullName();
    }

    public String getStableId() {
        return fullName();
    }

    public String getFullName() {
        return fullName();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExampleResult)) {
            return false;
        }
        ExampleResult that = (ExampleResult) other;
        return sourceOrderIndex == that.sourceOrderIndex
                && specQualifiedName.equals(that.specQualifiedName)
                && methodName.equals(that.methodName)
                && displayName.equals(that.displayName)
                && status.equals(that.status)
                && detail.equals(that.detail)
                && equalNullable(failureDetail, that.failureDetail);
    }

    @Override
    public int hashCode() {
        int result = specQualifiedName.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 31 * result + displayName.hashCode();
        result = 31 * result + sourceOrderIndex;
        result = 31 * result + status.hashCode();
        result = 31 * result + detail.hashCode();
        result = 31 * result + (failureDetail == null ? 0 : failureDetail.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ExampleResult{" +
                "specQualifiedName='" + specQualifiedName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", sourceOrderIndex=" + sourceOrderIndex +
                ", sourceFilePath='" + sourceFilePath + '\'' +
                ", sourceLine=" + sourceLine +
                ", status=" + status +
                ", detail='" + detail + '\'' +
                ", failureDetail=" + failureDetail +
                '}';
    }

    private static boolean equalNullable(Object left, Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private static String validateText(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String safeDetail(String detail) {
        if (detail == null) {
            return "";
        }
        return detail;
    }

    private static String safeSourceFilePath(String sourceFilePath) {
        if (sourceFilePath == null || sourceFilePath.trim().length() == 0) {
            return "";
        }
        return sourceFilePath;
    }
}
