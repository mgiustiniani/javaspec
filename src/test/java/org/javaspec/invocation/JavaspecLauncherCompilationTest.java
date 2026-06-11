package org.javaspec.invocation;

import org.javaspec.bootstrap.BootstrapContext;
import org.javaspec.bootstrap.BootstrapHook;
import org.javaspec.compilation.SourceCompilationException;
import org.javaspec.compilation.SourceCompilationResult;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.discovery.SpecExample;
import org.javaspec.model.DescribedClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JavaspecLauncherCompilationTest {
    private static final String EVENT_PROPERTY = "javaspec.phase34.launcher.events";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void sourceOnlySpecAndSubjectCompileAndBootstrapHookSeesTemporaryClassLoaderOutput() throws Exception {
        requireJdkCompiler();
        File sourceRoot = temporaryFolder.newFolder("programmatic-source-root");
        File specRoot = temporaryFolder.newFolder("programmatic-spec-root");
        File outputDirectory = new File(temporaryFolder.getRoot(), "programmatic-classes");
        File eventFile = new File(temporaryFolder.getRoot(), "programmatic-events.txt");
        writeProgrammaticSubject(sourceRoot);
        writeProgrammaticSpec(specRoot);

        String previousEventFile = System.getProperty(EVENT_PROPERTY);
        System.setProperty(EVENT_PROPERTY, eventFile.getAbsolutePath());
        try {
            JavaspecInvocationResult result = JavaspecLauncher.run(
                    JavaspecInvocation
                            .discovering(SpecDiscoveryRequest.of(specRoot), currentClassLoader())
                            .withBootstrapHook(CompiledClassLoadingBootstrapHook.class.getName())
                            .withCompilation(
                                    sourceRoot,
                                    specRoot,
                                    outputDirectory,
                                    Collections.<File>emptyList()
                            )
            );

            assertTrue(result.hasSourceCompilationResult());
            SourceCompilationResult compilation = result.sourceCompilationResult();
            assertTrue(compilation.compilerAvailable());
            assertTrue(compilation.successful());
            assertEquals(2, compilation.sourceFileCount());
            assertEquals(outputDirectory, compilation.outputDirectory());
            assertEquals(Collections.<String>emptyList(), compilation.diagnostics());
            assertTrue(classFileFor(outputDirectory, "com.phase34.ProgrammaticSubject").isFile());
            assertTrue(classFileFor(outputDirectory, "spec.com.phase34.ProgrammaticSubjectSpec").isFile());
            assertEquals(0, result.exitCode());
            assertEquals(1, result.runResult().totalCount());
            assertEquals(1, result.runResult().passedCount());
            assertEquals("hook:compiled\nexample:compiled\n", readFile(eventFile));
        } finally {
            restoreProperty(EVENT_PROPERTY, previousEventFile);
        }
    }

    @Test
    public void noSpecRunWithCompilationEnabledSkipsCompilationAndLeavesNoResultOrOutputDirectory() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("no-spec-source-root");
        File specRoot = temporaryFolder.newFolder("no-spec-spec-root");
        File outputDirectory = new File(temporaryFolder.getRoot(), "no-spec-classes");
        writeSource(sourceRoot, "com.phase34.BrokenNoSpecSource",
                "    public void broken( {\n");

        JavaspecInvocationResult result = JavaspecLauncher.run(
                JavaspecInvocation
                        .discovering(SpecDiscoveryRequest.of(specRoot), currentClassLoader())
                        .withCompilation(sourceRoot, specRoot, outputDirectory, Collections.<File>emptyList())
        );

        assertFalse(result.hasSourceCompilationResult());
        assertEquals(null, result.sourceCompilationResult());
        assertEquals(0, result.discoveredSpecs().size());
        assertEquals(0, result.runResult().totalCount());
        assertEquals(0, result.exitCode());
        assertFalse(outputDirectory.exists());
    }

    @Test
    public void compilationFailureThrowsDiagnosticsAndPreventsBootstrapHooksAndExamples() throws Exception {
        requireJdkCompiler();
        File sourceRoot = temporaryFolder.newFolder("failure-source-root");
        File outputDirectory = new File(temporaryFolder.getRoot(), "failure-classes");
        writeSource(sourceRoot, "com.phase34.BrokenSource",
                "    public String message() {\n" +
                        "        return missingSymbol;\n" +
                        "    }\n");
        FailureGuardBootstrapHook.runs = 0;
        FailureGuardSpec.runs = 0;
        List<DiscoveredSpec> specs = Collections.singletonList(spec(
                FailureGuardSpec.class,
                "it_must_not_run_after_compilation_failure"
        ));

        SourceCompilationException failure = assertThrows(SourceCompilationException.class, new org.junit.function.ThrowingRunnable() {
            @Override
            public void run() {
                JavaspecLauncher.run(
                        JavaspecInvocation
                                .forSpecs(specs, currentClassLoader())
                                .withBootstrapHook(FailureGuardBootstrapHook.class.getName())
                                .withCompilation(
                                        Collections.singletonList(sourceRoot),
                                        outputDirectory,
                                        Collections.<File>emptyList()
                                )
                );
            }
        });

        assertEquals(SourceCompilationException.Reason.COMPILATION_FAILED, failure.reason());
        assertTrue(failure.hasSourceCompilationResult());
        assertEquals(outputDirectory, failure.outputDirectory());
        assertEquals(outputDirectory, failure.sourceCompilationResult().outputDirectory());
        assertEquals(1, failure.sourceCompilationResult().sourceFileCount());
        assertFalse(failure.sourceCompilationResult().successful());
        assertTrue(containsFragment(failure.sourceCompilationResult().diagnostics(), "BrokenSource.java"));
        assertTrue(containsFragment(failure.sourceCompilationResult().diagnostics(), "missingSymbol"));
        assertEquals(0, FailureGuardBootstrapHook.runs);
        assertEquals(0, FailureGuardSpec.runs);
    }

    @Test
    public void sourceOnlySpecRemainsSkippedWhenCompilationIsNotEnabled() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("default-disabled-source-root");
        File specRoot = temporaryFolder.newFolder("default-disabled-spec-root");
        writeProgrammaticSubject(sourceRoot);
        writeProgrammaticSpec(specRoot);

        JavaspecInvocationResult result = JavaspecLauncher.run(
                JavaspecInvocation.discovering(SpecDiscoveryRequest.of(specRoot), currentClassLoader())
        );

        assertFalse(result.hasSourceCompilationResult());
        assertEquals(1, result.discoveredSpecs().size());
        assertEquals(1, result.runResult().totalCount());
        assertEquals(0, result.runResult().passedCount());
        assertEquals(1, result.runResult().skippedCount());
        assertEquals(0, result.exitCode());
    }

    public static final class CompiledClassLoadingBootstrapHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            try {
                Object subject = context.classLoader().loadClass("com.phase34.ProgrammaticSubject").newInstance();
                String message = String.valueOf(subject.getClass().getMethod("message").invoke(subject));
                writeEvent("hook:" + message);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    public static final class FailureGuardBootstrapHook implements BootstrapHook {
        private static int runs;

        public void bootstrap(BootstrapContext context) {
            runs++;
        }
    }

    public static final class FailureGuardSpec {
        private static int runs;

        public void it_must_not_run_after_compilation_failure() {
            runs++;
            throw new AssertionError("example ran after compilation failure");
        }
    }

    private static void requireJdkCompiler() {
        assertNotNull("These tests require a JDK with javax.tools.JavaCompiler", ToolProvider.getSystemJavaCompiler());
    }

    private static void writeProgrammaticSubject(File sourceRoot) throws Exception {
        writeSource(sourceRoot, "com.phase34.ProgrammaticSubject",
                "    public String message() {\n" +
                        "        return \"compiled\";\n" +
                        "    }\n");
    }

    private static void writeProgrammaticSpec(File specRoot) throws Exception {
        writeSource(specRoot, "spec.com.phase34.ProgrammaticSubjectSpec",
                "    public void it_executes_compiled_source_only_spec() {\n" +
                        "        String message = new com.phase34.ProgrammaticSubject().message();\n" +
                        "        if (!\"compiled\".equals(message)) {\n" +
                        "            throw new AssertionError(\"compiled source was not visible to the example\");\n" +
                        "        }\n" +
                        "        try {\n" +
                        "            java.nio.file.Files.write(new java.io.File(System.getProperty(\"" + EVENT_PROPERTY + "\")).toPath(), (\"example:\" + message + \"\\n\").getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);\n" +
                        "        } catch (java.io.IOException ex) {\n" +
                        "            throw new RuntimeException(ex);\n" +
                        "        }\n" +
                        "    }\n");
    }

    private static void writeSource(File root, String qualifiedName, String body) throws Exception {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        File source = sourceFileFor(root, qualifiedName);
        writeFile(source,
                "package " + packageName + ";\n\n" +
                        "public class " + simpleName + " {\n" +
                        body +
                        "}\n");
    }

    private static File sourceFileFor(File root, String qualifiedName) {
        return new File(root, qualifiedName.replace('.', File.separatorChar) + ".java");
    }

    private static File classFileFor(File root, String qualifiedName) {
        return new File(root, qualifiedName.replace('.', File.separatorChar) + ".class");
    }

    private static void writeFile(File file, String content) throws Exception {
        File parent = file.getParentFile();
        assertTrue(parent == null || parent.isDirectory() || parent.mkdirs());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void writeEvent(String event) {
        try {
            File eventFile = new File(System.getProperty(EVENT_PROPERTY));
            Files.write(
                    eventFile.toPath(),
                    (event + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void restoreProperty(String key, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previousValue);
        }
    }

    private static boolean containsFragment(List<String> lines, String fragment) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static DiscoveredSpec spec(Class<?> specClass, String exampleMethodName) {
        return DiscoveredSpec.of(
                new File(specClass.getName().replace('.', File.separatorChar) + ".java"),
                specClass.getName(),
                DescribedClass.of(specClass.getName()),
                Arrays.asList(SpecExample.of(exampleMethodName, 0))
        );
    }

    private static ClassLoader currentClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return JavaspecLauncherCompilationTest.class.getClassLoader();
    }
}
