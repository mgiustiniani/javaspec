package io.github.jvmspec.cli.run;

import io.github.jvmspec.extension.ExtensionLoadingException;
import io.github.jvmspec.extension.JavaspecExtensionActivator;
import io.github.jvmspec.extension.JavaspecExtensionLoader;
import io.github.jvmspec.formatter.RunFormatter;
import io.github.jvmspec.formatter.RunFormatterRegistry;
import io.github.jvmspec.runner.RunResult;

import java.io.PrintStream;
import java.util.List;

/**
 * Orchestrates extension loading, activation, and formatter validation.
 * <p>Extracted from {@link io.github.jvmspec.cli.Main Main} to isolate extension
 * logic and enable unit testing.</p>
 */
public final class ExtensionOrchestrator {
    private static final int EXIT_OK = 0;
    private static final int EXIT_USAGE = 64;

    private ExtensionOrchestrator() {
    }

    /**
     * Loads the {@link RunFormatterRegistry} from extensions on the given class loader.
     *
     * @param classLoader the class loader for extension discovery
     * @param err         error stream for diagnostic messages
     * @return a loaded registry, or {@code null} with the exit code set on failure
     */
    public static LoadResult<RunFormatterRegistry> loadFormatterRegistry(ClassLoader classLoader, PrintStream err) {
        try {
            RunFormatterRegistry registry = JavaspecExtensionLoader.loadRunFormatterRegistry(classLoader);
            return LoadResult.success(registry);
        } catch (ExtensionLoadingException ex) {
            err.println("Error: Could not load javaspec extensions: " + messageOf(ex));
            return LoadResult.failure(EXIT_USAGE);
        }
    }

    /**
     * Activates configuration-declared extensions in configured order against the
     * given registry instance.
     *
     * @param extensionClassNames list of extension class names (may be null or empty)
     * @param classLoader         the class loader for extension resolution
     * @param runFormatters       the formatter registry to configure
     * @param err                 error stream for diagnostic messages
     * @return EXIT_OK on success, EXIT_USAGE on activation failure
     */
    public static int activateExtensions(
            List<String> extensionClassNames,
            ClassLoader classLoader,
            RunFormatterRegistry runFormatters,
            PrintStream err
    ) {
        if (extensionClassNames == null || extensionClassNames.isEmpty()) {
            return EXIT_OK;
        }
        try {
            JavaspecExtensionActivator.activate(extensionClassNames, classLoader, runFormatters);
            return EXIT_OK;
        } catch (ExtensionLoadingException ex) {
            err.println("Error: Extension activation failed: " + messageOf(ex));
            return EXIT_USAGE;
        }
    }

    /**
     * Validates that the effective formatter name is registered in the registry.
     *
     * @param effectiveFormatter the formatter name to validate
     * @param registry           the formatter registry
     * @param err                error stream for diagnostic messages
     * @return EXIT_OK if valid, EXIT_USAGE if not found
     */
    public static int validateFormatter(
            String effectiveFormatter,
            RunFormatterRegistry registry,
            PrintStream err
    ) {
        if (registry.contains(effectiveFormatter)) {
            return EXIT_OK;
        }
        err.println("Error: Invalid formatter: " + effectiveFormatter
                + ". Valid values: " + joinNames(registry.formatterNames()) + ".");
        return EXIT_USAGE;
    }

    /**
     * Looks up the formatter and formats the run result.
     *
     * @param runResult      the run result to format
     * @param formatter      the formatter name
     * @param runFormatters  the formatter registry
     * @param out            output stream
     */
    public static void format(
            RunResult runResult,
            String formatter,
            RunFormatterRegistry runFormatters,
            PrintStream out
    ) {
        // Use RunResult as a placeholder for the actual type
        // The actual formatting is done by the RunFormatter
        RunFormatter runFormatter = runFormatters.lookup(formatter);
        if (runFormatter == null) {
            runFormatter = runFormatters.lookup(RunFormatterRegistry.FORMATTER_PROGRESS);
        }
        // Format using the RunFormatter — requires RunResult import
        runFormatter.format(runResult, out);
    }

    /**
     * Normalizes a formatter name via the registry.
     */
    public static String normalizeFormatter(String formatter) {
        return RunFormatterRegistry.normalizeName(formatter);
    }

    private static String joinNames(List<String> names) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(names.get(i));
        }
        return builder.toString();
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }

    /**
     * Simple result wrapper for operations that may fail with an exit code.
     */
    public static final class LoadResult<T> {
        private final T value;
        private final int exitCode;
        private final boolean success;

        private LoadResult(T value, int exitCode, boolean success) {
            this.value = value;
            this.exitCode = exitCode;
            this.success = success;
        }

        static <T> LoadResult<T> success(T value) {
            return new LoadResult<T>(value, 0, true);
        }

        static <T> LoadResult<T> failure(int exitCode) {
            return new LoadResult<T>(null, exitCode, false);
        }

        public T value() { return value; }
        public int exitCode() { return exitCode; }
        public boolean isSuccess() { return success; }
    }
}
