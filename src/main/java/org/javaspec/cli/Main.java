package org.javaspec.cli;

import org.javaspec.diagnostics.RunDiagnostics;
import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.formatter.RunFormatter;
import org.javaspec.formatter.RunFormatterRegistry;
import org.javaspec.cli.run.ClasspathSelection;
import org.javaspec.cli.CliArgumentParser;
import org.javaspec.cli.run.ExtensionOrchestrator;
import org.javaspec.cli.run.RunOrchestratorResult;
import org.javaspec.runner.RunResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Main {
    static final int EXIT_OK = 0;
    static final int EXIT_MISSING_NOT_GENERATED = 1;
    static final int EXIT_COMPILATION_FAILED = 1;
    static final int EXIT_USAGE = 64;
    static final int EXIT_IO_ERROR = 70;

    static final String DEFAULT_SOURCE_ROOT = "src/main/java";
    static final String DEFAULT_SPEC_ROOT = "src/test/java";
    static final String DEFAULT_COMPILE_OUTPUT = "target/javaspec-classes";

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
        } else {
            handler = new DescribeCommandHandler();
        }
        return handler.execute(parsed, in, out, err);
    }



    static ConstructorPolicy resolveConstructorPolicy(ParsedArguments parsed) {
        if (parsed.effectiveConstructorPolicy != null) {
            return parsed.effectiveConstructorPolicy;
        }
        return ConstructorPolicy.defaultPolicy();
    }

    static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }


}
