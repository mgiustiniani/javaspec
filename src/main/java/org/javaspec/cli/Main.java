package org.javaspec.cli;

import org.javaspec.config.ConfigurationException;
import org.javaspec.config.JavaspecConfiguration;
import org.javaspec.config.JavaspecSuiteConfiguration;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecDiscovery;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.discovery.SpecNamingConvention;
import org.javaspec.discovery.TypeCheckResult;
import org.javaspec.discovery.TypeExistenceChecker;
import org.javaspec.generation.SpecFileGenerator;
import org.javaspec.generation.SpecGenerationPlan;
import org.javaspec.generation.SpecSkeletonGenerator;
import org.javaspec.generation.SpecSupportFileGenerator;
import org.javaspec.generation.ClassConstructorUpdater;
import org.javaspec.generation.ClassMethodUpdater;
import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.generation.TypeFileGenerator;
import org.javaspec.generation.TypeGenerationPlan;
import org.javaspec.generation.TypeSkeletonGenerator;
import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class Main {
    private static final int EXIT_OK = 0;
    private static final int EXIT_MISSING_NOT_GENERATED = 1;
    private static final int EXIT_USAGE = 64;
    private static final int EXIT_IO_ERROR = 70;

    private static final String DEFAULT_SOURCE_ROOT = "src/main/java";
    private static final String DEFAULT_SPEC_ROOT = "src/test/java";

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

        ParsedArguments parsed = parse(args);
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

        if ("run".equals(parsed.command)) {
            return runSpecifications(parsed, in, out, err);
        }
        return describeClass(parsed, out, err);
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
        if (!parsed.specRootSpecified) {
            parsed.specRoot = selectedSuite.specDirectory();
        }
        if (!parsed.sourceRootSpecified) {
            parsed.sourceRoot = selectedSuite.sourceDirectory();
        }
        parsed.effectiveConstructorPolicy = parsed.constructorPolicyOverride == null
                ? configuration.constructorPolicy()
                : parsed.constructorPolicyOverride;
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

    private static ConstructorPolicy resolveConstructorPolicy(ParsedArguments parsed) {
        if (parsed.effectiveConstructorPolicy != null) {
            return parsed.effectiveConstructorPolicy;
        }
        return ConstructorPolicy.defaultPolicy();
    }

    private static int describeClass(ParsedArguments parsed, PrintStream out, PrintStream err) {
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

    private static int runSpecifications(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err) {
        File specRoot = new File(parsed.specRoot);
        File sourceRoot = new File(parsed.sourceRoot);
        BufferedReader input = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

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
            return EXIT_OK;
        }

        out.println("Found " + specs.size() + " specification(s) in " + specRoot.getPath() + ".");
        boolean missingWithoutGeneration = false;
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            DescribedType describedType = spec.describedType();
            try {
                if (!ensureRelatedSpecs(describedType, specs, specRoot, sourceRoot, input, out, parsed.generate, parsed.namingConvention)) {
                    missingWithoutGeneration = true;
                }
            } catch (IOException ex) {
                err.println("I/O error while generating related specification: " + messageOf(ex));
                return EXIT_IO_ERROR;
            } catch (SecurityException ex) {
                err.println("I/O error while generating related specification: " + messageOf(ex));
                return EXIT_IO_ERROR;
            }

            if (parsed.generate && describedType.hasMethods()) {
                SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(describedType, specRoot, parsed.namingConvention);
                try {
                    File supportFile = SpecSupportFileGenerator.writeOrUpdate(supportPlan);
                    out.println("Updated specification support: " + supportFile.getPath());
                } catch (IOException ex) {
                    err.println("I/O error while updating specification support: " + messageOf(ex));
                    err.println("Target path: " + supportPlan.targetFile().getPath());
                    return EXIT_IO_ERROR;
                } catch (SecurityException ex) {
                    err.println("I/O error while updating specification support: " + messageOf(ex));
                    err.println("Target path: " + supportPlan.targetFile().getPath());
                    return EXIT_IO_ERROR;
                }
            }

            TypeCheckResult checkResult;
            try {
                checkResult = TypeExistenceChecker.check(describedType, sourceRoot, effectiveClassLoader());
            } catch (SecurityException ex) {
                err.println("I/O error while checking type existence: " + messageOf(ex));
                err.println("Described type: " + describedType.qualifiedName());
                return EXIT_IO_ERROR;
            }

            if (checkResult.isPresent()) {
                out.println(presentMessage(spec, checkResult));
                if (checkResult.sourceFilePresent()) {
                    out.println("Source file: " + checkResult.sourceFile().getPath());
                }
                if (checkResult.classpathPresent()) {
                    out.println("Classpath: present");
                    if (checkResult.classpathKind() != null && !checkResult.classpathKindMatches()) {
                        out.println("Classpath type kind: " + checkResult.classpathKind().displayName()
                                + " (expected " + describedType.kind().displayName() + ")");
                    }
                }
                // If the spec declares constructors, update the existing class.
                if (describedType.hasConstructors() && checkResult.sourceFilePresent()) {
                    ConstructorPolicy policy = resolveConstructorPolicy(parsed);
                    File sourceFile = checkResult.sourceFile();
                    try {
                        ClassConstructorUpdater.updateFile(sourceFile, describedType, policy);
                        out.println("Updated constructors in " + sourceFile.getPath()
                                + " (policy: " + policy.name().toLowerCase().replace('_', '-') + ")");
                    } catch (IOException ex) {
                        err.println("I/O error while updating constructors: " + messageOf(ex));
                        err.println("Target path: " + sourceFile.getPath());
                        return EXIT_IO_ERROR;
                    }
                }
                if (describedType.hasMethods() && checkResult.sourceFilePresent()) {
                    File sourceFile = checkResult.sourceFile();
                    try {
                        String existingSource = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
                        String updatedSource = ClassMethodUpdater.updateSource(existingSource, describedType);
                        if (!existingSource.equals(updatedSource)) {
                            boolean accepted = parsed.generate || askToUpdateMethods(input, out, sourceFile, describedType);
                            if (!accepted) {
                                missingWithoutGeneration = true;
                                continue;
                            }
                            Files.write(sourceFile.toPath(), updatedSource.getBytes(StandardCharsets.UTF_8));
                            out.println("Updated methods in " + sourceFile.getPath());
                        }
                    } catch (IOException ex) {
                        err.println("I/O error while updating methods: " + messageOf(ex));
                        err.println("Target path: " + sourceFile.getPath());
                        return EXIT_IO_ERROR;
                    }
                }
                continue;
            }

            TypeGenerationPlan plan = TypeSkeletonGenerator.plan(describedType, sourceRoot);
            out.println(spec.specQualifiedName() + " describes missing " + describedType.kind().displayName() + " " + describedType.qualifiedName() + ".");
            out.println("Spec file: " + spec.specFile().getPath());
            out.println("Target path: " + plan.targetFile().getPath());

            if (!parsed.generate) {
                boolean accepted;
                try {
                    accepted = askToGenerate(input, out, plan);
                } catch (IOException ex) {
                    err.println("I/O error while reading generation confirmation: " + messageOf(ex));
                    return EXIT_IO_ERROR;
                }
                if (!accepted) {
                    missingWithoutGeneration = true;
                    continue;
                }
            }

            try {
                File generatedFile = TypeFileGenerator.write(plan);
                out.println("Generated " + plan.describedType().kind().displayName() + " skeleton: " + generatedFile.getPath());
            } catch (IOException ex) {
                err.println("I/O error while generating " + plan.describedType().kind().displayName() + " skeleton: " + messageOf(ex));
                err.println("Target path: " + plan.targetFile().getPath());
                return EXIT_IO_ERROR;
            } catch (SecurityException ex) {
                err.println("I/O error while generating " + plan.describedType().kind().displayName() + " skeleton: " + messageOf(ex));
                err.println("Target path: " + plan.targetFile().getPath());
                return EXIT_IO_ERROR;
            }
        }

        if (missingWithoutGeneration) {
            out.println("No production files were written.");
            return EXIT_MISSING_NOT_GENERATED;
        }
        return EXIT_OK;
    }

    private static boolean ensureRelatedSpecs(
            DescribedType owner,
            List<DiscoveredSpec> specs,
            File specRoot,
            File sourceRoot,
            BufferedReader input,
            PrintStream out,
            boolean generate,
            SpecNamingConvention namingConvention
    ) throws IOException {
        boolean allAccepted = true;
        List<DescribedType> relatedTypes = relatedTypesOf(owner);
        for (int i = 0; i < relatedTypes.size(); i++) {
            DescribedType relatedType = relatedTypes.get(i);
            TypeCheckResult checkResult = TypeExistenceChecker.check(relatedType, sourceRoot, effectiveClassLoader());
            if (checkResult.isPresent()) {
                continue;
            }

            SpecGenerationPlan specPlan = SpecSkeletonGenerator.plan(relatedType, specRoot, namingConvention);
            if (specPlan.targetFile().exists() || isSpecKnown(specs, specPlan.specQualifiedName())) {
                continue;
            }

            out.println("Related " + relatedType.kind().displayName() + " " + relatedType.qualifiedName() + " is missing.");
            out.println("Spec target path: " + specPlan.targetFile().getPath());
            boolean accepted = generate || askToGenerateSpec(input, out, specPlan);
            if (!accepted) {
                allAccepted = false;
                continue;
            }

            SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(relatedType, specRoot, namingConvention);
            File generatedSupport = SpecSupportFileGenerator.writeOrUpdate(supportPlan);
            File generatedSpec = SpecFileGenerator.write(specPlan);
            out.println("Generated related specification support: " + generatedSupport.getPath());
            out.println("Generated related specification: " + generatedSpec.getPath());
            specs.add(DiscoveredSpec.of(generatedSpec, specPlan.specQualifiedName(), relatedType));
        }
        return allAccepted;
    }

    private static boolean isSpecKnown(List<DiscoveredSpec> specs, String specQualifiedName) {
        for (int i = 0; i < specs.size(); i++) {
            if (specQualifiedName.equals(specs.get(i).specQualifiedName())) {
                return true;
            }
        }
        return false;
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

    private static String presentMessage(DiscoveredSpec spec, TypeCheckResult checkResult) {
        DescribedType describedType = spec.describedType();
        if (JavaTypeKind.CLASS.equals(describedType.kind())) {
            return spec.specQualifiedName() + " describes " + describedType.qualifiedName() + "; class exists.";
        }
        return spec.specQualifiedName() + " describes " + describedType.kind().displayName() + " "
                + describedType.qualifiedName() + "; type exists.";
    }

    private static String promptTarget(DescribedType describedType) {
        if (JavaTypeKind.CLASS.equals(describedType.kind())) {
            return describedType.qualifiedName();
        }
        return describedType.kind().displayName() + " " + describedType.qualifiedName();
    }

    private static boolean askToGenerateSpec(BufferedReader input, PrintStream out, SpecGenerationPlan plan) throws IOException {
        while (true) {
            out.println("Do you want me to create specification " + plan.specQualifiedName()
                    + " for " + promptTarget(plan.describedType()) + "? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }

    private static boolean askToGenerate(BufferedReader input, PrintStream out, TypeGenerationPlan plan) throws IOException {
        while (true) {
            out.println("Do you want me to create " + promptTarget(plan.describedType()) + " for you? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }

    private static boolean askToUpdateMethods(
            BufferedReader input,
            PrintStream out,
            File sourceFile,
            DescribedType describedType
    ) throws IOException {
        while (true) {
            out.println("Do you want me to add missing method skeletons to "
                    + promptTarget(describedType) + " in " + sourceFile.getPath() + "? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }

    private static ParsedArguments parse(String[] args) {
        ParsedArguments parsed = new ParsedArguments();
        parsed.sourceRoot = DEFAULT_SOURCE_ROOT;
        parsed.specRoot = DEFAULT_SPEC_ROOT;

        List<String> operands = new ArrayList<String>();
        int index = 0;
        while (index < args.length) {
            String arg = args[index];
            if ("--help".equals(arg) || "-h".equals(arg)) {
                parsed.helpRequested = true;
                return parsed;
            } else if ("--generate".equals(arg)) {
                parsed.generate = true;
                index++;
            } else if ("--config".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.configPath = args[index + 1];
                if (parsed.configPath.length() == 0) {
                    parsed.errorMessage = "Configuration file must not be empty.";
                    return parsed;
                }
                index += 2;
            } else if ("--suite".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.suiteName = args[index + 1].trim();
                if (parsed.suiteName.length() == 0) {
                    parsed.errorMessage = "Suite name must not be empty.";
                    return parsed;
                }
                index += 2;
            } else if ("--source-dir".equals(arg) || "--source-root".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.sourceRoot = args[index + 1];
                parsed.sourceRootSpecified = true;
                if (parsed.sourceRoot.length() == 0) {
                    parsed.errorMessage = "Source directory must not be empty.";
                    return parsed;
                }
                index += 2;
            } else if ("--spec-dir".equals(arg) || "--spec-root".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.specRoot = args[index + 1];
                parsed.specRootSpecified = true;
                if (parsed.specRoot.length() == 0) {
                    parsed.errorMessage = "Spec directory must not be empty.";
                    return parsed;
                }
                index += 2;
            } else if ("--class".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                if (parsed.classFilters == null) {
                    parsed.classFilters = new ArrayList<String>();
                }
                String filterValue = args[index + 1].trim();
                if (filterValue.length() == 0) {
                    parsed.errorMessage = "Class filter must not be empty.";
                    return parsed;
                }
                parsed.classFilters.add(filterValue);
                index += 2;
            } else if ("--example".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                if (parsed.exampleFilters == null) {
                    parsed.exampleFilters = new ArrayList<String>();
                }
                String filterValue = args[index + 1].trim();
                if (filterValue.length() == 0) {
                    parsed.errorMessage = "Example filter must not be empty.";
                    return parsed;
                }
                parsed.exampleFilters.add(filterValue);
                index += 2;
            } else if ("--constructor-policy".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.constructorPolicy = args[index + 1];
                if ("delete".equals(parsed.constructorPolicy)) {
                    parsed.constructorPolicyOverride = ConstructorPolicy.DELETE;
                } else if ("preserve".equals(parsed.constructorPolicy)) {
                    parsed.constructorPolicyOverride = ConstructorPolicy.PRESERVE;
                } else if ("comment".equals(parsed.constructorPolicy)) {
                    parsed.constructorPolicyOverride = ConstructorPolicy.COMMENT;
                } else {
                    parsed.errorMessage = "Invalid constructor policy: " + parsed.constructorPolicy
                            + ". Valid values: delete, preserve, comment.";
                    return parsed;
                }
                index += 2;
            } else if (arg.startsWith("-")) {
                parsed.errorMessage = "Unknown option: " + arg;
                return parsed;
            } else {
                operands.add(arg);
                index++;
            }
        }

        if (operands.size() == 0) {
            parsed.errorMessage = "Missing command.";
            return parsed;
        }

        parsed.command = operands.get(0);
        if ("describe".equals(parsed.command) || "desc".equals(parsed.command)) {
            if (operands.size() == 1) {
                parsed.errorMessage = "Missing class name.";
                return parsed;
            }
            if (operands.size() > 2) {
                parsed.errorMessage = "Unexpected argument: " + operands.get(2);
                return parsed;
            }
            if (parsed.generate) {
                parsed.errorMessage = "The --generate option belongs to run; describe creates only a specification skeleton.";
                return parsed;
            }
            if (parsed.sourceRootSpecified) {
                parsed.errorMessage = "The source directory is used by run; describe writes only to the spec directory.";
                return parsed;
            }
            if (parsed.classFilters != null) {
                parsed.errorMessage = "The --class option belongs to run; describe does not support class filters.";
                return parsed;
            }
            if (parsed.exampleFilters != null) {
                parsed.errorMessage = "The --example option belongs to run; describe does not support example filters.";
                return parsed;
            }
            parsed.className = operands.get(1);
            return parsed;
        }

        if ("run".equals(parsed.command)) {
            if (operands.size() > 1) {
                parsed.errorMessage = "Unexpected argument: " + operands.get(1);
                return parsed;
            }
            return parsed;
        }

        parsed.errorMessage = "Unknown command: " + operands.get(0);
        return parsed;
    }

    private static ClassLoader effectiveClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }
        return Main.class.getClassLoader();
    }

    private static void printUsageError(PrintStream err, String message) {
        err.println("Error: " + message);
        printUsage(err);
    }

    private static void printUsage(PrintStream stream) {
        stream.println("Usage:");
        stream.println("  javaspec describe <ClassName> [--config <file>] [--suite <name>] [--spec-dir <dir>]");
        stream.println("  javaspec desc <ClassName> [--config <file>] [--suite <name>] [--spec-root <dir>]");
        stream.println("  javaspec run [--config <file>] [--suite <name>] [--spec-dir <dir>] [--source-dir <dir>] [--generate] [--constructor-policy <delete|preserve|comment>] [--class <name>] [--example <name>]");
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
        stream.println("  --generate            With run, answer yes to missing production type generation prompts.");
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

    private static final class ParsedArguments {
        private String command;
        private String className;
        private String sourceRoot;
        private String specRoot;
        private boolean sourceRootSpecified;
        private boolean specRootSpecified;
        private boolean generate;
        private boolean helpRequested;
        private String errorMessage;
        private String configPath;
        private String suiteName;
        private String constructorPolicy;
        private ConstructorPolicy constructorPolicyOverride;
        private ConstructorPolicy effectiveConstructorPolicy;
        private JavaspecConfiguration configuration;
        private JavaspecSuiteConfiguration selectedSuite;
        private SpecNamingConvention namingConvention;
        private List<String> classFilters;
        private List<String> exampleFilters;
    }
}
