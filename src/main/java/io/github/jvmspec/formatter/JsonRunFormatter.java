package io.github.jvmspec.formatter;

import io.github.jvmspec.runner.ExampleResult;
import io.github.jvmspec.runner.RunResult;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Machine-readable JSON formatter for run results.
 *
 * <p>Emits a single JSON object with run totals, an overall success flag, and one entry per
 * executed example. Intended for tool and agent integrations that would otherwise have to
 * scrape the human-oriented formatters.</p>
 */
public final class JsonRunFormatter implements RunFormatter {
    public String name() {
        return RunFormatterRegistry.FORMATTER_JSON;
    }

    public void format(RunResult runResult, PrintStream out) {
        Objects.requireNonNull(runResult, "runResult must not be null");
        Objects.requireNonNull(out, "out must not be null");

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"schemaVersion\": 1,\n");
        json.append("  \"success\": ").append(runResult.isSuccessful()).append(",\n");
        json.append("  \"totals\": {\n");
        json.append("    \"total\": ").append(runResult.totalCount()).append(",\n");
        json.append("    \"passed\": ").append(runResult.passedCount()).append(",\n");
        json.append("    \"failed\": ").append(runResult.failedCount()).append(",\n");
        json.append("    \"broken\": ").append(runResult.brokenCount()).append(",\n");
        json.append("    \"skipped\": ").append(runResult.skippedCount()).append(",\n");
        json.append("    \"pending\": ").append(runResult.pendingCount()).append("\n");
        json.append("  },\n");
        json.append("  \"examples\": [");
        List<ExampleResult> examples = runResult.exampleResults();
        for (int i = 0; i < examples.size(); i++) {
            ExampleResult example = examples.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("\n    {\n");
            json.append("      \"spec\": \"").append(escape(example.specQualifiedName())).append("\",\n");
            json.append("      \"example\": \"").append(escape(example.methodName())).append("\",\n");
            json.append("      \"displayName\": \"").append(escape(example.displayName())).append("\",\n");
            json.append("      \"status\": \"").append(statusName(example)).append("\"");
            String detail = example.detail();
            if (detail != null && detail.length() > 0) {
                json.append(",\n      \"detail\": \"").append(escape(detail)).append("\"");
            }
            json.append("\n    }");
        }
        if (!examples.isEmpty()) {
            json.append("\n  ");
        }
        json.append("]\n");
        json.append("}");
        out.println(json.toString());
    }

    private static String statusName(ExampleResult example) {
        return example.status().name().toLowerCase(Locale.ROOT);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }
}
