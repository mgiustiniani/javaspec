package io.github.jvmspec.cli;

import io.github.jvmspec.cli.run.ClasspathArgument;
import io.github.jvmspec.config.JavaspecConfiguration;
import io.github.jvmspec.config.JavaspecSuiteConfiguration;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.profile.TargetProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed command-line arguments for the javaspec CLI.
 * <p>Extracted from {@link Main Main} to isolate argument parsing and
 * enable unit testing.</p>
 */
public final class ParsedArguments {
    String command;
    String className;
    String sourceRoot;
    String specRoot;
    boolean sourceRootSpecified;
    boolean specRootSpecified;
    boolean generate;
    boolean dryRun;
    boolean stopOnFailure;
    boolean autoCheckPredictions;
    boolean verbose;
    boolean compile;
    String compileOutputPath;
    boolean compileOutputSpecified;
    boolean helpRequested;
    String errorMessage;
    String configPath;
    String suiteName;
    String reportPath;
    String reportOption;
    boolean reportSpecified;
    String junitXmlPath;
    String junitXmlOption;
    boolean junitXmlSpecified;
    List<ClasspathArgument> classpathArguments;
    String formatter;
    boolean formatterSpecified;
    String formatterOverride;
    String effectiveFormatter;
    String profile;
    boolean profileSpecified;
    TargetProfile profileOverride;
    TargetProfile effectiveProfile;
    String constructorPolicy;
    ConstructorPolicy constructorPolicyOverride;
    ConstructorPolicy effectiveConstructorPolicy;
    JavaspecConfiguration configuration;
    JavaspecSuiteConfiguration selectedSuite;
    List<String> effectiveBootstrapHooks;
    boolean effectiveBootstrapDiscovery;
    List<String> effectiveExtensions;
    SpecNamingConvention namingConvention;
    List<String> classFilters;
    List<String> exampleFilters;
    String prophesizeOutputDir;
    String prophesizePackageName;
    boolean prophesizeOverwrite;
    String resolvePomPath;
    boolean resolvePomSpecified;
    String releaseVersion;

    public ParsedArguments() {
    }

    public void addClasspathArgument(ClasspathArgument argument) {
        if (classpathArguments == null) {
            classpathArguments = new ArrayList<ClasspathArgument>();
        }
        classpathArguments.add(argument);
    }

    public boolean hasExplicitClasspath() {
        return classpathArguments != null && !classpathArguments.isEmpty();
    }

    public String firstClasspathOption() {
        if (!hasExplicitClasspath()) {
            return "--classpath";
        }
        return classpathArguments.get(0).optionName();
    }
}
