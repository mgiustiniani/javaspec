package io.github.jvmspec.junit.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaspecTestEnginePhase17Test {
    private static final String ENGINE_ID = "javaspec";

    @Test
    void serviceLoaderDiscoversJavaspecEngineId() {
        List<String> engineIds = new ArrayList<String>();
        for (TestEngine engine : ServiceLoader.load(TestEngine.class)) {
            engineIds.add(engine.getId());
        }

        assertTrue(engineIds.contains(ENGINE_ID), "Expected ServiceLoader engine ids to include javaspec but were " + engineIds);
    }

    @Test
    void noSpecDiscoveryAndExecutionSucceeds(@TempDir Path temp) throws Exception {
        Path specRoot = Files.createDirectories(temp.resolve("empty-specs"));

        RunOutcome outcome = execute(requestBuilder().configurationParameter("javaspec.specRoot", specRoot.toString()).build());

        assertEquals(0, outcome.summary().getTestsFoundCount());
        assertEquals(0, outcome.summary().getTestsFailedCount());
        assertEquals(0, outcome.recorder().failedEvents().size(), "No descriptor should fail when no specs are present");
    }

    @Test
    void compiledPassingSpecIsDiscoveredAndExecutedSuccessfully(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("specs");
        Path source = writeSpec(specRoot, "phase17.pass", "PassingSpec",
                "    public void it_passes() {\n" +
                "    }\n");

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            Class<?> specClass = classLoader.loadClass("phase17.pass.PassingSpec");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .selectors(DiscoverySelectors.selectClass(specClass))
                            .build(),
                    classLoader
            );

            assertEquals(1, outcome.summary().getTestsFoundCount());
            assertEquals(1, outcome.summary().getTestsSucceededCount());
            assertEquals(0, outcome.summary().getTestsFailedCount());
            assertEquals(0, outcome.summary().getTestsSkippedCount());
        }
    }

    @Test
    void descriptorsUseStableUniqueIdsDisplayNamesAndSources(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("sources-specs");
        Path source = writeSpec(specRoot, "phase17.sources", "SourceSpec",
                "    public void it_has_source_metadata() {\n" +
                "    }\n");

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            Class<?> specClass = classLoader.loadClass("phase17.sources.SourceSpec");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .selectors(DiscoverySelectors.selectClass(specClass))
                            .build(),
                    classLoader
            );

            assertEquals(1, outcome.summary().getTestsSucceededCount());
            assertTrue(outcome.recorder().startedUniqueIds().contains(
                    "[engine:javaspec]/[spec:phase17.sources.SourceSpec]/[example:it_has_source_metadata]"),
                    "Expected stable example unique id in " + outcome.recorder().startedUniqueIds());
            assertTrue(outcome.recorder().startedDisplayNames().contains(
                    "phase17.sources.SourceSpec#it_has_source_metadata"),
                    "Expected stable example display name in " + outcome.recorder().startedDisplayNames());
            SourceEvent specSource = outcome.recorder().sourceFor("phase17.sources.SourceSpec");
            assertTrue(specSource.source() instanceof ClassSource,
                    "Expected ClassSource for spec descriptor but was " + specSource.source());
            assertEquals("phase17.sources.SourceSpec", ((ClassSource) specSource.source()).getClassName());
            SourceEvent exampleSource = outcome.recorder().sourceFor("phase17.sources.SourceSpec#it_has_source_metadata");
            assertTrue(exampleSource.source() instanceof MethodSource,
                    "Expected MethodSource for example descriptor but was " + exampleSource.source());
            MethodSource methodSource = (MethodSource) exampleSource.source();
            assertEquals("phase17.sources.SourceSpec", methodSource.getClassName());
            assertEquals("it_has_source_metadata", methodSource.getMethodName());
        }
    }

    @Test
    void canonicalPhpspecStyleSpecIsDiscoveredAndExecutedSuccessfully(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("specs");
        Path packageDirectory = Files.createDirectories(specRoot.resolve("phase46"));
        Path source = packageDirectory.resolve("GreetingSpec.java");
        Files.write(source, (
                "package phase46;\n\n" +
                        "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
                        "public class GreetingSpec extends ObjectBehavior<GreetingSpec.Greeting> {\n" +
                        "    public GreetingSpec() { super(Greeting.class); }\n" +
                        "\n" +
                        "    public void let() { beConstructedWith(\"Ada\"); }\n" +
                        "\n" +
                        "    public void it_greets_the_configured_subject() {\n" +
                        "        match(subject().message()).shouldReturn(\"Hello Ada\");\n" +
                        "    }\n" +
                        "\n" +
                        "    public static class Greeting {\n" +
                        "        private final String name;\n" +
                        "\n" +
                        "        public Greeting(String name) { this.name = name; }\n" +
                        "\n" +
                        "        public String message() { return \"Hello \" + name; }\n" +
                        "    }\n" +
                        "}\n").getBytes(StandardCharsets.UTF_8));

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            Class<?> specClass = classLoader.loadClass("phase46.GreetingSpec");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .selectors(DiscoverySelectors.selectClass(specClass))
                            .build(),
                    classLoader
            );

            assertEquals(1, outcome.summary().getTestsFoundCount());
            assertEquals(1, outcome.summary().getTestsSucceededCount());
            assertEquals(0, outcome.summary().getTestsFailedCount());
            assertEquals(0, outcome.summary().getTestsSkippedCount());
            assertTrue(outcome.recorder().startedDisplayNames().contains("phase46.GreetingSpec#it_greets_the_configured_subject"),
                    "Expected example display name in " + outcome.recorder().startedDisplayNames());
        }
    }

    @Test
    void exampleDataRowsArePublishedAsDynamicJUnitPlatformDescriptors(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("specs");
        Path packageDirectory = Files.createDirectories(specRoot.resolve("phase47"));
        Path source = packageDirectory.resolve("ExampleDataSpec.java");
        Files.write(source, (
                "package phase47;\n\n" +
                        "public class ExampleDataSpec extends io.github.jvmspec.api.ObjectBehavior<ExampleDataSpec.NameNormalizer> {\n" +
                        "    public ExampleDataSpec() { super(NameNormalizer.class); }\n" +
                        "\n" +
                        "    public void it_reports_row_results() {\n" +
                        "        examples(row(\"Alice\", \"Alice\"), row(\" Bob \", \"Robert\"))\n" +
                        "            .verify(new io.github.jvmspec.api.Example2<String, String>() {\n" +
                        "                public void run(String input, String expected) {\n" +
                        "                    match(subject().normalize(input)).shouldReturn(expected);\n" +
                        "                }\n" +
                        "            });\n" +
                        "    }\n" +
                        "\n" +
                        "    public static class NameNormalizer {\n" +
                        "        public String normalize(String value) { return value.trim(); }\n" +
                        "    }\n" +
                        "}\n").getBytes(StandardCharsets.UTF_8));

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            Class<?> specClass = classLoader.loadClass("phase47.ExampleDataSpec");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .selectors(DiscoverySelectors.selectClass(specClass))
                            .build(),
                    classLoader
            );

            assertTrue(outcome.recorder().dynamicallyRegisteredDisplayNames()
                    .contains("it_reports_row_results[row 1] [Alice, Alice]"),
                    "Expected row 1 dynamic descriptor in " + outcome.recorder().dynamicallyRegisteredDisplayNames());
            assertTrue(outcome.recorder().dynamicallyRegisteredDisplayNames()
                    .contains("it_reports_row_results[row 2] [ Bob , Robert]"),
                    "Expected row 2 dynamic descriptor in " + outcome.recorder().dynamicallyRegisteredDisplayNames());
            assertTrue(outcome.recorder().startedDisplayNames()
                    .contains("it_reports_row_results[row 1] [Alice, Alice]"),
                    "Expected row 1 start event in " + outcome.recorder().startedDisplayNames());
            assertTrue(outcome.recorder().startedDisplayNames()
                    .contains("it_reports_row_results[row 2] [ Bob , Robert]"),
                    "Expected row 2 start event in " + outcome.recorder().startedDisplayNames());
            FinishedEvent rowFailure = failedEventFor(outcome.recorder(), "it_reports_row_results[row 2]");
            assertTrue(rowFailure.throwable().getMessage().contains("Example data row 2 [ Bob , Robert] failed"));
            assertTrue(rowFailure.throwable().getMessage().contains("Expected equality(Robert) but got Bob"));
        }
    }

    @Test
    void rowUniqueIdSelectorsPublishOnlyTheSelectedRowDescriptor(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("row-selector-specs");
        Path packageDirectory = Files.createDirectories(specRoot.resolve("phase47/selector"));
        Path source = packageDirectory.resolve("RowSelectorSpec.java");
        Files.write(source, (
                "package phase47.selector;\n\n" +
                        "public class RowSelectorSpec extends io.github.jvmspec.api.ObjectBehavior<RowSelectorSpec.NameNormalizer> {\n" +
                        "    public static int selectedRuns = 0;\n" +
                        "\n" +
                        "    public RowSelectorSpec() { super(NameNormalizer.class); }\n" +
                        "\n" +
                        "    public void it_selected_by_row_unique_id() {\n" +
                        "        selectedRuns++;\n" +
                        "        examples(row(\"Alice\", \"Alice\"), row(\" Bob \", \"Bob\"))\n" +
                        "            .verify(new io.github.jvmspec.api.Example2<String, String>() {\n" +
                        "                public void run(String input, String expected) {\n" +
                        "                    match(subject().normalize(input)).shouldReturn(expected);\n" +
                        "                }\n" +
                        "            });\n" +
                        "    }\n" +
                        "\n" +
                        "    public void it_should_not_run() {\n" +
                        "        throw new AssertionError(\"row unique id should select only the owning example\");\n" +
                        "    }\n" +
                        "\n" +
                        "    public static class NameNormalizer {\n" +
                        "        public String normalize(String value) { return value.trim(); }\n" +
                        "    }\n" +
                        "}\n").getBytes(StandardCharsets.UTF_8));

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            Class<?> specClass = classLoader.loadClass("phase47.selector.RowSelectorSpec");
            UniqueId selectedRowId = UniqueId.forEngine(ENGINE_ID)
                    .append("spec", "phase47.selector.RowSelectorSpec")
                    .append("example", "it_selected_by_row_unique_id")
                    .append("row", "2");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .selectors(DiscoverySelectors.selectUniqueId(selectedRowId))
                            .build(),
                    classLoader
            );

            assertEquals(2, outcome.summary().getTestsFoundCount());
            assertEquals(0, outcome.summary().getTestsFailedCount());
            assertEquals(1, staticInt(specClass, "selectedRuns"));
            assertFalse(outcome.recorder().dynamicallyRegisteredDisplayNames()
                    .contains("it_selected_by_row_unique_id[row 1] [Alice, Alice]"),
                    "Expected row 1 to stay unpublished for row-2 selector in "
                            + outcome.recorder().dynamicallyRegisteredDisplayNames());
            assertTrue(outcome.recorder().dynamicallyRegisteredDisplayNames()
                    .contains("it_selected_by_row_unique_id[row 2] [ Bob , Bob]"),
                    "Expected selected row 2 dynamic descriptor in "
                            + outcome.recorder().dynamicallyRegisteredDisplayNames());
        }
    }

    @Test
    void rowUniqueIdSelectorsDoNotIsolateRowsFromOwningExampleExecution(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("row-selector-inline-specs");
        Path packageDirectory = Files.createDirectories(specRoot.resolve("phase47/inline"));
        Path source = packageDirectory.resolve("InlineRowSelectorSpec.java");
        Files.write(source, (
                "package phase47.inline;\n\n" +
                        "public class InlineRowSelectorSpec extends io.github.jvmspec.api.ObjectBehavior<InlineRowSelectorSpec.NameNormalizer> {\n" +
                        "    public static int selectedRuns = 0;\n" +
                        "\n" +
                        "    public InlineRowSelectorSpec() { super(NameNormalizer.class); }\n" +
                        "\n" +
                        "    public void it_row_selector_runs_full_example_body() {\n" +
                        "        selectedRuns++;\n" +
                        "        examples(row(\"Alice\", \"Alicia\"), row(\" Bob \", \"Bob\"))\n" +
                        "            .verify(new io.github.jvmspec.api.Example2<String, String>() {\n" +
                        "                public void run(String input, String expected) {\n" +
                        "                    match(subject().normalize(input)).shouldReturn(expected);\n" +
                        "                }\n" +
                        "            });\n" +
                        "    }\n" +
                        "\n" +
                        "    public static class NameNormalizer {\n" +
                        "        public String normalize(String value) { return value.trim(); }\n" +
                        "    }\n" +
                        "}\n").getBytes(StandardCharsets.UTF_8));

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("inline-row-classes"), source)) {
            Class<?> specClass = classLoader.loadClass("phase47.inline.InlineRowSelectorSpec");
            UniqueId selectedRowId = UniqueId.forEngine(ENGINE_ID)
                    .append("spec", "phase47.inline.InlineRowSelectorSpec")
                    .append("example", "it_row_selector_runs_full_example_body")
                    .append("row", "2");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .selectors(DiscoverySelectors.selectUniqueId(selectedRowId))
                            .build(),
                    classLoader
            );

            assertEquals(1, staticInt(specClass, "selectedRuns"));
            assertEquals(1, outcome.summary().getTestsFailedCount());
            TestExecutionSummary.Failure failure = failureFor(outcome.summary(), "it_row_selector_runs_full_example_body");
            assertTrue(failure.getException().getMessage().contains("Example data row 1 [Alice, Alicia] failed"));
            assertFalse(outcome.recorder().dynamicallyRegisteredDisplayNames()
                    .contains("it_row_selector_runs_full_example_body[row 1] [Alice, Alicia]"),
                    "Row 1 descriptor must remain filtered by row-2 selector in "
                            + outcome.recorder().dynamicallyRegisteredDisplayNames());
            assertFalse(outcome.recorder().dynamicallyRegisteredDisplayNames()
                    .contains("it_row_selector_runs_full_example_body[row 2] [ Bob , Bob]"),
                    "Row 2 is not reached because rows execute inline in owning example body: "
                            + outcome.recorder().dynamicallyRegisteredDisplayNames());
        }
    }

    @Test
    void assertionFailureAndUnexpectedThrowableMapToJUnitPlatformFailures(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("specs");
        Path source = writeSpec(specRoot, "phase17.outcomes", "OutcomeSpec",
                "    public void it_passes() {\n" +
                "    }\n" +
                "\n" +
                "    public void it_fails_with_assertion() {\n" +
                "        throw new AssertionError(\"expected assertion failure\");\n" +
                "    }\n" +
                "\n" +
                "    public void it_breaks_with_runtime_exception() {\n" +
                "        throw new IllegalStateException(\"unexpected boom\");\n" +
                "    }\n");

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            RunOutcome outcome = execute(
                    requestBuilder().configurationParameter("javaspec.specRoot", specRoot.toString()).build(),
                    classLoader
            );

            assertEquals(3, outcome.summary().getTestsFoundCount());
            assertEquals(1, outcome.summary().getTestsSucceededCount());
            assertEquals(2, outcome.summary().getTestsFailedCount());
            assertEquals(0, outcome.summary().getTestsSkippedCount());

            TestExecutionSummary.Failure assertionFailure = failureFor(outcome.summary(), "it_fails_with_assertion");
            assertTrue(assertionFailure.getException() instanceof AssertionError);
            assertTrue(assertionFailure.getException().getClass().getName().contains("JavaspecAssertionFailure"));
            assertTrue(assertionFailure.getException().getMessage().contains("Assertion failed"));
            assertTrue(assertionFailure.getException().getMessage().contains("expected assertion failure"));

            TestExecutionSummary.Failure brokenFailure = failureFor(outcome.summary(), "it_breaks_with_runtime_exception");
            assertTrue(brokenFailure.getException() instanceof RuntimeException);
            assertTrue(brokenFailure.getException().getClass().getName().contains("JavaspecBrokenExampleException"));
            assertTrue(brokenFailure.getException().getMessage().contains("Example method threw an unexpected throwable"));
            assertTrue(brokenFailure.getException().getMessage().contains("unexpected boom"));
        }
    }

    @Test
    void pendingExamplesMapToSkippedEventsWithPendingReasonAndNoFailure(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("specs");
        Path source = writeSpec(specRoot, "phase17.pending", "PendingSpec",
                "    @io.github.jvmspec.api.Pending(reason = \"engine pending\")\n" +
                "    public void it_is_pending() {\n" +
                "        throw new AssertionError(\"pending example should not run\");\n" +
                "    }\n");

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            RunOutcome outcome = execute(
                    requestBuilder().configurationParameter("javaspec.specRoot", specRoot.toString()).build(),
                    classLoader
            );

            assertEquals(1, outcome.summary().getTestsFoundCount());
            assertEquals(0, outcome.summary().getTestsSucceededCount());
            assertEquals(0, outcome.summary().getTestsFailedCount());
            assertEquals(1, outcome.summary().getTestsSkippedCount());
            assertEquals(0, outcome.recorder().failedEvents().size());
            SkippedEvent skipped = skippedEventFor(outcome.recorder(), "it_is_pending");
            assertEquals("Pending: engine pending", skipped.reason());
        }
    }

    @Test
    void sourceOnlyNonLoadableSpecsAreReportedAsSkipped(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("specs");
        writeSpec(specRoot, "phase17.sourceonly", "SourceOnlySpec",
                "    public void it_is_discovered_but_not_loadable() {\n" +
                "    }\n");

        RunOutcome outcome = execute(requestBuilder().configurationParameter("javaspec.specRoot", specRoot.toString()).build());

        assertEquals(1, outcome.summary().getTestsFoundCount());
        assertEquals(0, outcome.summary().getTestsSucceededCount());
        assertEquals(0, outcome.summary().getTestsFailedCount());
        assertEquals(1, outcome.summary().getTestsSkippedCount());
        SkippedEvent skipped = skippedEventFor(outcome.recorder(), "it_is_discovered_but_not_loadable");
        assertTrue(skipped.reason().contains("Specification class not found"), skipped.reason());
        assertTrue(skipped.reason().contains("Compile the spec/test sources"), skipped.reason());
        assertTrue(skipped.reason().contains("javaspec classpath"), skipped.reason());
    }

    @Test
    void configFileSuiteSpecRootClassAndExampleFiltersSelectOnlyMatchingExamples(@TempDir Path temp) throws Exception {
        Path configuredRoot = temp.resolve("configured-specs");
        Path defaultRoot = temp.resolve("default-specs");
        Path configuredSource = writeSpec(configuredRoot, "phase17.configured", "ConfiguredSpec",
                "    public static int firstRuns = 0;\n" +
                "    public static int selectedRuns = 0;\n" +
                "\n" +
                "    public void it_first() {\n" +
                "        firstRuns++;\n" +
                "    }\n" +
                "\n" +
                "    public void it_selected() {\n" +
                "        selectedRuns++;\n" +
                "    }\n");
        Path otherSource = writeSpec(configuredRoot, "phase17.configured", "OtherSpec",
                "    public void it_selected() {\n" +
                "        throw new AssertionError(\"class filter should exclude this spec\");\n" +
                "    }\n");
        writeSpec(defaultRoot, "phase17.configured", "DefaultSuiteSpec",
                "    public void it_selected() {\n" +
                "        throw new AssertionError(\"suite selection should exclude this spec\");\n" +
                "    }\n");
        Path configFile = temp.resolve("javaspec.conf");
        Files.write(configFile, Arrays.asList(
                "defaultSuite=default",
                "suite.default.specDir=" + defaultRoot.toString(),
                "suite.acceptance.specDir=" + configuredRoot.toString()
        ), StandardCharsets.UTF_8);

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), configuredSource, otherSource)) {
            Class<?> configuredClass = classLoader.loadClass("phase17.configured.ConfiguredSpec");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.configFile", configFile.toString())
                            .configurationParameter("javaspec.suite", "acceptance")
                            .configurationParameter("javaspec.classFilters", "phase17.configured.ConfiguredSpec")
                            .configurationParameter("javaspec.exampleFilters", "it_selected")
                            .build(),
                    classLoader
            );

            assertEquals(1, outcome.summary().getTestsFoundCount());
            assertEquals(1, outcome.summary().getTestsSucceededCount());
            assertEquals(0, outcome.summary().getTestsFailedCount());
            assertEquals(0, staticInt(configuredClass, "firstRuns"));
            assertEquals(1, staticInt(configuredClass, "selectedRuns"));
        }
    }

    @Test
    void specDirAliasAndSingularClassAndExampleFiltersSelectOnlyMatchingExamples(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("alias-specs");
        Path targetSource = writeSpec(specRoot, "phase17.filters", "FilterTargetSpec",
                "    public static int firstRuns = 0;\n" +
                "    public static int secondRuns = 0;\n" +
                "\n" +
                "    public void it_first() {\n" +
                "        firstRuns++;\n" +
                "    }\n" +
                "\n" +
                "    public void it_second() {\n" +
                "        secondRuns++;\n" +
                "    }\n");
        Path otherSource = writeSpec(specRoot, "phase17.filters", "OtherSpec",
                "    public void it_second() {\n" +
                "        throw new AssertionError(\"singular class filter should exclude this spec\");\n" +
                "    }\n");

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), targetSource, otherSource)) {
            Class<?> targetClass = classLoader.loadClass("phase17.filters.FilterTargetSpec");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specDir", specRoot.toString())
                            .configurationParameter("javaspec.classFilter", "FilterTargetSpec")
                            .configurationParameter("javaspec.exampleFilter", "it_second")
                            .build(),
                    classLoader
            );

            assertEquals(1, outcome.summary().getTestsFoundCount());
            assertEquals(1, outcome.summary().getTestsSucceededCount());
            assertEquals(0, staticInt(targetClass, "firstRuns"));
            assertEquals(1, staticInt(targetClass, "secondRuns"));
        }
    }

    @Test
    void methodSelectorsFilterToSelectedExample(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("selector-specs");
        Path source = writeSpec(specRoot, "phase17.selectors", "MethodSelectorSpec",
                "    public static int firstRuns = 0;\n" +
                "    public static int secondRuns = 0;\n" +
                "\n" +
                "    public void it_first() {\n" +
                "        firstRuns++;\n" +
                "    }\n" +
                "\n" +
                "    public void it_second() {\n" +
                "        secondRuns++;\n" +
                "    }\n");

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            Class<?> specClass = classLoader.loadClass("phase17.selectors.MethodSelectorSpec");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .selectors(DiscoverySelectors.selectMethod(specClass, "it_second"))
                            .build(),
                    classLoader
            );

            assertEquals(1, outcome.summary().getTestsFoundCount());
            assertEquals(1, outcome.summary().getTestsSucceededCount());
            assertEquals(0, staticInt(specClass, "firstRuns"));
            assertEquals(1, staticInt(specClass, "secondRuns"));
        }
    }

    @Test
    void packageSelectorsFilterToSelectedPackage(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("package-selector-specs");
        Path selectedSource = writeSpec(specRoot, "phase17.packages.selected", "PackageSelectedSpec",
                "    public static int selectedRuns = 0;\n" +
                "\n" +
                "    public void it_runs_in_selected_package() {\n" +
                "        selectedRuns++;\n" +
                "    }\n");
        Path otherSource = writeSpec(specRoot, "phase17.packages.other", "PackageOtherSpec",
                "    public void it_should_not_run() {\n" +
                "        throw new AssertionError(\"package selector should exclude this spec\");\n" +
                "    }\n");

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), selectedSource, otherSource)) {
            Class<?> selectedClass = classLoader.loadClass("phase17.packages.selected.PackageSelectedSpec");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .selectors(DiscoverySelectors.selectPackage("phase17.packages.selected"))
                            .build(),
                    classLoader
            );

            assertEquals(1, outcome.summary().getTestsFoundCount());
            assertEquals(1, outcome.summary().getTestsSucceededCount());
            assertEquals(1, staticInt(selectedClass, "selectedRuns"));
        }
    }

    @Test
    void uniqueIdSelectorsFilterToSelectedExample(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("unique-id-specs");
        Path source = writeSpec(specRoot, "phase17.unique", "UniqueSelectorSpec",
                "    public static int skippedRuns = 0;\n" +
                "    public static int selectedRuns = 0;\n" +
                "\n" +
                "    public void it_skipped_by_unique_id() {\n" +
                "        skippedRuns++;\n" +
                "    }\n" +
                "\n" +
                "    public void it_selected_by_unique_id() {\n" +
                "        selectedRuns++;\n" +
                "    }\n");

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            Class<?> specClass = classLoader.loadClass("phase17.unique.UniqueSelectorSpec");
            UniqueId selectedExampleId = UniqueId.forEngine(ENGINE_ID)
                    .append("spec", "phase17.unique.UniqueSelectorSpec")
                    .append("example", "it_selected_by_unique_id");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .selectors(DiscoverySelectors.selectUniqueId(selectedExampleId))
                            .build(),
                    classLoader
            );

            assertEquals(1, outcome.summary().getTestsFoundCount());
            assertEquals(1, outcome.summary().getTestsSucceededCount());
            assertEquals(0, staticInt(specClass, "skippedRuns"));
            assertEquals(1, staticInt(specClass, "selectedRuns"));
        }
    }

    @Test
    void stopOnFailureSkipsRemainingDiscoveredExamples(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("stop-specs");
        Path source = writeSpec(specRoot, "phase17.stop", "StopOnFailureSpec",
                "    public static int afterFailureRuns = 0;\n" +
                "\n" +
                "    public void it_fails_first() {\n" +
                "        throw new AssertionError(\"first failure\");\n" +
                "    }\n" +
                "\n" +
                "    public void it_should_be_skipped_after_failure() {\n" +
                "        afterFailureRuns++;\n" +
                "    }\n");

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), source)) {
            Class<?> specClass = classLoader.loadClass("phase17.stop.StopOnFailureSpec");
            RunOutcome outcome = execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .configurationParameter("javaspec.stopOnFailure", "true")
                            .build(),
                    classLoader
            );

            assertEquals(2, outcome.summary().getTestsFoundCount());
            assertEquals(0, outcome.summary().getTestsSucceededCount());
            assertEquals(1, outcome.summary().getTestsFailedCount());
            assertEquals(1, outcome.summary().getTestsSkippedCount());
            assertEquals(0, staticInt(specClass, "afterFailureRuns"));
            SkippedEvent skipped = skippedEventFor(outcome.recorder(), "it_should_be_skipped_after_failure");
            assertTrue(skipped.reason().contains("stop-on-failure"), skipped.reason());
        }
    }

    @Test
    void bootstrapDiscoveryParameterExecutesServiceLoaderHookBeforeExamples(@TempDir Path temp) throws Exception {
        RunOutcome outcome = executeBootstrapDiscoveryCase(
                temp,
                "enabled",
                "true",
                "discovered"
        );

        assertEquals(1, outcome.summary().getTestsFoundCount());
        assertEquals(1, outcome.summary().getTestsSucceededCount());
        assertEquals(0, outcome.summary().getTestsFailedCount());
    }

    @Test
    void absentOrFalseBootstrapDiscoveryParameterLeavesServiceLoaderHookUnexecuted(@TempDir Path temp) throws Exception {
        RunOutcome absent = executeBootstrapDiscoveryCase(
                temp,
                "absent",
                null,
                ""
        );
        RunOutcome disabled = executeBootstrapDiscoveryCase(
                temp,
                "disabled",
                "false",
                ""
        );

        assertEquals(1, absent.summary().getTestsSucceededCount());
        assertEquals(0, absent.summary().getTestsFailedCount());
        assertEquals(1, disabled.summary().getTestsSucceededCount());
        assertEquals(0, disabled.summary().getTestsFailedCount());
    }

    @Test
    void invalidBootstrapDiscoveryParameterThrowsConfigurationError(@TempDir Path temp) throws Exception {
        Path specRoot = Files.createDirectories(temp.resolve("invalid-bootstrap-discovery-specs"));

        JUnitException failure = assertThrows(JUnitException.class, () -> execute(
                requestBuilder()
                        .configurationParameter("javaspec.specRoot", specRoot.toString())
                        .configurationParameter("javaspec.bootstrapDiscovery", "definitely")
                        .build()
        ));

        assertThrowableContains(failure, "Invalid javaspec configuration parameter 'javaspec.bootstrapDiscovery'");
        assertThrowableContains(failure, "expected true or false");
        assertThrowableContains(failure, "definitely");
    }

    @Test
    void configurationParameterExtensionsRegisterFormatterAndFormatterParameterSelectsIt(@TempDir Path temp) throws Exception {
        Path specRoot = temp.resolve("formatter-specs");
        Path specSource = writeSpec(specRoot, "phase32.engine", "FormatterSpec",
                "    public void it_passes_with_configured_formatter() {\n" +
                "    }\n");
        Path noopExtension = writeNoopExtensionSource(temp, "phase32.engine.NoopPhase32Extension");
        Path formatterExtension = writeFormatterExtensionSource(
                temp,
                "phase32.engine.Phase32EngineFormatterExtension",
                "phase32-engine",
                "junit phase32 formatter"
        );

        try (URLClassLoader classLoader = compileToClassLoader(
                temp.resolve("classes"),
                specSource,
                noopExtension,
                formatterExtension
        )) {
            CapturedRunOutcome outcome = executeCapturingOutput(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .configurationParameter("javaspec.extensions",
                                    "phase32.engine.NoopPhase32Extension ; phase32.engine.Phase32EngineFormatterExtension")
                            .configurationParameter("javaspec.formatter", "phase32-engine")
                            .build(),
                    classLoader
            );

            assertEquals(1, outcome.runOutcome().summary().getTestsFoundCount());
            assertEquals(1, outcome.runOutcome().summary().getTestsSucceededCount());
            assertEquals(0, outcome.runOutcome().summary().getTestsFailedCount());
            assertTrue(outcome.output().contains("junit phase32 formatter total=1 passed=1 failed=0"), outcome.output());
        }
    }

    @Test
    void invalidFormatterParameterListsExtensionRegisteredFormatterNames(@TempDir Path temp) throws Exception {
        Path specRoot = Files.createDirectories(temp.resolve("invalid-formatter-specs"));
        Path formatterExtension = writeFormatterExtensionSource(
                temp,
                "phase32.engine.AvailableFormatterExtension",
                "phase32-available",
                "available junit formatter"
        );

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), formatterExtension)) {
            JUnitException failure = assertThrows(JUnitException.class, () -> execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .configurationParameter("javaspec.extensions", "phase32.engine.AvailableFormatterExtension")
                            .configurationParameter("javaspec.formatter", "missing")
                            .build(),
                    classLoader
            ));

            assertThrowableContains(failure, "Invalid javaspec formatter: missing.");
            assertThrowableContains(failure, "Valid values: progress, pretty, json, phase32-available.");
        }
    }

    @Test
    void activationFailureParameterFailsWithJUnitExceptionMessage(@TempDir Path temp) throws Exception {
        Path specRoot = Files.createDirectories(temp.resolve("activation-failure-specs"));
        Path failingExtension = writeFailingExtensionSource(
                temp,
                "phase32.engine.FailingPhase32Extension",
                "phase 32 JUnit extension failure"
        );

        try (URLClassLoader classLoader = compileToClassLoader(temp.resolve("classes"), failingExtension)) {
            JUnitException failure = assertThrows(JUnitException.class, () -> execute(
                    requestBuilder()
                            .configurationParameter("javaspec.specRoot", specRoot.toString())
                            .configurationParameter("javaspec.extensions", "phase32.engine.FailingPhase32Extension")
                            .build(),
                    classLoader
            ));

            assertThrowableContains(failure, "javaspec extension activation failed:");
            assertThrowableContains(failure, "phase32.engine.FailingPhase32Extension");
            assertThrowableContains(failure, "phase 32 JUnit extension failure");
        }
    }

    @Test
    void engineSourceUsesCanonicalLauncherAndDoesNotCallSystemExit() throws Exception {
        Path sourcePath = Paths.get("src/main/java/io/github/jvmspec/junit/platform/JavaspecTestEngine.java");
        String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);

        assertTrue(source.contains("JavaspecLauncher.run"), "Engine should delegate execution to the canonical JavaspecLauncher");
        assertFalse(source.contains("System.exit"), "JUnit Platform engine must never terminate the hosting JVM");
        assertFalse(source.contains("io.github.jvmspec.cli.Main"), "JUnit Platform engine should not execute through the CLI adapter");
    }

    private static LauncherDiscoveryRequestBuilder requestBuilder() {
        return LauncherDiscoveryRequestBuilder.request().filters(EngineFilter.includeEngines(ENGINE_ID));
    }

    private static RunOutcome execute(LauncherDiscoveryRequest request) {
        return execute(request, Thread.currentThread().getContextClassLoader());
    }

    private static RunOutcome execute(LauncherDiscoveryRequest request, ClassLoader classLoader) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            Launcher launcher = LauncherFactory.create();
            SummaryGeneratingListener summary = new SummaryGeneratingListener();
            RecordingListener recorder = new RecordingListener();
            launcher.execute(request, summary, recorder);
            return new RunOutcome(summary.getSummary(), recorder);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private static CapturedRunOutcome executeCapturingOutput(LauncherDiscoveryRequest request, ClassLoader classLoader) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(output, true, "UTF-8");
        try {
            System.setOut(capture);
            RunOutcome outcome = execute(request, classLoader);
            capture.flush();
            return new CapturedRunOutcome(outcome, new String(output.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
            capture.close();
        }
    }

    private static TestExecutionSummary.Failure failureFor(TestExecutionSummary summary, String displayNameFragment) {
        List<TestExecutionSummary.Failure> failures = summary.getFailures();
        for (int i = 0; i < failures.size(); i++) {
            TestExecutionSummary.Failure failure = failures.get(i);
            if (failure.getTestIdentifier().getDisplayName().contains(displayNameFragment)) {
                return failure;
            }
        }
        throw new AssertionError("Expected failure containing display name fragment '" + displayNameFragment + "' but got " + failures);
    }

    private static void assertThrowableContains(Throwable throwable, String expectedFragment) {
        if (throwableContains(throwable, expectedFragment)) {
            return;
        }
        throw new AssertionError("Expected throwable chain to contain '" + expectedFragment + "' but was: " + throwable,
                throwable);
    }

    private static boolean throwableContains(Throwable throwable, String expectedFragment) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(expectedFragment)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static FinishedEvent failedEventFor(RecordingListener recorder, String displayNameFragment) {
        List<FinishedEvent> failedEvents = recorder.failedEvents();
        for (int i = 0; i < failedEvents.size(); i++) {
            FinishedEvent failed = failedEvents.get(i);
            if (failed.displayName().contains(displayNameFragment)) {
                return failed;
            }
        }
        throw new AssertionError("Expected failed event containing display name fragment '" + displayNameFragment
                + "' but got " + failedEvents);
    }

    private static SkippedEvent skippedEventFor(RecordingListener recorder, String displayNameFragment) {
        List<SkippedEvent> skippedEvents = recorder.skippedEvents();
        for (int i = 0; i < skippedEvents.size(); i++) {
            SkippedEvent skipped = skippedEvents.get(i);
            if (skipped.displayName().contains(displayNameFragment)) {
                return skipped;
            }
        }
        throw new AssertionError("Expected skipped event containing display name fragment '" + displayNameFragment
                + "' but got " + skippedEvents);
    }

    private static int staticInt(Class<?> type, String fieldName) throws Exception {
        return type.getField(fieldName).getInt(null);
    }

    private static String staticString(Class<?> type, String fieldName) throws Exception {
        return (String) type.getField(fieldName).get(null);
    }

    private static RunOutcome executeBootstrapDiscoveryCase(
            Path temp,
            String caseName,
            String parameterValue,
            String expectedMarker
    ) throws Exception {
        Path caseRoot = temp.resolve("bootstrap-discovery-" + caseName);
        Path specRoot = caseRoot.resolve("specs");
        String packageName = "phase33.bootstrap." + caseName;
        String stateName = packageName + ".BootstrapDiscoveryState";
        String hookName = packageName + ".DiscoveredBootstrapHook";
        Path specSource = writeSpec(specRoot, packageName, "BootstrapDiscoverySpec",
                "    public void it_observes_bootstrap_discovery_marker() {\n" +
                        "        if (!\"" + expectedMarker + "\".equals(BootstrapDiscoveryState.marker)) {\n" +
                        "            throw new AssertionError(\"unexpected bootstrap discovery marker: \" + BootstrapDiscoveryState.marker);\n" +
                        "        }\n" +
                        "        BootstrapDiscoveryState.exampleRuns++;\n" +
                        "    }\n");
        Path stateSource = writeBootstrapDiscoveryStateSource(caseRoot, packageName);
        Path hookSource = writeBootstrapDiscoveryHookSource(caseRoot, packageName);
        Path classesRoot = caseRoot.resolve("classes");

        try (URLClassLoader classLoader = compileToClassLoader(classesRoot, specSource, stateSource, hookSource)) {
            writeServiceProvider(classesRoot, "io.github.jvmspec.bootstrap.BootstrapHook", hookName);
            LauncherDiscoveryRequestBuilder builder = requestBuilder()
                    .configurationParameter("javaspec.specRoot", specRoot.toString());
            if (parameterValue != null) {
                builder.configurationParameter("javaspec.bootstrapDiscovery", parameterValue);
            }
            RunOutcome outcome = execute(builder.build(), classLoader);
            Class<?> stateClass = classLoader.loadClass(stateName);
            assertEquals(expectedMarker, staticString(stateClass, "marker"));
            assertEquals(1, staticInt(stateClass, "exampleRuns"));
            return outcome;
        }
    }

    private static Path writeBootstrapDiscoveryStateSource(Path root, String packageName) throws IOException {
        Path source = sourcePath(root.resolve("bootstrap-discovery-sources"), packageName + ".BootstrapDiscoveryState");
        Files.createDirectories(source.getParent());
        Files.write(source, (
                "package " + packageName + ";\n\n" +
                "public final class BootstrapDiscoveryState {\n" +
                "    public static String marker = \"\";\n" +
                "    public static int exampleRuns = 0;\n\n" +
                "    private BootstrapDiscoveryState() {\n" +
                "    }\n\n" +
                "    public static void reset() {\n" +
                "        marker = \"\";\n" +
                "        exampleRuns = 0;\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        return source;
    }

    private static Path writeBootstrapDiscoveryHookSource(Path root, String packageName) throws IOException {
        Path source = sourcePath(root.resolve("bootstrap-discovery-sources"), packageName + ".DiscoveredBootstrapHook");
        Files.createDirectories(source.getParent());
        Files.write(source, (
                "package " + packageName + ";\n\n" +
                "import io.github.jvmspec.bootstrap.BootstrapContext;\n" +
                "import io.github.jvmspec.bootstrap.BootstrapHook;\n\n" +
                "public final class DiscoveredBootstrapHook implements BootstrapHook {\n" +
                "    public void bootstrap(BootstrapContext context) {\n" +
                "        BootstrapDiscoveryState.reset();\n" +
                "        BootstrapDiscoveryState.marker = \"discovered\";\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        return source;
    }

    private static void writeServiceProvider(Path classesRoot, String serviceTypeName, String providerTypeName) throws IOException {
        Path serviceFile = classesRoot.resolve("META-INF").resolve("services").resolve(serviceTypeName);
        Files.createDirectories(serviceFile.getParent());
        Files.write(serviceFile, (providerTypeName + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static Path writeSpec(Path specRoot, String packageName, String simpleName, String classBody) throws IOException {
        Path packageDirectory = specRoot;
        if (packageName.length() > 0) {
            packageDirectory = specRoot.resolve(packageName.replace('.', File.separatorChar));
        }
        Files.createDirectories(packageDirectory);
        Path source = packageDirectory.resolve(simpleName + ".java");
        StringBuilder builder = new StringBuilder();
        if (packageName.length() > 0) {
            builder.append("package ").append(packageName).append(";\n\n");
        }
        builder.append("public class ").append(simpleName).append(" {\n");
        builder.append(classBody);
        builder.append("}\n");
        Files.write(source, builder.toString().getBytes(StandardCharsets.UTF_8));
        return source;
    }

    private static Path writeFormatterExtensionSource(
            Path root,
            String qualifiedName,
            String formatterName,
            String linePrefix
    ) throws IOException {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        Path source = sourcePath(root.resolve("extension-sources"), qualifiedName);
        Files.createDirectories(source.getParent());
        Files.write(source, (
                "package " + packageName + ";\n\n" +
                "import io.github.jvmspec.extension.ExtensionContext;\n" +
                "import io.github.jvmspec.extension.JavaspecExtension;\n" +
                "import io.github.jvmspec.formatter.RunFormatter;\n" +
                "import io.github.jvmspec.runner.RunResult;\n\n" +
                "import java.io.PrintStream;\n\n" +
                "public final class " + simpleName + " implements JavaspecExtension {\n" +
                "    public void configure(ExtensionContext context) {\n" +
                "        context.runFormatters().register(\"" + formatterName + "\", new RunFormatter() {\n" +
                "            public void format(RunResult runResult, PrintStream out) {\n" +
                "                out.println(\"" + linePrefix + " total=\" + runResult.totalExamples()\n" +
                "                        + \" passed=\" + runResult.passedCount()\n" +
                "                        + \" failed=\" + runResult.failedCount());\n" +
                "            }\n" +
                "        });\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        return source;
    }

    private static Path writeNoopExtensionSource(Path root, String qualifiedName) throws IOException {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        Path source = sourcePath(root.resolve("extension-sources"), qualifiedName);
        Files.createDirectories(source.getParent());
        Files.write(source, (
                "package " + packageName + ";\n\n" +
                "import io.github.jvmspec.extension.ExtensionContext;\n" +
                "import io.github.jvmspec.extension.JavaspecExtension;\n\n" +
                "public final class " + simpleName + " implements JavaspecExtension {\n" +
                "    public void configure(ExtensionContext context) {\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        return source;
    }

    private static Path writeFailingExtensionSource(Path root, String qualifiedName, String message) throws IOException {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        Path source = sourcePath(root.resolve("extension-sources"), qualifiedName);
        Files.createDirectories(source.getParent());
        Files.write(source, (
                "package " + packageName + ";\n\n" +
                "import io.github.jvmspec.extension.ExtensionContext;\n" +
                "import io.github.jvmspec.extension.JavaspecExtension;\n\n" +
                "public final class " + simpleName + " implements JavaspecExtension {\n" +
                "    public void configure(ExtensionContext context) {\n" +
                "        throw new IllegalStateException(\"" + message + "\");\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        return source;
    }

    private static Path sourcePath(Path root, String qualifiedName) {
        return root.resolve(qualifiedName.replace('.', File.separatorChar) + ".java");
    }

    private static URLClassLoader compileToClassLoader(Path classesRoot, Path... sources) throws Exception {
        Files.createDirectories(classesRoot);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "These tests require a JDK compiler, not only a JRE");
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8);
        try {
            List<File> files = new ArrayList<File>();
            for (int i = 0; i < sources.length; i++) {
                files.add(sources[i].toFile());
            }
            List<String> options = Arrays.asList(
                    "-d", classesRoot.toString(),
                    "-classpath", System.getProperty("java.class.path")
            );
            Boolean compiled = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    options,
                    null,
                    fileManager.getJavaFileObjectsFromFiles(files)
            ).call();
            assertTrue(compiled.booleanValue(), "Expected temporary specs to compile: " + files);
        } finally {
            fileManager.close();
        }
        URL[] urls = new URL[] {classesRoot.toUri().toURL()};
        return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
    }

    private static final class CapturedRunOutcome {
        private final RunOutcome runOutcome;
        private final String output;

        private CapturedRunOutcome(RunOutcome runOutcome, String output) {
            this.runOutcome = runOutcome;
            this.output = output;
        }

        RunOutcome runOutcome() {
            return runOutcome;
        }

        String output() {
            return output;
        }
    }

    private static final class RunOutcome {
        private final TestExecutionSummary summary;
        private final RecordingListener recorder;

        private RunOutcome(TestExecutionSummary summary, RecordingListener recorder) {
            this.summary = summary;
            this.recorder = recorder;
        }

        TestExecutionSummary summary() {
            return summary;
        }

        RecordingListener recorder() {
            return recorder;
        }
    }

    private static final class RecordingListener implements TestExecutionListener {
        private final List<String> startedDisplayNames = new ArrayList<String>();
        private final List<String> startedUniqueIds = new ArrayList<String>();
        private final List<String> dynamicallyRegisteredDisplayNames = new ArrayList<String>();
        private final List<SourceEvent> sources = new ArrayList<SourceEvent>();
        private final List<FinishedEvent> failedEvents = new ArrayList<FinishedEvent>();
        private final List<SkippedEvent> skippedEvents = new ArrayList<SkippedEvent>();

        public void dynamicTestRegistered(TestIdentifier testIdentifier) {
            dynamicallyRegisteredDisplayNames.add(testIdentifier.getDisplayName());
            recordSource(testIdentifier);
        }

        public void executionStarted(TestIdentifier testIdentifier) {
            startedDisplayNames.add(testIdentifier.getDisplayName());
            startedUniqueIds.add(testIdentifier.getUniqueId());
            recordSource(testIdentifier);
        }

        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            skippedEvents.add(new SkippedEvent(testIdentifier.getDisplayName(), reason));
        }

        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                failedEvents.add(new FinishedEvent(testIdentifier.getDisplayName(), testExecutionResult.getThrowable().orElse(null)));
            }
        }

        List<String> startedDisplayNames() {
            return startedDisplayNames;
        }

        List<String> startedUniqueIds() {
            return startedUniqueIds;
        }

        List<String> dynamicallyRegisteredDisplayNames() {
            return dynamicallyRegisteredDisplayNames;
        }

        SourceEvent sourceFor(String displayName) {
            for (int i = 0; i < sources.size(); i++) {
                SourceEvent source = sources.get(i);
                if (source.displayName().equals(displayName)) {
                    return source;
                }
            }
            throw new AssertionError("Expected source for display name '" + displayName + "' but got " + sources);
        }

        private void recordSource(TestIdentifier testIdentifier) {
            if (testIdentifier.getSource().isPresent()) {
                sources.add(new SourceEvent(testIdentifier.getDisplayName(), testIdentifier.getSource().get()));
            }
        }

        List<FinishedEvent> failedEvents() {
            return failedEvents;
        }

        List<SkippedEvent> skippedEvents() {
            return skippedEvents;
        }
    }

    private static final class SourceEvent {
        private final String displayName;
        private final Object source;

        private SourceEvent(String displayName, Object source) {
            this.displayName = displayName;
            this.source = source;
        }

        String displayName() {
            return displayName;
        }

        Object source() {
            return source;
        }

        public String toString() {
            return "SourceEvent{" + displayName + ", source=" + source + "}";
        }
    }

    private static final class FinishedEvent {
        private final String displayName;
        private final Throwable throwable;

        private FinishedEvent(String displayName, Throwable throwable) {
            this.displayName = displayName;
            this.throwable = throwable;
        }

        String displayName() {
            return displayName;
        }

        Throwable throwable() {
            return throwable;
        }

        public String toString() {
            return "FinishedEvent{" + displayName + ", throwable=" + throwable + "}";
        }
    }

    private static final class SkippedEvent {
        private final String displayName;
        private final String reason;

        private SkippedEvent(String displayName, String reason) {
            this.displayName = displayName;
            this.reason = reason;
        }

        String displayName() {
            return displayName;
        }

        String reason() {
            return reason;
        }

        public String toString() {
            return "SkippedEvent{" + displayName + ", reason='" + reason + "'}";
        }
    }
}
