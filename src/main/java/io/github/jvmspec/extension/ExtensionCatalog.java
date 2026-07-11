package io.github.jvmspec.extension;

import io.github.jvmspec.formatter.RunFormatter;
import io.github.jvmspec.formatter.RunFormatterRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Describes the extension and formatter providers currently visible on a class loader.
 *
 * <p>The catalog is used by diagnostic output (e.g. {@code javaspec list-extensions})
 * and by classpath-repair suggestions embedded in {@link ExtensionLoadingException}
 * messages.</p>
 */
public final class ExtensionCatalog {

    private final List<String> formatterNames;
    private final List<String> extensionClassNames;

    private ExtensionCatalog(List<String> formatterNames, List<String> extensionClassNames) {
        this.formatterNames = Collections.unmodifiableList(new ArrayList<String>(formatterNames));
        this.extensionClassNames = Collections.unmodifiableList(
                new ArrayList<String>(extensionClassNames));
    }

    /**
     * Discovers all extension and formatter providers visible on the given class loader.
     *
     * @param classLoader the class loader used for discovery
     * @return the catalog of discovered providers
     */
    public static ExtensionCatalog discover(ClassLoader classLoader) {
        List<String> formatters = discoverFormatters(classLoader);
        List<String> extensions = discoverExtensions(classLoader);
        return new ExtensionCatalog(formatters, extensions);
    }

    /**
     * Returns the names of discovered {@link RunFormatter} providers (both built-in and external).
     */
    public List<String> formatterNames() {
        return formatterNames;
    }

    /**
     * Returns the fully-qualified class names of discovered
     * {@link JavaspecExtension} / {@link Extension} providers.
     */
    public List<String> extensionClassNames() {
        return extensionClassNames;
    }

    /** Returns {@code true} when no external providers were discovered. */
    public boolean isEmpty() {
        return formatterNames.isEmpty() && extensionClassNames.isEmpty();
    }

    /**
     * Prints the catalog to {@code out} in a human-readable format.
     *
     * @param out    the stream to print to
     * @param indent indentation prefix for each line
     */
    public void print(java.io.PrintStream out, String indent) {
        out.println(indent + "Formatters:");
        if (formatterNames.isEmpty()) {
            out.println(indent + "  (none discovered via ServiceLoader; built-in: progress, pretty)");
        } else {
            for (int i = 0; i < formatterNames.size(); i++) {
                out.println(indent + "  " + formatterNames.get(i));
            }
        }
        out.println(indent + "Extensions:");
        if (extensionClassNames.isEmpty()) {
            out.println(indent + "  (none discovered via ServiceLoader)");
        } else {
            for (int i = 0; i < extensionClassNames.size(); i++) {
                out.println(indent + "  " + extensionClassNames.get(i));
            }
        }
    }

    /**
     * Returns a comma-separated string of extension class names for use in
     * diagnostic messages.  Returns {@code "<none>"} when the catalog is empty.
     */
    public String extensionNamesForDiagnostic() {
        if (extensionClassNames.isEmpty()) {
            return "<none>";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < extensionClassNames.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(extensionClassNames.get(i));
        }
        return builder.toString();
    }

    /**
     * Returns a suggestion paragraph that explains how to add a missing extension
     * to the run classpath.
     *
     * @param missingClassName the extension class that could not be found
     * @return human-readable suggestion text
     */
    public static String classpathRepairSuggestion(String missingClassName) {
        return "To add the missing extension to the run classpath, use one of:\n"
                + "  javaspec run --classpath <path-to-extension.jar> ...\n"
                + "  javaspec run --classpath-file <file-with-paths> ...\n"
                + "  javaspec run --resolve-pom <pom.xml> ...\n"
                + "The extension class '" + missingClassName + "' must be on the classpath at runtime.";
    }

    // -------------------------------------------------------------------------
    // Discovery helpers

    private static List<String> discoverFormatters(ClassLoader classLoader) {
        RunFormatterRegistry builtIn = RunFormatterRegistry.builtIn();
        Set<String> names = new LinkedHashSet<String>(builtIn.names());
        // Also discover via ServiceLoader
        try {
            Iterator<RunFormatter> it =
                    ServiceLoader.load(RunFormatter.class, classLoader).iterator();
            while (safeHasNext(it)) {
                RunFormatter formatter = safeNext(it);
                if (formatter != null) {
                    names.add(formatter.name());
                }
            }
        } catch (ServiceConfigurationError ignored) { /* best-effort */ }
        return new ArrayList<String>(names);
    }

    private static List<String> discoverExtensions(ClassLoader classLoader) {
        Set<String> names = new LinkedHashSet<String>();
        collectExtensionNames(JavaspecExtension.class, classLoader, names);
        collectExtensionNames(Extension.class, classLoader, names);
        return new ArrayList<String>(names);
    }

    private static <T extends JavaspecExtension> void collectExtensionNames(
            Class<T> serviceType, ClassLoader classLoader, Set<String> names) {
        try {
            Iterator<T> it = ServiceLoader.load(serviceType, classLoader).iterator();
            while (safeHasNext(it)) {
                T ext = safeNext(it);
                if (ext != null) {
                    names.add(ext.getClass().getName());
                }
            }
        } catch (ServiceConfigurationError ignored) { /* best-effort */ }
    }

    private static <T> boolean safeHasNext(Iterator<T> it) {
        try {
            return it.hasNext();
        } catch (ServiceConfigurationError ex) {
            return false;
        }
    }

    private static <T> T safeNext(Iterator<T> it) {
        try {
            return it.next();
        } catch (ServiceConfigurationError ex) {
            return null;
        }
    }
}
