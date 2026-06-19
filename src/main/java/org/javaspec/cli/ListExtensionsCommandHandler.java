package org.javaspec.cli;

import org.javaspec.extension.ExtensionCatalog;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Handles the {@code list-extensions} command.
 *
 * <p>Prints the extension and formatter providers currently visible on the run
 * classpath, together with a brief usage hint.  This command exits {@code 0}
 * regardless of whether any providers are discovered.</p>
 */
final class ListExtensionsCommandHandler implements CommandHandler {

    @Override
    public int execute(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err) {
        ClassLoader classLoader = effectiveClassLoader();
        ExtensionCatalog catalog = ExtensionCatalog.discover(classLoader);

        out.println("javaspec extension catalog (classpath-based ServiceLoader discovery):");
        catalog.print(out, "  ");
        out.println();
        out.println("To add extensions, use one of:");
        out.println("  javaspec run --classpath <path-to-extension.jar> ...");
        out.println("  javaspec run --classpath-file <file-with-paths> ...");
        out.println("  javaspec run --resolve-pom <pom.xml> ...");
        out.println();
        out.println("External providers are registered via META-INF/services/<interface-name>.");
        return Main.EXIT_OK;
    }

    private static ClassLoader effectiveClassLoader() {
        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        return ctx != null ? ctx : ListExtensionsCommandHandler.class.getClassLoader();
    }
}
