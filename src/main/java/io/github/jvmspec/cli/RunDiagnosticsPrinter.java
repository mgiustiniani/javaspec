package io.github.jvmspec.cli;

import io.github.jvmspec.diagnostics.RunDiagnostics;
import io.github.jvmspec.formatter.RunFormatter;
import io.github.jvmspec.formatter.RunFormatterRegistry;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.runner.RunResult;

import io.github.jvmspec.cli.run.ClasspathSelection;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

/**
 * Prints runner summary, execution diagnostics, and run configuration for javaspec CLI.
 */
final class RunDiagnosticsPrinter {

    void printRunnerSummary(
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

    void printExecutionDiagnostics(
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

    void printRunConfiguration(
            ParsedArguments parsed,
            PrintStream out,
            ClasspathSelection classpathSelection
    ) {
        out.println("Run configuration:");
        out.println("  Selected suite: " + parsed.selectedSuite.name());
        out.println("  Spec root: " + parsed.specRoot);
        out.println("  Source root: " + parsed.sourceRoot);
        out.println("  Spec package prefix: " + parsed.namingConvention.specPackagePrefix());
        out.println("  Production package prefix: " + displayPrefix(parsed.namingConvention.productionPackagePrefix()));
        out.println("  Constructor policy: " + policyOptionName(ConfigurationHelper.resolveConstructorPolicy(parsed)));
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
        if (parsed.generationReportPath != null) {
            out.println("  Generation report path: " + parsed.generationReportPath);
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
}
