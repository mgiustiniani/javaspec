package io.github.jvmspec.api;

final class ExampleRows {
    private ExampleRows() {
    }

    static AssertionError assertionFailure(int index, String rowDescription, AssertionError cause) {
        AssertionError error = new AssertionError(message(index, rowDescription, cause));
        error.initCause(cause);
        return error;
    }

    static RuntimeException unexpectedFailure(int index, String rowDescription, Throwable cause) {
        RuntimeException error = new RuntimeException(message(index, rowDescription, cause), cause);
        return error;
    }

    private static String message(int index, String rowDescription, Throwable cause) {
        String detail = cause.getMessage();
        if (detail == null || detail.length() == 0) {
            detail = cause.getClass().getName();
        }
        return "Example data row " + index + " " + rowDescription + " failed: " + detail;
    }
}
