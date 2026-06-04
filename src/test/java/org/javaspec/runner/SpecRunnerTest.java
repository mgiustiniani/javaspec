package org.javaspec.runner;

import org.javaspec.api.ObjectBehavior;
import org.javaspec.api.Pending;
import org.javaspec.api.PendingExampleException;
import org.javaspec.api.Skip;
import org.javaspec.api.SkipExampleException;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecExample;
import org.javaspec.doubles.InterfaceDouble;
import org.javaspec.invocation.JavaspecExitCode;
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
    public void annotationBasedSkipAndPendingResultsUseReasonsAndSkipPrecedence() {
        RunResult result = run(
                AnnotationSignalSpec.class,
                "it_is_skipped_by_default",
                "it_is_skipped_with_value",
                "it_is_skipped_with_reason",
                "it_is_pending_by_default",
                "it_is_pending_with_value",
                "it_is_pending_with_reason",
                "it_prefers_skip_over_pending"
        );

        assertEquals(7, result.totalCount());
        assertEquals(0, result.passedCount());
        assertEquals(0, result.failedCount());
        assertEquals(0, result.brokenCount());
        assertEquals(4, result.skippedCount());
        assertEquals(3, result.pendingCount());
        assertEquals(7, result.skippedOrPendingCount());
        assertTrue(result.isSuccessful());

        assertExample(result.exampleResults().get(0), ExampleStatus.SKIPPED, "Skipped by javaspec.");
        assertExample(result.exampleResults().get(1), ExampleStatus.SKIPPED, "skip value");
        assertExample(result.exampleResults().get(2), ExampleStatus.SKIPPED, "skip reason");
        assertExample(result.exampleResults().get(3), ExampleStatus.PENDING, "Pending by javaspec.");
        assertExample(result.exampleResults().get(4), ExampleStatus.PENDING, "pending value");
        assertExample(result.exampleResults().get(5), ExampleStatus.PENDING, "pending reason");
        assertExample(result.exampleResults().get(6), ExampleStatus.SKIPPED, "skip wins");
    }

    @Test
    public void annotationSkipAndPendingDoNotConstructSpecOrRunLifecycleOrExampleBody() {
        AnnotationLifecycleSpec.reset();

        RunResult result = run(
                AnnotationLifecycleSpec.class,
                "it_is_skipped_without_lifecycle",
                "it_is_pending_without_lifecycle"
        );

        assertEquals(2, result.totalCount());
        assertEquals(1, result.skippedCount());
        assertEquals(1, result.pendingCount());
        assertEquals(0, AnnotationLifecycleSpec.constructorCalls);
        assertEquals(0, AnnotationLifecycleSpec.letCalls);
        assertEquals(0, AnnotationLifecycleSpec.letGoCalls);
        assertEquals(0, AnnotationLifecycleSpec.exampleCalls);
    }

    @Test
    public void runtimeSkipAndPendingExceptionsFromExamplesAndConvenienceMethodsMapToNonExecutedResults() {
        RunResult result = run(
                RuntimeSignalSpec.class,
                "it_skips_with_direct_exception",
                "it_pends_with_direct_exception",
                "it_skips_with_convenience_method",
                "it_pends_with_convenience_method"
        );

        assertEquals(4, result.totalCount());
        assertEquals(0, result.passedCount());
        assertEquals(0, result.failedCount());
        assertEquals(0, result.brokenCount());
        assertEquals(2, result.skippedCount());
        assertEquals(2, result.pendingCount());
        assertTrue(result.isSuccessful());
        assertFalse(result.hasFailures());
        assertEquals(0, JavaspecExitCode.from(result));

        assertExample(result.exampleResults().get(0), ExampleStatus.SKIPPED, "direct skip");
        assertExample(result.exampleResults().get(1), ExampleStatus.PENDING, "direct pending");
        assertExample(result.exampleResults().get(2), ExampleStatus.SKIPPED, "helper skip");
        assertExample(result.exampleResults().get(3), ExampleStatus.PENDING, "helper pending");
    }

    @Test
    public void runtimeSkipAndPendingFromLetStillRunLetGo() {
        LetSkipSignalSpec.reset();
        RunResult skipped = run(LetSkipSignalSpec.class, "it_never_reaches_example_body");

        assertEquals(1, skipped.skippedCount());
        assertEquals(0, skipped.pendingCount());
        assertExample(skipped.exampleResults().get(0), ExampleStatus.SKIPPED, "skip in let");
        assertEquals(Arrays.asList("let", "letGo"), LetSkipSignalSpec.events);

        LetPendingSignalSpec.reset();
        RunResult pending = run(LetPendingSignalSpec.class, "it_never_reaches_example_body");

        assertEquals(0, pending.skippedCount());
        assertEquals(1, pending.pendingCount());
        assertExample(pending.exampleResults().get(0), ExampleStatus.PENDING, "pending in let");
        assertEquals(Arrays.asList("let", "letGo"), LetPendingSignalSpec.events);
    }

    @Test
    public void letGoFailureAfterSkipOrPendingSignalBecomesBroken() {
        RunResult afterSkip = run(LetGoFailsAfterSkipSpec.class, "it_is_interrupted_by_let");
        ExampleResult brokenAfterSkip = afterSkip.exampleResults().get(0);

        assertEquals(1, afterSkip.brokenCount());
        assertEquals(0, afterSkip.skippedCount());
        assertEquals(0, afterSkip.pendingCount());
        assertEquals(ExampleStatus.BROKEN, brokenAfterSkip.status());
        assertEquals("letGo() failed after skipped example", brokenAfterSkip.detail());
        assertNotNull(brokenAfterSkip.failureDetail());
        assertEquals(IllegalStateException.class.getName(), brokenAfterSkip.failureDetail().throwableClassName());
        assertEquals("teardown after skip", brokenAfterSkip.failureDetail().message());

        RunResult afterPending = run(LetGoFailsAfterPendingSpec.class, "it_is_interrupted_by_let");
        ExampleResult brokenAfterPending = afterPending.exampleResults().get(0);

        assertEquals(1, afterPending.brokenCount());
        assertEquals(0, afterPending.skippedCount());
        assertEquals(0, afterPending.pendingCount());
        assertEquals(ExampleStatus.BROKEN, brokenAfterPending.status());
        assertEquals("letGo() failed after pending example", brokenAfterPending.detail());
        assertNotNull(brokenAfterPending.failureDetail());
        assertEquals(IllegalStateException.class.getName(), brokenAfterPending.failureDetail().throwableClassName());
        assertEquals("teardown after pending", brokenAfterPending.failureDetail().message());
    }

    @Test
    public void aggregateResultsKeepSkippedAndPendingCountsSeparateAndSuccessful() {
        ExampleResult passed = ExampleResult.of("spec.example.AggregateSpec", "it_passes", "it_passes", 0,
                ExampleStatus.PASSED, "", null);
        ExampleResult skipped = ExampleResult.of("spec.example.AggregateSpec", "it_skips", "it skips", 1,
                ExampleStatus.SKIPPED, "skip reason", null);
        ExampleResult pending = ExampleResult.of("spec.example.AggregateSpec", "it_pends", "it pends", 2,
                ExampleStatus.PENDING, "pending reason", null);
        SpecResult specResult = SpecResult.of("spec.example.AggregateSpec", Arrays.asList(passed, skipped, pending));
        RunResult runResult = RunResult.of(Arrays.asList(specResult));

        assertEquals(3, specResult.totalCount());
        assertEquals(1, specResult.passedCount());
        assertEquals(1, specResult.skippedCount());
        assertEquals(1, specResult.pendingCount());
        assertEquals(2, specResult.skippedOrPendingCount());
        assertEquals(Arrays.asList(skipped), specResult.skippedExamples());
        assertEquals(Arrays.asList(pending), specResult.pendingExamples());
        assertEquals(Arrays.asList(skipped, pending), specResult.skippedOrPendingExamples());
        assertTrue(specResult.isSuccessful());

        assertEquals(3, runResult.totalCount());
        assertEquals(1, runResult.passedCount());
        assertEquals(1, runResult.skippedCount());
        assertEquals(1, runResult.pendingCount());
        assertEquals(2, runResult.skippedOrPendingCount());
        assertEquals(Arrays.asList(skipped), runResult.skippedExamples());
        assertEquals(Arrays.asList(pending), runResult.pendingExamples());
        assertEquals(Arrays.asList(skipped, pending), runResult.skippedOrPendingExamples());
        assertTrue(runResult.isSuccessful());
        assertFalse(runResult.hasFailures());
        assertEquals(0, JavaspecExitCode.from(runResult));
    }

    @Test
    public void exampleResultsPropagateStableIdsAndSourceLocationFromDiscoveredMetadata() {
        File specFile = new File("src/test/java/spec/example/MetadataSpec.java");
        SpecExample example = SpecExample.of("it_reports_metadata", 2, 37);
        DiscoveredSpec spec = DiscoveredSpec.of(
                specFile,
                "spec.example.MetadataSpec",
                DescribedType.of("example.Metadata"),
                Arrays.asList(example)
        );

        ExampleResult result = ExampleResult.passed(spec, example);
        SpecResult specResult = SpecResult.executable(spec, Arrays.asList(result));

        assertEquals("spec.example.MetadataSpec#it_reports_metadata", result.fullName());
        assertEquals(result.fullName(), result.id());
        assertEquals(result.fullName(), result.stableId());
        assertEquals(specFile.getPath(), result.sourceFilePath());
        assertEquals(specFile.getPath(), result.sourceFile());
        assertTrue(result.hasSourceFile());
        assertEquals(37, result.sourceLine());
        assertEquals(37, result.lineNumber());
        assertTrue(result.hasSourceLine());
        assertTrue(result.hasSourceLocation());
        assertEquals("spec.example.MetadataSpec", specResult.id());
        assertEquals("spec.example.MetadataSpec", specResult.stableId());
        assertEquals(specFile.getPath(), specResult.sourceFilePath());
        assertTrue(specResult.hasSourceFile());
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
    public void executesCompiledObjectBehaviorSpecUsingInterfaceDoubles() {
        RunResult result = run(
                Phase8DoubleSpec.class,
                "it_passes_with_interface_double",
                "it_fails_with_interface_double_verification"
        );

        assertEquals(2, result.totalCount());
        assertEquals(1, result.passedCount());
        assertEquals(1, result.failedCount());
        assertEquals(0, result.brokenCount());

        ExampleResult failed = result.exampleResults().get(1);
        assertEquals(ExampleStatus.FAILED, failed.status());
        assertNotNull(failed.failureDetail());
        assertTrue(failed.failureDetail().message().contains("method 'publish' not to have been called"));
    }

    @Test
    public void skipsNonLoadableSpecClassAndMissingReflectedExampleMethodsWithActionableReasons() {
        DiscoveredSpec nonLoadableSpec = namedSpec(
                "missing.runner.NonLoadableSpec",
                "missing.runner.NonLoadable",
                "it_is_skipped"
        );
        DiscoveredSpec missingMethodSpec = spec(MissingMethodSpec.class, "it_is_missing");
        DiscoveredSpec nonPublicMethodSpec = spec(NonPublicMethodSpec.class, "it_is_not_public");
        DiscoveredSpec noArgMismatchSpec = spec(NoArgMismatchSpec.class, "it_requires_argument");

        RunResult result = SpecRunner.run(
                Arrays.asList(nonLoadableSpec, missingMethodSpec, nonPublicMethodSpec, noArgMismatchSpec),
                SpecRunnerTest.class.getClassLoader()
        );

        assertEquals(4, result.totalCount());
        assertEquals(0, result.passedCount());
        assertEquals(0, result.failedCount());
        assertEquals(0, result.brokenCount());
        assertEquals(4, result.skippedCount());
        assertTrue(result.isSuccessful());
        assertFalse(result.hasFailures());

        SpecResult nonLoadableResult = result.specResults().get(0);
        assertFalse(nonLoadableResult.isExecutable());
        assertNotExecutableReasonIsActionable(nonLoadableResult.notExecutableReason());
        ExampleResult nonLoadableExample = nonLoadableResult.exampleResults().get(0);
        assertEquals(ExampleStatus.SKIPPED, nonLoadableExample.status());
        assertNotExecutableReasonIsActionable(nonLoadableExample.detail());
        assertFalse(nonLoadableExample.hasFailureDetail());

        assertReflectedMethodSkip(result.specResults().get(1), "it_is_missing");
        assertReflectedMethodSkip(result.specResults().get(2), "it_is_not_public");
        assertReflectedMethodSkip(result.specResults().get(3), "it_requires_argument");
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

    private static void assertExample(ExampleResult result, ExampleStatus status, String detail) {
        assertEquals(status, result.status());
        assertEquals(detail, result.detail());
        assertFalse(result.hasFailureDetail());
    }

    private static void assertNotExecutableReasonIsActionable(String reason) {
        assertTrue(reason.contains("Specification class not found"));
        assertTrue(reason.contains("compiled specification class is not available"));
        assertTrue(reason.contains("Compile the spec/test sources"));
        assertTrue(reason.contains("javaspec classpath"));
    }

    private static void assertReflectedMethodSkip(SpecResult specResult, String methodName) {
        assertTrue(specResult.isExecutable());
        ExampleResult example = specResult.exampleResults().get(0);
        assertEquals(ExampleStatus.SKIPPED, example.status());
        assertTrue(example.detail().contains("Example method not found or not public no-arg: " + methodName));
        assertTrue(example.detail().contains("discovered specification source may not match the compiled specification class"));
        assertTrue(example.detail().contains("Recompile test/spec sources"));
        assertTrue(example.detail().contains("public no-argument example method"));
        assertFalse(example.hasFailureDetail());
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

    public static final class Phase8DoubleSpec extends ObjectBehavior<Object> {
        public void it_passes_with_interface_double() {
            InterfaceDouble<Phase8Collaborator> collaborator = interfaceDouble(Phase8Collaborator.class);
            collaborator.when("compose", "Ada").thenReturn("Hello Ada");

            String message = collaborator.instance().compose("Ada");
            collaborator.instance().publish(message);

            shouldReturn(message, "Hello Ada");
            shouldHaveBeenCalledWith(collaborator.instance(), "compose", "Ada");
            shouldHaveBeenCalledTimes(collaborator.instance(), "publish", 1, "Hello Ada");
        }

        public void it_fails_with_interface_double_verification() {
            Phase8Collaborator collaborator = doubleFor(Phase8Collaborator.class);
            collaborator.publish("unexpected");

            shouldNotHaveBeenCalled(collaborator, "publish");
        }
    }

    public interface Phase8Collaborator {
        String compose(String name);

        void publish(String message);
    }

    public static final class AnnotationSignalSpec {
        @Skip
        public void it_is_skipped_by_default() {
            throw new AssertionError("annotation skip should not run the example body");
        }

        @Skip("skip value")
        public void it_is_skipped_with_value() {
            throw new AssertionError("annotation skip value should not run the example body");
        }

        @Skip(reason = "skip reason")
        public void it_is_skipped_with_reason() {
            throw new AssertionError("annotation skip reason should not run the example body");
        }

        @Pending
        public void it_is_pending_by_default() {
            throw new AssertionError("annotation pending should not run the example body");
        }

        @Pending("pending value")
        public void it_is_pending_with_value() {
            throw new AssertionError("annotation pending value should not run the example body");
        }

        @Pending(reason = "pending reason")
        public void it_is_pending_with_reason() {
            throw new AssertionError("annotation pending reason should not run the example body");
        }

        @Skip(reason = "skip wins")
        @Pending(reason = "pending loses")
        public void it_prefers_skip_over_pending() {
            throw new AssertionError("skip must take precedence over pending");
        }
    }

    public static final class AnnotationLifecycleSpec {
        static int constructorCalls;
        static int letCalls;
        static int letGoCalls;
        static int exampleCalls;

        public AnnotationLifecycleSpec() {
            constructorCalls++;
        }

        static void reset() {
            constructorCalls = 0;
            letCalls = 0;
            letGoCalls = 0;
            exampleCalls = 0;
        }

        public void let() {
            letCalls++;
        }

        @Skip(reason = "not runnable")
        public void it_is_skipped_without_lifecycle() {
            exampleCalls++;
        }

        @Pending(reason = "not implemented")
        public void it_is_pending_without_lifecycle() {
            exampleCalls++;
        }

        public void letGo() {
            letGoCalls++;
        }
    }

    public static final class RuntimeSignalSpec extends ObjectBehavior<Object> {
        public void it_skips_with_direct_exception() {
            throw new SkipExampleException("direct skip");
        }

        public void it_pends_with_direct_exception() {
            throw new PendingExampleException("direct pending");
        }

        public void it_skips_with_convenience_method() {
            skip("helper skip");
        }

        public void it_pends_with_convenience_method() {
            pending("helper pending");
        }
    }

    public static final class LetSkipSignalSpec {
        static List<String> events = new ArrayList<String>();

        static void reset() {
            events = new ArrayList<String>();
        }

        public void let() {
            events.add("let");
            throw new SkipExampleException("skip in let");
        }

        public void it_never_reaches_example_body() {
            events.add("example");
        }

        public void letGo() {
            events.add("letGo");
        }
    }

    public static final class LetPendingSignalSpec {
        static List<String> events = new ArrayList<String>();

        static void reset() {
            events = new ArrayList<String>();
        }

        public void let() {
            events.add("let");
            throw new PendingExampleException("pending in let");
        }

        public void it_never_reaches_example_body() {
            events.add("example");
        }

        public void letGo() {
            events.add("letGo");
        }
    }

    public static final class LetGoFailsAfterSkipSpec {
        public void let() {
            throw new SkipExampleException("skip before teardown");
        }

        public void it_is_interrupted_by_let() {
        }

        public void letGo() {
            throw new IllegalStateException("teardown after skip");
        }
    }

    public static final class LetGoFailsAfterPendingSpec {
        public void let() {
            throw new PendingExampleException("pending before teardown");
        }

        public void it_is_interrupted_by_let() {
        }

        public void letGo() {
            throw new IllegalStateException("teardown after pending");
        }
    }

    public static final class MissingMethodSpec {
        public void it_present() {
        }
    }

    public static final class NonPublicMethodSpec {
        private void it_is_not_public() {
        }
    }

    public static final class NoArgMismatchSpec {
        public void it_requires_argument(String value) {
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
