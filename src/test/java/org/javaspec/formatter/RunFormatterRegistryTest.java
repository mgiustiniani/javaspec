package org.javaspec.formatter;

import org.javaspec.runner.ExampleResult;
import org.javaspec.runner.ExampleStatus;
import org.javaspec.runner.FailureDetail;
import org.javaspec.runner.RunResult;
import org.javaspec.runner.SpecResult;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RunFormatterRegistryTest {
    @Test
    public void builtInRegistryExposesDeterministicNamesAndNormalizedLookup() {
        RunFormatterRegistry registry = RunFormatterRegistry.builtIn();

        assertEquals(Arrays.asList("progress", "pretty"), RunFormatterRegistry.builtInFormatterNames());
        assertEquals(Arrays.asList("progress", "pretty"), registry.formatterNames());
        assertTrue(registry.lookup(" progress ") instanceof ProgressRunFormatter);
        assertTrue(registry.lookup("\tPRETTY\n") instanceof PrettyRunFormatter);
        assertSame(registry.lookup("progress"), registry.get(" PROGRESS "));
    }

    @Test
    public void registeringCustomFormatterByExplicitNameAppendsDeterministically() {
        RunFormatterRegistry registry = RunFormatterRegistry.builtIn();
        RunFormatter customFormatter = new RunFormatter() {
            public String name() {
                return "ignored";
            }

            public void format(RunResult runResult, PrintStream out) {
                out.println("custom");
            }
        };

        registry.register("  TAP  ", customFormatter);

        assertEquals(Arrays.asList("progress", "pretty", "tap"), registry.formatterNames());
        assertSame(customFormatter, registry.lookup("tap"));
        assertSame(customFormatter, registry.lookup(" TAP "));
    }

    @Test
    public void replacingExistingFormatterKeepsNameOrder() {
        RunFormatterRegistry registry = RunFormatterRegistry.builtIn();
        RunFormatter replacement = new RunFormatter() {
            public void format(RunResult runResult, PrintStream out) {
                out.println("replacement");
            }
        };

        registry.register(" PRETTY ", replacement);

        assertEquals(Arrays.asList("progress", "pretty"), registry.formatterNames());
        assertSame(replacement, registry.lookup("pretty"));
    }

    @Test
    public void progressFormatterProducesCurrentConciseCliOutput() {
        String output = format(new ProgressRunFormatter(), mixedRunResult());

        assertTrue(output.contains("Examples: 4 total, 1 passed, 1 failed, 1 broken, 1 skipped."));
        assertTrue(output.contains("Failed examples:"));
        assertTrue(output.contains("  FAILED spec.example.CalculatorSpec#it_fails (fails cleanly): Assertion failed - java.lang.AssertionError: expected sum"));
        assertTrue(output.contains("Broken examples:"));
        assertTrue(output.contains("  BROKEN spec.example.CalculatorSpec#it_breaks (breaks loudly): Example method threw an unexpected throwable - java.lang.IllegalStateException: boom"));
        assertFalse(output.contains("Example results:"));
        assertFalse(output.contains("PASSED spec.example.CalculatorSpec#it_passes"));
        assertFalse(output.contains("SKIPPED spec.example.CalculatorSpec#it_is_pending"));
    }

    @Test
    public void prettyFormatterProducesCurrentPerExampleCliOutput() {
        String output = format(new PrettyRunFormatter(), mixedRunResult());

        assertTrue(output.contains("Example results:"));
        assertTrue(output.contains("  PASSED spec.example.CalculatorSpec#it_passes"));
        assertTrue(output.contains("  FAILED spec.example.CalculatorSpec#it_fails (fails cleanly): Assertion failed - java.lang.AssertionError: expected sum"));
        assertTrue(output.contains("  BROKEN spec.example.CalculatorSpec#it_breaks (breaks loudly): Example method threw an unexpected throwable - java.lang.IllegalStateException: boom"));
        assertTrue(output.contains("  SKIPPED spec.example.CalculatorSpec#it_is_pending (is pending): pending implementation"));
        assertTrue(output.contains("Examples: 4 total, 1 passed, 1 failed, 1 broken, 1 skipped."));
        assertTrue(output.contains("Failed examples:"));
        assertTrue(output.contains("Broken examples:"));
    }

    private static String format(RunFormatter formatter, RunResult runResult) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        formatter.format(runResult, new PrintStream(out));
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static RunResult mixedRunResult() {
        ExampleResult passed = ExampleResult.of(
                "spec.example.CalculatorSpec",
                "it_passes",
                "it_passes",
                0,
                ExampleStatus.PASSED,
                "",
                null
        );
        ExampleResult failed = ExampleResult.of(
                "spec.example.CalculatorSpec",
                "it_fails",
                "fails cleanly",
                1,
                ExampleStatus.FAILED,
                "Assertion failed",
                FailureDetail.of(new AssertionError("expected sum"))
        );
        ExampleResult broken = ExampleResult.of(
                "spec.example.CalculatorSpec",
                "it_breaks",
                "breaks loudly",
                2,
                ExampleStatus.BROKEN,
                "Example method threw an unexpected throwable",
                FailureDetail.of(new IllegalStateException("boom"))
        );
        ExampleResult skipped = ExampleResult.of(
                "spec.example.CalculatorSpec",
                "it_is_pending",
                "is pending",
                3,
                ExampleStatus.SKIPPED,
                "pending implementation",
                null
        );
        return RunResult.of(Collections.singletonList(SpecResult.of(
                "spec.example.CalculatorSpec",
                Arrays.asList(passed, failed, broken, skipped)
        )));
    }
}
