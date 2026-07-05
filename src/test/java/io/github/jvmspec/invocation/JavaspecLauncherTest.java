package io.github.jvmspec.invocation;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecDiscoveryRequest;
import io.github.jvmspec.discovery.SpecExample;
import io.github.jvmspec.extension.ExtensionContext;
import io.github.jvmspec.extension.ExtensionLoadingException;
import io.github.jvmspec.extension.JavaspecExtension;
import io.github.jvmspec.formatter.RunFormatter;
import io.github.jvmspec.model.DescribedClass;
import io.github.jvmspec.runner.RunResult;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JavaspecLauncherTest {
    @Test
    public void launchDiscoversRunsAndReturnsStructuredResult() {
        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(testJavaRoot())
                .withClassFilter("io.github.jvmspec.fixtures.cli.FailingSubject")
                .withExampleFilter("it_passes");

        JavaspecInvocationResult result = JavaspecLauncher.launch(
                JavaspecInvocation.discovering(request, currentClassLoader())
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.successful());
        assertEquals(1, result.discoveredSpecs().size());
        assertEquals("spec.io.github.jvmspec.fixtures.cli.FailingSubjectSpec", result.discoveredSpecs().get(0).specQualifiedName());
        assertEquals(1, result.runResult().totalCount());
        assertEquals(1, result.runResult().passedCount());
        assertFalse(result.runResult().hasFailures());
    }

    @Test
    public void exitCodeMappingTreatsPassingSkippedAndNoSpecRunsAsSuccess() {
        assertInvocationExit(0, 1, 1, 0, 0, 0, spec(
                "spec.io.github.jvmspec.fixtures.cli.FailingSubjectSpec",
                "io.github.jvmspec.fixtures.cli.FailingSubject",
                "it_passes"
        ));
        assertInvocationExit(0, 1, 0, 0, 0, 1, spec(
                "spec.com.example.MissingExecutableSpec",
                "com.example.MissingExecutable",
                "it_is_skipped"
        ));
        assertInvocationExit(0, 1, 0, 0, 0, 0, 1, spec(
                "spec.io.github.jvmspec.fixtures.cli.FailingSubjectSpec",
                "io.github.jvmspec.fixtures.cli.FailingSubject",
                "it_is_pending"
        ));
        JavaspecInvocationResult noSpecResult = JavaspecLauncher.run(
                JavaspecInvocation.forSpecs(Collections.<DiscoveredSpec>emptyList(), currentClassLoader())
        );
        assertFalse(noSpecResult.hasRunFormatterRegistry());
        assertEquals(null, noSpecResult.runFormatterRegistry());
        assertEquals(0, noSpecResult.exitCode());
        assertTrue(noSpecResult.successful());
        assertEquals(0, noSpecResult.discoveredSpecs().size());
        assertEquals(0, noSpecResult.runResult().totalCount());
    }

    @Test
    public void exitCodeMappingTreatsFailedAndBrokenRunsAsFailure() {
        assertInvocationExit(1, 1, 0, 1, 0, 0, spec(
                "spec.io.github.jvmspec.fixtures.cli.FailingSubjectSpec",
                "io.github.jvmspec.fixtures.cli.FailingSubject",
                "it_fails"
        ));
        assertInvocationExit(1, 1, 0, 0, 1, 0, spec(
                "spec.io.github.jvmspec.fixtures.cli.Phase9BrokenStopSubjectSpec",
                "io.github.jvmspec.fixtures.cli.Phase9BrokenStopSubject",
                "it_breaks_first"
        ));
    }

    @Test
    public void configuredExtensionsActivateBeforeExamplesAndExposeFormatterRegistry() throws Exception {
        LauncherExtensionState.reset();
        List<DiscoveredSpec> specs = Collections.singletonList(spec(
                ExtensionAwareSpec.class.getName(),
                ExtensionAwareSpec.class.getName(),
                "it_runs_after_extension_activation"
        ));

        JavaspecInvocationResult result = JavaspecLauncher.run(
                JavaspecInvocation.forSpecs(specs, currentClassLoader())
                        .withExtension(LauncherFormatterExtension.class.getName())
        );

        assertEquals(0, result.exitCode());
        assertEquals(1, result.runResult().passedCount());
        assertEquals(list("extension", "example"), LauncherExtensionState.events());
        assertTrue(result.hasRunFormatterRegistry());
        assertTrue(result.runFormatterRegistry().contains("launcher-custom"));
        ByteArrayOutputStream formatted = new ByteArrayOutputStream();
        result.runFormatterRegistry().lookup("launcher-custom")
                .format(result.runResult(), new PrintStream(formatted));
        assertEquals("launcher custom total=1\n", new String(formatted.toByteArray(), "UTF-8"));
    }

    @Test
    public void configuredExtensionFailurePropagatesBeforeExamplesRun() {
        FailingExtensionSpec.runs = 0;
        List<DiscoveredSpec> specs = Collections.singletonList(spec(
                FailingExtensionSpec.class.getName(),
                FailingExtensionSpec.class.getName(),
                "it_would_run_if_activation_succeeded"
        ));

        ExtensionLoadingException failure = assertThrows(ExtensionLoadingException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                JavaspecLauncher.run(
                        JavaspecInvocation.forSpecs(specs, currentClassLoader())
                                .withExtension(FailingLauncherExtension.class.getName())
                );
            }
        });

        assertTrue(failure.getMessage().contains(FailingLauncherExtension.class.getName()));
        assertTrue(failure.getMessage().contains("phase 32 launcher extension failure"));
        assertEquals(0, FailingExtensionSpec.runs);
    }

    @Test
    public void launcherDoesNotTerminateJvmWithSystemExit() throws Exception {
        Process process = new ProcessBuilder(
                javaExecutable(),
                "-cp",
                testRuntimeClasspath(),
                NoExitProbe.class.getName()
        ).redirectErrorStream(true).start();

        String output = readFully(process.getInputStream());
        int processExitCode = process.waitFor();

        assertEquals(output, 0, processExitCode);
        assertTrue(output, output.contains("after-launch:0:0"));
    }

    private static void assertInvocationExit(
            int expectedExitCode,
            int expectedTotal,
            int expectedPassed,
            int expectedFailed,
            int expectedBroken,
            int expectedSkipped,
            DiscoveredSpec spec
    ) {
        assertInvocationExit(expectedExitCode, expectedTotal, expectedPassed, expectedFailed, expectedBroken,
                expectedSkipped, 0, spec);
    }

    private static void assertInvocationExit(
            int expectedExitCode,
            int expectedTotal,
            int expectedPassed,
            int expectedFailed,
            int expectedBroken,
            int expectedSkipped,
            int expectedPending,
            DiscoveredSpec spec
    ) {
        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        specs.add(spec);

        JavaspecInvocationResult result = JavaspecLauncher.run(
                JavaspecInvocation.forSpecs(specs, currentClassLoader())
        );
        RunResult runResult = result.runResult();

        assertEquals(expectedExitCode, result.exitCode());
        assertEquals(expectedExitCode == 0, result.successful());
        assertEquals(specs, result.discoveredSpecs());
        assertEquals(expectedTotal, runResult.totalCount());
        assertEquals(expectedPassed, runResult.passedCount());
        assertEquals(expectedFailed, runResult.failedCount());
        assertEquals(expectedBroken, runResult.brokenCount());
        assertEquals(expectedSkipped, runResult.skippedCount());
        assertEquals(expectedPending, runResult.pendingCount());
        assertEquals(expectedExitCode, JavaspecExitCode.from(runResult));
    }

    private static DiscoveredSpec spec(String specQualifiedName, String describedClassName, String... exampleMethodNames) {
        List<SpecExample> examples = new ArrayList<SpecExample>();
        for (int i = 0; i < exampleMethodNames.length; i++) {
            examples.add(SpecExample.of(exampleMethodNames[i], i));
        }
        return DiscoveredSpec.of(
                new File(specQualifiedName.replace('.', File.separatorChar) + ".java"),
                specQualifiedName,
                DescribedClass.of(describedClassName),
                examples
        );
    }

    private static ClassLoader currentClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return JavaspecLauncherTest.class.getClassLoader();
    }

    private static File testJavaRoot() {
        return new File(System.getProperty("user.dir"), "src/test/java");
    }

    private static String javaExecutable() {
        File binDirectory = new File(System.getProperty("java.home"), "bin");
        String executableName = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        return new File(binDirectory, executableName).getAbsolutePath();
    }

    private static String testRuntimeClasspath() {
        String surefireClasspath = System.getProperty("surefire.test.class.path");
        if (surefireClasspath != null && surefireClasspath.length() > 0) {
            return surefireClasspath;
        }
        return System.getProperty("java.class.path");
    }

    private static String readFully(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), "UTF-8");
    }

    public static final class LauncherFormatterExtension implements JavaspecExtension {
        public void configure(ExtensionContext context) {
            LauncherExtensionState.record("extension");
            context.runFormatters().register("launcher-custom", new RunFormatter() {
                public void format(RunResult runResult, PrintStream out) {
                    out.println("launcher custom total=" + runResult.totalExamples());
                }
            });
        }
    }

    public static final class ExtensionAwareSpec {
        public void it_runs_after_extension_activation() {
            if (!LauncherExtensionState.events().contains("extension")) {
                throw new AssertionError("extension did not activate before the example");
            }
            LauncherExtensionState.record("example");
        }
    }

    public static final class FailingLauncherExtension implements JavaspecExtension {
        public void configure(ExtensionContext context) {
            throw new IllegalStateException("phase 32 launcher extension failure");
        }
    }

    public static final class FailingExtensionSpec {
        static int runs;

        public void it_would_run_if_activation_succeeded() {
            runs++;
        }
    }

    private static final class LauncherExtensionState {
        private static final List<String> EVENTS = new ArrayList<String>();

        private LauncherExtensionState() {
        }

        static synchronized void reset() {
            EVENTS.clear();
        }

        static synchronized void record(String event) {
            EVENTS.add(event);
        }

        static synchronized List<String> events() {
            return new ArrayList<String>(EVENTS);
        }
    }

    private static List<String> list(String first, String second) {
        List<String> values = new ArrayList<String>();
        values.add(first);
        values.add(second);
        return values;
    }

    public static final class NoExitProbe {
        public static void main(String[] args) {
            JavaspecInvocationResult result = JavaspecLauncher.run(
                    JavaspecInvocation.forSpecs(Collections.<DiscoveredSpec>emptyList(), NoExitProbe.class.getClassLoader())
            );
            System.out.println("after-launch:" + result.exitCode() + ":" + result.runResult().totalCount());
        }
    }
}
