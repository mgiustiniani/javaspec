package org.javaspec.cli;

import org.javaspec.cli.run.BootstrapOrchestrator;
import org.javaspec.cli.run.ClasspathResolver;
import org.javaspec.cli.run.ClasspathSelection;
import org.javaspec.cli.run.CompilationOrchestrator;
import org.javaspec.cli.run.ExtensionOrchestrator;
import org.javaspec.cli.run.GenerationOrchestrator;
import org.javaspec.cli.run.GenerationOrchestratorResult;
import org.javaspec.cli.run.ReportOrchestrator;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecDiscovery;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.formatter.RunFormatterRegistry;
import org.javaspec.invocation.JavaspecExitCode;
import org.javaspec.runner.RunResult;
import org.javaspec.runner.SpecRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Handles the {@code run} command.
 * <p>Orchestrates the full javaspec run pipeline: classpath, extensions, discovery,
 * profile enforcement, generation, compilation, bootstrap hooks, execution, and reporting.</p>
 */
final class RunCommandHandler implements CommandHandler {
    @Override
    public int execute(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err) {
        File specRoot = new File(parsed.specRoot);
        File sourceRoot = new File(parsed.sourceRoot);
        BufferedReader input = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        ClasspathSelection classpathSelection = ClasspathResolver.select(
                parsed.classpathArguments,
                err
        );
        if (classpathSelection.exitCode() != Main.EXIT_OK) {
            return classpathSelection.exitCode();
        }
        ClassLoader selectedClassLoader = classpathSelection.classLoader();
        ExtensionOrchestrator.LoadResult<RunFormatterRegistry> loadResult =
                ExtensionOrchestrator.loadFormatterRegistry(selectedClassLoader, err);
        if (!loadResult.isSuccess()) {
            return loadResult.exitCode();
        }
        RunFormatterRegistry runFormatters = loadResult.value();
        int extensionActivationExitCode = ExtensionOrchestrator.activateExtensions(
                parsed.effectiveExtensions,
                selectedClassLoader,
                runFormatters,
                err
        );
        if (extensionActivationExitCode != Main.EXIT_OK) {
            return extensionActivationExitCode;
        }
        int formatterExitCode = ExtensionOrchestrator.validateFormatter(
                parsed.effectiveFormatter,
                runFormatters,
                err
        );
        if (formatterExitCode != Main.EXIT_OK) {
            UsagePrinter.printUsage(err);
            return formatterExitCode;
        }

        if (parsed.verbose) {
            new RunDiagnosticsPrinter().printRunConfiguration(parsed, out, classpathSelection);
        }

        SpecDiscoveryRequest discoveryRequest = SpecDiscoveryRequest.of(specRoot, parsed.namingConvention);
        if (parsed.classFilters != null) {
            for (int fi = 0; fi < parsed.classFilters.size(); fi++) {
                discoveryRequest = discoveryRequest.withClassFilter(parsed.classFilters.get(fi));
            }
        }
        if (parsed.exampleFilters != null) {
            for (int fi = 0; fi < parsed.exampleFilters.size(); fi++) {
                discoveryRequest = discoveryRequest.withExampleFilter(parsed.exampleFilters.get(fi));
            }
        }

        List<DiscoveredSpec> specs;
        try {
            specs = SpecDiscovery.discover(discoveryRequest);
        } catch (SecurityException ex) {
            err.println("I/O error while discovering specifications: " + ConfigurationHelper.messageOf(ex));
            err.println("Spec root: " + specRoot.getPath());
            return Main.EXIT_IO_ERROR;
        }

        if (specs.size() == 0) {
            out.println("No specifications found in " + specRoot.getPath() + ".");
            return ReportOrchestrator.writeRequested(
                    ReportOrchestrator.emptyResult(),
                    parsed.reportPath,
                    parsed.junitXmlPath,
                    err
            );
        }

        out.println("Found " + specs.size() + " specification(s) in " + specRoot.getPath() + ".");
        int profileEnforcementExitCode = new ProfileEnforcementOrchestrator().enforceProfileCompatibility(parsed.effectiveProfile, specs, err);
        if (profileEnforcementExitCode != Main.EXIT_OK) {
            return profileEnforcementExitCode;
        }
        GenerationOrchestratorResult genResult = GenerationOrchestrator.execute(
                specs,
                specRoot,
                sourceRoot,
                input,
                out,
                err,
                parsed.generate,
                parsed.dryRun,
                parsed.namingConvention,
                selectedClassLoader,
                ConfigurationHelper.resolveConstructorPolicy(parsed)
        );
        if (!genResult.shouldProceed()) {
            return genResult.exitCode();
        }

        ClasspathSelection executionClasspathSelection = classpathSelection;
        if (parsed.compile && !parsed.dryRun) {
            executionClasspathSelection = CompilationOrchestrator.compile(
                    parsed.compileOutputPath,
                    sourceRoot,
                    specRoot,
                    classpathSelection,
                    out,
                    err
            );

            if (executionClasspathSelection.exitCode() != Main.EXIT_OK) {
                return executionClasspathSelection.exitCode();
            }
            selectedClassLoader = executionClasspathSelection.classLoader();
        }

        int bootstrapExitCode = BootstrapOrchestrator.execute(
                parsed.effectiveBootstrapHooks,
                parsed.effectiveBootstrapDiscovery,
                selectedClassLoader,
                specs,
                err
        );
        if (bootstrapExitCode != Main.EXIT_OK) {
            return bootstrapExitCode;
        }

        RunResult runResult = SpecRunner.run(specs, selectedClassLoader, parsed.stopOnFailure);
        RunDiagnosticsPrinter diagnosticsPrinter = new RunDiagnosticsPrinter();
        diagnosticsPrinter.printRunnerSummary(runResult, out, parsed.effectiveFormatter, runFormatters);
        diagnosticsPrinter.printExecutionDiagnostics(runResult, out, executionClasspathSelection);
        int reportExitCode = ReportOrchestrator.writeRequested(
                runResult,
                parsed.reportPath,
                parsed.junitXmlPath,
                err
        );
        if (reportExitCode != Main.EXIT_OK) {
            return reportExitCode;
        }
        return JavaspecExitCode.from(runResult);
    }
}
