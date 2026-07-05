package io.github.jvmspec.reporting;

import io.github.jvmspec.runner.ExampleResult;
import io.github.jvmspec.runner.FailureDetail;
import io.github.jvmspec.runner.RunResult;
import io.github.jvmspec.runner.SpecResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dependency-free JSON writer for run reports.
 */
public final class RunReportWriter {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private RunReportWriter() {
    }

    public static void write(RunResult runResult, File file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        write(runResult, file, ReportMetadata.current());
    }

    public static void write(RunResult runResult, File file, ReportMetadata metadata) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        try {
            write(runResult, writer, metadata);
        } finally {
            writer.close();
        }
    }

    public static void write(RunResult runResult, Writer writer) throws IOException {
        Objects.requireNonNull(writer, "writer must not be null");
        write(runResult, writer, ReportMetadata.current());
    }

    public static void write(RunResult runResult, Writer writer, ReportMetadata metadata) throws IOException {
        Objects.requireNonNull(writer, "writer must not be null");
        writer.write(toJson(runResult, metadata));
    }

    public static String toJson(RunResult runResult) {
        Objects.requireNonNull(runResult, "runResult must not be null");
        return toJson(runResult, ReportMetadata.current());
    }

    public static String toJson(RunResult runResult, ReportMetadata metadata) {
        Objects.requireNonNull(runResult, "runResult must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        StringBuilder builder = new StringBuilder();
        appendRunResult(builder, runResult, metadata);
        return builder.toString();
    }

    private static void appendRunResult(StringBuilder builder, RunResult runResult, ReportMetadata metadata) {
        builder.append("{\n");
        builder.append("  \"schemaVersion\": 1,\n");
        builder.append("  \"metadata\": ");
        appendMetadata(builder, metadata, "  ");
        builder.append(",\n");
        builder.append("  \"summary\": ");
        appendSummary(builder,
                runResult.totalCount(),
                runResult.passedCount(),
                runResult.failedCount(),
                runResult.brokenCount(),
                runResult.skippedCount(),
                runResult.pendingCount(),
                runResult.isSuccessful(),
                "  ");
        builder.append(",\n");
        builder.append("  \"specs\": ");
        appendSpecs(builder, runResult.specResults());
        builder.append("\n");
        builder.append("}\n");
    }

    private static void appendMetadata(StringBuilder builder, ReportMetadata metadata, String indent) {
        builder.append("{\n");
        builder.append(indent).append("  \"timestamp\": ");
        appendJsonString(builder, metadata.timestamp());
        builder.append(",\n");
        builder.append(indent).append("  \"hostname\": ");
        appendJsonString(builder, metadata.hostname());
        builder.append(",\n");
        builder.append(indent).append("  \"time\": ").append(metadata.timeSeconds()).append(",\n");
        builder.append(indent).append("  \"properties\": ");
        appendProperties(builder, metadata.properties(), indent + "  ");
        builder.append("\n");
        builder.append(indent).append("}");
    }

    private static void appendProperties(StringBuilder builder, Map<String, String> properties, String indent) {
        if (properties.isEmpty()) {
            builder.append("{}");
            return;
        }
        builder.append("{\n");
        int index = 0;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (index > 0) {
                builder.append(",\n");
            }
            builder.append(indent).append("  ");
            appendJsonString(builder, entry.getKey());
            builder.append(": ");
            appendJsonString(builder, entry.getValue());
            index++;
        }
        builder.append("\n");
        builder.append(indent).append("}");
    }

    private static void appendSpecs(StringBuilder builder, List<SpecResult> specs) {
        if (specs.isEmpty()) {
            builder.append("[]");
            return;
        }
        builder.append("[\n");
        for (int i = 0; i < specs.size(); i++) {
            if (i > 0) {
                builder.append(",\n");
            }
            appendSpec(builder, specs.get(i));
        }
        builder.append("\n  ]");
    }

    private static void appendSpec(StringBuilder builder, SpecResult spec) {
        builder.append("    {\n");
        builder.append("      \"name\": ");
        appendJsonString(builder, spec.specQualifiedName());
        builder.append(",\n");
        builder.append("      \"id\": ");
        appendJsonString(builder, spec.stableId());
        builder.append(",\n");
        builder.append("      \"stableId\": ");
        appendJsonString(builder, spec.stableId());
        builder.append(",\n");
        builder.append("      \"sourceFile\": ");
        appendNullableJsonString(builder, spec.hasSourceFile() ? spec.sourceFilePath() : null);
        builder.append(",\n");
        builder.append("      \"executable\": ").append(spec.executable()).append(",\n");
        builder.append("      \"notExecutableReason\": ");
        appendJsonString(builder, spec.notExecutableReason());
        builder.append(",\n");
        builder.append("      \"summary\": ");
        appendSummary(builder,
                spec.totalCount(),
                spec.passedCount(),
                spec.failedCount(),
                spec.brokenCount(),
                spec.skippedCount(),
                spec.pendingCount(),
                spec.isSuccessful(),
                "      ");
        builder.append(",\n");
        builder.append("      \"examples\": ");
        appendExamples(builder, spec.exampleResults());
        builder.append("\n");
        builder.append("    }");
    }

    private static void appendExamples(StringBuilder builder, List<ExampleResult> examples) {
        if (examples.isEmpty()) {
            builder.append("[]");
            return;
        }
        builder.append("[\n");
        for (int i = 0; i < examples.size(); i++) {
            if (i > 0) {
                builder.append(",\n");
            }
            appendExample(builder, examples.get(i));
        }
        builder.append("\n      ]");
    }

    private static void appendExample(StringBuilder builder, ExampleResult example) {
        builder.append("        {\n");
        builder.append("          \"specName\": ");
        appendJsonString(builder, example.specQualifiedName());
        builder.append(",\n");
        builder.append("          \"id\": ");
        appendJsonString(builder, example.stableId());
        builder.append(",\n");
        builder.append("          \"stableId\": ");
        appendJsonString(builder, example.stableId());
        builder.append(",\n");
        builder.append("          \"fullName\": ");
        appendJsonString(builder, example.fullName());
        builder.append(",\n");
        builder.append("          \"method\": ");
        appendJsonString(builder, example.methodName());
        builder.append(",\n");
        builder.append("          \"displayName\": ");
        appendJsonString(builder, example.displayName());
        builder.append(",\n");
        builder.append("          \"sourceOrderIndex\": ").append(example.sourceOrderIndex()).append(",\n");
        builder.append("          \"source\": ");
        appendSourceLocation(builder, example);
        builder.append(",\n");
        builder.append("          \"status\": ");
        appendJsonString(builder, example.status().name());
        builder.append(",\n");
        builder.append("          \"detail\": ");
        appendJsonString(builder, example.detail());
        builder.append(",\n");
        builder.append("          \"failure\": ");
        appendFailure(builder, example.failureDetail());
        builder.append("\n");
        builder.append("        }");
    }

    private static void appendSourceLocation(StringBuilder builder, ExampleResult example) {
        if (!example.hasSourceLocation()) {
            builder.append("null");
            return;
        }
        builder.append("{\n");
        boolean wroteField = false;
        if (example.hasSourceFile()) {
            builder.append("            \"file\": ");
            appendJsonString(builder, example.sourceFilePath());
            wroteField = true;
        }
        if (example.hasSourceLine()) {
            if (wroteField) {
                builder.append(",\n            ");
            } else {
                builder.append("            ");
            }
            builder.append("\"line\": ").append(example.sourceLine());
            wroteField = true;
        }
        if (wroteField) {
            builder.append("\n");
        }
        builder.append("          }");
    }

    private static void appendFailure(StringBuilder builder, FailureDetail failure) {
        if (failure == null) {
            builder.append("null");
            return;
        }
        builder.append("{\n");
        builder.append("            \"throwableClassName\": ");
        appendJsonString(builder, failure.throwableClassName());
        builder.append(",\n");
        builder.append("            \"message\": ");
        appendJsonString(builder, failure.message());
        builder.append(",\n");
        builder.append("            \"stackTrace\": ");
        appendStringArray(builder, failure.stackTrace(), "            ");
        builder.append("\n");
        builder.append("          }");
    }

    private static void appendSummary(
            StringBuilder builder,
            int total,
            int passed,
            int failed,
            int broken,
            int skipped,
            int pending,
            boolean successful,
            String indent
    ) {
        builder.append("{\n");
        builder.append(indent).append("  \"total\": ").append(total).append(",\n");
        builder.append(indent).append("  \"passed\": ").append(passed).append(",\n");
        builder.append(indent).append("  \"failed\": ").append(failed).append(",\n");
        builder.append(indent).append("  \"broken\": ").append(broken).append(",\n");
        builder.append(indent).append("  \"skipped\": ").append(skipped).append(",\n");
        builder.append(indent).append("  \"pending\": ").append(pending).append(",\n");
        builder.append(indent).append("  \"successful\": ").append(successful).append("\n");
        builder.append(indent).append("}");
    }

    private static void appendStringArray(StringBuilder builder, List<String> values, String indent) {
        if (values.isEmpty()) {
            builder.append("[]");
            return;
        }
        builder.append("[\n");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(",\n");
            }
            builder.append(indent).append("  ");
            appendJsonString(builder, values.get(i));
        }
        builder.append("\n");
        builder.append(indent).append("]");
    }

    private static void appendNullableJsonString(StringBuilder builder, String value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        appendJsonString(builder, value);
    }

    private static void appendJsonString(StringBuilder builder, String value) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '"') {
                builder.append("\\\"");
            } else if (character == '\\') {
                builder.append("\\\\");
            } else if (character == '\b') {
                builder.append("\\b");
            } else if (character == '\f') {
                builder.append("\\f");
            } else if (character == '\n') {
                builder.append("\\n");
            } else if (character == '\r') {
                builder.append("\\r");
            } else if (character == '\t') {
                builder.append("\\t");
            } else if (character < 0x20 || Character.isSurrogate(character)) {
                appendUnicodeEscape(builder, character);
            } else {
                builder.append(character);
            }
        }
        builder.append('"');
    }

    private static void appendUnicodeEscape(StringBuilder builder, char character) {
        builder.append("\\u");
        builder.append(HEX[(character >> 12) & 0x0f]);
        builder.append(HEX[(character >> 8) & 0x0f]);
        builder.append(HEX[(character >> 4) & 0x0f]);
        builder.append(HEX[character & 0x0f]);
    }
}
