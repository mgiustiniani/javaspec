package io.github.jvmspec.api;

/**
 * Immutable result for one PHPSpec-style example-data row.
 */
public final class ExampleDataRowResult {
    private final int index;
    private final String description;
    private final String status;
    private final String detail;

    private ExampleDataRowResult(int index, String description, String status, String detail) {
        if (index < 1) {
            throw new IllegalArgumentException("index must be positive: " + index);
        }
        this.index = index;
        this.description = safe(description);
        this.status = safe(status);
        this.detail = safe(detail);
    }

    public static ExampleDataRowResult passed(int index, String description) {
        return new ExampleDataRowResult(index, description, "PASSED", "");
    }

    public static ExampleDataRowResult failed(int index, String description, Throwable failure) {
        return new ExampleDataRowResult(index, description, "FAILED", detailOf(failure));
    }

    public static ExampleDataRowResult broken(int index, String description, Throwable failure) {
        return new ExampleDataRowResult(index, description, "BROKEN", detailOf(failure));
    }

    public int index() {
        return index;
    }

    public String description() {
        return description;
    }

    public String status() {
        return status;
    }

    public String detail() {
        return detail;
    }

    public boolean isPassed() {
        return "PASSED".equals(status);
    }

    private static String detailOf(Throwable failure) {
        if (failure == null) {
            return "";
        }
        String message = failure.getMessage();
        if (message == null || message.length() == 0) {
            return failure.getClass().getName();
        }
        return message;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
