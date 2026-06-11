package org.javaspec.reporting;

import org.javaspec.runner.ExampleResult;
import org.javaspec.runner.ExampleStatus;
import org.javaspec.runner.FailureDetail;
import org.javaspec.runner.RunResult;
import org.javaspec.runner.SpecResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunReportWriterTest {
    private static final String TIMESTAMP = "2026-06-11T12:34:56Z";
    private static final String HOSTNAME = "ci-host";
    private static final ReportMetadata METADATA = ReportMetadata.of(TIMESTAMP, HOSTNAME, 1234L);

    @Test
    public void emptyRunResultJsonIncludesSchemaMetadataZeroSummaryAndEmptySpecs() {
        String json = RunReportWriter.toJson(RunResult.of(Collections.<SpecResult>emptyList()), METADATA);

        assertEquals(
                "{\n" +
                        "  \"schemaVersion\": 1,\n" +
                        "  \"metadata\": {\n" +
                        "    \"timestamp\": \"2026-06-11T12:34:56Z\",\n" +
                        "    \"hostname\": \"ci-host\",\n" +
                        "    \"time\": 1.234,\n" +
                        "    \"properties\": {\n" +
                        "      \"javaspec.report.schemaVersion\": \"1\",\n" +
                        "      \"javaspec.report.tool\": \"javaspec\"\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"summary\": {\n" +
                        "    \"total\": 0,\n" +
                        "    \"passed\": 0,\n" +
                        "    \"failed\": 0,\n" +
                        "    \"broken\": 0,\n" +
                        "    \"skipped\": 0,\n" +
                        "    \"pending\": 0,\n" +
                        "    \"successful\": true\n" +
                        "  },\n" +
                        "  \"specs\": []\n" +
                        "}\n",
                json
        );
    }

    @Test
    public void runResultJsonIncludesStableIdsAndSourceMetadataAlongsideExistingFields() {
        String specName = "spec.example.MetadataSpec";
        String sourceFile = "src/test/java/spec/example/MetadataSpec.java";
        ExampleResult passed = ExampleResult.of(
                specName,
                "it_has_metadata",
                "it has metadata",
                7,
                ExampleStatus.PASSED,
                "",
                null,
                sourceFile,
                42
        );

        String json = RunReportWriter.toJson(RunResult.of(Collections.singletonList(
                SpecResult.of(specName, sourceFile, Collections.singletonList(passed))
        )), METADATA);

        assertContains(json, "\"schemaVersion\": 1");
        assertContains(json, "\"metadata\": {");
        assertContains(json, "\"summary\": {\n    \"total\": 1");
        assertContains(json, "\"passed\": 1");
        assertContains(json, "\"failed\": 0");
        assertContains(json, "\"broken\": 0");
        assertContains(json, "\"skipped\": 0");
        assertContains(json, "\"pending\": 0");
        assertContains(json, "\"successful\": true");
        assertContains(json, "\"name\": \"spec.example.MetadataSpec\"");
        assertContains(json, "\"id\": \"spec.example.MetadataSpec\"");
        assertContains(json, "\"stableId\": \"spec.example.MetadataSpec\"");
        assertContains(json, "\"sourceFile\": \"src/test/java/spec/example/MetadataSpec.java\"");
        assertContains(json, "\"specName\": \"spec.example.MetadataSpec\"");
        assertContains(json, "\"id\": \"spec.example.MetadataSpec#it_has_metadata\"");
        assertContains(json, "\"stableId\": \"spec.example.MetadataSpec#it_has_metadata\"");
        assertContains(json, "\"fullName\": \"spec.example.MetadataSpec#it_has_metadata\"");
        assertContains(json, "\"method\": \"it_has_metadata\"");
        assertContains(json, "\"displayName\": \"it has metadata\"");
        assertContains(json, "\"sourceOrderIndex\": 7");
        assertContains(json, "\"source\": {");
        assertContains(json, "\"file\": \"src/test/java/spec/example/MetadataSpec.java\"");
        assertContains(json, "\"line\": 42");
        assertContains(json, "\"status\": \"PASSED\"");
        assertContains(json, "\"failure\": null");
    }

    @Test
    public void metadataStringsAndPropertiesAreEscapedInJson() {
        String emoji = "\uD83D\uDE00";
        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put(
                "prop\"name\\path\ncontrol" + '\u0001' + emoji,
                "value\treturn\rcontrol" + '\u0002' + " emoji " + emoji
        );
        ReportMetadata metadata = ReportMetadata.of(
                "2026-06-11T12:34:56Z \"quote\" \\slash\ncontrol " + '\u0003' + " emoji " + emoji,
                "host\"name\\path\tcontrol" + '\u0004' + " emoji " + emoji,
                1500L,
                properties
        );

        String json = RunReportWriter.toJson(RunResult.of(Collections.<SpecResult>emptyList()), metadata);

        assertContains(json, "\"timestamp\": \"2026-06-11T12:34:56Z \\\"quote\\\" \\\\slash\\ncontrol \\u0003 emoji \\ud83d\\ude00\"");
        assertContains(json, "\"hostname\": \"host\\\"name\\\\path\\tcontrol\\u0004 emoji \\ud83d\\ude00\"");
        assertContains(json, "\"time\": 1.5");
        assertContains(json, "\"prop\\\"name\\\\path\\ncontrol\\u0001\\ud83d\\ude00\": \"value\\treturn\\rcontrol\\u0002 emoji \\ud83d\\ude00\"");
        assertEquals(-1, json.indexOf('\u0001'));
        assertEquals(-1, json.indexOf('\u0002'));
        assertEquals(-1, json.indexOf('\u0003'));
        assertEquals(-1, json.indexOf('\u0004'));
        assertEquals(-1, json.indexOf('\uD83D'));
        assertEquals(-1, json.indexOf('\uDE00'));
    }

    @Test
    public void pendingRunResultJsonIncludesRunAndSpecPendingCountsAndPendingStatus() {
        String specName = "spec.example.PendingSpec";
        ExampleResult skipped = ExampleResult.of(
                specName,
                "it_is_skipped",
                "is skipped",
                0,
                ExampleStatus.SKIPPED,
                "skipped temporarily",
                null
        );
        ExampleResult pending = ExampleResult.of(
                specName,
                "it_is_pending",
                "is pending",
                1,
                ExampleStatus.PENDING,
                "awaiting implementation",
                null
        );

        String json = RunReportWriter.toJson(RunResult.of(Collections.singletonList(
                SpecResult.of(specName, Arrays.asList(skipped, pending))
        )), METADATA);

        assertContains(json, "\"summary\": {\n    \"total\": 2");
        assertContains(json, "\"passed\": 0");
        assertContains(json, "\"failed\": 0");
        assertContains(json, "\"broken\": 0");
        assertContains(json, "\"skipped\": 1");
        assertContains(json, "\"pending\": 1");
        assertContains(json, "\"successful\": true");
        assertContains(json, "\"summary\": {\n        \"total\": 2");
        assertContains(json, "\"status\": \"SKIPPED\"");
        assertContains(json, "\"detail\": \"skipped temporarily\"");
        assertContains(json, "\"status\": \"PENDING\"");
        assertContains(json, "\"detail\": \"awaiting implementation\"");
        assertContains(json, "\"failure\": null");
    }

    @Test
    public void failedAndBrokenRunResultJsonIncludesExamplesFailuresAndEscapedStrings() {
        String emoji = "\uD83D\uDE00";
        AssertionError assertion = new AssertionError("expected \"quoted\" \\ newline\ncontrol " + '\u0003' + " unicode ☃ surrogate " + emoji);
        assertion.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("spec.example.ReportSpec", "it_fails", "File\\Name.java", 17)
        });
        IllegalStateException broken = new IllegalStateException("broken backslash \\ quote \" newline\ncontrol " + '\u0004');
        broken.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("spec.example.ReportSpec", "it_breaks", "BrokenFile.java", 23)
        });
        String specName = "spec.example.Report\"Spec\\Name\nControl" + '\u0001' + emoji;
        ExampleResult failed = ExampleResult.of(
                specName,
                "it_\"fails\\now",
                "fails with newline\nand tab\t",
                0,
                ExampleStatus.FAILED,
                "detail has control " + '\u0002' + " and surrogate " + emoji,
                FailureDetail.of(assertion)
        );
        ExampleResult brokenResult = ExampleResult.of(
                specName,
                "it_breaks",
                "breaks",
                1,
                ExampleStatus.BROKEN,
                "unexpected throwable",
                FailureDetail.of(broken)
        );

        String json = RunReportWriter.toJson(RunResult.of(Collections.singletonList(
                SpecResult.of(specName, Arrays.asList(failed, brokenResult))
        )), METADATA);

        assertContains(json, "\"schemaVersion\": 1");
        assertContains(json, "\"metadata\": {");
        assertContains(json, "\"total\": 2");
        assertContains(json, "\"passed\": 0");
        assertContains(json, "\"failed\": 1");
        assertContains(json, "\"broken\": 1");
        assertContains(json, "\"pending\": 0");
        assertContains(json, "\"successful\": false");
        assertContains(json, "\"name\": \"spec.example.Report\\\"Spec\\\\Name\\nControl\\u0001\\ud83d\\ude00\"");
        assertContains(json, "\"method\": \"it_\\\"fails\\\\now\"");
        assertContains(json, "\"displayName\": \"fails with newline\\nand tab\\t\"");
        assertContains(json, "\"status\": \"FAILED\"");
        assertContains(json, "\"status\": \"BROKEN\"");
        assertContains(json, "\"detail\": \"detail has control \\u0002 and surrogate \\ud83d\\ude00\"");
        assertContains(json, "\"throwableClassName\": \"java.lang.AssertionError\"");
        assertContains(json, "\"message\": \"expected \\\"quoted\\\" \\\\ newline\\ncontrol \\u0003 unicode ☃ surrogate \\ud83d\\ude00\"");
        assertContains(json, "\"stackTrace\": [");
        assertContains(json, "\"spec.example.ReportSpec.it_fails(File\\\\Name.java:17)\"");
        assertContains(json, "\"throwableClassName\": \"java.lang.IllegalStateException\"");
        assertContains(json, "\"message\": \"broken backslash \\\\ quote \\\" newline\\ncontrol \\u0004\"");
        assertEquals(-1, json.indexOf('\u0001'));
        assertEquals(-1, json.indexOf('\u0002'));
        assertEquals(-1, json.indexOf('\u0003'));
        assertEquals(-1, json.indexOf('\u0004'));
        assertEquals(-1, json.indexOf('\uD83D'));
        assertEquals(-1, json.indexOf('\uDE00'));
    }

    private static void assertContains(String json, String expected) {
        assertTrue("Expected JSON to contain: " + expected + "\nActual JSON:\n" + json, json.contains(expected));
    }
}
