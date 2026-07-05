package io.github.jvmspec.diagnostics;

import io.github.jvmspec.api.Pending;
import io.github.jvmspec.api.Skip;
import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecExample;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.runner.ExampleResult;
import io.github.jvmspec.runner.RunResult;
import io.github.jvmspec.runner.SpecResult;
import io.github.jvmspec.runner.SpecRunner;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RunDiagnosticsTest {
    @Test
    public void executionAvailabilityLinesAreDeterministicForUnavailableSpecsAndReflectedMethods() {
        DiscoveredSpec nonExecutableSpec = discoveredSpec(
                new File("src/test/java/spec/diagnostics/SourceOnlySpec.java"),
                "spec.diagnostics.SourceOnlySpec",
                "diagnostics.SourceOnly",
                SpecExample.of("it_is_discovered_but_not_compiled", 0, 11)
        );
        DiscoveredSpec missingMethodSpec = discoveredSpec(
                new File("src/test/java/spec/diagnostics/MissingMethodSpec.java"),
                MissingMethodSpec.class.getName(),
                "diagnostics.MissingMethod",
                SpecExample.of("it_is_missing_from_compiled_class", 0, 23)
        );

        RunResult result = SpecRunner.run(
                Arrays.asList(nonExecutableSpec, missingMethodSpec),
                RunDiagnosticsTest.class.getClassLoader()
        );

        assertEquals(Arrays.asList(
                "Specification spec.diagnostics.SourceOnlySpec is not executable "
                        + "(source: src/test/java/spec/diagnostics/SourceOnlySpec.java): "
                        + "Specification class not found: spec.diagnostics.SourceOnlySpec. "
                        + "The specification source was discovered, but the compiled specification class is not available "
                        + "to the runner classloader. Compile the spec/test sources and add the compiled output and "
                        + "required dependencies to the javaspec classpath.",
                "Example " + MissingMethodSpec.class.getName() + "#it_is_missing_from_compiled_class is not executable "
                        + "(source: src/test/java/spec/diagnostics/MissingMethodSpec.java:23): "
                        + "Example method not found or not public no-arg: it_is_missing_from_compiled_class. "
                        + "The discovered specification source may not match the compiled specification class available "
                        + "to the runner. Recompile test/spec sources so the compiled class contains a public "
                        + "no-argument example method."
        ), RunDiagnostics.executionAvailabilityLines(result));
    }

    @Test
    public void explicitSkipAndPendingExamplesAreNotExecutionAvailabilityProblems() {
        RunResult result = SpecRunner.run(
                discoveredSpec(
                        new File("src/test/java/spec/diagnostics/ExplicitSignalSpec.java"),
                        ExplicitSignalSpec.class.getName(),
                        "diagnostics.ExplicitSignal",
                        SpecExample.of("it_is_explicitly_skipped", 0, 10),
                        SpecExample.of("it_is_explicitly_pending", 1, 15)
                ),
                RunDiagnosticsTest.class.getClassLoader()
        );

        assertEquals(1, result.skippedCount());
        assertEquals(1, result.pendingCount());
        assertTrue(RunDiagnostics.executionAvailabilityLines(result).isEmpty());
    }

    @Test
    public void diagnosticsListIsImmutableAndDefensive() {
        RunResult result = SpecRunner.run(
                discoveredSpec(
                        new File("src/test/java/spec/diagnostics/ImmutableSourceOnlySpec.java"),
                        "spec.diagnostics.ImmutableSourceOnlySpec",
                        "diagnostics.ImmutableSourceOnly",
                        SpecExample.of("it_is_discovered_but_not_compiled", 0, 7)
                ),
                RunDiagnosticsTest.class.getClassLoader()
        );

        List<String> lines = RunDiagnostics.executionAvailabilityLines(result);
        assertEquals(1, lines.size());
        assertCannotAdd(lines);

        List<String> secondRead = RunDiagnostics.executionAvailabilityLines(result);
        assertEquals(1, secondRead.size());
        assertEquals(lines, secondRead);
        assertCannotAdd(RunDiagnostics.executionAvailabilityLines(RunResult.of(Arrays.<SpecResult>asList())));
    }

    private static DiscoveredSpec discoveredSpec(
            File sourceFile,
            String specQualifiedName,
            String describedQualifiedName,
            SpecExample... examples
    ) {
        return DiscoveredSpec.of(
                sourceFile,
                specQualifiedName,
                DescribedType.of(describedQualifiedName),
                Arrays.asList(examples)
        );
    }

    private static void assertCannotAdd(List<String> lines) {
        try {
            lines.add("unexpected mutation");
            fail("Expected diagnostics lines to be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected immutable list.
        }
    }

    public static final class MissingMethodSpec {
        public void it_present_in_compiled_class() {
        }
    }

    public static final class ExplicitSignalSpec {
        @Skip(reason = "explicitly skipped")
        public void it_is_explicitly_skipped() {
            throw new AssertionError("explicit @Skip example should not run");
        }

        @Pending(reason = "explicitly pending")
        public void it_is_explicitly_pending() {
            throw new AssertionError("explicit @Pending example should not run");
        }
    }
}
