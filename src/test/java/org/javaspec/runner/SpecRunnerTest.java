package org.javaspec.runner;

import org.javaspec.api.ObjectBehavior;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecExample;
import org.javaspec.model.DescribedType;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SpecRunnerTest {
    @Test
    public void executesOnlyDiscoveredExampleMetadata() {
        MetadataOnlySpec.reset();

        RunResult result = run(MetadataOnlySpec.class, "it_in_metadata");

        assertEquals(1, result.totalCount());
        assertEquals(1, result.passedCount());
        assertEquals(0, result.failedCount());
        assertEquals(0, result.brokenCount());
        assertEquals(0, result.skippedCount());
        assertTrue(MetadataOnlySpec.includedExampleRan);
        assertFalse(MetadataOnlySpec.undiscoveredExampleRan);
    }

    @Test
    public void mapsExampleOutcomesAndExposesCountsAndFailureDetails() {
        RunResult result = run(StatusMappingSpec.class, "it_passes", "it_fails", "it_breaks");

        assertEquals(3, result.totalCount());
        assertEquals(1, result.passedCount());
        assertEquals(1, result.failedCount());
        assertEquals(1, result.brokenCount());
        assertEquals(0, result.skippedCount());
        assertFalse(result.isSuccessful());
        assertTrue(result.hasFailures());
        assertEquals(2, result.failures().size());

        SpecResult specResult = result.specResults().get(0);
        assertTrue(specResult.isExecutable());
        assertEquals(3, specResult.totalCount());
        assertEquals(1, specResult.passedCount());
        assertEquals(1, specResult.failedCount());
        assertEquals(1, specResult.brokenCount());
        assertEquals(0, specResult.skippedCount());
        assertEquals(2, specResult.failures().size());

        ExampleResult passed = result.exampleResults().get(0);
        assertEquals(ExampleStatus.PASSED, passed.status());
        assertTrue(passed.isPassed());
        assertFalse(passed.hasFailureDetail());

        ExampleResult failed = result.exampleResults().get(1);
        assertEquals(ExampleStatus.FAILED, failed.status());
        assertTrue(failed.isFailed());
        assertEquals("Assertion failed", failed.detail());
        assertNotNull(failed.failureDetail());
        assertEquals(AssertionError.class.getName(), failed.failureDetail().throwableClassName());
        assertEquals("expected failure", failed.failureDetail().message());
        assertTrue(failed.failureDetail().summary().contains("expected failure"));
        assertFalse(failed.failureDetail().stackTrace().isEmpty());

        ExampleResult broken = result.exampleResults().get(2);
        assertEquals(ExampleStatus.BROKEN, broken.status());
        assertTrue(broken.isBroken());
        assertEquals("Example method threw an unexpected throwable", broken.detail());
        assertNotNull(broken.failureDetail());
        assertEquals(IllegalStateException.class.getName(), broken.failureDetail().throwableClassName());
        assertEquals("unexpected boom", broken.failureDetail().message());
        assertTrue(broken.failureDetail().summary().contains("unexpected boom"));
        assertEquals(Arrays.asList(failed), result.failedExamples());
        assertEquals(Arrays.asList(broken), result.brokenExamples());
    }

    @Test
    public void executesCompiledObjectBehaviorSpecUsingExpandedMatchers() {
        RunResult result = run(
                Phase7MatcherSpec.class,
                "it_passes_with_expanded_matchers",
                "it_fails_with_expanded_matcher"
        );

        assertEquals(2, result.totalCount());
        assertEquals(1, result.passedCount());
        assertEquals(1, result.failedCount());
        assertEquals(0, result.brokenCount());

        ExampleResult failed = result.exampleResults().get(1);
        assertEquals(ExampleStatus.FAILED, failed.status());
        assertNotNull(failed.failureDetail());
        assertTrue(failed.failureDetail().message().contains("not to end with city"));
    }

    @Test
    public void skipsNonLoadableSpecClassAndMissingReflectedExampleMethod() {
        DiscoveredSpec nonLoadableSpec = namedSpec(
                "missing.runner.NonLoadableSpec",
                "missing.runner.NonLoadable",
                "it_is_skipped"
        );
        DiscoveredSpec missingMethodSpec = spec(MissingMethodSpec.class, "it_is_missing");

        RunResult result = SpecRunner.run(
                Arrays.asList(nonLoadableSpec, missingMethodSpec),
                SpecRunnerTest.class.getClassLoader()
        );

        assertEquals(2, result.totalCount());
        assertEquals(0, result.passedCount());
        assertEquals(0, result.failedCount());
        assertEquals(0, result.brokenCount());
        assertEquals(2, result.skippedCount());
        assertTrue(result.isSuccessful());
        assertFalse(result.hasFailures());

        SpecResult nonLoadableResult = result.specResults().get(0);
        assertFalse(nonLoadableResult.isExecutable());
        assertTrue(nonLoadableResult.notExecutableReason().contains("Specification class not found"));
        ExampleResult nonLoadableExample = nonLoadableResult.exampleResults().get(0);
        assertEquals(ExampleStatus.SKIPPED, nonLoadableExample.status());
        assertTrue(nonLoadableExample.detail().contains("Specification class not found"));
        assertFalse(nonLoadableExample.hasFailureDetail());

        SpecResult missingMethodResult = result.specResults().get(1);
        assertTrue(missingMethodResult.isExecutable());
        ExampleResult missingMethodExample = missingMethodResult.exampleResults().get(0);
        assertEquals(ExampleStatus.SKIPPED, missingMethodExample.status());
        assertTrue(missingMethodExample.detail().contains("Example method not found"));
        assertFalse(missingMethodExample.hasFailureDetail());
    }

    @Test
    public void createsFreshSpecInstancePerExample() {
        FreshInstanceSpec.reset();

        RunResult result = run(FreshInstanceSpec.class, "it_records_first_instance", "it_records_second_instance");

        assertEquals(2, result.totalCount());
        assertEquals(2, result.passedCount());
        assertEquals(2, FreshInstanceSpec.constructorCalls);
        assertEquals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2)), FreshInstanceSpec.observedInstanceNumbers);
    }

    @Test
    public void runsLetBeforeEachExampleAndLetGoAfterEachExampleIncludingFailure() {
        LifecycleSpec.reset();

        RunResult result = run(LifecycleSpec.class, "it_passes_with_lifecycle", "it_fails_with_lifecycle");

        assertEquals(2, result.totalCount());
        assertEquals(1, result.passedCount());
        assertEquals(1, result.failedCount());
        assertEquals(0, result.brokenCount());
        assertEquals(Arrays.asList(
                "let:1",
                "example:passes:1:true",
                "letGo:1:true",
                "let:2",
                "example:fails:2:true",
                "letGo:2:true"
        ), LifecycleSpec.events);
    }

    private static RunResult run(Class<?> specClass, String... exampleNames) {
        return SpecRunner.run(spec(specClass, exampleNames), SpecRunnerTest.class.getClassLoader());
    }

    private static DiscoveredSpec spec(Class<?> specClass, String... exampleNames) {
        return namedSpec(specClass.getName(), "example." + specClass.getSimpleName(), exampleNames);
    }

    private static DiscoveredSpec namedSpec(String specQualifiedName, String describedQualifiedName, String... exampleNames) {
        List<SpecExample> examples = new ArrayList<SpecExample>();
        for (int i = 0; i < exampleNames.length; i++) {
            examples.add(SpecExample.of(exampleNames[i], i));
        }
        return DiscoveredSpec.of(
                new File("unused/" + specQualifiedName.replace('.', '/') + ".java"),
                specQualifiedName,
                DescribedType.of(describedQualifiedName),
                examples
        );
    }

    public static final class MetadataOnlySpec {
        static boolean includedExampleRan;
        static boolean undiscoveredExampleRan;

        public static void reset() {
            includedExampleRan = false;
            undiscoveredExampleRan = false;
        }

        public void it_in_metadata() {
            includedExampleRan = true;
        }

        public void it_not_in_metadata() {
            undiscoveredExampleRan = true;
        }
    }

    public static final class StatusMappingSpec {
        public void it_passes() {
        }

        public void it_fails() {
            throw new AssertionError("expected failure");
        }

        public void it_breaks() {
            throw new IllegalStateException("unexpected boom");
        }
    }

    public static final class Phase7MatcherSpec extends ObjectBehavior<Object> {
        public void it_passes_with_expanded_matchers() {
            match("emerald city").shouldNotStartWith("ruby");
            match(Arrays.asList("heart", "brain")).shouldHaveCount(2);
        }

        public void it_fails_with_expanded_matcher() {
            match("emerald city").shouldNotEndWith("city");
        }
    }

    public static final class MissingMethodSpec {
        public void it_present() {
        }
    }

    public static final class FreshInstanceSpec {
        static int constructorCalls;
        static List<Integer> observedInstanceNumbers = new ArrayList<Integer>();

        private final int instanceNumber;

        public FreshInstanceSpec() {
            constructorCalls++;
            instanceNumber = constructorCalls;
        }

        public static void reset() {
            constructorCalls = 0;
            observedInstanceNumbers = new ArrayList<Integer>();
        }

        public void it_records_first_instance() {
            observedInstanceNumbers.add(Integer.valueOf(instanceNumber));
        }

        public void it_records_second_instance() {
            observedInstanceNumbers.add(Integer.valueOf(instanceNumber));
        }
    }

    public static final class LifecycleSpec {
        static int constructorCalls;
        static List<String> events = new ArrayList<String>();

        private final int instanceNumber;
        private boolean prepared;

        public LifecycleSpec() {
            constructorCalls++;
            instanceNumber = constructorCalls;
        }

        public static void reset() {
            constructorCalls = 0;
            events = new ArrayList<String>();
        }

        public void let() {
            prepared = true;
            events.add("let:" + instanceNumber);
        }

        public void it_passes_with_lifecycle() {
            events.add("example:passes:" + instanceNumber + ":" + prepared);
        }

        public void it_fails_with_lifecycle() {
            events.add("example:fails:" + instanceNumber + ":" + prepared);
            throw new AssertionError("failure after example body");
        }

        public void letGo() {
            events.add("letGo:" + instanceNumber + ":" + prepared);
        }
    }
}
