package org.javaspec.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.javaspec.config.ConfigurationException;
import org.javaspec.config.JavaspecConfiguration;
import org.javaspec.config.JavaspecSuiteConfiguration;
import org.javaspec.diagnostics.RunDiagnostics;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.discovery.SpecNamingConvention;
import org.javaspec.invocation.JavaspecInvocation;
import org.javaspec.invocation.JavaspecInvocationResult;
import org.javaspec.invocation.JavaspecLauncher;
import org.javaspec.reporting.JUnitXmlReportWriter;
import org.javaspec.reporting.RunReportWriter;
import org.javaspec.runner.ExampleResult;
import org.javaspec.runner.RunResult;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
            runClassLoader = createRunClassLoader(originalContextClassLoader);

            getLog().info("javaspec: running suite '" + selectedSuite.name() + "' from "
                    + discoveryRequest.specRoot().getPath() + ".");

            Thread.currentThread().setContextClassLoader(runClassLoader);
            JavaspecInvocation invocation = JavaspecInvocation
                    .discovering(discoveryRequest, runClassLoader)
                    .withStopOnFailure(stopOnFailure);
            JavaspecInvocationResult invocationResult = JavaspecLauncher.run(invocation);
            RunResult runResult = invocationResult.runResult();

            logSummary(invocationResult);
            logExecutionDiagnostics(runResult);
            writeReports(runResult, configuration);
            handleFailures(runResult);
        } catch (MojoExecutionException ex) {
            throw ex;
        } catch (MojoFailureException ex) {
            throw ex;
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

    private URLClassLoader createRunClassLoader(ClassLoader contextClassLoader) throws MojoExecutionException {
        ClassLoader parent = contextClassLoader == null ? JavaspecRunMojo.class.getClassLoader() : contextClassLoader;
        List<String> elements = testClasspathElements == null ? new ArrayList<String>() : testClasspathElements;
        URL[] urls = new URL[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            String element = elements.get(i);
            if (element == null || element.trim().length() == 0) {
                throw new MojoExecutionException("Invalid test classpath element at index " + i + ": value must not be blank.");
            }
            File classpathEntry = projectFile(new File(element));
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
