package org.javaspec.extension;

import org.javaspec.fixtures.extension.BlankNameRunFormatter;
import org.javaspec.fixtures.extension.CountingDualExtension;
import org.javaspec.fixtures.extension.ServiceLoadedAliasExtension;
import org.javaspec.fixtures.extension.ServiceLoadedJavaspecExtension;
import org.javaspec.fixtures.extension.ServiceLoadedRunFormatter;
import org.javaspec.fixtures.extension.ThrowingJavaspecExtension;
import org.javaspec.formatter.ProgressRunFormatter;
import org.javaspec.formatter.RunFormatter;
import org.javaspec.formatter.RunFormatterRegistry;
import org.javaspec.runner.RunResult;
import org.javaspec.runner.SpecResult;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaspecExtensionLoaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void builtInsArePresentFirstWhenNoServiceProvidersExist() throws Exception {
        RunFormatterRegistry registry = loadRegistry(temporaryFolder.newFolder("empty-services"));

        assertEquals(Arrays.asList("progress", "pretty"), registry.formatterNames());
        assertTrue(registry.lookup("progress") instanceof ProgressRunFormatter);
    }

    @Test
    public void serviceLoaderRunFormatterProviderIsRegisteredByName() throws Exception {
        File serviceRoot = serviceRoot();
        writeService(serviceRoot, RunFormatter.class, ServiceLoadedRunFormatter.class);

        RunFormatterRegistry registry = loadRegistry(serviceRoot);

        assertEquals(Arrays.asList("progress", "pretty", "external"), registry.formatterNames());
        assertTrue(registry.lookup("external") instanceof ServiceLoadedRunFormatter);
        assertEquals("external formatter total=0 passed=0 failed=0\n", format(registry.lookup(" EXTERNAL ")));
    }

    @Test
    public void javaspecExtensionProviderCanRegisterFormatterThroughContext() throws Exception {
        File serviceRoot = serviceRoot();
        writeService(serviceRoot, JavaspecExtension.class, ServiceLoadedJavaspecExtension.class);

        RunFormatterRegistry registry = loadRegistry(serviceRoot);

        assertEquals(Arrays.asList("progress", "pretty", "extension"), registry.formatterNames());
        assertEquals("extension formatter total=0\n", format(registry.lookup("extension")));
    }

    @Test
    public void configuredExtensionClassesRegisterFormatterInOrderAfterDiscoveryAndPreserveDuplicates() throws Exception {
        ConfiguredExtensionState.reset();

        RunFormatterRegistry registry = JavaspecExtensionLoader.loadRunFormatterRegistry(
                currentClassLoader(),
                Arrays.asList(
                        FirstConfiguredExtension.class.getName(),
                        SecondConfiguredExtension.class.getName(),
                        FirstConfiguredExtension.class.getName()
                )
        );

        assertEquals(Arrays.asList("first", "second", "first"), ConfiguredExtensionState.events());
        assertEquals(Arrays.asList("progress", "pretty", "configured-order"), registry.formatterNames());
        assertEquals("configured extension order=first>second>first\n", format(registry.lookup("configured-order")));
    }

    @Test
    public void emptyConfiguredExtensionListKeepsBuiltInAndServiceLoaderBehaviorUnchanged() throws Exception {
        File serviceRoot = serviceRoot();
        writeService(serviceRoot, RunFormatter.class, ServiceLoadedRunFormatter.class);
        writeService(serviceRoot, JavaspecExtension.class, ServiceLoadedJavaspecExtension.class);

        RunFormatterRegistry registry = loadRegistry(serviceRoot, Collections.<String>emptyList());

        assertEquals(Arrays.asList("progress", "pretty", "external", "extension"), registry.formatterNames());
        assertEquals("external formatter total=0 passed=0 failed=0\n", format(registry.lookup("external")));
        assertEquals("extension formatter total=0\n", format(registry.lookup("extension")));
    }

    @Test
    public void extensionAliasProviderIsSupported() throws Exception {
        File serviceRoot = serviceRoot();
        writeService(serviceRoot, Extension.class, ServiceLoadedAliasExtension.class);

        RunFormatterRegistry registry = loadRegistry(serviceRoot);

        assertEquals(Arrays.asList("progress", "pretty", "alias"), registry.formatterNames());
        assertEquals("alias formatter total=0\n", format(registry.lookup("alias")));
    }

    @Test
    public void sameImplementationListedUnderBothExtensionServiceTypesIsConfiguredOnlyOnce() throws Exception {
        CountingDualExtension.reset();
        File serviceRoot = serviceRoot();
        writeService(serviceRoot, JavaspecExtension.class, CountingDualExtension.class);
        writeService(serviceRoot, Extension.class, CountingDualExtension.class);

        RunFormatterRegistry registry = loadRegistry(serviceRoot);

        assertEquals(1, CountingDualExtension.configureCount());
        assertEquals(Arrays.asList("progress", "pretty", "dual"), registry.formatterNames());
        assertEquals("dual extension configured 1 time(s)\n", format(registry.lookup("dual")));
    }

    @Test
    public void blankFormatterNameProducesExtensionLoadingExceptionWithProviderDetail() throws Exception {
        File serviceRoot = serviceRoot();
        writeService(serviceRoot, RunFormatter.class, BlankNameRunFormatter.class);

        try {
            loadRegistry(serviceRoot);
            fail("Expected ExtensionLoadingException");
        } catch (ExtensionLoadingException ex) {
            assertTrue(ex.getMessage().contains("Invalid run formatter provider " + BlankNameRunFormatter.class.getName()));
            assertTrue(ex.getMessage().contains("formatter name must not be blank"));
        }
    }

    @Test
    public void throwingExtensionProducesExtensionLoadingExceptionWithProviderAndServiceDetail() throws Exception {
        File serviceRoot = serviceRoot();
        writeService(serviceRoot, JavaspecExtension.class, ThrowingJavaspecExtension.class);

        try {
            loadRegistry(serviceRoot);
            fail("Expected ExtensionLoadingException");
        } catch (ExtensionLoadingException ex) {
            assertTrue(ex.getMessage().contains("Could not configure javaspec extension "
                    + ThrowingJavaspecExtension.class.getName()));
            assertTrue(ex.getMessage().contains("from service " + JavaspecExtension.class.getName()));
            assertTrue(ex.getMessage().contains("boom from extension configure"));
        }
    }

    @Test
    public void returnedRegistrySupportsProgrammaticLookupAndRegistration() throws Exception {
        RunFormatterRegistry registry = loadRegistry(temporaryFolder.newFolder("programmatic-services"));
        RunFormatter formatter = new RunFormatter() {
            public void format(RunResult runResult, PrintStream out) {
                out.println("programmatic formatter");
            }
        };

        registry.register(" Programmatic ", formatter);

        assertSame(formatter, registry.lookup("programmatic"));
        assertEquals(Arrays.asList("progress", "pretty", "programmatic"), registry.formatterNames());
    }

    private RunFormatterRegistry loadRegistry(File serviceRoot) throws Exception {
        return loadRegistry(serviceRoot, null);
    }

    private RunFormatterRegistry loadRegistry(File serviceRoot, List<String> configuredExtensions) throws Exception {
        URLClassLoader classLoader = new URLClassLoader(
                new URL[] {serviceRoot.toURI().toURL()},
                JavaspecExtensionLoaderTest.class.getClassLoader()
        );
        try {
            if (configuredExtensions == null) {
                return JavaspecExtensionLoader.loadRunFormatterRegistry(classLoader);
            }
            return JavaspecExtensionLoader.loadRunFormatterRegistry(classLoader, configuredExtensions);
        } finally {
            classLoader.close();
        }
    }

    private File serviceRoot() throws Exception {
        return temporaryFolder.newFolder("service-root-" + System.nanoTime());
    }

    private static void writeService(File serviceRoot, Class<?> serviceType, Class<?>... providerTypes) throws Exception {
        File serviceFile = new File(new File(serviceRoot, "META-INF" + File.separator + "services"), serviceType.getName());
        File parent = serviceFile.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < providerTypes.length; i++) {
            content.append(providerTypes[i].getName()).append('\n');
        }
        Files.write(serviceFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String format(RunFormatter formatter) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        formatter.format(emptyRunResult(), new PrintStream(output));
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static RunResult emptyRunResult() {
        return RunResult.of(Collections.<SpecResult>emptyList());
    }

    private static ClassLoader currentClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return JavaspecExtensionLoaderTest.class.getClassLoader();
    }

    public static final class FirstConfiguredExtension implements JavaspecExtension {
        public void configure(ExtensionContext context) {
            ConfiguredExtensionState.record("first");
            registerOrderFormatter(context);
        }
    }

    public static final class SecondConfiguredExtension implements JavaspecExtension {
        public void configure(ExtensionContext context) {
            ConfiguredExtensionState.record("second");
            registerOrderFormatter(context);
        }
    }

    private static void registerOrderFormatter(ExtensionContext context) {
        context.runFormatters().register("configured-order", new RunFormatter() {
            public void format(RunResult runResult, PrintStream out) {
                out.println("configured extension order=" + ConfiguredExtensionState.joinedEvents());
            }
        });
    }

    private static final class ConfiguredExtensionState {
        private static final List<String> EVENTS = new ArrayList<String>();

        private ConfiguredExtensionState() {
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

        static synchronized String joinedEvents() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < EVENTS.size(); i++) {
                if (i > 0) {
                    builder.append('>');
                }
                builder.append(EVENTS.get(i));
            }
            return builder.toString();
        }
    }
}
