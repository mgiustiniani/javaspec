package org.javaspec.cli;

import org.javaspec.compatibility.ProfileEnforcement;
import org.javaspec.compatibility.ProfileEnforcementReport;
import org.javaspec.compatibility.ProfileViolation;
import org.javaspec.config.ConfigurationException;
import org.javaspec.config.JavaspecConfiguration;
import org.javaspec.config.JavaspecSuiteConfiguration;
import org.javaspec.diagnostics.RunDiagnostics;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecDiscovery;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.discovery.SpecNamingConvention;
import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.generation.SpecFileGenerator;
import org.javaspec.generation.SpecGenerationPlan;
import org.javaspec.generation.SpecSkeletonGenerator;
import org.javaspec.generation.SpecSupportFileGenerator;
import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.formatter.RunFormatter;
import org.javaspec.formatter.RunFormatterRegistry;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.cli.run.ClasspathArgument;
import org.javaspec.cli.run.ClasspathResolver;
import org.javaspec.cli.run.ClasspathSelection;
import org.javaspec.cli.CliArgumentParser;
import org.javaspec.cli.run.BootstrapOrchestrator;
import org.javaspec.cli.run.CompilationOrchestrator;
import org.javaspec.cli.run.ExtensionOrchestrator;
import org.javaspec.cli.run.GenerationOrchestrator;
import org.javaspec.cli.run.GenerationOrchestratorResult;
import org.javaspec.cli.run.ReportOrchestrator;
import org.javaspec.cli.run.RunOrchestratorResult;
import org.javaspec.invocation.JavaspecExitCode;
import org.javaspec.profile.TargetProfile;
import org.javaspec.runner.RunResult;
import org.javaspec.runner.SpecRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Main {
    private static final int EXIT_OK = 0;
    private static final int EXIT_MISSING_NOT_GENERATED = 1;
    private static final int EXIT_COMPILATION_FAILED = 1;
    private static final int EXIT_USAGE = 64;
    private static final int EXIT_IO_ERROR = 70;

    private static final String DEFAULT_SOURCE_ROOT = "src/main/java";
    private static final String DEFAULT_SPEC_ROOT = "src/test/java";
    private static final String DEFAULT_COMPILE_OUTPUT = "target/javaspec-classes";

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
            printUsageError(err, "Arguments must not be null.");
            return EXIT_USAGE;
        }
        if (in == null) {
            printUsageError(err, "Input must not be null.");
            return EXIT_USAGE;
        }

        ParsedArguments parsed = CliArgumentParser.parse(args);
        if (parsed.helpRequested) {
            printUsage(out);
            return EXIT_OK;
        }
        if (parsed.errorMessage != null) {
            printUsageError(err, parsed.errorMessage);
            return EXIT_USAGE;
        }

        int configurationExitCode = applyConfiguration(parsed, err);
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

    private static int applyConfiguration(ParsedArguments parsed, PrintStream err) {
        JavaspecConfiguration configuration;
        if (parsed.configPath == null) {
            configuration = JavaspecConfiguration.defaults();
        } else {
            File configFile = new File(parsed.configPath);
            try {
                configuration = JavaspecConfiguration.load(configFile);
            } catch (ConfigurationException ex) {
                err.println("Error: Invalid configuration: " + messageOf(ex));
                return EXIT_USAGE;
            } catch (IOException ex) {
                err.println("Error: I/O error while reading configuration: " + messageOf(ex));
                err.println("Config path: " + configFile.getPath());
                return EXIT_IO_ERROR;
            } catch (SecurityException ex) {
                err.println("Error: I/O error while reading configuration: " + messageOf(ex));
                err.println("Config path: " + configFile.getPath());
                return EXIT_IO_ERROR;
            }
        }

        String selectedSuiteName = parsed.suiteName == null ? configuration.defaultSuiteName() : parsed.suiteName;
        JavaspecSuiteConfiguration selectedSuite;
        try {
            selectedSuite = configuration.suite(selectedSuiteName);
        } catch (ConfigurationException ex) {
            err.println("Error: Invalid configuration: " + messageOf(ex));
            return EXIT_USAGE;
        }

        parsed.configuration = configuration;
        parsed.selectedSuite = selectedSuite;
        parsed.effectiveBootstrapHooks = bootstrapHooksFor(configuration, selectedSuite);
        parsed.effectiveBootstrapDiscovery = configuration.effectiveBootstrapDiscovery(selectedSuite);
        parsed.effectiveExtensions = extensionsFor(configuration, selectedSuite);
        if (!parsed.specRootSpecified) {
            parsed.specRoot = selectedSuite.specDirectory();
        }
        if (!parsed.sourceRootSpecified) {
            parsed.sourceRoot = selectedSuite.sourceDirectory();
        }
        parsed.effectiveConstructorPolicy = parsed.constructorPolicyOverride == null
                ? configuration.constructorPolicy()
                : parsed.constructorPolicyOverride;
        parsed.effectiveProfile = parsed.profileOverride == null
                ? configuration.profile()
                : parsed.profileOverride;
        parsed.effectiveFormatter = parsed.formatterOverride == null
                ? formatterFromConfiguration(configuration.formatter())
                : parsed.formatterOverride;
        if ("run".equals(parsed.command)) {
            if (!parsed.reportSpecified && configuration.jsonReportFile() != null) {
                parsed.reportPath = configuration.jsonReportFile();
            }
            if (!parsed.junitXmlSpecified && configuration.junitXmlReportFile() != null) {
                parsed.junitXmlPath = configuration.junitXmlReportFile();
            }
        }
        try {
            parsed.namingConvention = SpecNamingConvention.from(selectedSuite);
        } catch (IllegalArgumentException ex) {
            err.println("Error: Invalid naming metadata: " + messageOf(ex));
            return EXIT_USAGE;
        } catch (RuntimeException ex) {
            err.println("Error: Invalid naming metadata: " + messageOf(ex));
            return EXIT_USAGE;
        }
        return EXIT_OK;
    }

    private static List<String> bootstrapHooksFor(
            JavaspecConfiguration configuration,
            JavaspecSuiteConfiguration selectedSuite
    ) {
        List<String> hooks = new ArrayList<String>();
        hooks.addAll(configuration.bootstrapHooks());
        hooks.addAll(selectedSuite.bootstrapHooks());
        if (hooks.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(hooks);
    }

    private static List<String> extensionsFor(
            JavaspecConfiguration configuration,
            JavaspecSuiteConfiguration selectedSuite
    ) {
        List<String> extensions = new ArrayList<String>();
        extensions.addAll(configuration.extensions());
        extensions.addAll(selectedSuite.extensions());
        if (extensions.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(extensions);
    }

    private static ConstructorPolicy resolveConstructorPolicy(ParsedArguments parsed) {
        if (parsed.effectiveConstructorPolicy != null) {
            return parsed.effectiveConstructorPolicy;
        }
        return ConstructorPolicy.defaultPolicy();
    }

    static int describeClass(ParsedArguments parsed, PrintStream out, PrintStream err) {
        DescribedClass describedClass;
        try {
            describedClass = DescribedClass.of(parsed.className);
        } catch (IllegalArgumentException ex) {
            printUsageError(err, "Invalid class name: " + ex.getMessage());
            return EXIT_USAGE;
        }

        SpecNamingConvention namingConvention = parsed.namingConvention;
        File specRoot = new File(parsed.specRoot);
        DescribedType describedType = DescribedType.of(describedClass);
        SpecGenerationPlan plan;
        SpecGenerationPlan supportPlan;
        try {
            plan = SpecSkeletonGenerator.plan(describedType, specRoot, namingConvention);
            supportPlan = SpecSkeletonGenerator.supportPlan(describedType, specRoot, namingConvention);
        } catch (IllegalArgumentException ex) {
            printUsageError(err, "Naming error: " + messageOf(ex));
            return EXIT_USAGE;
        }
        if (plan.targetFile().exists()) {
            out.println("Specification " + plan.specQualifiedName() + " exists; no generation needed.");
            out.println("Spec file: " + plan.targetFile().getPath());
            try {
                if (!supportPlan.targetFile().exists()) {
                    File generatedSupport = SpecSupportFileGenerator.writeOrUpdate(supportPlan);
                    out.println("Generated specification support: " + generatedSupport.getPath());
                }
            } catch (IOException ex) {
                err.println("I/O error while generating specification support: " + messageOf(ex));
                err.println("Target path: " + supportPlan.targetFile().getPath());
                return EXIT_IO_ERROR;
            } catch (SecurityException ex) {
                err.println("I/O error while generating specification support: " + messageOf(ex));
                err.println("Target path: " + supportPlan.targetFile().getPath());
                return EXIT_IO_ERROR;
            }
            out.println("No production class was generated.");
            return EXIT_OK;
        }

        try {
            File generatedSupport = SpecSupportFileGenerator.writeOrUpdate(supportPlan);
            File generatedFile = SpecFileGenerator.write(plan);
            out.println("Generated specification support: " + generatedSupport.getPath());
            out.println("Generated specification: " + generatedFile.getPath());
            out.println("Specification class: " + plan.specQualifiedName());
            out.println("Described class: " + describedClass.qualifiedName());
            out.println("No production class was generated. Run `javaspec run` to continue the PHPSpec-style workflow.");
            return EXIT_OK;
        } catch (IOException ex) {
            err.println("I/O error while generating specification: " + messageOf(ex));
            err.println("Target path: " + plan.targetFile().getPath());
            return EXIT_IO_ERROR;
        } catch (SecurityException ex) {
            err.println("I/O error while generating specification: " + messageOf(ex));
            err.println("Target path: " + plan.targetFile().getPath());
            return EXIT_IO_ERROR;
        }
    }

    static int runSpecifications(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err) {
        File specRoot = new File(parsed.specRoot);
        File sourceRoot = new File(parsed.sourceRoot);
        BufferedReader input = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        ClasspathSelection classpathSelection = ClasspathResolver.select(
                parsed.classpathArguments,
                err
        );
        if (classpathSelection.exitCode() != EXIT_OK) {
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
        if (extensionActivationExitCode != EXIT_OK) {
            return extensionActivationExitCode;
        }
        int formatterExitCode = ExtensionOrchestrator.validateFormatter(
                parsed.effectiveFormatter,
                runFormatters,
                err
        );
        if (formatterExitCode != EXIT_OK) {
            printUsage(err);
            return formatterExitCode;
        }

        if (parsed.verbose) {
            printRunConfiguration(parsed, out, classpathSelection);
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
            err.println("I/O error while discovering specifications: " + messageOf(ex));
            err.println("Spec root: " + specRoot.getPath());
            return EXIT_IO_ERROR;
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
        int profileEnforcementExitCode = enforceProfileCompatibility(parsed.effectiveProfile, specs, err);
        if (profileEnforcementExitCode != EXIT_OK) {
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
                resolveConstructorPolicy(parsed)
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

            if (executionClasspathSelection.exitCode() != EXIT_OK) {
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
        if (bootstrapExitCode != EXIT_OK) {
            return bootstrapExitCode;
        }

        RunResult runResult = SpecRunner.run(specs, selectedClassLoader, parsed.stopOnFailure);
        printRunnerSummary(runResult, out, parsed.effectiveFormatter, runFormatters);
        printExecutionDiagnostics(runResult, out, executionClasspathSelection);
        int reportExitCode = ReportOrchestrator.writeRequested(
                runResult,
                parsed.reportPath,
                parsed.junitXmlPath,
                err
        );
        if (reportExitCode != EXIT_OK) {
            return reportExitCode;
        }
        return JavaspecExitCode.from(runResult);
    }

    private static int enforceProfileCompatibility(
            TargetProfile targetProfile,
            List<DiscoveredSpec> specs,
            PrintStream err
    ) {
        ProfileEnforcement enforcement = ProfileEnforcement.defaultEnforcement();
        List<ProfileEnforcementFinding> deniedFindings = new ArrayList<ProfileEnforcementFinding>();
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            DescribedType describedType = spec.describedType();
            addDeniedProfileFinding(
                    deniedFindings,
                    enforcement,
                    targetProfile,
                    spec.specQualifiedName() + " -> " + promptTarget(describedType),
                    describedType
            );
            List<DescribedType> relatedTypes = relatedTypesOf(describedType);
            for (int ri = 0; ri < relatedTypes.size(); ri++) {
                DescribedType relatedType = relatedTypes.get(ri);
                addDeniedProfileFinding(
                        deniedFindings,
                        enforcement,
                        targetProfile,
                        "related to " + spec.specQualifiedName() + " -> " + promptTarget(relatedType),
                        relatedType
                );
            }
        }
        if (deniedFindings.isEmpty()) {
            return EXIT_OK;
        }
        printProfileCompatibilityError(deniedFindings, err);
        return EXIT_USAGE;
    }

    private static void addDeniedProfileFinding(
            List<ProfileEnforcementFinding> deniedFindings,
            ProfileEnforcement enforcement,
            TargetProfile targetProfile,
            String sourceDescription,
            DescribedType describedType
    ) {
        ProfileEnforcementReport report = enforcement.enforce(targetProfile, describedType);
        if (report.isDenied()) {
            deniedFindings.add(ProfileEnforcementFinding.of(sourceDescription, report));
        }
    }

    private static void printProfileCompatibilityError(
            List<ProfileEnforcementFinding> deniedFindings,
            PrintStream err
    ) {
        ProfileEnforcementFinding firstFinding = deniedFindings.get(0);
        ProfileEnforcementReport firstReport = firstFinding.report();
        ProfileViolation firstViolation = firstReport.violations().get(0);
        err.println("Profile compatibility error: " + firstViolation.message());
        err.println("Selected profile: " + firstReport.targetProfile().key()
                + " (" + firstReport.targetProfile().displayLabel() + ")");
        err.println("Spec/type: " + firstFinding.sourceDescription());
        if (deniedFindings.size() == 1 && firstReport.violations().size() == 1) {
            return;
        }
        err.println("Violations:");
        for (int i = 0; i < deniedFindings.size(); i++) {
            ProfileEnforcementFinding finding = deniedFindings.get(i);
            List<ProfileViolation> violations = finding.report().violations();
            for (int vi = 0; vi < violations.size(); vi++) {
                err.println("  - " + finding.sourceDescription() + ": " + violations.get(vi).summaryLine());
            }
        }
    }


    private static String promptTarget(DescribedType describedType) {
        if (JavaTypeKind.CLASS.equals(describedType.kind())) {
            return describedType.qualifiedName();
        }
        return describedType.kind().displayName() + " " + describedType.qualifiedName();
    }

    private static List<DescribedType> relatedTypesOf(DescribedType owner) {
        List<DescribedType> relatedTypes = new ArrayList<DescribedType>();
        addExtendedRelatedTypes(relatedTypes, owner);
        addImplementedRelatedTypes(relatedTypes, owner);
        addPermittedRelatedTypes(relatedTypes, owner);
        return relatedTypes;
    }

    private static void addExtendedRelatedTypes(List<DescribedType> relatedTypes, DescribedType owner) {
        JavaTypeKind relatedKind = (JavaTypeKind.INTERFACE.equals(owner.kind()) || JavaTypeKind.SEALED_INTERFACE.equals(owner.kind()))
                ? JavaTypeKind.INTERFACE
                : JavaTypeKind.CLASS;
        for (int i = 0; i < owner.extendedTypeNames().size(); i++) {
            addRelatedType(relatedTypes, DescribedType.of(owner.extendedTypeNames().get(i), relatedKind));
        }
    }

    private static void addImplementedRelatedTypes(List<DescribedType> relatedTypes, DescribedType owner) {
        for (int i = 0; i < owner.implementedTypeNames().size(); i++) {
            addRelatedType(relatedTypes, DescribedType.of(owner.implementedTypeNames().get(i), JavaTypeKind.INTERFACE));
        }
    }

    private static void addPermittedRelatedTypes(List<DescribedType> relatedTypes, DescribedType owner) {
        if (JavaTypeKind.SEALED_INTERFACE.equals(owner.kind())) {
            return;
        }
        for (int i = 0; i < owner.permittedTypeNames().size(); i++) {
            String permittedTypeName = owner.permittedTypeNames().get(i);
            addRelatedType(relatedTypes, DescribedType.of(
                    permittedTypeName,
                    JavaTypeKind.FINAL_CLASS,
                    singleList(owner.qualifiedName()),
                    emptyStringList(),
                    emptyStringList()
            ));
        }
    }

    private static List<String> singleList(String value) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return values;
    }

    private static List<String> emptyStringList() {
        return new ArrayList<String>();
    }

    private static void addRelatedType(List<DescribedType> relatedTypes, DescribedType candidate) {
        for (int i = 0; i < relatedTypes.size(); i++) {
            if (relatedTypes.get(i).qualifiedName().equals(candidate.qualifiedName())) {
                return;
            }
        }
        relatedTypes.add(candidate);
    }

    private static void printRunnerSummary(
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

    private static void printExecutionDiagnostics(
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

    private static void printRunConfiguration(ParsedArguments parsed, PrintStream out, ClasspathSelection classpathSelection) {
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
        printUsageError(err, "Invalid formatter: " + selectedFormatterDisplay(parsed)
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


    private static void printUsageError(PrintStream err, String message) {
        err.println("Error: " + message);
        printUsage(err);
    }

    private static void printUsage(PrintStream stream) {
        stream.println("Usage:");
        stream.println("  javaspec describe <ClassName> [--config <file>] [--suite <name>] [--spec-dir <dir>]");
        stream.println("  javaspec desc <ClassName> [--config <file>] [--suite <name>] [--spec-root <dir>]");
        stream.println("  javaspec run [--config <file>] [--suite <name>] [--spec-dir <dir>] [--source-dir <dir>] [--classpath <path-list>] [--classpath-file <file>] [--compile] [--compile-output <dir>] [--generate] [--dry-run] [--stop-on-failure] [--formatter <progress|pretty>] [--profile <java8|java11|java17|java21|java25>] [--verbose] [--report <file>] [--junit-xml <file>] [--constructor-policy <delete|preserve|comment>] [--class <name>] [--example <name>]");
        stream.println();
        stream.println("Commands:");
        stream.println("  describe <ClassName>  Create a PHPSpec-style specification skeleton; never creates production code.");
        stream.println("  desc <ClassName>      Alias for describe.");
        stream.println("  run                   Discover specs and check whether their described production types exist.");
        stream.println();
        stream.println("Options:");
        stream.println("  --config <file>       Load javaspec configuration from file.");
        stream.println("  --suite <name>        Select a configured suite (default: configuration default suite).");
        stream.println("  --spec-dir <dir>      Spec root to inspect and write to (default: " + DEFAULT_SPEC_ROOT + ").");
        stream.println("  --spec-root <dir>     Alias for --spec-dir.");
        stream.println("  --source-dir <dir>    Source root used by run (default: " + DEFAULT_SOURCE_ROOT + ").");
        stream.println("  --source-root <dir>   Alias for --source-dir.");
        stream.println("  --classpath <paths>   With run, add explicit classpath entries separated by '" + File.pathSeparator + "'.");
        stream.println("  --classpath-file <file> With run, read UTF-8 classpath entries, one per non-comment line.");
        stream.println("  --compile             With run, compile source and spec trees before executable examples.");
        stream.println("  --compile-output <dir> With run, write compiled classes to <dir> (default: " + DEFAULT_COMPILE_OUTPUT + ").");
        stream.println("  --generate            With run, answer yes to missing production type generation prompts.");
        stream.println("  --dry-run             With run, report pending generation/update work without writing files or prompting.");
        stream.println("  --stop-on-failure     With run, stop after the first failed or broken executable example.");
        stream.println("  --formatter <value>   With run, choose example output formatter. Valid values: progress, pretty.");
        stream.println("  --profile <value>     With run, override target profile. Valid values: java8, java11, java17, java21, java25.");
        stream.println("  --verbose             With run, print effective run configuration before discovery.");
        stream.println("  --report <file>       With run, write a UTF-8 JSON runner report.");
        stream.println("  --report-file <file>  Alias for --report.");
        stream.println("  --junit-xml <file>    With run, write a UTF-8 JUnit XML-compatible runner report.");
        stream.println("  --junit-xml-file <file> Alias for --junit-xml.");
        stream.println("  --constructor-policy  Constructor handling policy. Valid values: delete, preserve, comment (default: comment).");
        stream.println("  --class <name>        With run, filter specs by described class name (exact match, repeatable).");
        stream.println("  --example <name>      With run, filter examples by method name, display name, or order index (repeatable).");
        stream.println("  --help, -h            Show this help.");
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }

    private static final class ProfileEnforcementFinding {
        private final String sourceDescription;
        private final ProfileEnforcementReport report;

        private ProfileEnforcementFinding(String sourceDescription, ProfileEnforcementReport report) {
            this.sourceDescription = sourceDescription;
            this.report = report;
        }

        static ProfileEnforcementFinding of(String sourceDescription, ProfileEnforcementReport report) {
            return new ProfileEnforcementFinding(sourceDescription, report);
        }

        String sourceDescription() {
            return sourceDescription;
        }

        ProfileEnforcementReport report() {
            return report;
        }
    }


}
