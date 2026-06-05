package org.javaspec.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
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
import java.util.Set;

/**
 * Gradle task that adapts to the canonical no-JUnit javaspec launcher.
 */
public class JavaspecRunTask extends DefaultTask {
    private final JavaspecExtension taskOptions;
    private JavaspecExtension extension;
    private FileCollection defaultClasspath;

    public JavaspecRunTask() {
        this.taskOptions = new JavaspecExtension(getProject());
        setGroup("verification");
        setDescription("Runs javaspec specifications with the canonical no-JUnit launcher.");
    }

    @Internal
    public boolean isSkip() {
        return booleanOption(
                taskOptions.skipOption(),
                extension == null ? null : extension.skipOption(),
                "javaspec.skip",
                false
        );
    }

    public void setSkip(boolean skip) {
        taskOptions.setSkip(skip);
    }

    @Internal
    public boolean isFailOnFailure() {
        return booleanOption(
                taskOptions.failOnFailureOption(),
                extension == null ? null : extension.failOnFailureOption(),
                "javaspec.failOnFailure",
                true
        );
    }

    public void setFailOnFailure(boolean failOnFailure) {
        taskOptions.setFailOnFailure(failOnFailure);
    }

    @Internal
    public boolean isStopOnFailure() {
        return booleanOption(
                taskOptions.stopOnFailureOption(),
                extension == null ? null : extension.stopOnFailureOption(),
                "javaspec.stopOnFailure",
                false
        );
    }

    public void setStopOnFailure(boolean stopOnFailure) {
        taskOptions.setStopOnFailure(stopOnFailure);
    }

    @Internal
    public File getConfigFile() {
        return fileOption(
                taskOptions.configFileOption(),
                extension == null ? null : extension.configFileOption(),
                "javaspec.configFile"
        );
    }

    public void setConfigFile(Object configFile) {
        taskOptions.setConfigFile(configFile);
    }

    @Internal
    public String getSuite() {
        return stringOption(
                taskOptions.suiteOption(),
                extension == null ? null : extension.suiteOption(),
                "javaspec.suite"
        );
    }

    public void setSuite(String suite) {
        taskOptions.setSuite(suite);
    }

    @Internal
    public File getSpecDir() {
        return fileOption(
                taskOptions.specDirOption(),
                extension == null ? null : extension.specDirOption(),
                "javaspec.specDir"
        );
    }

    public void setSpecDir(Object specDir) {
        taskOptions.setSpecDir(specDir);
    }

    @Internal
    public File getSpecRoot() {
        return fileOption(
                taskOptions.specRootOption(),
                extension == null ? null : extension.specRootOption(),
                "javaspec.specRoot"
        );
    }

    public void setSpecRoot(Object specRoot) {
        taskOptions.setSpecRoot(specRoot);
    }

    @Internal
    public FileCollection getClasspath() {
        if (taskOptions.classpathOption() != null) {
            return taskOptions.classpathOption();
        }
        if (extension != null && extension.classpathOption() != null) {
            return extension.classpathOption();
        }
        if (defaultClasspath != null) {
            return defaultClasspath;
        }
        return getProject().files();
    }

    public void setClasspath(Object classpath) {
        taskOptions.setClasspath(classpath);
    }

    public void classpath(Object... paths) {
        taskOptions.classpath(paths);
    }

    @Internal
    public String getFormatter() {
        return stringOption(
                taskOptions.formatterOption(),
                extension == null ? null : extension.formatterOption(),
                "javaspec.formatter"
        );
    }

    public void setFormatter(String formatter) {
        taskOptions.setFormatter(formatter);
    }

    @Internal
    public File getReportFile() {
        return reportFile();
    }

    public void setReportFile(Object reportFile) {
        taskOptions.setReportFile(reportFile);
    }

    @Internal
    public File getJsonReportFile() {
        return jsonReportFile();
    }

    public void setJsonReportFile(Object jsonReportFile) {
        taskOptions.setJsonReportFile(jsonReportFile);
    }

    @Internal
    public File getJunitXmlReportFile() {
        return junitXmlReportFile();
    }

    @Internal
    public File getJUnitXmlReportFile() {
        return junitXmlReportFile();
    }

    public void setJunitXmlReportFile(Object junitXmlReportFile) {
        taskOptions.setJunitXmlReportFile(junitXmlReportFile);
    }

    public void setJUnitXmlReportFile(Object junitXmlReportFile) {
        taskOptions.setJUnitXmlReportFile(junitXmlReportFile);
    }

    @Internal
    public File getJunitXmlFile() {
        return junitXmlFile();
    }

    @Internal
    public File getJUnitXmlFile() {
        return junitXmlFile();
    }

    public void setJunitXmlFile(Object junitXmlFile) {
        taskOptions.setJunitXmlFile(junitXmlFile);
    }

    public void setJUnitXmlFile(Object junitXmlFile) {
        taskOptions.setJUnitXmlFile(junitXmlFile);
    }

    @Internal
    public List<String> getClassFilters() {
        return taskOptions.getClassFilters();
    }

    public void setClassFilters(Object classFilters) {
        taskOptions.setClassFilters(classFilters);
    }

    public void classFilters(Object... classFilters) {
        taskOptions.classFilters(classFilters);
    }

    @Internal
    public String getClassFiltersProperty() {
        return taskOptions.getClassFiltersProperty();
    }

    public void setClassFiltersProperty(String classFiltersProperty) {
        taskOptions.setClassFiltersProperty(classFiltersProperty);
    }

    @Internal
    public String getClassFilter() {
        return taskOptions.getClassFilter();
    }

    public void setClassFilter(String classFilter) {
        taskOptions.setClassFilter(classFilter);
    }

    @Internal
    public String getClassNameFilter() {
        return taskOptions.getClassNameFilter();
    }

    public void setClassNameFilter(String classNameFilter) {
        taskOptions.setClassNameFilter(classNameFilter);
    }

    @Internal
    public String getClassName() {
        return taskOptions.getClassName();
    }

    public void setClassName(String className) {
        taskOptions.setClassName(className);
    }

    @Internal
    public List<String> getEffectiveClassFiltersForInputs() {
        return collectClassFilters();
    }

    @Internal
    public List<String> getExampleFilters() {
        return taskOptions.getExampleFilters();
    }

    public void setExampleFilters(Object exampleFilters) {
        taskOptions.setExampleFilters(exampleFilters);
    }

    public void exampleFilters(Object... exampleFilters) {
        taskOptions.exampleFilters(exampleFilters);
    }

    @Internal
    public String getExampleFiltersProperty() {
        return taskOptions.getExampleFiltersProperty();
    }

    public void setExampleFiltersProperty(String exampleFiltersProperty) {
        taskOptions.setExampleFiltersProperty(exampleFiltersProperty);
    }

    @Internal
    public String getExampleFilter() {
        return taskOptions.getExampleFilter();
    }

    public void setExampleFilter(String exampleFilter) {
        taskOptions.setExampleFilter(exampleFilter);
    }

    @Internal
    public String getExampleNameFilter() {
        return taskOptions.getExampleNameFilter();
    }

    public void setExampleNameFilter(String exampleNameFilter) {
        taskOptions.setExampleNameFilter(exampleNameFilter);
    }

    @Internal
    public String getExampleName() {
        return taskOptions.getExampleName();
    }

    public void setExampleName(String exampleName) {
        taskOptions.setExampleName(exampleName);
    }

    @Internal
    public List<String> getEffectiveExampleFiltersForInputs() {
        return collectExampleFilters();
    }

    @TaskAction
    public void runJavaspec() {
        if (isSkip()) {
            getLogger().lifecycle("javaspec: execution skipped.");
            return;
        }

        boolean effectiveFailOnFailure = isFailOnFailure();
        boolean effectiveStopOnFailure = isStopOnFailure();

        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader runClassLoader = null;
        try {
            JavaspecConfiguration configuration = loadConfiguration();
            JavaspecSuiteConfiguration selectedSuite = selectSuite(configuration);
            SpecDiscoveryRequest discoveryRequest = createDiscoveryRequest(selectedSuite);
            List<File> classpathEntries = classpathEntries();
            runClassLoader = createRunClassLoader(originalContextClassLoader, classpathEntries);
            RunFormatterRegistry runFormatters = runFormatterRegistry(runClassLoader);
            String effectiveFormatter = effectiveFormatter(configuration, runFormatters);

            getLogger().lifecycle("javaspec: running suite '" + selectedSuite.name()
                    + "' from " + discoveryRequest.specRoot().getPath() + ".");
            getLogger().lifecycle("javaspec: using " + classpathEntries.size() + " Gradle classpath element(s).");
            if (classpathEntries.isEmpty()) {
                getLogger().warn("javaspec: Gradle classpath is empty; configure classpath if specifications require compiled classes.");
            }
            getLogger().lifecycle("javaspec: formatter " + effectiveFormatter + ".");

            Thread.currentThread().setContextClassLoader(runClassLoader);
            JavaspecInvocation invocation = JavaspecInvocation
                    .discovering(discoveryRequest, runClassLoader)
                    .withStopOnFailure(effectiveStopOnFailure);
            JavaspecInvocationResult invocationResult = JavaspecLauncher.run(invocation);
            RunResult runResult = invocationResult.runResult();

            logDiscoverySummary(invocationResult);
            renderFormattedSummary(runResult, effectiveFormatter, runFormatters);
            logExecutionDiagnostics(runResult, classpathEntries.size());
            logFailureWarnings(runResult);
            writeReports(runResult, configuration);
            handleFailures(runResult, effectiveFailOnFailure);
        } catch (GradleException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new GradleException("javaspec execution failed: " + messageOf(ex), ex);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            closeClassLoader(runClassLoader);
        }
    }

    void setJavaspecExtension(JavaspecExtension extension) {
        this.extension = extension;
    }

    void setDefaultClasspath(FileCollection defaultClasspath) {
        this.defaultClasspath = defaultClasspath;
    }

    private JavaspecConfiguration loadConfiguration() {
        File configuredFile = getConfigFile();
        if (configuredFile == null) {
            return JavaspecConfiguration.defaults();
        }
        try {
            return JavaspecConfiguration.load(configuredFile);
        } catch (ConfigurationException ex) {
            throw new GradleException("Invalid javaspec configuration: " + messageOf(ex)
                    + " (configFile: " + configuredFile.getPath() + ").", ex);
        } catch (IOException ex) {
            throw new GradleException("I/O error while reading javaspec configuration: " + messageOf(ex)
                    + " (configFile: " + configuredFile.getPath() + ").", ex);
        } catch (SecurityException ex) {
            throw new GradleException("I/O error while reading javaspec configuration: " + messageOf(ex)
                    + " (configFile: " + configuredFile.getPath() + ").", ex);
        }
    }

    private JavaspecSuiteConfiguration selectSuite(JavaspecConfiguration configuration) {
        String selectedSuiteName = trimmedOrNull(getSuite());
        if (selectedSuiteName == null) {
            selectedSuiteName = configuration.defaultSuiteName();
        }
        try {
            return configuration.suite(selectedSuiteName);
        } catch (ConfigurationException ex) {
            throw new GradleException("Invalid javaspec suite selection: " + messageOf(ex), ex);
        }
    }

    private SpecDiscoveryRequest createDiscoveryRequest(JavaspecSuiteConfiguration selectedSuite) {
        File effectiveSpecRoot = specDirectoryFor(selectedSuite);
        SpecNamingConvention namingConvention;
        try {
            namingConvention = SpecNamingConvention.from(selectedSuite);
        } catch (IllegalArgumentException ex) {
            throw new GradleException("Invalid javaspec naming configuration: " + messageOf(ex), ex);
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
        if (taskOptions.specDirOption() != null) {
            return taskOptions.specDirOption();
        }
        if (taskOptions.specRootOption() != null) {
            return taskOptions.specRootOption();
        }
        if (extension != null && extension.specDirOption() != null) {
            return extension.specDirOption();
        }
        if (extension != null && extension.specRootOption() != null) {
            return extension.specRootOption();
        }
        File propertySpecDir = projectFileProperty("javaspec.specDir");
        if (propertySpecDir != null) {
            return propertySpecDir;
        }
        File propertySpecRoot = projectFileProperty("javaspec.specRoot");
        if (propertySpecRoot != null) {
            return propertySpecRoot;
        }
        return getProject().file(selectedSuite.specDirectory());
    }

    private List<File> classpathEntries() {
        Set<File> files = getClasspath().getFiles();
        return new ArrayList<File>(files);
    }

    private URLClassLoader createRunClassLoader(ClassLoader contextClassLoader, List<File> classpathEntries) {
        ClassLoader parent = JavaspecRunTask.class.getClassLoader();
        if (parent == null) {
            parent = contextClassLoader;
        }
        URL[] urls = new URL[classpathEntries.size()];
        for (int i = 0; i < classpathEntries.size(); i++) {
            File classpathEntry = classpathEntries.get(i);
            if (classpathEntry == null) {
                throw new GradleException("Invalid Gradle classpath element at index " + i + ": value must not be null.");
            }
            try {
                urls[i] = classpathEntry.toURI().toURL();
            } catch (MalformedURLException ex) {
                throw new GradleException("Invalid Gradle classpath element: " + classpathEntry.getPath()
                        + " (" + messageOf(ex) + ").", ex);
            } catch (SecurityException ex) {
                throw new GradleException("I/O error while preparing Gradle classpath element: "
                        + classpathEntry.getPath() + " (" + messageOf(ex) + ").", ex);
            }
        }
        return new URLClassLoader(urls, parent);
    }

    private RunFormatterRegistry runFormatterRegistry(ClassLoader runClassLoader) {
        try {
            return JavaspecExtensionLoader.loadRunFormatterRegistry(runClassLoader);
        } catch (ExtensionLoadingException ex) {
            throw new GradleException("Could not load javaspec extensions: " + messageOf(ex), ex);
        }
    }

    private String effectiveFormatter(JavaspecConfiguration configuration, RunFormatterRegistry runFormatters) {
        String configuredFormatter = trimmedOrNull(getFormatter());
        if (configuredFormatter == null) {
            configuredFormatter = configuration.formatter();
        }
        String normalized = RunFormatterRegistry.normalizeName(configuredFormatter);
        if (normalized == null || !runFormatters.contains(normalized)) {
            throw new GradleException("Invalid javaspec formatter: " + configuredFormatter
                    + ". Valid values: " + validFormatterNames(runFormatters) + ".");
        }
        return normalized;
    }

    private void logDiscoverySummary(JavaspecInvocationResult invocationResult) {
        int specCount = invocationResult.discoveredSpecs().size();
        if (specCount == 0) {
            getLogger().lifecycle("javaspec: no specifications found.");
        } else {
            getLogger().lifecycle("javaspec: found " + specCount + " specification(s).");
        }
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
            logLifecycleLines(buffer.toString(StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException ex) {
            throw new GradleException("UTF-8 is not available while rendering javaspec output: " + messageOf(ex), ex);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private void logLifecycleLines(String text) {
        int start = 0;
        while (start < text.length()) {
            int end = start;
            while (end < text.length() && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
                end++;
            }
            String line = text.substring(start, end);
            if (line.length() > 0) {
                getLogger().lifecycle("javaspec: " + line);
            }
            if (end == text.length()) {
                return;
            }
            char terminator = text.charAt(end);
            end++;
            if (terminator == '\r' && end < text.length() && text.charAt(end) == '\n') {
                end++;
            }
            start = end;
        }
    }

    private void logExecutionDiagnostics(RunResult runResult, int classpathElementCount) {
        List<String> lines = RunDiagnostics.executionAvailabilityLines(runResult);
        if (lines.isEmpty()) {
            return;
        }
        getLogger().warn("javaspec: Execution diagnostics:");
        for (int i = 0; i < lines.size(); i++) {
            getLogger().warn("javaspec: - " + lines.get(i));
        }
        getLogger().warn("javaspec: Gradle classpath contains " + classpathElementCount
                + " element(s); this task needs compiled spec classes and dependencies on its configured/default "
                + "test runtime classpath.");
    }

    private void logFailureWarnings(RunResult runResult) {
        if (!runResult.hasFailures()) {
            return;
        }
        List<ExampleResult> failures = runResult.failures();
        for (int i = 0; i < failures.size(); i++) {
            ExampleResult failure = failures.get(i);
            getLogger().warn("javaspec: " + failure.status().name().toLowerCase(Locale.ROOT)
                    + " " + failure.fullName() + " - " + failure.detail());
            if (failure.failureDetail() != null) {
                getLogger().warn("javaspec: " + failure.failureDetail().summary());
            }
        }
    }

    private void writeReports(RunResult runResult, JavaspecConfiguration configuration) {
        File report = reportFile();
        File jsonReport = jsonReportFile();
        if (report == null && jsonReport == null) {
            report = configurationFileOrNull(configuration.jsonReportFile());
        }
        writeJsonReportIfRequested(runResult, report);
        if (!sameFileParameter(report, jsonReport)) {
            writeJsonReportIfRequested(runResult, jsonReport);
        }

        File junitXmlReport = junitXmlReportFile();
        File junitXml = junitXmlFile();
        if (junitXmlReport == null && junitXml == null) {
            junitXmlReport = configurationFileOrNull(configuration.junitXmlReportFile());
        }
        writeJUnitXmlReportIfRequested(runResult, junitXmlReport);
        if (!sameFileParameter(junitXmlReport, junitXml)) {
            writeJUnitXmlReportIfRequested(runResult, junitXml);
        }
    }

    private File configurationFileOrNull(String path) {
        if (path == null) {
            return null;
        }
        return getProject().file(path);
    }

    private void writeJsonReportIfRequested(RunResult runResult, File configuredFile) {
        if (configuredFile == null) {
            return;
        }
        try {
            ensureParentDirectory(configuredFile);
            RunReportWriter.write(runResult, configuredFile);
            getLogger().lifecycle("javaspec: wrote JSON report to " + configuredFile.getPath() + ".");
        } catch (IOException ex) {
            throw new GradleException("I/O error while writing javaspec JSON report: " + messageOf(ex)
                    + " (reportFile: " + configuredFile.getPath() + ").", ex);
        } catch (SecurityException ex) {
            throw new GradleException("I/O error while writing javaspec JSON report: " + messageOf(ex)
                    + " (reportFile: " + configuredFile.getPath() + ").", ex);
        }
    }

    private void writeJUnitXmlReportIfRequested(RunResult runResult, File configuredFile) {
        if (configuredFile == null) {
            return;
        }
        try {
            ensureParentDirectory(configuredFile);
            JUnitXmlReportWriter.write(runResult, configuredFile);
            getLogger().lifecycle("javaspec: wrote JUnit XML report to " + configuredFile.getPath() + ".");
        } catch (IOException ex) {
            throw new GradleException("I/O error while writing javaspec JUnit XML report: " + messageOf(ex)
                    + " (junitXmlReportFile: " + configuredFile.getPath() + ").", ex);
        } catch (SecurityException ex) {
            throw new GradleException("I/O error while writing javaspec JUnit XML report: " + messageOf(ex)
                    + " (junitXmlReportFile: " + configuredFile.getPath() + ").", ex);
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

    private void handleFailures(RunResult runResult, boolean effectiveFailOnFailure) {
        if (!runResult.hasFailures()) {
            return;
        }
        String message = "javaspec found failed or broken examples: failed=" + runResult.failedCount()
                + ", broken=" + runResult.brokenCount() + ".";
        if (effectiveFailOnFailure) {
            throw new GradleException(message);
        }
        getLogger().warn(message);
        getLogger().warn("javaspec: failOnFailure=false, Gradle build will continue.");
    }

    private List<String> collectClassFilters() {
        List<String> filters = new ArrayList<String>();
        if (extension != null) {
            addListFilters(filters, extension.classFiltersOption(), "javaspec.classFilters");
        }
        addListFilters(filters, taskOptions.classFiltersOption(), "classFilters");
        if (extension != null) {
            addDelimitedFilters(filters, extension.classFiltersPropertyOption(), "javaspec.classFilters");
        }
        addDelimitedFilters(filters, taskOptions.classFiltersPropertyOption(), "classFiltersProperty");
        addDelimitedFilters(filters, projectProperty("javaspec.classFilters"), "javaspec.classFilters");
        if (extension != null) {
            addSingleFilter(filters, extension.classFilterOption(), "javaspec.classFilter");
        }
        addSingleFilter(filters, taskOptions.classFilterOption(), "classFilter");
        addSingleFilter(filters, projectProperty("javaspec.classFilter"), "javaspec.classFilter");
        if (extension != null) {
            addSingleFilter(filters, extension.classNameFilterOption(), "javaspec.class");
        }
        addSingleFilter(filters, taskOptions.classNameFilterOption(), "classNameFilter");
        addSingleFilter(filters, projectProperty("javaspec.class"), "javaspec.class");
        return filters;
    }

    private List<String> collectExampleFilters() {
        List<String> filters = new ArrayList<String>();
        if (extension != null) {
            addListFilters(filters, extension.exampleFiltersOption(), "javaspec.exampleFilters");
        }
        addListFilters(filters, taskOptions.exampleFiltersOption(), "exampleFilters");
        if (extension != null) {
            addDelimitedFilters(filters, extension.exampleFiltersPropertyOption(), "javaspec.exampleFilters");
        }
        addDelimitedFilters(filters, taskOptions.exampleFiltersPropertyOption(), "exampleFiltersProperty");
        addDelimitedFilters(filters, projectProperty("javaspec.exampleFilters"), "javaspec.exampleFilters");
        if (extension != null) {
            addSingleFilter(filters, extension.exampleFilterOption(), "javaspec.exampleFilter");
        }
        addSingleFilter(filters, taskOptions.exampleFilterOption(), "exampleFilter");
        addSingleFilter(filters, projectProperty("javaspec.exampleFilter"), "javaspec.exampleFilter");
        if (extension != null) {
            addSingleFilter(filters, extension.exampleNameFilterOption(), "javaspec.example");
        }
        addSingleFilter(filters, taskOptions.exampleNameFilterOption(), "exampleNameFilter");
        addSingleFilter(filters, projectProperty("javaspec.example"), "javaspec.example");
        return filters;
    }

    private void addListFilters(List<String> target, List<String> source, String parameterName) {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.size(); i++) {
            addSingleFilter(target, source.get(i), parameterName + "[" + i + "]");
        }
    }

    private void addDelimitedFilters(List<String> target, String value, String parameterName) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new GradleException("Invalid " + parameterName + ": value must not be blank.");
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

    private void addSingleFilter(List<String> target, String value, String parameterName) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new GradleException("Invalid " + parameterName + ": value must not be blank.");
        }
        target.add(trimmed);
    }

    private boolean booleanOption(Boolean taskValue, Boolean extensionValue, String propertyName, boolean defaultValue) {
        if (taskValue != null) {
            return taskValue.booleanValue();
        }
        if (extensionValue != null) {
            return extensionValue.booleanValue();
        }
        String propertyValue = projectProperty(propertyName);
        if (propertyValue != null) {
            return parseBooleanProperty(propertyName, propertyValue);
        }
        return defaultValue;
    }

    private String stringOption(String taskValue, String extensionValue, String propertyName) {
        if (taskValue != null) {
            return taskValue;
        }
        if (extensionValue != null) {
            return extensionValue;
        }
        return projectProperty(propertyName);
    }

    private File fileOption(File taskValue, File extensionValue, String propertyName) {
        if (taskValue != null) {
            return taskValue;
        }
        if (extensionValue != null) {
            return extensionValue;
        }
        return projectFileProperty(propertyName);
    }

    private File reportFile() {
        return fileOption(
                taskOptions.reportFileOption(),
                extension == null ? null : extension.reportFileOption(),
                "javaspec.reportFile"
        );
    }

    private File jsonReportFile() {
        return fileOption(
                taskOptions.jsonReportFileOption(),
                extension == null ? null : extension.jsonReportFileOption(),
                "javaspec.jsonReportFile"
        );
    }

    private File junitXmlReportFile() {
        return fileOption(
                taskOptions.junitXmlReportFileOption(),
                extension == null ? null : extension.junitXmlReportFileOption(),
                "javaspec.junitXmlReportFile"
        );
    }

    private File junitXmlFile() {
        return fileOption(
                taskOptions.junitXmlFileOption(),
                extension == null ? null : extension.junitXmlFileOption(),
                "javaspec.junitXmlFile"
        );
    }

    private File projectFileProperty(String propertyName) {
        String propertyValue = projectProperty(propertyName);
        if (propertyValue == null) {
            return null;
        }
        if (propertyValue.trim().length() == 0) {
            throw new GradleException("Invalid " + propertyName + ": value must not be blank.");
        }
        return getProject().file(propertyValue);
    }

    private String projectProperty(String propertyName) {
        Object value = getProject().findProperty(propertyName);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private boolean parseBooleanProperty(String propertyName, String value) {
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        throw new GradleException("Invalid " + propertyName + ": expected true or false but was '" + value + "'.");
    }

    private boolean sameFileParameter(File left, File right) {
        if (left == null || right == null) {
            return false;
        }
        return left.getAbsoluteFile().getPath().equals(right.getAbsoluteFile().getPath());
    }

    private void closeClassLoader(URLClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException ex) {
            Logger logger = getLogger();
            logger.warn("javaspec: could not close run classloader: " + messageOf(ex));
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

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
