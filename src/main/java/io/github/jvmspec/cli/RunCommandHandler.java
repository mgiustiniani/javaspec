package io.github.jvmspec.cli;

import io.github.jvmspec.cli.run.BootstrapOrchestrator;
import io.github.jvmspec.cli.run.ClasspathResolver;
import io.github.jvmspec.cli.run.ClasspathSelection;
import io.github.jvmspec.cli.run.CompilationOrchestrator;
import io.github.jvmspec.cli.run.ExtensionOrchestrator;
import io.github.jvmspec.cli.run.GenerationActivity;
import io.github.jvmspec.cli.run.GenerationOrchestrator;
import io.github.jvmspec.cli.run.GenerationOrchestratorResult;
import io.github.jvmspec.cli.run.ReportOrchestrator;
import io.github.jvmspec.resolver.DependencyResolutionException;
import io.github.jvmspec.resolver.DependencyResolver;
import io.github.jvmspec.resolver.DependencyResolverLoader;
import io.github.jvmspec.internal.type.ConstructorDiscoveryException;
import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.internal.language.LanguageRuntime;
import io.github.jvmspec.discovery.SpecDiscoveryRequest;
import io.github.jvmspec.formatter.JsonRunFormatter;
import io.github.jvmspec.formatter.RunFormatterRegistry;
import io.github.jvmspec.generation.StubMarkerScanner;
import io.github.jvmspec.invocation.JavaspecExitCode;
import io.github.jvmspec.runner.ExampleResult;
import io.github.jvmspec.runner.ExampleStatus;
import io.github.jvmspec.runner.FailureDetail;
import io.github.jvmspec.runner.RunResult;
import io.github.jvmspec.runner.SpecResult;
import io.github.jvmspec.runner.SpecRunner;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the {@code run} command.
 * <p>Orchestrates the full javaspec run pipeline: classpath, extensions, discovery,
 * profile enforcement, generation, compilation, bootstrap hooks, execution, and reporting.</p>
 */
final class RunCommandHandler implements CommandHandler {
    @Override
    public int execute(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err) {
        GenerationReportState reportState = new GenerationReportState();
        if (!RunFormatterRegistry.FORMATTER_JSON.equals(parsed.effectiveFormatter)) {
            int exitCode = executePipeline(parsed, in, out, err, null, reportState);
            return GenerationReportWriter.write(
                    parsed.generationReportPath, reportState, parsed, exitCode, err);
        }
        ByteArrayOutputStream jsonBytes = new ByteArrayOutputStream();
        PrintStream jsonOut = new PrintStream(jsonBytes, true);
        int pipelineExitCode = executePipeline(parsed, in, err, err, jsonOut, reportState);
        int exitCode = GenerationReportWriter.write(
                parsed.generationReportPath, reportState, parsed, pipelineExitCode, err);
        if (exitCode != pipelineExitCode) {
            jsonBytes.reset();
        }
        if (jsonBytes.size() == 0) {
            new JsonRunFormatter().format(machineResultForEarlyExit(exitCode), jsonOut);
        }
        jsonOut.flush();
        out.print(new String(jsonBytes.toByteArray(), StandardCharsets.UTF_8));
        out.flush();
        return exitCode;
    }

    private int executePipeline(
            ParsedArguments parsed,
            InputStream in,
            PrintStream out,
            PrintStream err,
            PrintStream jsonOut,
            GenerationReportState reportState
    ) {
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

        if (parsed.resolvePomSpecified) {
            java.io.File pomFile = new java.io.File(parsed.resolvePomPath);
            if (!pomFile.isFile()) {
                err.println("Error: POM file not found: " + pomFile.getPath());
                return Main.EXIT_USAGE;
            }
            DependencyResolver resolver = DependencyResolverLoader.findFor(
                    pomFile, classpathSelection.classLoader());
            if (resolver == null) {
                err.println("Error: No dependency resolver available for: " + pomFile.getPath());
                err.println("Add a DependencyResolver provider to the classpath via ServiceLoader.");
                return Main.EXIT_USAGE;
            }
            java.util.List<java.io.File> resolved;
            try {
                resolved = resolver.resolve(pomFile);
            } catch (DependencyResolutionException ex) {
                err.println("Error: Dependency resolution failed: " + ex.getMessage());
                return Main.EXIT_IO_ERROR;
            }
            if (parsed.verbose) {
                err.println("[resolve-pom] Using resolver: " + resolver.name());
                err.println("[resolve-pom] Resolved " + resolved.size() + " artifact(s) from " + pomFile.getPath());
            }
            classpathSelection = ClasspathResolver.withResolvedDependencies(
                    resolved, classpathSelection, err);
            if (classpathSelection.exitCode() != Main.EXIT_OK) {
                return classpathSelection.exitCode();
            }
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
            specs = LanguageRuntime.javaSpecFrontend().discover(discoveryRequest);
        } catch (ConstructorDiscoveryException ex) {
            err.println(ex.getMessage());
            return Main.EXIT_MISSING_NOT_GENERATED;
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
        GenerationActivity generationActivity = new GenerationActivity();
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
                ConfigurationHelper.resolveConstructorPolicy(parsed),
                new File(Main.DEFAULT_GENERATED_SOURCES),
                generationActivity
        );
        reportState.generationCompleted(genResult, generationActivity);
        if (!genResult.shouldProceed()) {
            return genResult.exitCode();
        }

        List<StubMarkerScanner.StubLocation> pendingStubs = reportPendingStubs(sourceRoot, out, err);
        reportState.pendingStubs(pendingStubs);

        ClasspathSelection executionClasspathSelection = classpathSelection;
        if (parsed.compile && !parsed.dryRun) {
            executionClasspathSelection = CompilationOrchestrator.compile(
                    parsed.compileOutputPath,
                    sourceRoot,
                    specRoot,
                    new File(Main.DEFAULT_GENERATED_SOURCES),
                    classpathSelection,
                    parsed.releaseVersion,
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

        RunResult runResult = SpecRunner.run(specs, selectedClassLoader, parsed.stopOnFailure,
                parsed.autoCheckPredictions);
        runResult = withPendingStubResult(runResult, pendingStubs);
        RunDiagnosticsPrinter diagnosticsPrinter = new RunDiagnosticsPrinter();
        diagnosticsPrinter.printRunnerSummary(
                runResult, jsonOut == null ? out : jsonOut, parsed.effectiveFormatter, runFormatters);
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

    private static RunResult machineResultForEarlyExit(int exitCode) {
        if (exitCode == Main.EXIT_OK) {
            return ReportOrchestrator.emptyResult();
        }
        String detail = "Run stopped before example execution (exit code " + exitCode + ").";
        ExampleResult stopped = ExampleResult.of(
                "javaspec.cli.Run",
                "run_stopped_before_example_execution",
                "run stopped before example execution",
                0,
                ExampleStatus.BROKEN,
                detail,
                FailureDetail.of(new IllegalStateException(detail)),
                null,
                0
        );
        return RunResult.of(java.util.Arrays.asList(
                SpecResult.of("javaspec.cli.Run", java.util.Arrays.asList(stopped))));
    }

    /**
     * Reports generated stubs still pending hand implementation, identified by the
     * {@code // javaspec:stub} marker the generator leaves as the first body line.
     */
    private static List<StubMarkerScanner.StubLocation> reportPendingStubs(File sourceRoot, PrintStream out, PrintStream err) {
        List<StubMarkerScanner.StubLocation> pendingStubs;
        try {
            pendingStubs = StubMarkerScanner.scan(sourceRoot);
        } catch (java.io.IOException ex) {
            err.println("Warning: could not scan for pending stubs: " + ex.getMessage());
            return new ArrayList<StubMarkerScanner.StubLocation>();
        }
        if (pendingStubs.isEmpty()) {
            return pendingStubs;
        }
        out.println("Pending stubs: " + pendingStubs.size() + " (implement and remove the "
                + StubMarkerScanner.STUB_MARKER + " marker)");
        for (int i = 0; i < pendingStubs.size(); i++) {
            out.println("  " + pendingStubs.get(i));
        }
        return pendingStubs;
    }

    private static RunResult withPendingStubResult(RunResult runResult, List<StubMarkerScanner.StubLocation> pendingStubs) {
        if (pendingStubs == null || pendingStubs.isEmpty()) {
            return runResult;
        }
        List<SpecResult> specResults = new ArrayList<SpecResult>(runResult.specResults());
        StubMarkerScanner.StubLocation first = pendingStubs.get(0);
        String detail = "Generated production stubs pending implementation: " + pendingStubs.size();
        AssertionError failure = new AssertionError(detail + "; first stub at " + first);
        ExampleResult pendingStubExample = ExampleResult.of(
                "javaspec.generation.PendingStubs",
                "generated_stubs_pending_implementation",
                "generated stubs pending implementation",
                0,
                ExampleStatus.BROKEN,
                detail,
                FailureDetail.of(failure),
                first.file().getPath(),
                first.line()
        );
        specResults.add(SpecResult.of("javaspec.generation.PendingStubs", java.util.Arrays.asList(pendingStubExample)));
        return RunResult.of(specResults);
    }
}
