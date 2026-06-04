package org.javaspec.formatter;

import org.javaspec.runner.ExampleResult;
import org.javaspec.runner.RunResult;

import java.io.PrintStream;
import java.util.List;

final class RunFormatterSupport {
    private RunFormatterSupport() {
    }

    static void printExampleSummary(RunResult runResult, PrintStream out) {
        out.println("Examples: " + runResult.totalCount()
                + " total, " + runResult.passedCount() + " passed, "
                + runResult.failedCount() + " failed, "
                + runResult.brokenCount() + " broken, "
                + runResult.skippedCount() + " skipped, "
                + runResult.pendingCount() + " pending.");
    }

    static void printExampleResults(String heading, List<ExampleResult> results, PrintStream out) {
        if (results.isEmpty()) {
            return;
        }
        out.println(heading);
        for (int i = 0; i < results.size(); i++) {
            out.println(formatExampleResult(results.get(i)));
        }
    }

    static String formatExampleResult(ExampleResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("  ").append(result.status()).append(' ').append(result.fullName());
        if (!result.displayName().equals(result.methodName())) {
            builder.append(" (").append(result.displayName()).append(')');
        }
        if (result.detail().length() > 0) {
            builder.append(": ").append(result.detail());
        }
        if (result.hasFailureDetail()) {
            builder.append(" - ").append(result.failureDetail().summary());
        }
        return builder.toString();
    }
}
