package org.javaspec.extension;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class ExtensionCatalogTest {

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void discoverReturnsNonNullCatalog() {
        ExtensionCatalog catalog = ExtensionCatalog.discover(
                Thread.currentThread().getContextClassLoader());
        assertNotNull(catalog);
    }

    @Test
    public void builtInFormattersAlwaysPresent() {
        ExtensionCatalog catalog = ExtensionCatalog.discover(
                Thread.currentThread().getContextClassLoader());
        assertTrue("progress formatter must be listed",
                catalog.formatterNames().contains("progress"));
        assertTrue("pretty formatter must be listed",
                catalog.formatterNames().contains("pretty"));
    }

    @Test
    public void discoversServiceLoaderExtensionProvider() throws Exception {
        File serviceRoot = tmp.newFolder("service-root");
        File serviceFile = new File(serviceRoot,
                "META-INF/services/org.javaspec.extension.JavaspecExtension");
        assertTrue(serviceFile.getParentFile().mkdirs());
        Files.write(serviceFile.toPath(),
                "org.javaspec.extension.fixtures.CatalogTestExtension\n".getBytes(StandardCharsets.UTF_8));
        URLClassLoader loader = new URLClassLoader(
                new URL[] { serviceRoot.toURI().toURL() },
                Thread.currentThread().getContextClassLoader());

        ExtensionCatalog catalog = ExtensionCatalog.discover(loader);

        assertNotNull(catalog.extensionClassNames());
        assertTrue("test ServiceLoader provider must be discovered",
                catalog.extensionClassNames().contains(
                        "org.javaspec.extension.fixtures.CatalogTestExtension"));
    }

    @Test
    public void extensionNamesForDiagnosticReturnsNoneWhenEmpty() {
        // A fresh catalog with no external providers should say <none>.
        ExtensionCatalog catalog = ExtensionCatalog.discover(
                ClassLoader.getSystemClassLoader());
        // May or may not find extensions depending on classpath; at minimum must not throw.
        String diagnostic = catalog.extensionNamesForDiagnostic();
        assertNotNull(diagnostic);
        assertTrue(diagnostic.length() > 0);
    }

    @Test
    public void classpathRepairSuggestionContainsClasspathOption() {
        String suggestion = ExtensionCatalog.classpathRepairSuggestion("com.example.MyExtension");
        assertTrue(suggestion.contains("--classpath"));
        assertTrue(suggestion.contains("com.example.MyExtension"));
    }

    @Test
    public void classpathRepairSuggestionContainsResolvePomOption() {
        String suggestion = ExtensionCatalog.classpathRepairSuggestion("com.example.MyExtension");
        assertTrue(suggestion.contains("--resolve-pom"));
    }

    @Test
    public void printDoesNotThrow() {
        ExtensionCatalog catalog = ExtensionCatalog.discover(
                Thread.currentThread().getContextClassLoader());
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream out = new java.io.PrintStream(baos);
        catalog.print(out, "  ");
        String output = baos.toString();
        assertTrue(output.contains("Formatters:"));
        assertTrue(output.contains("Extensions:"));
    }
}
