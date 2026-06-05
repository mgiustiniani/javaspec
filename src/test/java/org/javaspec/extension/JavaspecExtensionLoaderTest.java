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
import java.util.Arrays;
import java.util.Collections;

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
        URLClassLoader classLoader = new URLClassLoader(
                new URL[] {serviceRoot.toURI().toURL()},
                JavaspecExtensionLoaderTest.class.getClassLoader()
        );
        try {
            return JavaspecExtensionLoader.loadRunFormatterRegistry(classLoader);
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
}
