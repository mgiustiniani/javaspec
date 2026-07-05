package io.github.jvmspec.reporting;

import io.github.jvmspec.runner.ExampleResult;
import io.github.jvmspec.runner.FailureDetail;
import io.github.jvmspec.runner.RunResult;

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
 * Dependency-free JUnit XML-compatible writer for run reports.
 */
public final class JUnitXmlReportWriter {
    private JUnitXmlReportWriter() {
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
        writer.write(toXml(runResult, metadata));
    }

    public static String toXml(RunResult runResult) {
        Objects.requireNonNull(runResult, "runResult must not be null");
        return toXml(runResult, ReportMetadata.current());
    }

    public static String toXml(RunResult runResult, ReportMetadata metadata) {
        Objects.requireNonNull(runResult, "runResult must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        StringBuilder builder = new StringBuilder();
        appendRunResult(builder, runResult, metadata);
        return builder.toString();
    }

    private static void appendRunResult(StringBuilder builder, RunResult runResult, ReportMetadata metadata) {
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<testsuite name=\"javaspec\" tests=\"").append(runResult.totalCount())
                .append("\" failures=\"").append(runResult.failedCount())
                .append("\" errors=\"").append(runResult.brokenCount())
                .append("\" skipped=\"").append(runResult.skippedOrPendingCount())
                .append("\" timestamp=\"");
        appendXmlAttribute(builder, metadata.timestamp());
        builder.append("\" hostname=\"");
        appendXmlAttribute(builder, metadata.hostname());
        builder.append("\" time=\"");
        appendXmlAttribute(builder, metadata.timeSeconds());
        builder.append("\">\n");
        appendProperties(builder, metadata.properties());
        List<ExampleResult> examples = runResult.exampleResults();
        for (int i = 0; i < examples.size(); i++) {
            appendTestCase(builder, examples.get(i));
        }
        builder.append("</testsuite>\n");
    }

    private static void appendProperties(StringBuilder builder, Map<String, String> properties) {
        if (properties.isEmpty()) {
            builder.append("  <properties/>\n");
            return;
        }
        builder.append("  <properties>\n");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            builder.append("    <property name=\"");
            appendXmlAttribute(builder, entry.getKey());
            builder.append("\" value=\"");
            appendXmlAttribute(builder, entry.getValue());
            builder.append("\"/>\n");
        }
        builder.append("  </properties>\n");
    }

    private static void appendTestCase(StringBuilder builder, ExampleResult example) {
        builder.append("  <testcase classname=\"");
        appendXmlAttribute(builder, example.specQualifiedName());
        builder.append("\" name=\"");
        appendXmlAttribute(builder, example.methodName());
        builder.append("\" time=\"0\"");
        if (example.hasSourceFile()) {
            builder.append(" file=\"");
            appendXmlAttribute(builder, example.sourceFilePath());
            builder.append("\"");
        }
        if (example.hasSourceLine()) {
            builder.append(" line=\"").append(example.sourceLine()).append("\"");
        }

        if (example.isPassed()) {
            builder.append("/>\n");
            return;
        }

        builder.append(">\n");
        if (example.isFailed()) {
            appendFailureOrError(builder, "failure", example);
        } else if (example.isBroken()) {
            appendFailureOrError(builder, "error", example);
        } else if (example.isSkippedOrPending()) {
            appendSkipped(builder, example);
        }
        builder.append("  </testcase>\n");
    }

    private static void appendFailureOrError(StringBuilder builder, String elementName, ExampleResult example) {
        FailureDetail failure = example.failureDetail();
        String type = failure == null ? example.status().name() : failure.throwableClassName();
        String message = failure == null || !failure.hasMessage() ? example.detail() : failure.message();

        builder.append("    <").append(elementName).append(" type=\"");
        appendXmlAttribute(builder, type);
        builder.append("\" message=\"");
        appendXmlAttribute(builder, message);
        builder.append("\">");
        appendFailureText(builder, example, failure);
        builder.append("</").append(elementName).append(">\n");
    }

    private static void appendSkipped(StringBuilder builder, ExampleResult example) {
        String message = skippedMessage(example);
        builder.append("    <skipped");
        if (message.length() > 0) {
            builder.append(" message=\"");
            appendXmlAttribute(builder, message);
            builder.append("\"");
        }
        builder.append("/>\n");
    }

    private static String skippedMessage(ExampleResult example) {
        String detail = example.detail();
        if (!example.isPending()) {
            return detail;
        }
        if (isBlank(detail) || "Pending by javaspec.".equals(detail)) {
            return "Pending by javaspec.";
        }
        return "Pending: " + detail;
    }

    private static void appendFailureText(StringBuilder builder, ExampleResult example, FailureDetail failure) {
        boolean wroteLine = false;
        if (example.detail().length() > 0) {
            appendXmlText(builder, example.detail());
            wroteLine = true;
        }
        if (failure != null) {
            if (wroteLine) {
                builder.append('\n');
            }
            appendXmlText(builder, failure.summary());
            wroteLine = true;
            List<String> stackTrace = failure.stackTrace();
            for (int i = 0; i < stackTrace.size(); i++) {
                if (wroteLine) {
                    builder.append('\n');
                }
                appendXmlText(builder, "at " + stackTrace.get(i));
                wroteLine = true;
            }
        }
    }

    private static void appendXmlAttribute(StringBuilder builder, String value) {
        appendEscapedXml(builder, value, true);
    }

    private static void appendXmlText(StringBuilder builder, String value) {
        appendEscapedXml(builder, value, false);
    }

    private static void appendEscapedXml(StringBuilder builder, String value, boolean attribute) {
        if (value == null) {
            return;
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '&') {
                builder.append("&amp;");
            } else if (character == '<') {
                builder.append("&lt;");
            } else if (character == '>') {
                builder.append("&gt;");
            } else if (attribute && character == '"') {
                builder.append("&quot;");
            } else if (attribute && character == '\'') {
                builder.append("&apos;");
            } else if (attribute && character == '\n') {
                builder.append("&#10;");
            } else if (attribute && character == '\r') {
                builder.append("&#13;");
            } else if (attribute && character == '\t') {
                builder.append("&#9;");
            } else if (Character.isHighSurrogate(character)) {
                if (i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
                    appendCodePointReference(builder, Character.toCodePoint(character, value.charAt(i + 1)));
                    i++;
                } else {
                    appendUnicodeEscapeLiteral(builder, character);
                }
            } else if (Character.isLowSurrogate(character)) {
                appendUnicodeEscapeLiteral(builder, character);
            } else if (isXmlCharacter(character)) {
                builder.append(character);
            } else {
                appendUnicodeEscapeLiteral(builder, character);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static boolean isXmlCharacter(char character) {
        return character == '\t'
                || character == '\n'
                || character == '\r'
                || (character >= 0x20 && character <= 0xd7ff)
                || (character >= 0xe000 && character <= 0xfffd);
    }

    private static void appendCodePointReference(StringBuilder builder, int codePoint) {
        builder.append("&#x");
        builder.append(Integer.toHexString(codePoint).toUpperCase());
        builder.append(';');
    }

    private static void appendUnicodeEscapeLiteral(StringBuilder builder, char character) {
        builder.append("\\u");
        String hex = Integer.toHexString(character).toUpperCase();
        for (int i = hex.length(); i < 4; i++) {
            builder.append('0');
        }
        builder.append(hex);
    }
}
