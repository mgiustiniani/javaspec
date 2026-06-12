package org.javaspec.cli;

import org.javaspec.cli.CliArgumentParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;

public final class Main {
    static final int EXIT_OK = 0;
    static final int EXIT_MISSING_NOT_GENERATED = 1;
    static final int EXIT_COMPILATION_FAILED = 1;
    static final int EXIT_USAGE = 64;
    static final int EXIT_IO_ERROR = 70;

    static final String DEFAULT_SOURCE_ROOT = "src/main/java";
    static final String DEFAULT_SPEC_ROOT = "src/test/java";
    static final String DEFAULT_COMPILE_OUTPUT = "target/javaspec-classes";
    static final String DEFAULT_GENERATED_SOURCES = "target/generated-sources/javaspec";

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.in, System.out, System.err));
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        return run(args, new ByteArrayInputStream(new byte[0]), out, err);
    }

    public static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
        if (args == null) {
            UsagePrinter.printUsageError(err, "Arguments must not be null.");
            return EXIT_USAGE;
        }
        if (in == null) {
            UsagePrinter.printUsageError(err, "Input must not be null.");
            return EXIT_USAGE;
        }

        ParsedArguments parsed = CliArgumentParser.parse(args);
        if (parsed.helpRequested) {
            UsagePrinter.printUsage(out);
            return EXIT_OK;
        }
        if (parsed.errorMessage != null) {
            UsagePrinter.printUsageError(err, parsed.errorMessage);
            return EXIT_USAGE;
        }

        int configurationExitCode = new ConfigurationOrchestrator().applyConfiguration(parsed, err);
        if (configurationExitCode != EXIT_OK) {
            return configurationExitCode;
        }

        CommandHandler handler;
        if ("run".equals(parsed.command)) {
            handler = new RunCommandHandler();
        } else if ("prophesize".equals(parsed.command)) {
            handler = new ProphesizeCommandHandler();
        } else {
            handler = new DescribeCommandHandler();
        }
        return handler.execute(parsed, in, out, err);
    }




}
