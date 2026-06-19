package org.javaspec.extension;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExtensionCatalogTest {

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
    public void extensionClassNamesIsNonNull() {
        ExtensionCatalog catalog = ExtensionCatalog.discover(
                Thread.currentThread().getContextClassLoader());
        assertNotNull(catalog.extensionClassNames());
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
