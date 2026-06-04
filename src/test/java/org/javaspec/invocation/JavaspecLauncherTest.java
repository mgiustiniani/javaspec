package org.javaspec.invocation;

import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.discovery.SpecExample;
import org.javaspec.model.DescribedClass;
import org.javaspec.runner.RunResult;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JavaspecLauncherTest {
    @Test
    public void launchDiscoversRunsAndReturnsStructuredResult() {
        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(testJavaRoot())
                .withClassFilter("org.javaspec.fixtures.cli.FailingSubject")
                .withExampleFilter("it_passes");

        JavaspecInvocationResult result = JavaspecLauncher.launch(
                JavaspecInvocation.discovering(request, currentClassLoader())
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.successful());
        assertEquals(1, result.discoveredSpecs().size());
        assertEquals("spec.org.javaspec.fixtures.cli.FailingSubjectSpec", result.discoveredSpecs().get(0).specQualifiedName());
        assertEquals(1, result.runResult().totalCount());
        assertEquals(1, result.runResult().passedCount());
        assertFalse(result.runResult().hasFailures());
    }

    @Test
    public void exitCodeMappingTreatsPassingSkippedAndNoSpecRunsAsSuccess() {
        assertInvocationExit(0, 1, 1, 0, 0, 0, spec(
                "spec.org.javaspec.fixtures.cli.FailingSubjectSpec",
                "org.javaspec.fixtures.cli.FailingSubject",
                "it_passes"
        ));
        assertInvocationExit(0, 1, 0, 0, 0, 1, spec(
                "spec.com.example.MissingExecutableSpec",
                "com.example.MissingExecutable",
                "it_is_skipped"
        ));
        assertInvocationExit(0, 1, 0, 0, 0, 0, 1, spec(
                "spec.org.javaspec.fixtures.cli.FailingSubjectSpec",
                "org.javaspec.fixtures.cli.FailingSubject",
                "it_is_pending"
        ));
        JavaspecInvocationResult noSpecResult = JavaspecLauncher.run(
                JavaspecInvocation.forSpecs(Collections.<DiscoveredSpec>emptyList(), currentClassLoader())
        );
        assertEquals(0, noSpecResult.exitCode());
        assertTrue(noSpecResult.successful());
        assertEquals(0, noSpecResult.discoveredSpecs().size());
        assertEquals(0, noSpecResult.runResult().totalCount());
    }

    @Test
    public void exitCodeMappingTreatsFailedAndBrokenRunsAsFailure() {
        assertInvocationExit(1, 1, 0, 1, 0, 0, spec(
                "spec.org.javaspec.fixtures.cli.FailingSubjectSpec",
                "org.javaspec.fixtures.cli.FailingSubject",
                "it_fails"
        ));
        assertInvocationExit(1, 1, 0, 0, 1, 0, spec(
                "spec.org.javaspec.fixtures.cli.Phase9BrokenStopSubjectSpec",
                "org.javaspec.fixtures.cli.Phase9BrokenStopSubject",
                "it_breaks_first"
        ));
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

    public static final class NoExitProbe {
        public static void main(String[] args) {
            JavaspecInvocationResult result = JavaspecLauncher.run(
                    JavaspecInvocation.forSpecs(Collections.<DiscoveredSpec>emptyList(), NoExitProbe.class.getClassLoader())
            );
            System.out.println("after-launch:" + result.exitCode() + ":" + result.runResult().totalCount());
        }
    }
}
