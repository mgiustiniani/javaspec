package org.javaspec.formatter;

import org.javaspec.runner.RunResult;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Concise summary-oriented formatter compatible with the original CLI progress output.
 */
public final class ProgressRunFormatter implements RunFormatter {
    public String name() {
        return RunFormatterRegistry.FORMATTER_PROGRESS;
    }

    public void format(RunResult runResult, PrintStream out) {
        Objects.requireNonNull(runResult, "runResult must not be null");
        Objects.requireNonNull(out, "out must not be null");
        RunFormatterSupport.printExampleSummary(runResult, out);
        RunFormatterSupport.printExampleResults("Failed examples:", runResult.failedExamples(), out);
        RunFormatterSupport.printExampleResults("Broken examples:", runResult.brokenExamples(), out);
    }
}
