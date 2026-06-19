package org.javaspec.cli;

import org.javaspec.cli.run.ClasspathArgument;
import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.profile.TargetProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses command-line arguments for the javaspec CLI.
 * <p>Extracted from {@link org.javaspec.cli.Main Main} to isolate argument
 * parsing and enable unit testing.</p>
 */
public final class CliArgumentParser {
    private static final String DEFAULT_SOURCE_ROOT = "src/main/java";
    private static final String DEFAULT_SPEC_ROOT = "src/test/java";
    private static final String DEFAULT_COMPILE_OUTPUT = "target/javaspec-classes";

    private CliArgumentParser() {
    }

    /**
     * Parses the given command-line arguments into a {@link ParsedArguments}.
     *
     * @param args the raw command-line arguments
     * @return a parsed arguments object
     */
    public static ParsedArguments parse(String[] args) {
        ParsedArguments parsed = new ParsedArguments();
        parsed.sourceRoot = DEFAULT_SOURCE_ROOT;
        parsed.specRoot = DEFAULT_SPEC_ROOT;
        parsed.compileOutputPath = DEFAULT_COMPILE_OUTPUT;

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
            } else if ("--dry-run".equals(arg)) {
                parsed.dryRun = true;
                index++;
            } else if ("--stop-on-failure".equals(arg)) {
                parsed.stopOnFailure = true;
                index++;
            } else if ("--auto-check-predictions".equals(arg)) {
                parsed.autoCheckPredictions = true;
                index++;
            } else if ("--no-auto-check-predictions".equals(arg)) {
                parsed.autoCheckPredictions = false;
                index++;
            } else if ("--verbose".equals(arg)) {
                parsed.verbose = true;
                index++;
            } else if ("--report".equals(arg) || "--report-file".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.reportPath = args[index + 1];
                parsed.reportOption = arg;
                parsed.reportSpecified = true;
                if (parsed.reportPath.length() == 0) {
                    parsed.errorMessage = "Report file must not be empty.";
                    return parsed;
                }
                index += 2;
            } else if ("--junit-xml".equals(arg) || "--junit-xml-file".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.junitXmlPath = args[index + 1];
                parsed.junitXmlOption = arg;
                parsed.junitXmlSpecified = true;
                if (parsed.junitXmlPath.length() == 0) {
                    parsed.errorMessage = "JUnit XML report file must not be empty.";
                    return parsed;
                }
                index += 2;
            } else if ("--formatter".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.formatter = args[index + 1].trim();
                parsed.formatterSpecified = true;
                if (parsed.formatter.length() == 0) {
                    parsed.errorMessage = "Formatter must not be empty.";
                    return parsed;
                }
                index += 2;
            } else if ("--profile".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.profile = args[index + 1].trim();
                parsed.profileSpecified = true;
                if (parsed.profile.length() == 0) {
                    parsed.errorMessage = "Profile must not be empty.";
                    return parsed;
                }
                index += 2;
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
            } else if ("--classpath".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                String classpath = args[index + 1];
                if (classpath.length() == 0) {
                    parsed.errorMessage = "Classpath must not be empty.";
                    return parsed;
                }
                parsed.addClasspathArgument(ClasspathArgument.pathList(arg, classpath));
                index += 2;
            } else if ("--classpath-file".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                String classpathFile = args[index + 1];
                if (classpathFile.length() == 0) {
                    parsed.errorMessage = "Classpath file must not be empty.";
                    return parsed;
                }
                parsed.addClasspathArgument(ClasspathArgument.file(arg, classpathFile));
                index += 2;
            } else if ("--compile".equals(arg)) {
                parsed.compile = true;
                index++;
            } else if ("--compile-output".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.compileOutputPath = args[index + 1];
                parsed.compileOutputSpecified = true;
                parsed.compile = true;
                if (parsed.compileOutputPath.length() == 0) {
                    parsed.errorMessage = "Compile output directory must not be empty.";
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
            } else if ("--output".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.prophesizeOutputDir = args[index + 1];
                if (parsed.prophesizeOutputDir.length() == 0) {
                    parsed.errorMessage = "Output directory must not be empty.";
                    return parsed;
                }
                index += 2;
            } else if ("--package".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.prophesizePackageName = args[index + 1].trim();
                if (parsed.prophesizePackageName.length() == 0) {
                    parsed.errorMessage = "Package name must not be empty.";
                    return parsed;
                }
                index += 2;
            } else if ("--overwrite".equals(arg)) {
                parsed.prophesizeOverwrite = true;
                index++;
            } else if ("--release".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                String release = args[index + 1].trim();
                if (release.length() == 0) {
                    parsed.errorMessage = "Release version must not be empty.";
                    return parsed;
                }
                if (!isValidReleaseVersion(release)) {
                    parsed.errorMessage = "Invalid release version: " + release
                            + ". Expected a positive integer (e.g. 8, 11, 17, 21).";
                    return parsed;
                }
                parsed.releaseVersion = release;
                index += 2;
            } else if ("--resolve-pom".equals(arg)) {
                if (index + 1 >= args.length) {
                    parsed.errorMessage = "Missing value for " + arg + ".";
                    return parsed;
                }
                parsed.resolvePomPath = args[index + 1];
                parsed.resolvePomSpecified = true;
                if (parsed.resolvePomPath.length() == 0) {
                    parsed.errorMessage = "POM file must not be empty.";
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
            if (parsed.dryRun) {
                parsed.errorMessage = "The --dry-run option belongs to run; describe creates only a specification skeleton.";
                return parsed;
            }
            if (parsed.stopOnFailure) {
                parsed.errorMessage = "The --stop-on-failure option belongs to run; describe does not execute examples.";
                return parsed;
            }
            if (parsed.formatterSpecified) {
                parsed.errorMessage = "The --formatter option belongs to run; describe does not execute examples.";
                return parsed;
            }
            if (parsed.profileSpecified) {
                parsed.errorMessage = "The --profile option belongs to run; describe does not execute examples.";
                return parsed;
            }
            if (parsed.verbose) {
                parsed.errorMessage = "The --verbose option belongs to run; describe does not discover specifications.";
                return parsed;
            }
            if (parsed.reportSpecified) {
                parsed.errorMessage = "The " + parsed.reportOption + " option belongs to run; describe does not execute examples.";
                return parsed;
            }
            if (parsed.junitXmlSpecified) {
                parsed.errorMessage = "The " + parsed.junitXmlOption + " option belongs to run; describe does not execute examples.";
                return parsed;
            }
            if (parsed.hasExplicitClasspath()) {
                parsed.errorMessage = "The " + parsed.firstClasspathOption() + " option belongs to run; describe does not execute examples.";
                return parsed;
            }
            if (parsed.compileOutputSpecified) {
                parsed.errorMessage = "The --compile-output option belongs to run; describe does not execute examples.";
                return parsed;
            }
            if (parsed.compile) {
                parsed.errorMessage = "The --compile option belongs to run; describe does not execute examples.";
                return parsed;
            }
            if (parsed.resolvePomSpecified) {
                parsed.errorMessage = "The --resolve-pom option belongs to run; describe does not execute examples.";
                return parsed;
            }
            if (parsed.releaseVersion != null) {
                parsed.errorMessage = "The --release option belongs to run; describe does not execute examples.";
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

        if ("list-extensions".equals(parsed.command)) {
            if (operands.size() > 1) {
                parsed.errorMessage = "Unexpected argument: " + operands.get(1);
                return parsed;
            }
            return parsed;
        }

        if ("run".equals(parsed.command)) {
            if (operands.size() > 1) {
                parsed.errorMessage = "Unexpected argument: " + operands.get(1);
                return parsed;
            }
            if (parsed.formatterSpecified) {
                parsed.formatterOverride = normalizeFormatter(parsed.formatter);
                if (parsed.formatterOverride == null) {
                    parsed.errorMessage = "Formatter must not be empty.";
                    return parsed;
                }
            }
            if (parsed.profileSpecified) {
                try {
                    parsed.profileOverride = TargetProfile.parse(parsed.profile);
                } catch (IllegalArgumentException ex) {
                    parsed.errorMessage = "Invalid profile: " + parsed.profile
                            + ". Valid profiles: " + validProfileKeys() + ".";
                    return parsed;
                }
            }
            return parsed;
        }

        if ("prophesize".equals(parsed.command)) {
            if (operands.size() == 1) {
                parsed.errorMessage = "Missing class name.";
                return parsed;
            }
            if (operands.size() > 2) {
                parsed.errorMessage = "Unexpected argument: " + operands.get(2);
                return parsed;
            }
            if (parsed.generate) {
                parsed.errorMessage = "The --generate option belongs to run.";
                return parsed;
            }
            if (parsed.stopOnFailure) {
                parsed.errorMessage = "The --stop-on-failure option belongs to run.";
                return parsed;
            }
            if (parsed.formatterSpecified) {
                parsed.errorMessage = "The --formatter option belongs to run.";
                return parsed;
            }
            if (parsed.profileSpecified) {
                parsed.errorMessage = "The --profile option belongs to run.";
                return parsed;
            }
            if (parsed.reportSpecified) {
                parsed.errorMessage = "The " + parsed.reportOption + " option belongs to run.";
                return parsed;
            }
            if (parsed.junitXmlSpecified) {
                parsed.errorMessage = "The " + parsed.junitXmlOption + " option belongs to run.";
                return parsed;
            }
            if (parsed.hasExplicitClasspath()) {
                parsed.errorMessage = "The " + parsed.firstClasspathOption() + " option belongs to run.";
                return parsed;
            }
            if (parsed.compileOutputSpecified) {
                parsed.errorMessage = "The --compile-output option belongs to run.";
                return parsed;
            }
            if (parsed.compile) {
                parsed.errorMessage = "The --compile option belongs to run.";
                return parsed;
            }
            if (parsed.resolvePomSpecified) {
                parsed.errorMessage = "The --resolve-pom option belongs to run.";
                return parsed;
            }
            if (parsed.releaseVersion != null) {
                parsed.errorMessage = "The --release option belongs to run.";
                return parsed;
            }
            if (parsed.verbose) {
                parsed.errorMessage = "The --verbose option belongs to run.";
                return parsed;
            }
            if (parsed.sourceRootSpecified) {
                parsed.errorMessage = "The --source-root option belongs to describe/run.";
                return parsed;
            }
            if (parsed.specRootSpecified) {
                parsed.errorMessage = "The --spec-root option belongs to describe/run.";
                return parsed;
            }
            parsed.className = operands.get(1);
            return parsed;
        }

        if ("list-extensions".equals(operands.get(0))) {
            // Already handled above; fall through should not happen.
            return parsed;
        }
        parsed.errorMessage = "Unknown command: " + operands.get(0);
        return parsed;
    }

    private static boolean isValidReleaseVersion(String value) {
        try {
            int v = Integer.parseInt(value);
            return v > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String normalizeFormatter(String formatter) {
        return org.javaspec.formatter.RunFormatterRegistry.normalizeName(formatter);
    }

    private static String validProfileKeys() {
        StringBuilder builder = new StringBuilder();
        List<TargetProfile> profiles = TargetProfile.orderedProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(profiles.get(i).key());
        }
        return builder.toString();
    }
}
