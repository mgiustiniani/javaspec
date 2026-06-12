package org.javaspec.cli.run;

import org.javaspec.bootstrap.BootstrapException;
import org.javaspec.bootstrap.BootstrapRunner;
import org.javaspec.discovery.DiscoveredSpec;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrates bootstrap hook execution and bootstrap discovery.
 * <p>Extracted from {@link org.javaspec.cli.Main Main} to isolate bootstrap
 * logic and enable unit testing.</p>
 */
public final class BootstrapOrchestrator {
    private static final int EXIT_OK = 0;
    private static final int EXIT_USAGE = 64;

    private BootstrapOrchestrator() {
    }

    /**
     * Executes configured bootstrap hooks and optional bootstrap discovery.
     *
     * @param bootstrapHooks     list of hook class names (may be null or empty)
     * @param bootstrapDiscovery whether to perform bootstrap discovery
     * @param classLoader        the class loader for hook and discovery resolution
     * @param specs              the spec list (may be modified by discovery)
     * @param err                error stream for diagnostic messages
     * @return EXIT_OK on success, EXIT_USAGE on bootstrap failure
     */
    public static int execute(
            List<String> bootstrapHooks,
            boolean bootstrapDiscovery,
            ClassLoader classLoader,
            List<DiscoveredSpec> specs,
            PrintStream err
    ) {
        List<String> hooks = bootstrapHooks == null ? Collections.<String>emptyList() : bootstrapHooks;
        if (hooks.isEmpty() && !bootstrapDiscovery) {
            return EXIT_OK;
        }
        try {
            BootstrapRunner.run(hooks, classLoader, specs, bootstrapDiscovery);
            return EXIT_OK;
        } catch (BootstrapException ex) {
            err.println("Error: Bootstrap execution failed: " + messageOf(ex));
            return EXIT_USAGE;
        }
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
