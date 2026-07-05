package io.github.jvmspec.formatter;

import io.github.jvmspec.runner.ExampleResult;
import io.github.jvmspec.runner.ExampleStatus;
import io.github.jvmspec.runner.FailureDetail;
import io.github.jvmspec.runner.RunResult;
import io.github.jvmspec.runner.SpecResult;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonRunFormatterTest {

    private static RunResult sampleRunResult() {
        ExampleResult passed = ExampleResult.of(
                "spec.com.example.GreeterSpec",
                "it_greets",
                "it greets",
                0,
                ExampleStatus.PASSED,
                null,
                null
        );
        ExampleResult failed = ExampleResult.of(
                "spec.com.example.GreeterSpec",
                "it_greets_loudly",
                "it greets loudly",
                1,
                ExampleStatus.FAILED,
                "Expected \"HELLO\" but got null",
                FailureDetail.of(new AssertionError("Expected \"HELLO\" but got null"))
        );
        List<ExampleResult> examples = new ArrayList<ExampleResult>(Arrays.asList(passed, failed));
        SpecResult specResult = SpecResult.of("spec.com.example.GreeterSpec", examples);
        return RunResult.of(Arrays.asList(specResult));
    }

    private static String format(RunResult runResult) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true);
        new JsonRunFormatter().format(runResult, out);
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    public void isRegisteredAsBuiltInFormatterNamedJson() {
        RunFormatterRegistry registry = RunFormatterRegistry.builtIn();
        assertTrue("json formatter must be registered as a built-in", registry.contains("json"));
        assertTrue("json must be listed among built-in formatter names",
                RunFormatterRegistry.builtInFormatterNames().contains("json"));
        assertEquals("json", new JsonRunFormatter().name());
    }

    @Test
    public void rendersTotalsAndSuccessFlag() {
        String output = format(sampleRunResult());

        assertTrue("output must be a JSON object", output.trim().startsWith("{"));
        assertTrue("output must close the JSON object", output.trim().endsWith("}"));
        assertTrue(output.contains("\"total\": 2"));
        assertTrue(output.contains("\"passed\": 1"));
        assertTrue(output.contains("\"failed\": 1"));
        assertTrue(output.contains("\"broken\": 0"));
        assertTrue(output.contains("\"skipped\": 0"));
        assertTrue(output.contains("\"pending\": 0"));
        assertTrue(output.contains("\"success\": false"));
    }

    @Test
    public void rendersOneEntryPerExampleWithStatus() {
        String output = format(sampleRunResult());

        assertTrue(output.contains("\"examples\""));
        assertTrue(output.contains("\"spec\": \"spec.com.example.GreeterSpec\""));
        assertTrue(output.contains("\"example\": \"it_greets\""));
        assertTrue(output.contains("\"status\": \"passed\""));
        assertTrue(output.contains("\"example\": \"it_greets_loudly\""));
        assertTrue(output.contains("\"status\": \"failed\""));
    }

    @Test
    public void escapesJsonSpecialCharactersInDetail() {
        String output = format(sampleRunResult());

        assertTrue("double quotes in failure detail must be escaped",
                output.contains("Expected \\\"HELLO\\\" but got null"));
    }
}
