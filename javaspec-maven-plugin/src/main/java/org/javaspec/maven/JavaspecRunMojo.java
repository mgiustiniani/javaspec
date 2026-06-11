package org.javaspec.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.javaspec.bootstrap.BootstrapException;
import org.javaspec.compilation.SourceCompilationException;
import org.javaspec.compilation.SourceCompilationResult;
import org.javaspec.config.ConfigurationException;
import org.javaspec.config.JavaspecConfiguration;
import org.javaspec.config.JavaspecSuiteConfiguration;
import org.javaspec.diagnostics.RunDiagnostics;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.discovery.SpecNamingConvention;
import org.javaspec.extension.ExtensionLoadingException;
import org.javaspec.extension.JavaspecExtensionLoader;
import org.javaspec.formatter.RunFormatter;
import org.javaspec.formatter.RunFormatterRegistry;
import org.javaspec.invocation.JavaspecInvocation;
import org.javaspec.invocation.JavaspecInvocationResult;
import org.javaspec.invocation.JavaspecLauncher;
import org.javaspec.reporting.JUnitXmlReportWriter;
import org.javaspec.reporting.RunReportWriter;
import org.javaspec.runner.ExampleResult;
import org.javaspec.runner.RunResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maven execution adapter for the canonical javaspec launcher.
 */
@Mojo(
        name = "run",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.TEST,
        requiresProject = true,
        threadSafe = true
)
public final class JavaspecRunMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.testClasspathElements}", readonly = true, required = true)
    private List<String> testClasspathElements;

    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(property = "javaspec.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "javaspec.failOnFailure", defaultValue = "true")
    private boolean failOnFailure;

    @Parameter(property = "javaspec.configFile")
    private File configFile;

    @Parameter(property = "javaspec.suite")
    private String suite;

    @Parameter(property = "javaspec.specDir")
    private File specDir;

    @Parameter(property = "javaspec.specRoot")
    private File specRoot;

    @Parameter
    private List<String> classFilters;

    @Parameter(property = "javaspec.classFilters")
    private String classFiltersProperty;

    @Parameter(property = "javaspec.classFilter")
    private String classFilter;

    @Parameter(property = "javaspec.class")
    private String classNameFilter;

    @Parameter
    private List<String> exampleFilters;

    @Parameter(property = "javaspec.exampleFilters")
    private String exampleFiltersProperty;

    @Parameter(property = "javaspec.exampleFilter")
    private String exampleFilter;

    @Parameter(property = "javaspec.example")
    private String exampleNameFilter;

    @Parameter(property = "javaspec.stopOnFailure", defaultValue = "false")
    private boolean stopOnFailure;

    @Parameter(property = "javaspec.reportFile")
    private File reportFile;

    @Parameter(property = "javaspec.jsonReportFile")
    private File jsonReportFile;

    @Parameter(property = "javaspec.junitXmlReportFile")
    private File junitXmlReportFile;

    @Parameter(property = "javaspec.junitXmlFile")
    private File junitXmlFile;

    @Parameter(property = "javaspec.formatter")
    private String formatter;

    @Parameter(property = "javaspec.extensions")
    private String extensionsProperty;

    @Parameter(property = "javaspec.bootstrapDiscovery")
    private String bootstrapDiscovery;

    @Parameter(property = "javaspec.compile", defaultValue = "false")
    private boolean compile;

    @Parameter(property = "javaspec.compileOutput")
    private File compileOutput;

    @Parameter(property = "javaspec.compileOutputDirectory")
    private File compileOutputDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("javaspec: execution skipped.");
            return;
        }

        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader runClassLoader = null;
        try {
            JavaspecConfiguration configuration = loadConfiguration();
            JavaspecSuiteConfiguration selectedSuite = selectSuite(configuration);
            SpecDiscoveryRequest discoveryRequest = createDiscoveryRequest(selectedSuite);
            List<File> classpathEntries = classpathEntries();
            runClassLoader = createRunClassLoader(originalContextClassLoader, classpathEntries);

            List<String> effectiveExtensions = extensionsFor(configuration, selectedSuite);
            RunFormatterRegistry runFormatters = runFormatterRegistry(runClassLoader, effectiveExtensions);
            String effectiveFormatter = effectiveFormatter(configuration, runFormatters);
            boolean effectiveBootstrapDiscovery = effectiveBootstrapDiscovery(configuration, selectedSuite);

            getLog().info("javaspec: running suite '" + selectedSuite.name() + "' from "
                    + discoveryRequest.specRoot().getPath() + ".");
            getLog().info("javaspec: formatter " + effectiveFormatter + ".");

            Thread.currentThread().setContextClassLoader(runClassLoader);
            JavaspecInvocation invocation = JavaspecInvocation
                    .discovering(discoveryRequest, runClassLoader)
                    .withBootstrapHooks(bootstrapHooksFor(configuration, selectedSuite))
                    .withBootstrapDiscovery(effectiveBootstrapDiscovery)
                    .withExtensions(effectiveExtensions)
                    .withStopOnFailure(stopOnFailure);
            if (compilationRequested()) {
                invocation = invocation.withCompilation(
                        sourceDirectoryFor(selectedSuite),
                        discoveryRequest.specRoot(),
                        effectiveCompilationOutputDirectory(),
                        classpathEntries
                );
            }
            JavaspecInvocationResult invocationResult = JavaspecLauncher.run(invocation);
            RunResult runResult = invocationResult.runResult();
            RunFormatterRegistry outputFormatters = invocationResult.hasRunFormatterRegistry()
                    ? invocationResult.runFormatterRegistry()
                    : runFormatters;

            logSourceCompilation(invocationResult);
            logSummary(invocationResult);
            renderFormattedSummary(runResult, effectiveFormatter, outputFormatters);
            logExecutionDiagnostics(runResult);
            writeReports(runResult, configuration);
            handleFailures(runResult);
        } catch (MojoExecutionException ex) {
            throw ex;
        } catch (MojoFailureException ex) {
            throw ex;
        } catch (SourceCompilationException ex) {
            handleCompilationFailure(ex);
        } catch (ExtensionLoadingException ex) {
            throw new MojoFailureException("javaspec extension activation failed: " + messageOf(ex), ex);
        } catch (BootstrapException ex) {
            throw new MojoExecutionException("javaspec bootstrap execution failed: " + messageOf(ex), ex);
        } catch (RuntimeException ex) {
            throw new MojoExecutionException("javaspec execution failed: " + messageOf(ex), ex);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            closeClassLoader(runClassLoader);
        }
    }

    private JavaspecConfiguration loadConfiguration() throws MojoExecutionException {
        if (configFile == null) {
            return JavaspecConfiguration.defaults();
        }

        File effectiveConfigFile = projectFile(configFile);
        try {
            return JavaspecConfiguration.load(effectiveConfigFile);
        } catch (ConfigurationException ex) {
            throw new MojoExecutionException("Invalid javaspec configuration: " + messageOf(ex)
                    + " (configFile: " + effectiveConfigFile.getPath() + ").", ex);
        } catch (IOException ex) {
            throw new MojoExecutionException("I/O error while reading javaspec configuration: " + messageOf(ex)
                    + " (configFile: " + effectiveConfigFile.getPath() + ").", ex);
        } catch (SecurityException ex) {
            throw new MojoExecutionException("I/O error while reading javaspec configuration: " + messageOf(ex)
                    + " (configFile: " + effectiveConfigFile.getPath() + ").", ex);
        }
    }

    private JavaspecSuiteConfiguration selectSuite(JavaspecConfiguration configuration) throws MojoExecutionException {
        String selectedSuiteName = trimmedOrNull(suite);
        if (selectedSuiteName == null) {
            selectedSuiteName = configuration.defaultSuiteName();
        }

        try {
            return configuration.suite(selectedSuiteName);
        } catch (ConfigurationException ex) {
            throw new MojoExecutionException("Invalid javaspec suite selection: " + messageOf(ex), ex);
        }
    }

    private List<String> bootstrapHooksFor(
            JavaspecConfiguration configuration,
            JavaspecSuiteConfiguration selectedSuite
    ) {
        List<String> hooks = new ArrayList<String>();
        hooks.addAll(configuration.bootstrapHooks());
        hooks.addAll(selectedSuite.bootstrapHooks());
        return hooks;
    }

    private boolean effectiveBootstrapDiscovery(
            JavaspecConfiguration configuration,
            JavaspecSuiteConfiguration selectedSuite
    ) throws MojoExecutionException {
        boolean adapterBootstrapDiscovery = bootstrapDiscoveryParameter();
        return configuration.effectiveBootstrapDiscovery(selectedSuite) || adapterBootstrapDiscovery;
    }

    private boolean bootstrapDiscoveryParameter() throws MojoExecutionException {
        if (bootstrapDiscovery == null) {
            return false;
        }
        String trimmed = bootstrapDiscovery.trim();
        if (trimmed.length() == 0) {
            throw new MojoExecutionException("Invalid javaspec.bootstrapDiscovery: value must not be blank.");
        }
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        throw new MojoExecutionException("Invalid javaspec.bootstrapDiscovery: expected true or false but was '"
                + bootstrapDiscovery + "'.");
    }

    private SpecDiscoveryRequest createDiscoveryRequest(JavaspecSuiteConfiguration selectedSuite) throws MojoExecutionException {
        File effectiveSpecRoot = specDirectoryFor(selectedSuite);
        SpecNamingConvention namingConvention;
        try {
            namingConvention = SpecNamingConvention.from(selectedSuite);
        } catch (IllegalArgumentException ex) {
            throw new MojoExecutionException("Invalid javaspec naming configuration: " + messageOf(ex), ex);
        }

        SpecDiscoveryRequest request = SpecDiscoveryRequest.forSuite(selectedSuite.name(), effectiveSpecRoot, namingConvention);
        List<String> effectiveClassFilters = collectClassFilters();
        if (!effectiveClassFilters.isEmpty()) {
            request = request.withClassFilters(effectiveClassFilters);
        }
        List<String> effectiveExampleFilters = collectExampleFilters();
        if (!effectiveExampleFilters.isEmpty()) {
            request = request.withExampleFilters(effectiveExampleFilters);
        }
        return request;
    }

    private File specDirectoryFor(JavaspecSuiteConfiguration selectedSuite) {
        if (specDir != null) {
            return projectFile(specDir);
        }
        if (specRoot != null) {
            return projectFile(specRoot);
        }
        return projectFile(new File(selectedSuite.specDirectory()));
    }

    private File sourceDirectoryFor(JavaspecSuiteConfiguration selectedSuite) {
        return projectFile(new File(selectedSuite.sourceDirectory()));
    }

    private boolean compilationRequested() {
        return compile || compileOutput != null || compileOutputDirectory != null;
    }

    private File effectiveCompilationOutputDirectory() {
        if (compileOutput != null) {
            return projectFile(compileOutput);
        }
        if (compileOutputDirectory != null) {
            return projectFile(compileOutputDirectory);
        }
        return projectFile(new File("target/javaspec-classes"));
    }

    private List<File> classpathEntries() throws MojoExecutionException {
        List<String> elements = testClasspathElements == null ? new ArrayList<String>() : testClasspathElements;
        List<File> entries = new ArrayList<File>();
        for (int i = 0; i < elements.size(); i++) {
            String element = elements.get(i);
            if (element == null || element.trim().length() == 0) {
                throw new MojoExecutionException("Invalid test classpath element at index " + i + ": value must not be blank.");
            }
            entries.add(projectFile(new File(element)));
        }
        return entries;
    }

    private URLClassLoader createRunClassLoader(
            ClassLoader contextClassLoader,
            List<File> classpathEntries
    ) throws MojoExecutionException {
        ClassLoader parent = contextClassLoader == null ? JavaspecRunMojo.class.getClassLoader() : contextClassLoader;
        URL[] urls = new URL[classpathEntries.size()];
        for (int i = 0; i < classpathEntries.size(); i++) {
            File classpathEntry = classpathEntries.get(i);
            try {
                urls[i] = classpathEntry.toURI().toURL();
            } catch (MalformedURLException ex) {
                throw new MojoExecutionException("Invalid test classpath element: " + classpathEntry.getPath()
                        + " (" + messageOf(ex) + ").", ex);
            } catch (SecurityException ex) {
                throw new MojoExecutionException("I/O error while preparing test classpath element: "
                        + classpathEntry.getPath() + " (" + messageOf(ex) + ").", ex);
            }
        }
        getLog().debug("javaspec: using " + urls.length + " Maven test classpath element(s).");
        return new URLClassLoader(urls, parent);
    }

    private List<String> collectClassFilters() throws MojoExecutionException {
        List<String> filters = new ArrayList<String>();
        addListFilters(filters, classFilters, "classFilters");
        addDelimitedFilters(filters, classFiltersProperty, "javaspec.classFilters");
        addSingleFilter(filters, classFilter, "javaspec.classFilter");
        addSingleFilter(filters, classNameFilter, "javaspec.class");
        return filters;
    }

    private List<String> collectExampleFilters() throws MojoExecutionException {
        List<String> filters = new ArrayList<String>();
        addListFilters(filters, exampleFilters, "exampleFilters");
        addDelimitedFilters(filters, exampleFiltersProperty, "javaspec.exampleFilters");
        addSingleFilter(filters, exampleFilter, "javaspec.exampleFilter");
        addSingleFilter(filters, exampleNameFilter, "javaspec.example");
        return filters;
    }

    private void addListFilters(List<String> target, List<String> source, String parameterName) throws MojoExecutionException {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.size(); i++) {
            addSingleFilter(target, source.get(i), parameterName + "[" + i + "]");
        }
    }

    private void addDelimitedFilters(List<String> target, String value, String parameterName) throws MojoExecutionException {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new MojoExecutionException("Invalid " + parameterName + ": value must not be blank.");
        }
        int start = 0;
        int index = 0;
        while (start <= value.length()) {
            int separator = value.indexOf(',', start);
            String segment;
            if (separator < 0) {
                segment = value.substring(start);
                start = value.length() + 1;
            } else {
                segment = value.substring(start, separator);
                start = separator + 1;
            }
            addSingleFilter(target, segment, parameterName + " entry " + index);
            index++;
        }
    }

    private void addSingleFilter(List<String> target, String value, String parameterName) throws MojoExecutionException {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new MojoExecutionException("Invalid " + parameterName + ": value must not be blank.");
        }
        target.add(trimmed);
    }

    private void logSourceCompilation(JavaspecInvocationResult invocationResult) {
        if (!invocationResult.hasSourceCompilationResult()) {
            return;
        }
        SourceCompilationResult result = invocationResult.sourceCompilationResult();
        if (result.sourceFileCount() <= 0) {
            return;
        }
        getLog().info("javaspec: compiled " + result.sourceFileCount()
                + " source file(s) to " + result.outputDirectory().getPath() + ".");
    }

    private void logSummary(JavaspecInvocationResult invocationResult) {
        RunResult runResult = invocationResult.runResult();
        int specCount = invocationResult.discoveredSpecs().size();
        if (specCount == 0) {
            getLog().info("javaspec: no specifications found.");
        } else {
            getLog().info("javaspec: found " + specCount + " specification(s).");
        }
        getLog().info("javaspec: examples total=" + runResult.totalCount()
                + ", passed=" + runResult.passedCount()
                + ", failed=" + runResult.failedCount()
                + ", broken=" + runResult.brokenCount()
                + ", skipped=" + runResult.skippedCount()
                + ", pending=" + runResult.pendingCount() + ".");
        if (runResult.hasFailures()) {
            List<ExampleResult> failures = runResult.failures();
            for (int i = 0; i < failures.size(); i++) {
                ExampleResult failure = failures.get(i);
                getLog().warn("javaspec: " + failure.status().name().toLowerCase(Locale.ROOT)
                        + " " + failure.fullName() + " - " + failure.detail());
                if (failure.failureDetail() != null) {
                    getLog().warn("javaspec: " + failure.failureDetail().summary());
                }
            }
        }
    }

    private void logExecutionDiagnostics(RunResult runResult) {
        List<String> lines = RunDiagnostics.executionAvailabilityLines(runResult);
        if (lines.isEmpty()) {
            return;
        }
        getLog().warn("javaspec: Execution diagnostics:");
        for (int i = 0; i < lines.size(); i++) {
            getLog().warn("javaspec: - " + lines.get(i));
        }
        getLog().warn("javaspec: Maven test classpath contains " + testClasspathElementCount()
                + " element(s); javaspec:run needs compiled test/spec classes and dependencies "
                + "on the Maven test classpath.");
    }

    private int testClasspathElementCount() {
        if (testClasspathElements == null) {
            return 0;
        }
        return testClasspathElements.size();
    }

    private void writeReports(RunResult runResult, JavaspecConfiguration configuration) throws MojoExecutionException {
        File effectiveReportFile = reportFile;
        File effectiveJsonReportFile = jsonReportFile;
        if (effectiveReportFile == null && effectiveJsonReportFile == null) {
            effectiveReportFile = configurationFileOrNull(configuration.jsonReportFile());
        }
        writeJsonReportIfRequested(runResult, effectiveReportFile);
        if (!sameFileParameter(effectiveReportFile, effectiveJsonReportFile)) {
            writeJsonReportIfRequested(runResult, effectiveJsonReportFile);
        }

        File effectiveJunitXmlReportFile = junitXmlReportFile;
        File effectiveJunitXmlFile = junitXmlFile;
        if (effectiveJunitXmlReportFile == null && effectiveJunitXmlFile == null) {
            effectiveJunitXmlReportFile = configurationFileOrNull(configuration.junitXmlReportFile());
        }
        writeJUnitXmlReportIfRequested(runResult, effectiveJunitXmlReportFile);
        if (!sameFileParameter(effectiveJunitXmlReportFile, effectiveJunitXmlFile)) {
            writeJUnitXmlReportIfRequested(runResult, effectiveJunitXmlFile);
        }
    }

    private File configurationFileOrNull(String path) {
        if (path == null) {
            return null;
        }
        return new File(path);
    }

    private void writeJsonReportIfRequested(RunResult runResult, File configuredFile) throws MojoExecutionException {
        if (configuredFile == null) {
            return;
        }
        File report = projectFile(configuredFile);
        try {
            ensureParentDirectory(report);
            RunReportWriter.write(runResult, report);
            getLog().info("javaspec: wrote JSON report to " + report.getPath() + ".");
        } catch (IOException ex) {
            throw new MojoExecutionException("I/O error while writing javaspec JSON report: " + messageOf(ex)
                    + " (reportFile: " + report.getPath() + ").", ex);
        } catch (SecurityException ex) {
            throw new MojoExecutionException("I/O error while writing javaspec JSON report: " + messageOf(ex)
                    + " (reportFile: " + report.getPath() + ").", ex);
        }
    }

    private void writeJUnitXmlReportIfRequested(RunResult runResult, File configuredFile) throws MojoExecutionException {
        if (configuredFile == null) {
            return;
        }
        File report = projectFile(configuredFile);
        try {
            ensureParentDirectory(report);
            JUnitXmlReportWriter.write(runResult, report);
            getLog().info("javaspec: wrote JUnit XML report to " + report.getPath() + ".");
        } catch (IOException ex) {
            throw new MojoExecutionException("I/O error while writing javaspec JUnit XML report: " + messageOf(ex)
                    + " (junitXmlReportFile: " + report.getPath() + ").", ex);
        } catch (SecurityException ex) {
            throw new MojoExecutionException("I/O error while writing javaspec JUnit XML report: " + messageOf(ex)
                    + " (junitXmlReportFile: " + report.getPath() + ").", ex);
        }
    }

    private void ensureParentDirectory(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent == null || parent.isDirectory()) {
            return;
        }
        if (parent.exists() && !parent.isDirectory()) {
            throw new IOException("Report parent path is not a directory: " + parent.getPath());
        }
        if (!parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Could not create report parent directory: " + parent.getPath());
        }
    }

    private void handleFailures(RunResult runResult) throws MojoFailureException {
        if (!runResult.hasFailures()) {
            return;
        }
        String message = "javaspec found failed or broken examples: failed=" + runResult.failedCount()
                + ", broken=" + runResult.brokenCount() + ".";
        if (failOnFailure) {
            throw new MojoFailureException(message);
        }
        getLog().warn(message);
        getLog().warn("javaspec: failOnFailure=false, Maven build will continue.");
    }

    private void handleCompilationFailure(
            SourceCompilationException ex
    ) throws MojoExecutionException, MojoFailureException {
        if (ex.reason() == SourceCompilationException.Reason.COMPILER_UNAVAILABLE) {
            throw new MojoExecutionException("javaspec compilation failed: Java compiler is not available. "
                    + "Run javaspec with a JDK or disable compilation.", ex);
        }
        if (ex.reason() == SourceCompilationException.Reason.COMPILATION_FAILED) {
            logCompilationDiagnostics(ex);
            throw new MojoFailureException("javaspec compilation failed: Compilation failed", ex);
        }
        if (ex.reason() == SourceCompilationException.Reason.IO_ERROR) {
            throw new MojoExecutionException("javaspec compilation failed: " + messageOf(ex), ex);
        }
        throw new MojoExecutionException("javaspec compilation failed: " + messageOf(ex), ex);
    }

    private void logCompilationDiagnostics(SourceCompilationException ex) {
        if (!ex.hasSourceCompilationResult()) {
            return;
        }
        List<String> diagnostics = ex.sourceCompilationResult().diagnostics();
        for (int i = 0; i < diagnostics.size(); i++) {
            getLog().error("javaspec:   " + diagnostics.get(i));
        }
    }

    private File projectFile(File file) {
        if (file.isAbsolute()) {
            return file;
        }
        return new File(projectBaseDirectory(), file.getPath());
    }

    private File projectBaseDirectory() {
        if (basedir != null) {
            return basedir;
        }
        return new File(".").getAbsoluteFile();
    }

    private boolean sameFileParameter(File left, File right) {
        if (left == null || right == null) {
            return false;
        }
        return projectFile(left).getAbsolutePath().equals(projectFile(right).getAbsolutePath());
    }

    private void closeClassLoader(URLClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException ex) {
            getLog().warn("javaspec: could not close run classloader: " + messageOf(ex));
        }
    }

    private List<String> extensionsFor(
            JavaspecConfiguration configuration,
            JavaspecSuiteConfiguration selectedSuite
    ) throws MojoExecutionException {
        List<String> extensions = new ArrayList<String>();
        extensions.addAll(configuration.extensions());
        extensions.addAll(selectedSuite.extensions());
        addDelimitedFilters(extensions, extensionsProperty, "javaspec.extensions");
        return extensions;
    }

    private RunFormatterRegistry runFormatterRegistry(
            ClassLoader runClassLoader,
            List<String> effectiveExtensions
    ) throws MojoFailureException {
        try {
            return JavaspecExtensionLoader.loadRunFormatterRegistry(runClassLoader, effectiveExtensions);
        } catch (ExtensionLoadingException ex) {
            throw new MojoFailureException("javaspec extension activation failed: " + messageOf(ex), ex);
        }
    }

    private String effectiveFormatter(
            JavaspecConfiguration configuration,
            RunFormatterRegistry runFormatters
    ) throws MojoFailureException {
        String configuredFormatter = trimmedOrNull(formatter);
        if (configuredFormatter == null) {
            configuredFormatter = configuration.formatter();
        }
        String normalized = RunFormatterRegistry.normalizeName(configuredFormatter);
        if (normalized == null || !runFormatters.contains(normalized)) {
            throw new MojoFailureException("Invalid javaspec formatter: " + configuredFormatter
                    + ". Valid values: " + validFormatterNames(runFormatters) + ".");
        }
        return normalized;
    }

    private void renderFormattedSummary(
            RunResult runResult,
            String formatterName,
            RunFormatterRegistry runFormatters
    ) {
        RunFormatter formatter = runFormatters.lookup(formatterName);
        if (formatter == null) {
            return;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream stream = null;
        try {
            stream = new PrintStream(buffer, true, StandardCharsets.UTF_8.name());
            formatter.format(runResult, stream);
            stream.flush();
            String output = buffer.toString(StandardCharsets.UTF_8.name());
            int start = 0;
            while (start < output.length()) {
                int end = start;
                while (end < output.length() && output.charAt(end) != '\n' && output.charAt(end) != '\r') {
                    end++;
                }
                String line = output.substring(start, end);
                if (line.length() > 0) {
                    getLog().info("javaspec: " + line);
                }
                if (end == output.length()) {
                    break;
                }
                char terminator = output.charAt(end);
                end++;
                if (terminator == '\r' && end < output.length() && output.charAt(end) == '\n') {
                    end++;
                }
                start = end;
            }
        } catch (UnsupportedEncodingException ex) {
            getLog().warn("javaspec: UTF-8 is not available while rendering output: " + messageOf(ex));
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private static String validFormatterNames(RunFormatterRegistry registry) {
        List<String> names = registry.formatterNames();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(names.get(i));
        }
        return builder.toString();
    }

    private static String trimmedOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return null;
        }
        return trimmed;
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
