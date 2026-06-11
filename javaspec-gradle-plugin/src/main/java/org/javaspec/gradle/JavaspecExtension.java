package org.javaspec.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle DSL options shared by the javaspec extension and javaspecRun task.
 */
public class JavaspecExtension {
    private final Project project;

    private Boolean skip;
    private Boolean failOnFailure;
    private Boolean stopOnFailure;
    private Boolean bootstrapDiscovery;
    private Boolean compile;
    private File configFile;
    private String suite;
    private File specDir;
    private File specRoot;
    private FileCollection classpath;
    private String formatter;
    private File reportFile;
    private File jsonReportFile;
    private File junitXmlReportFile;
    private File compileOutput;
    private File junitXmlFile;

    private final List<String> classFilters = new ArrayList<String>();
    private String classFiltersProperty;
    private String classFilter;
    private String classNameFilter;

    private final List<String> exampleFilters = new ArrayList<String>();
    private String exampleFiltersProperty;
    private String exampleFilter;
    private String exampleNameFilter;

    private final List<String> extensions = new ArrayList<String>();

    public JavaspecExtension(Project project) {
        this.project = project;
    }

    public boolean isSkip() {
        return skip != null && skip.booleanValue();
    }

    public void setSkip(boolean skip) {
        this.skip = Boolean.valueOf(skip);
    }

    public boolean isFailOnFailure() {
        return failOnFailure == null || failOnFailure.booleanValue();
    }

    public void setFailOnFailure(boolean failOnFailure) {
        this.failOnFailure = Boolean.valueOf(failOnFailure);
    }

    public boolean isStopOnFailure() {
        return stopOnFailure != null && stopOnFailure.booleanValue();
    }

    public void setStopOnFailure(boolean stopOnFailure) {
        this.stopOnFailure = Boolean.valueOf(stopOnFailure);
    }

    public boolean isBootstrapDiscovery() {
        return bootstrapDiscovery != null && bootstrapDiscovery.booleanValue();
    }

    public void setBootstrapDiscovery(boolean bootstrapDiscovery) {
        this.bootstrapDiscovery = Boolean.valueOf(bootstrapDiscovery);
    }

    public boolean isCompile() {
        return compile != null && compile.booleanValue();
    }

    public void setCompile(boolean compile) {
        this.compile = Boolean.valueOf(compile);
    }

    public File getConfigFile() {
        return configFile;
    }

    public void setConfigFile(Object configFile) {
        this.configFile = fileOrNull(configFile);
    }

    public String getSuite() {
        return suite;
    }

    public void setSuite(String suite) {
        this.suite = suite;
    }

    public File getSpecDir() {
        return specDir;
    }

    public void setSpecDir(Object specDir) {
        this.specDir = fileOrNull(specDir);
    }

    public File getSpecRoot() {
        return specRoot;
    }

    public void setSpecRoot(Object specRoot) {
        this.specRoot = fileOrNull(specRoot);
    }

    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(Object classpath) {
        this.classpath = fileCollectionOrNull(classpath);
    }

    public void classpath(Object... paths) {
        if (paths == null || paths.length == 0) {
            return;
        }
        FileCollection additional = project.files(paths);
        if (classpath == null) {
            classpath = additional;
        } else {
            classpath = classpath.plus(additional);
        }
    }

    public String getFormatter() {
        return formatter;
    }

    public void setFormatter(String formatter) {
        this.formatter = formatter;
    }

    public File getReportFile() {
        return reportFile;
    }

    public void setReportFile(Object reportFile) {
        this.reportFile = fileOrNull(reportFile);
    }

    public File getJsonReportFile() {
        return jsonReportFile;
    }

    public void setJsonReportFile(Object jsonReportFile) {
        this.jsonReportFile = fileOrNull(jsonReportFile);
    }

    public File getJunitXmlReportFile() {
        return junitXmlReportFile;
    }

    public File getJUnitXmlReportFile() {
        return junitXmlReportFile;
    }

    public void setJunitXmlReportFile(Object junitXmlReportFile) {
        this.junitXmlReportFile = fileOrNull(junitXmlReportFile);
    }

    public void setJUnitXmlReportFile(Object junitXmlReportFile) {
        setJunitXmlReportFile(junitXmlReportFile);
    }

    public File getJunitXmlFile() {
        return junitXmlFile;
    }

    public File getJUnitXmlFile() {
        return junitXmlFile;
    }

    public void setJunitXmlFile(Object junitXmlFile) {
        this.junitXmlFile = fileOrNull(junitXmlFile);
    }

    public void setJUnitXmlFile(Object junitXmlFile) {
        setJunitXmlFile(junitXmlFile);
    }

    public File getCompileOutput() {
        return compileOutput;
    }

    public void setCompileOutput(Object compileOutput) {
        this.compileOutput = fileOrNull(compileOutput);
    }

    public List<String> getClassFilters() {
        return classFilters;
    }

    public void setClassFilters(Object classFilters) {
        this.classFilters.clear();
        addFilterObject(this.classFilters, classFilters);
    }

    public void classFilters(Object... classFilters) {
        addFilterObject(this.classFilters, classFilters);
    }

    public String getClassFiltersProperty() {
        return classFiltersProperty;
    }

    public void setClassFiltersProperty(String classFiltersProperty) {
        this.classFiltersProperty = classFiltersProperty;
    }

    public String getClassFilter() {
        return classFilter;
    }

    public void setClassFilter(String classFilter) {
        this.classFilter = classFilter;
    }

    public String getClassNameFilter() {
        return classNameFilter;
    }

    public void setClassNameFilter(String classNameFilter) {
        this.classNameFilter = classNameFilter;
    }

    public String getClassName() {
        return classNameFilter;
    }

    public void setClassName(String className) {
        this.classNameFilter = className;
    }

    public List<String> getExampleFilters() {
        return exampleFilters;
    }

    public void setExampleFilters(Object exampleFilters) {
        this.exampleFilters.clear();
        addFilterObject(this.exampleFilters, exampleFilters);
    }

    public void exampleFilters(Object... exampleFilters) {
        addFilterObject(this.exampleFilters, exampleFilters);
    }

    public String getExampleFiltersProperty() {
        return exampleFiltersProperty;
    }

    public void setExampleFiltersProperty(String exampleFiltersProperty) {
        this.exampleFiltersProperty = exampleFiltersProperty;
    }

    public String getExampleFilter() {
        return exampleFilter;
    }

    public void setExampleFilter(String exampleFilter) {
        this.exampleFilter = exampleFilter;
    }

    public String getExampleNameFilter() {
        return exampleNameFilter;
    }

    public void setExampleNameFilter(String exampleNameFilter) {
        this.exampleNameFilter = exampleNameFilter;
    }

    public String getExampleName() {
        return exampleNameFilter;
    }

    public void setExampleName(String exampleName) {
        this.exampleNameFilter = exampleName;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Object extensions) {
        this.extensions.clear();
        addFilterObject(this.extensions, extensions);
    }

    public void extensions(Object... extensions) {
        addFilterObject(this.extensions, extensions);
    }

    Boolean skipOption() {
        return skip;
    }

    Boolean failOnFailureOption() {
        return failOnFailure;
    }

    Boolean stopOnFailureOption() {
        return stopOnFailure;
    }

    Boolean bootstrapDiscoveryOption() {
        return bootstrapDiscovery;
    }

    Boolean compileOption() {
        return compile;
    }

    File configFileOption() {
        return configFile;
    }

    String suiteOption() {
        return suite;
    }

    File specDirOption() {
        return specDir;
    }

    File specRootOption() {
        return specRoot;
    }

    FileCollection classpathOption() {
        return classpath;
    }

    String formatterOption() {
        return formatter;
    }

    File reportFileOption() {
        return reportFile;
    }

    File jsonReportFileOption() {
        return jsonReportFile;
    }

    File junitXmlReportFileOption() {
        return junitXmlReportFile;
    }

    File junitXmlFileOption() {
        return junitXmlFile;
    }

    File compileOutputOption() {
        return compileOutput;
    }

    List<String> classFiltersOption() {
        return classFilters;
    }

    String classFiltersPropertyOption() {
        return classFiltersProperty;
    }

    String classFilterOption() {
        return classFilter;
    }

    String classNameFilterOption() {
        return classNameFilter;
    }

    List<String> exampleFiltersOption() {
        return exampleFilters;
    }

    List<String> extensionsOption() {
        return extensions;
    }

    String exampleFiltersPropertyOption() {
        return exampleFiltersProperty;
    }

    String exampleFilterOption() {
        return exampleFilter;
    }

    String exampleNameFilterOption() {
        return exampleNameFilter;
    }

    private File fileOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return project.file(value);
    }

    private FileCollection fileCollectionOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return project.files(value);
    }

    private static void addFilterObject(List<String> target, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Iterable<?>) {
            Iterable<?> iterable = (Iterable<?>) value;
            for (Object element : iterable) {
                addFilterObject(target, element);
            }
            return;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                addFilterObject(target, Array.get(value, i));
            }
            return;
        }
        target.add(String.valueOf(value));
    }
}
