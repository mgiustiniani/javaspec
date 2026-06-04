package org.javaspec.formatter;

import org.javaspec.runner.ExampleResult;
import org.javaspec.runner.RunResult;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

/**
 * Per-example formatter compatible with the original CLI pretty output.
 */
public final class PrettyRunFormatter implements RunFormatter {
    public String name() {
        return RunFormatterRegistry.FORMATTER_PRETTY;
    }

    public void format(RunResult runResult, PrintStream out) {
        Objects.requireNonNull(runResult, "runResult must not be null");
        Objects.requireNonNull(out, "out must not be null");
        out.println("Example results:");
        List<ExampleResult> examples = runResult.exampleResults();
        for (int i = 0; i < examples.size(); i++) {
            out.println(RunFormatterSupport.formatExampleResult(examples.get(i)));
        }
        RunFormatterSupport.printExampleSummary(runResult, out);
        RunFormatterSupport.printExampleResults("Failed examples:", runResult.failedExamples(), out);
        RunFormatterSupport.printExampleResults("Broken examples:", runResult.brokenExamples(), out);
        RunFormatterSupport.printExampleResults("Pending examples:", runResult.pendingExamples(), out);
    }
}
