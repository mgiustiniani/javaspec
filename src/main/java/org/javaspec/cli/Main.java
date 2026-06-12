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

    static void printRunnerSummary(
            RunResult runResult,
            PrintStream out,
            String formatter,
            RunFormatterRegistry runFormatters
    ) {
        RunFormatter runFormatter = runFormatters.lookup(formatter);
        if (runFormatter == null) {
            runFormatter = runFormatters.lookup(RunFormatterRegistry.FORMATTER_PROGRESS);
        }
        runFormatter.format(runResult, out);
    }

    static void printExecutionDiagnostics(
            RunResult runResult,
            PrintStream out,
            ClasspathSelection classpathSelection
    ) {
        List<String> lines = RunDiagnostics.executionAvailabilityLines(runResult);
        if (lines.isEmpty()) {
            return;
        }
        out.println("Execution diagnostics:");
        for (int i = 0; i < lines.size(); i++) {
            out.println("  - " + lines.get(i));
        }
        if (classpathSelection.includesCompileOutput()) {
            out.println("  - Compile output classpath entry: "
                    + classpathSelection.compiledOutputDirectory().getPath());
        }
        if (classpathSelection.hasExplicitEntries()) {
            out.println("  - Explicit classpath entries provided: " + classpathSelection.entries().size()
                    + ". Verify these entries contain compiled spec classes and required dependencies.");
        } else {
            out.println("  - No explicit classpath entries were provided; javaspec used the current process classloader. "
                    + "Use --classpath or --classpath-file with compiled test/spec output and dependencies "
                    + "(for Maven, include target/test-classes and target/classes).");
        }
    }

    static void printRunConfiguration(ParsedArguments parsed, PrintStream out, ClasspathSelection classpathSelection) {
        out.println("Run configuration:");
        out.println("  Selected suite: " + parsed.selectedSuite.name());
        out.println("  Spec root: " + parsed.specRoot);
        out.println("  Source root: " + parsed.sourceRoot);
        out.println("  Spec package prefix: " + parsed.namingConvention.specPackagePrefix());
        out.println("  Production package prefix: " + displayPrefix(parsed.namingConvention.productionPackagePrefix()));
        out.println("  Constructor policy: " + policyOptionName(resolveConstructorPolicy(parsed)));
        out.println("  Profile: " + parsed.effectiveProfile.key());
        out.println("  Formatter: " + parsed.effectiveFormatter);
        if (parsed.compile) {
            out.println("  Compile: true");
            out.println("  Compile output: " + parsed.compileOutputPath);
        }
        if (parsed.effectiveBootstrapHooks != null && !parsed.effectiveBootstrapHooks.isEmpty()) {
            out.println("  Bootstrap hooks: " + joinNames(parsed.effectiveBootstrapHooks));
        }
        if (parsed.effectiveBootstrapDiscovery) {
            out.println("  Bootstrap discovery: true");
        }
        if (parsed.effectiveExtensions != null && !parsed.effectiveExtensions.isEmpty()) {
            out.println("  Extensions: " + joinNames(parsed.effectiveExtensions));
        }
        if (parsed.reportPath != null) {
            out.println("  Report path: " + parsed.reportPath);
        }
        if (parsed.junitXmlPath != null) {
            out.println("  JUnit XML path: " + parsed.junitXmlPath);
        }
        if (classpathSelection.hasExplicitEntries()) {
            out.println("  Explicit classpath entries:");
            List<File> classpathEntries = classpathSelection.entries();
            for (int i = 0; i < classpathEntries.size(); i++) {
                out.println("    " + classpathEntries.get(i).getPath());
            }
        }
        out.println("  Dry-run: " + parsed.dryRun);
        out.println("  Stop-on-failure: " + parsed.stopOnFailure);
    }

    private static String displayPrefix(String prefix) {
        if (prefix.length() == 0) {
            return "<none>";
        }
        return prefix;
    }

    private static String policyOptionName(ConstructorPolicy policy) {
        return policy.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String formatterFromConfiguration(String formatter) {
        String normalized = normalizeFormatter(formatter);
        if (normalized == null) {
            return RunFormatterRegistry.FORMATTER_PROGRESS;
        }
        return normalized;
    }

    private static String normalizeFormatter(String formatter) {
        return RunFormatterRegistry.normalizeName(formatter);
    }

    private static int validateEffectiveFormatter(
            ParsedArguments parsed,
            RunFormatterRegistry runFormatters,
            PrintStream err
    ) {
        if (runFormatters.contains(parsed.effectiveFormatter)) {
            return EXIT_OK;
        }
        UsagePrinter.printUsageError(err, "Invalid formatter: " + selectedFormatterDisplay(parsed)
                + ". Valid values: " + validFormatterNames(runFormatters) + ".");
        return EXIT_USAGE;
    }

    private static String selectedFormatterDisplay(ParsedArguments parsed) {
        if (parsed.formatterSpecified) {
            return parsed.formatter;
        }
        if (parsed.configuration != null) {
            return parsed.configuration.formatter();
        }
        if (parsed.effectiveFormatter != null) {
            return parsed.effectiveFormatter;
        }
        return RunFormatterRegistry.FORMATTER_PROGRESS;
    }

    private static String validFormatterNames(RunFormatterRegistry registry) {
        return joinNames(registry.formatterNames());
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




    static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }


}
