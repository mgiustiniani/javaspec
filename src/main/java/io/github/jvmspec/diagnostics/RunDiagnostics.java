package io.github.jvmspec.diagnostics;

import io.github.jvmspec.runner.ExampleResult;
import io.github.jvmspec.runner.RunResult;
import io.github.jvmspec.runner.SpecResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Dependency-free diagnostics for execution availability problems in a run result.
 */
public final class RunDiagnostics {
    private static final List<String> EMPTY_LINES = Collections.unmodifiableList(new ArrayList<String>());

    private static final String MISSING_EXAMPLE_METHOD_PREFIX = "Example method not found or not public void:";
    private static final String MISSING_EXAMPLE_METHOD_HINT =
            "The discovered specification source may not match the compiled specification class";

    private RunDiagnostics() {
    }

    public static List<String> executionAvailabilityLines(RunResult runResult) {
        Objects.requireNonNull(runResult, "runResult must not be null");

        List<String> lines = new ArrayList<String>();
        List<SpecResult> specResults = runResult.specResults();
        for (int i = 0; i < specResults.size(); i++) {
            SpecResult specResult = specResults.get(i);
            if (!specResult.isExecutable()) {
                lines.add(notExecutableSpecLine(specResult));
                continue;
            }
            addMissingExampleMethodLines(lines, specResult.exampleResults());
        }
        return immutableLines(lines);
    }

    public static List<String> executionAvailabilityDiagnostics(RunResult runResult) {
        return executionAvailabilityLines(runResult);
    }

    private static void addMissingExampleMethodLines(List<String> lines, List<ExampleResult> examples) {
        for (int i = 0; i < examples.size(); i++) {
            ExampleResult example = examples.get(i);
            if (isMissingExampleMethodSkip(example)) {
                lines.add(missingExampleMethodLine(example));
            }
        }
    }

    private static boolean isMissingExampleMethodSkip(ExampleResult example) {
        String detail = example.detail();
        return example.isSkipped()
                && detail.startsWith(MISSING_EXAMPLE_METHOD_PREFIX)
                && detail.indexOf(MISSING_EXAMPLE_METHOD_HINT) >= 0;
    }

    private static String notExecutableSpecLine(SpecResult specResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("Specification ").append(specResult.specQualifiedName()).append(" is not executable");
        appendSourceLocation(builder, specResult.sourceFilePath(), 0);
        builder.append(": ").append(specResult.notExecutableReason());
        return builder.toString();
    }

    private static String missingExampleMethodLine(ExampleResult example) {
        StringBuilder builder = new StringBuilder();
        builder.append("Example ").append(example.fullName()).append(" is not executable");
        appendSourceLocation(builder, example.sourceFilePath(), example.sourceLine());
        builder.append(": ").append(example.detail());
        return builder.toString();
    }

    private static void appendSourceLocation(StringBuilder builder, String sourceFilePath, int sourceLine) {
        if (sourceFilePath == null || sourceFilePath.length() == 0) {
            return;
        }
        builder.append(" (source: ").append(sourceFilePath);
        if (sourceLine > ExampleResult.UNKNOWN_SOURCE_LINE) {
            builder.append(":").append(sourceLine);
        }
        builder.append(")");
    }

    private static List<String> immutableLines(List<String> lines) {
        if (lines.isEmpty()) {
            return EMPTY_LINES;
        }
        return Collections.unmodifiableList(new ArrayList<String>(lines));
    }
}
