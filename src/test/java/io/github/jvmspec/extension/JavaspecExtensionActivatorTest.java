package io.github.jvmspec.extension;

import io.github.jvmspec.fixtures.extension.ServiceLoadedJavaspecExtension;
import io.github.jvmspec.formatter.RunFormatterRegistry;
import io.github.jvmspec.runner.RunResult;
import io.github.jvmspec.runner.SpecResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaspecExtensionActivatorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void activatesConfiguredExtensionsFromSuppliedClassLoader() throws Exception {
        File classes = temporaryFolder.newFolder("dynamic-extension-classes");
        compileDynamicExtension(classes);
        URLClassLoader classLoader = new URLClassLoader(
                new URL[] {classes.toURI().toURL()},
                JavaspecExtensionActivatorTest.class.getClassLoader()
        );
        try {
            RunFormatterRegistry registry = RunFormatterRegistry.builtIn();

            JavaspecExtensionActivator.activate(
                    Collections.singletonList("phase32.dynamic.DynamicConfiguredExtension"),
                    classLoader,
                    registry
            );

            assertTrue(registry.contains("dynamic-configured"));
            assertEquals("dynamic configured formatter total=0\n", format(registry, "dynamic-configured"));
        } finally {
            classLoader.close();
        }
    }

    @Test
    public void activatesInConfiguredOrderPreservesDuplicatesAndAcceptsAliasImplementations() {
        RecordingExtensionState.reset();
        RunFormatterRegistry registry = RunFormatterRegistry.builtIn();

        JavaspecExtensionActivator.activate(
                Arrays.asList(
                        FirstRecordingExtension.class.getName(),
                        SecondRecordingExtension.class.getName(),
                        FirstRecordingExtension.class.getName(),
                        AliasRecordingExtension.class.getName()
                ),
                currentClassLoader(),
                registry
        );

        assertEquals(Arrays.asList("first", "second", "first", "alias"), RecordingExtensionState.events());
        assertTrue(registry.contains("alias-recording"));
    }

    @Test
    public void wrongTypeMissingConstructorAndConfigureFailureNameOffendingClass() {
        assertActivationFailure(
                NotAnExtension.class.getName(),
                NotAnExtension.class.getName(),
                "does not implement",
                JavaspecExtension.class.getName(),
                Extension.class.getName()
        );
        assertActivationFailure(
                NoPublicNoArgumentConstructorExtension.class.getName(),
                NoPublicNoArgumentConstructorExtension.class.getName(),
                "public no-argument constructor"
        );
        assertActivationFailure(
                FailingConfiguredExtension.class.getName(),
                FailingConfiguredExtension.class.getName(),
                "could not be configured",
                "phase 32 configure failure"
        );
    }

    @Test
    public void missingClassDiagnosticsListNoneWhenNoProvidersAreDiscovered() throws Exception {
        URLClassLoader classLoader = new URLClassLoader(
                new URL[] {temporaryFolder.newFolder("empty-provider-root").toURI().toURL()},
                JavaspecExtensionActivatorTest.class.getClassLoader()
        );
        try {
            assertActivationFailure(
                    classLoader,
                    "com.example.DoesNotExist",
                    "com.example.DoesNotExist",
                    "was not found",
                    "Discovered extension providers: <none>"
            );
        } finally {
            classLoader.close();
        }
    }

    @Test
    public void missingClassDiagnosticsListDiscoveredProviderNames() throws Exception {
        File serviceRoot = temporaryFolder.newFolder("service-provider-root");
        writeService(serviceRoot, JavaspecExtension.class, ServiceLoadedJavaspecExtension.class);
        URLClassLoader classLoader = new URLClassLoader(
                new URL[] {serviceRoot.toURI().toURL()},
                JavaspecExtensionActivatorTest.class.getClassLoader()
        );
        try {
            assertActivationFailure(
                    classLoader,
                    "com.example.MissingConfiguredExtension",
                    "com.example.MissingConfiguredExtension",
                    "was not found",
                    "Discovered extension providers: " + ServiceLoadedJavaspecExtension.class.getName()
            );
        } finally {
            classLoader.close();
        }
    }

    private static void assertActivationFailure(String extensionClassName, String... expectedFragments) {
        assertActivationFailure(currentClassLoader(), extensionClassName, expectedFragments);
    }

    private static void assertActivationFailure(
            ClassLoader classLoader,
            String extensionClassName,
            String... expectedFragments
    ) {
        try {
            JavaspecExtensionActivator.activate(
                    Collections.singletonList(extensionClassName),
                    classLoader,
                    RunFormatterRegistry.builtIn()
            );
            fail("Expected ExtensionLoadingException for " + extensionClassName);
        } catch (ExtensionLoadingException expected) {
            String message = expected.getMessage();
            for (int i = 0; i < expectedFragments.length; i++) {
                assertTrue("Expected message to contain '" + expectedFragments[i] + "' but was: " + message,
                        message.contains(expectedFragments[i]));
            }
        }
    }

    private void compileDynamicExtension(File classes) throws Exception {
        File sourceRoot = temporaryFolder.newFolder("dynamic-extension-source");
        File sourceFile = new File(sourceRoot, "phase32/dynamic/DynamicConfiguredExtension.java");
        File parent = sourceFile.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        Files.write(sourceFile.toPath(), (
                "package phase32.dynamic;\n" +
                "\n" +
                "import io.github.jvmspec.extension.ExtensionContext;\n" +
                "import io.github.jvmspec.extension.JavaspecExtension;\n" +
                "import io.github.jvmspec.formatter.RunFormatter;\n" +
                "import io.github.jvmspec.runner.RunResult;\n" +
                "\n" +
                "import java.io.PrintStream;\n" +
                "\n" +
                "public final class DynamicConfiguredExtension implements JavaspecExtension {\n" +
                "    public void configure(ExtensionContext context) {\n" +
                "        context.runFormatterRegistry().register(\"dynamic-configured\", new RunFormatter() {\n" +
                "            public void format(RunResult runResult, PrintStream out) {\n" +
                "                out.println(\"dynamic configured formatter total=\" + runResult.totalExamples());\n" +
                "            }\n" +
                "        });\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("These tests require a JDK with javax.tools.JavaCompiler", compiler);
        ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
        int exitCode = compiler.run(
                null,
                compilerOutput,
                compilerOutput,
                "-d", classes.getAbsolutePath(),
                "-classpath", System.getProperty("java.class.path"),
                "-source", "1.8",
                "-target", "1.8",
                sourceFile.getAbsolutePath()
        );
        assertEquals(new String(compilerOutput.toByteArray(), StandardCharsets.UTF_8), 0, exitCode);
    }

    private static void writeService(File serviceRoot, Class<?> serviceType, Class<?> providerType) throws Exception {
        File serviceFile = new File(new File(serviceRoot, "META-INF" + File.separator + "services"), serviceType.getName());
        File parent = serviceFile.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        Files.write(serviceFile.toPath(), (providerType.getName() + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static String format(RunFormatterRegistry registry, String formatterName) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        registry.lookup(formatterName).format(RunResult.of(Collections.<SpecResult>emptyList()), new PrintStream(output));
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static ClassLoader currentClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return JavaspecExtensionActivatorTest.class.getClassLoader();
    }

    public static final class FirstRecordingExtension implements JavaspecExtension {
        public void configure(ExtensionContext context) {
            RecordingExtensionState.record("first");
        }
    }

    public static final class SecondRecordingExtension implements JavaspecExtension {
        public void configure(ExtensionContext context) {
            RecordingExtensionState.record("second");
        }
    }

    public static final class AliasRecordingExtension implements Extension {
        public void configure(ExtensionContext context) {
            RecordingExtensionState.record("alias");
            context.runFormatters().register("alias-recording", new io.github.jvmspec.formatter.RunFormatter() {
                public void format(RunResult runResult, PrintStream out) {
                    out.println("alias");
                }
            });
        }
    }

    public static final class FailingConfiguredExtension implements JavaspecExtension {
        public void configure(ExtensionContext context) {
            throw new IllegalStateException("phase 32 configure failure");
        }
    }

    public static final class NoPublicNoArgumentConstructorExtension implements JavaspecExtension {
        public NoPublicNoArgumentConstructorExtension(String value) {
        }

        public void configure(ExtensionContext context) {
        }
    }

    public static final class NotAnExtension {
    }

    private static final class RecordingExtensionState {
        private static final List<String> EVENTS = new ArrayList<String>();

        private RecordingExtensionState() {
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
}
