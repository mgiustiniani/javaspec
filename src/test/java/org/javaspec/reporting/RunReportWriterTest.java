package org.javaspec.reporting;

import org.javaspec.runner.ExampleResult;
import org.javaspec.runner.ExampleStatus;
import org.javaspec.runner.FailureDetail;
import org.javaspec.runner.RunResult;
import org.javaspec.runner.SpecResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunReportWriterTest {
    @Test
    public void emptyRunResultJsonIncludesSchemaZeroSummaryAndEmptySpecs() {
        String json = RunReportWriter.toJson(RunResult.of(Collections.<SpecResult>emptyList()));

        assertEquals(
                "{\n" +
                        "  \"schemaVersion\": 1,\n" +
                        "  \"summary\": {\n" +
                        "    \"total\": 0,\n" +
                        "    \"passed\": 0,\n" +
                        "    \"failed\": 0,\n" +
                        "    \"broken\": 0,\n" +
                        "    \"skipped\": 0,\n" +
                        "    \"successful\": true\n" +
                        "  },\n" +
                        "  \"specs\": []\n" +
                        "}\n",
                json
        );
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
        )));

        assertContains(json, "\"schemaVersion\": 1");
        assertContains(json, "\"total\": 2");
        assertContains(json, "\"passed\": 0");
        assertContains(json, "\"failed\": 1");
        assertContains(json, "\"broken\": 1");
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
