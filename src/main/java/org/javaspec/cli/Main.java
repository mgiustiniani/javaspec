package org.javaspec.cli;

import org.javaspec.compatibility.ProfileEnforcement;
import org.javaspec.compatibility.ProfileEnforcementReport;
import org.javaspec.compatibility.ProfileViolation;
import org.javaspec.diagnostics.RunDiagnostics;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.model.DescribedType;
import org.javaspec.formatter.RunFormatter;
import org.javaspec.formatter.RunFormatterRegistry;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.cli.run.ClasspathSelection;
import org.javaspec.cli.CliArgumentParser;
import org.javaspec.cli.run.ExtensionOrchestrator;
import org.javaspec.cli.run.RunOrchestratorResult;
import org.javaspec.profile.TargetProfile;
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

    static int enforceProfileCompatibility(
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
