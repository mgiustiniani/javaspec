package org.javaspec.cli;

import org.javaspec.config.ConfigurationException;
import org.javaspec.config.JavaspecConfiguration;
import org.javaspec.config.JavaspecSuiteConfiguration;
import org.javaspec.discovery.SpecNamingConvention;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.javaspec.formatter.RunFormatterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrates configuration loading and application for javaspec CLI commands.
 */
final class ConfigurationOrchestrator {

    /**
     * Applies configuration from file or defaults to the parsed arguments.
     *
     * @param parsed the parsed arguments to enrich with configuration values
     * @param err    the error stream for diagnostics
     * @return {@code EXIT_OK} on success, or an error exit code
     */
    int applyConfiguration(ParsedArguments parsed, PrintStream err) {
        JavaspecConfiguration configuration;
        if (parsed.configPath == null) {
            configuration = JavaspecConfiguration.defaults();
        } else {
            File configFile = new File(parsed.configPath);
            try {
                configuration = JavaspecConfiguration.load(configFile);
            } catch (ConfigurationException ex) {
                err.println("Error: Invalid configuration: " + Main.messageOf(ex));
                return Main.EXIT_USAGE;
            } catch (IOException ex) {
                err.println("Error: I/O error while reading configuration: " + Main.messageOf(ex));
                err.println("Config path: " + configFile.getPath());
                return Main.EXIT_IO_ERROR;
            } catch (SecurityException ex) {
                err.println("Error: I/O error while reading configuration: " + Main.messageOf(ex));
                err.println("Config path: " + configFile.getPath());
                return Main.EXIT_IO_ERROR;
            }
        }

        String selectedSuiteName = parsed.suiteName == null
                ? configuration.defaultSuiteName()
                : parsed.suiteName;
        JavaspecSuiteConfiguration selectedSuite;
        try {
            selectedSuite = configuration.suite(selectedSuiteName);
        } catch (ConfigurationException ex) {
            err.println("Error: Invalid configuration: " + Main.messageOf(ex));
            return Main.EXIT_USAGE;
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
            err.println("Error: Invalid naming metadata: " + Main.messageOf(ex));
            return Main.EXIT_USAGE;
        } catch (RuntimeException ex) {
            err.println("Error: Invalid naming metadata: " + Main.messageOf(ex));
            return Main.EXIT_USAGE;
        }
        return Main.EXIT_OK;
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
}
