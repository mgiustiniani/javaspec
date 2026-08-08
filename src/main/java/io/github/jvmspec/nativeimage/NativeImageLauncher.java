package io.github.jvmspec.nativeimage;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.formatter.RunFormatter;
import io.github.jvmspec.formatter.RunFormatterRegistry;
import io.github.jvmspec.runner.RunResult;
import io.github.jvmspec.runner.SpecRunner;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

/**
 * No-exit launcher for a project-specific GraalVM Native Image test executable.
 *
 * <p>The surrounding project generates a main class that embeds deterministic specification
 * metadata and statically references every specification and described type. Native Image then
 * links that closed set of project classes together with the javaspec core. This launcher only
 * executes that linked set: it intentionally does not perform runtime source discovery,
 * compilation, generation, classpath mutation, or extension loading.</p>
 */
public final class NativeImageLauncher {
    public static final int EXIT_OK = 0;
    public static final int EXIT_FAILURE = 1;
    public static final int EXIT_USAGE = 64;

    private static final String DEFAULT_FORMATTER = RunFormatterRegistry.FORMATTER_PRETTY;

    private NativeImageLauncher() {
    }

    /**
     * Runs linked specifications with the launcher's defining class loader.
     *
     * @param args  native runner options
     * @param specs build-generated specification metadata
     * @param out   standard output destination
     * @param err   diagnostic output destination
     * @return a process-compatible exit code without calling {@code System.exit}
     */
    public static int run(String[] args, List<DiscoveredSpec> specs, PrintStream out, PrintStream err) {
        return run(args, specs, NativeImageLauncher.class.getClassLoader(), out, err);
    }

    /**
     * Runs linked specifications with an explicit class loader.
     *
     * <p>The explicit loader keeps the API testable on a JVM and allows a generated entrypoint to
     * supply its defining loader. In a native executable all loadable specification classes must
     * already have been linked into the image.</p>
     */
    public static int run(
            String[] args,
            List<DiscoveredSpec> specs,
            ClassLoader classLoader,
            PrintStream out,
            PrintStream err
    ) {
        Objects.requireNonNull(specs, "specs must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(err, "err must not be null");

        ParsedOptions options = parse(args);
        if (options.error != null) {
            err.println("javaspec native: " + options.error);
            err.println("Run with --help for the supported native preview options.");
            return EXIT_USAGE;
        }
        if (options.help) {
            printHelp(out);
            return EXIT_OK;
        }

        RunFormatter formatter = RunFormatterRegistry.builtIn().lookup(options.formatter);
        if (formatter == null) {
            err.println("javaspec native: Unknown formatter: " + options.formatter
                    + ". Expected progress, pretty, or json.");
            return EXIT_USAGE;
        }

        RunResult result = SpecRunner.run(
                specs,
                classLoader,
                options.stopOnFailure,
                options.autoCheckPredictions
        );
        formatter.format(result, out);
        return result.hasFailures() ? EXIT_FAILURE : EXIT_OK;
    }

    private static ParsedOptions parse(String[] args) {
        if (args == null) {
            return ParsedOptions.error("Arguments must not be null.");
        }
        ParsedOptions options = new ParsedOptions();
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if (argument == null) {
                return ParsedOptions.error("Arguments must not contain null values.");
            }
            if ("--help".equals(argument) || "-h".equals(argument)) {
                options.help = true;
            } else if ("--stop-on-failure".equals(argument)) {
                options.stopOnFailure = true;
            } else if ("--auto-check-predictions".equals(argument)) {
                options.autoCheckPredictions = true;
            } else if ("--no-auto-check-predictions".equals(argument)) {
                options.autoCheckPredictions = false;
            } else if (argument.startsWith("--formatter=")) {
                String value = argument.substring("--formatter=".length());
                if (value.length() == 0) {
                    return ParsedOptions.error("Missing value for --formatter.");
                }
                options.formatter = value;
            } else if ("--formatter".equals(argument)) {
                if (i + 1 >= args.length || args[i + 1] == null || args[i + 1].length() == 0) {
                    return ParsedOptions.error("Missing value for --formatter.");
                }
                options.formatter = args[++i];
            } else {
                return ParsedOptions.error("Unsupported native option: " + argument + ".");
            }
        }
        return options;
    }

    private static void printHelp(PrintStream out) {
        out.println("Usage: <project-native-specs> [options]");
        out.println();
        out.println("Executes specifications linked into this executable at native-image build time.");
        out.println();
        out.println("Options:");
        out.println("  --formatter <progress|pretty|json>  Console formatter (default: pretty)");
        out.println("  --stop-on-failure                  Stop after the first failed or broken example");
        out.println("  --auto-check-predictions           Verify prophecy predictions automatically (default)");
        out.println("  --no-auto-check-predictions        Disable automatic prediction verification");
        out.println("  -h, --help                         Show this help");
        out.println();
        out.println("Native preview boundary:");
        out.println("  No runtime source discovery, compilation, generation, classpath mutation,");
        out.println("  ServiceLoader extension discovery, or bytecode-agent self-attach.");
    }

    private static final class ParsedOptions {
        private String formatter = DEFAULT_FORMATTER;
        private boolean stopOnFailure;
        private boolean autoCheckPredictions = true;
        private boolean help;
        private String error;

        private static ParsedOptions error(String message) {
            ParsedOptions options = new ParsedOptions();
            options.error = message;
            return options;
        }
    }
}
