package org.javaspec.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaspecRunMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runsCompiledSpecsFromInjectedTestClasspathAndWritesRequestedReports() throws Exception {
        CompiledSpecFixture fixture = compiledPassingFixture("PluginPassingSubject");
        File jsonFile = new File(temporaryFolder.getRoot(), "reports/json/passing-result.json");
        File xmlFile = new File(temporaryFolder.getRoot(), "reports/xml/passing-result.xml");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, fixture.specRoot, fixture.classesDirectory, log);
        set(mojo, "jsonReportFile", jsonFile);
        set(mojo, "junitXmlReportFile", xmlFile);

        mojo.execute();

        assertTrue(log.containsDebug("javaspec: using 1 Maven test classpath element(s)."));
        assertTrue(log.containsInfo("javaspec: running suite 'default' from " + fixture.specRoot.getPath() + "."));
        assertTrue(log.containsInfo("javaspec: found 1 specification(s)."));
        assertTrue(log.containsInfo("javaspec: examples total=1, passed=1, failed=0, broken=0, skipped=0, pending=0."));
        assertTrue(log.warnMessages.isEmpty());
        assertTrue(jsonFile.getParentFile().isDirectory());
        assertTrue(xmlFile.getParentFile().isDirectory());
        assertTrue(jsonFile.isFile());
        assertTrue(xmlFile.isFile());
        File specFile = sourceFileFor(fixture.specRoot, "spec.com.example.PluginPassingSubjectSpec");
        String json = readFile(jsonFile);
        assertContains(json, "\"status\": \"PASSED\"");
        assertPluginReportJsonMetadata(json, specFile, "it_uses_injected_test_classpath", 4);
        String xml = readFile(xmlFile);
        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"0\" time=\"0\">");
        assertPluginTestcaseHasSource(xml, specFile, "it_uses_injected_test_classpath", 4);
        assertParsesAsXml(xml);
    }

    @Test
    public void noSpecRunDoesNotFailBuild() throws Exception {
        File basedir = temporaryFolder.newFolder("no-spec-project");
        File specRoot = new File(basedir, "empty-specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(specRoot.mkdirs());
        assertTrue(classesDirectory.mkdirs());
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, specRoot, classesDirectory, log);

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: no specifications found."));
        assertTrue(log.containsInfo("javaspec: examples total=0, passed=0, failed=0, broken=0, skipped=0, pending=0."));
        assertTrue(log.warnMessages.isEmpty());
    }

    @Test
    public void skippedOnlyRunDoesNotFailBuildAndWarnsAboutExecutionAvailability() throws Exception {
        File basedir = temporaryFolder.newFolder("skipped-only-project");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        writeSpec(specRoot, "spec.com.example.MissingCompiledSubjectSpec",
                "    public void it_is_skipped_when_the_compiled_spec_class_is_absent() {\n" +
                        "    }\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, specRoot, classesDirectory, log);

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: found 1 specification(s)."));
        assertTrue(log.containsInfo("javaspec: examples total=1, passed=0, failed=0, broken=0, skipped=1, pending=0."));
        assertTrue(log.containsWarn("javaspec: Execution diagnostics:"));
        assertTrue(log.containsWarn("Specification spec.com.example.MissingCompiledSubjectSpec is not executable"));
        assertTrue(log.containsWarn("Specification class not found: spec.com.example.MissingCompiledSubjectSpec"));
        assertTrue(log.containsWarn("Maven test classpath contains 1 element(s)"));
        assertTrue(log.containsWarn("compiled test/spec classes and dependencies"));
        assertTrue(log.containsWarn("Maven test classpath"));
    }

    @Test
    public void pendingOnlyRunDoesNotFailBuildAndWritesPendingReports() throws Exception {
        CompiledSpecFixture fixture = compiledPendingFixture("PluginPendingSubject");
        File jsonFile = new File(temporaryFolder.getRoot(), "reports/json/pending-result.json");
        File xmlFile = new File(temporaryFolder.getRoot(), "reports/xml/pending-result.xml");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, fixture.specRoot, fixture.classesDirectory, log);
        set(mojo, "jsonReportFile", jsonFile);
        set(mojo, "junitXmlReportFile", xmlFile);

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: found 1 specification(s)."));
        assertTrue(log.containsInfo("javaspec: examples total=1, passed=0, failed=0, broken=0, skipped=0, pending=1."));
        assertTrue(log.warnMessages.isEmpty());
        String json = readFile(jsonFile);
        assertContains(json, "\"pending\": 1");
        assertContains(json, "\"status\": \"PENDING\"");
        assertContains(json, "\"detail\": \"plugin pending\"");
        String xml = readFile(xmlFile);
        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"1\" time=\"0\">");
        assertContains(xml, "<skipped message=\"Pending: plugin pending\"/>");
        assertParsesAsXml(xml);
    }

    @Test
    public void failedAndBrokenExamplesThrowByDefaultAfterReportsAreWritten() throws Exception {
        CompiledSpecFixture fixture = compiledFailureFixture("PluginFailingSubject");
        File jsonFile = new File(temporaryFolder.getRoot(), "failure-reports/json/result.json");
        File xmlFile = new File(temporaryFolder.getRoot(), "failure-reports/xml/result.xml");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, fixture.specRoot, fixture.classesDirectory, log);
        set(mojo, "jsonReportFile", jsonFile);
        set(mojo, "junitXmlReportFile", xmlFile);

        try {
            mojo.execute();
            fail("Expected failed and broken examples to fail the Maven build by default");
        } catch (MojoFailureException expected) {
            assertContains(expected.getMessage(), "failed=1, broken=1");
        }

        assertTrue(log.containsInfo("javaspec: examples total=2, passed=0, failed=1, broken=1, skipped=0, pending=0."));
        assertTrue(jsonFile.isFile());
        assertTrue(xmlFile.isFile());
        String json = readFile(jsonFile);
        assertContains(json, "\"failed\": 1");
        assertContains(json, "\"broken\": 1");
        assertContains(json, "\"status\": \"FAILED\"");
        assertContains(json, "\"status\": \"BROKEN\"");
        String xml = readFile(xmlFile);
        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"2\" failures=\"1\" errors=\"1\" skipped=\"0\" time=\"0\">");
        assertContains(xml, "<failure type=\"java.lang.AssertionError\" message=\"plugin assertion failed\">Assertion failed");
        assertContains(xml, "<error type=\"java.lang.IllegalStateException\" message=\"plugin example broken\">Example method threw an unexpected throwable");
        assertParsesAsXml(xml);
    }

    @Test
    public void failOnFailureFalseLogsWarningsAndContinuesForFailedAndBrokenExamples() throws Exception {
        CompiledSpecFixture fixture = compiledFailureFixture("PluginWarnOnlySubject");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, fixture.specRoot, fixture.classesDirectory, log);
        set(mojo, "failOnFailure", Boolean.FALSE);

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: examples total=2, passed=0, failed=1, broken=1, skipped=0, pending=0."));
        assertTrue(log.containsWarn("javaspec: failed spec.com.example.PluginWarnOnlySubjectSpec#it_fails - Assertion failed"));
        assertTrue(log.containsWarn("javaspec: broken spec.com.example.PluginWarnOnlySubjectSpec#it_breaks - Example method threw an unexpected throwable"));
        assertTrue(log.containsWarn("javaspec found failed or broken examples: failed=1, broken=1."));
        assertTrue(log.containsWarn("javaspec: failOnFailure=false, Maven build will continue."));
    }

    @Test
    public void skipTrueReturnsWithoutExecutionOrReportCreation() throws Exception {
        CompiledSpecFixture fixture = compiledFailureFixture("PluginSkippedSubject");
        File jsonFile = new File(temporaryFolder.getRoot(), "skip-reports/result.json");
        File xmlFile = new File(temporaryFolder.getRoot(), "skip-reports/result.xml");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, fixture.specRoot, fixture.classesDirectory, log);
        set(mojo, "skip", Boolean.TRUE);
        set(mojo, "jsonReportFile", jsonFile);
        set(mojo, "junitXmlReportFile", xmlFile);

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: execution skipped."));
        assertFalse(log.containsInfo("javaspec: examples total="));
        assertFalse(jsonFile.exists());
        assertFalse(xmlFile.exists());
        assertFalse(jsonFile.getParentFile().exists());
    }

    @Test
    public void configurationSuiteSelectionAndFiltersControlExecutedExamples() throws Exception {
        File basedir = temporaryFolder.newFolder("configured-project");
        File specRoot = new File(basedir, "configured-specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File configFile = new File(basedir, "javaspec.conf");
        writeFile(configFile,
                "defaultSuite=default\n" +
                        "suite.default.specDir=unused-specs\n" +
                        "suite.selected.specDir=configured-specs\n" +
                        "suite.selected.specPackagePrefix=selectedspec\n" +
                        "suite.selected.packagePrefix=com.acme\n");
        File specSource = writeSpec(specRoot, "selectedspec.FilteredSubjectSpec",
                "    public void it_selected_example() {\n" +
                        "    }\n" +
                        "\n" +
                        "    public void it_rejected_example() {\n" +
                        "        throw new AssertionError(\"example filter was not applied\");\n" +
                        "    }\n");
        compile(classesDirectory, specSource);
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, null, classesDirectory, log);
        set(mojo, "configFile", configFile);
        set(mojo, "suite", "selected");
        set(mojo, "classNameFilter", "com.acme.FilteredSubject");
        set(mojo, "exampleFilter", "it_selected_example");

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: running suite 'selected' from " + specRoot.getPath() + "."));
        assertTrue(log.containsInfo("javaspec: found 1 specification(s)."));
        assertTrue(log.containsInfo("javaspec: examples total=1, passed=1, failed=0, broken=0, skipped=0, pending=0."));
        assertTrue(log.warnMessages.isEmpty());
    }

    @Test
    public void configurationReportDestinationsAreUsedWhenPluginReportParametersAreAbsent() throws Exception {
        File basedir = temporaryFolder.newFolder("configured-report-project");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(specRoot.mkdirs());
        assertTrue(classesDirectory.mkdirs());
        File configFile = new File(basedir, "javaspec.conf");
        File jsonReport = new File(basedir, "build/javaspec/configured-report.json");
        File junitXmlReport = new File(basedir, "build/javaspec/configured-report.xml");
        writeFile(configFile,
                "suite.default.specDir=specs\n" +
                        "report=build/javaspec/configured-report.json\n" +
                        "junitXml=build/javaspec/configured-report.xml\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, null, classesDirectory, log);
        set(mojo, "configFile", configFile);

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: no specifications found."));
        assertTrue(log.containsInfo("javaspec: wrote JSON report to " + jsonReport.getPath() + "."));
        assertTrue(log.containsInfo("javaspec: wrote JUnit XML report to " + junitXmlReport.getPath() + "."));
        assertTrue(jsonReport.isFile());
        assertTrue(junitXmlReport.isFile());
        assertContains(readFile(jsonReport), "\"total\": 0");
        assertContains(readFile(junitXmlReport), "tests=\"0\" failures=\"0\" errors=\"0\"");
    }

    @Test
    public void explicitReportParametersOverrideConfiguredReportDestinations() throws Exception {
        File basedir = temporaryFolder.newFolder("explicit-report-override-project");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(specRoot.mkdirs());
        assertTrue(classesDirectory.mkdirs());
        File configFile = new File(basedir, "javaspec.conf");
        File configuredJsonReport = new File(basedir, "build/javaspec/configured-override-report.json");
        File configuredJunitXmlReport = new File(basedir, "build/javaspec/configured-override-report.xml");
        File explicitJsonReport = new File(basedir, "build/javaspec/explicit-report.json");
        File explicitJunitXmlReport = new File(basedir, "build/javaspec/explicit-report.xml");
        writeFile(configFile,
                "suite.default.specDir=specs\n" +
                        "jsonReportFile=build/javaspec/configured-override-report.json\n" +
                        "junitXmlReportFile=build/javaspec/configured-override-report.xml\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, null, classesDirectory, log);
        set(mojo, "configFile", configFile);
        set(mojo, "reportFile", explicitJsonReport);
        set(mojo, "junitXmlFile", explicitJunitXmlReport);

        mojo.execute();

        assertTrue(explicitJsonReport.isFile());
        assertTrue(explicitJunitXmlReport.isFile());
        assertFalse(configuredJsonReport.exists());
        assertFalse(configuredJunitXmlReport.exists());
        assertTrue(log.containsInfo("javaspec: wrote JSON report to " + explicitJsonReport.getPath() + "."));
        assertTrue(log.containsInfo("javaspec: wrote JUnit XML report to " + explicitJunitXmlReport.getPath() + "."));
    }

    @Test
    public void duplicateReportAliasesInConfigurationThrowMojoExecutionException() throws Exception {
        assertDuplicateReportAliasRejected(
                "report=build/reports/one.json\n" +
                        "jsonReportFile=build/reports/two.json\n",
                "jsonReportFile"
        );
        assertDuplicateReportAliasRejected(
                "junitXml=build/reports/one.xml\n" +
                        "junitXmlReportFile=build/reports/two.xml\n",
                "junitXmlReportFile"
        );
    }

    @Test
    public void invalidConfigurationThrowsMojoExecutionException() throws Exception {
        File basedir = temporaryFolder.newFolder("invalid-config-project");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File configFile = new File(basedir, "javaspec.conf");
        writeFile(configFile, "unknown=value\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, null, classesDirectory, log);
        set(mojo, "configFile", configFile);

        try {
            mojo.execute();
            fail("Expected invalid configuration to throw MojoExecutionException");
        } catch (MojoExecutionException expected) {
            assertContains(expected.getMessage(), "Invalid javaspec configuration:");
            assertContains(expected.getMessage(), "Unknown configuration key: unknown");
        }
    }

    @Test
    public void invalidJsonReportPathThrowsMojoExecutionException() throws Exception {
        File basedir = temporaryFolder.newFolder("invalid-report-project");
        File specRoot = new File(basedir, "empty-specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(specRoot.mkdirs());
        assertTrue(classesDirectory.mkdirs());
        File blockingParent = new File(basedir, "not-a-directory");
        writeFile(blockingParent, "I am a file, not a directory.\n");
        File invalidReportFile = new File(blockingParent, "result.json");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, specRoot, classesDirectory, log);
        set(mojo, "jsonReportFile", invalidReportFile);

        try {
            mojo.execute();
            fail("Expected invalid report path to throw MojoExecutionException");
        } catch (MojoExecutionException expected) {
            assertContains(expected.getMessage(), "I/O error while writing javaspec JSON report:");
            assertContains(expected.getMessage(), blockingParent.getPath());
        }
    }

    @Test
    public void invalidJUnitXmlReportPathThrowsMojoExecutionException() throws Exception {
        File basedir = temporaryFolder.newFolder("invalid-xml-report-project");
        File specRoot = new File(basedir, "empty-specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(specRoot.mkdirs());
        assertTrue(classesDirectory.mkdirs());
        File blockingParent = new File(basedir, "not-a-directory");
        writeFile(blockingParent, "I am a file, not a directory.\n");
        File invalidReportFile = new File(blockingParent, "result.xml");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, specRoot, classesDirectory, log);
        set(mojo, "junitXmlReportFile", invalidReportFile);

        try {
            mojo.execute();
            fail("Expected invalid JUnit XML report path to throw MojoExecutionException");
        } catch (MojoExecutionException expected) {
            assertContains(expected.getMessage(), "I/O error while writing javaspec JUnit XML report:");
            assertContains(expected.getMessage(), blockingParent.getPath());
        }
    }

    @Test
    public void pluginPomKeepsMavenApiAndAnnotationsProvidedAndJUnitTestScoped() throws Exception {
        Document pom = readXml(pluginProjectFile("pom.xml"));

        assertEquals(null, directDependencyScope(pom, "org.javaspec", "javaspec"));
        assertEquals("provided", directDependencyScope(pom, "org.apache.maven", "maven-plugin-api"));
        assertEquals("provided", directDependencyScope(pom, "org.apache.maven.plugin-tools", "maven-plugin-annotations"));
        assertEquals("test", directDependencyScope(pom, "junit", "junit"));
    }

    @Test
    public void mojoImplementationDelegatesToCanonicalJavaspecLauncher() throws Exception {
        String source = readFile(pluginProjectFile("src/main/java/org/javaspec/maven/JavaspecRunMojo.java"));

        assertContains(source, "import org.javaspec.invocation.JavaspecLauncher;");
        assertContains(source, "JavaspecLauncher.run(invocation)");
        assertFalse("Mojo must not terminate Maven JVM execution", source.contains("System.exit"));
        assertFalse("Mojo must not bypass the canonical launcher with the low-level runner",
                source.contains("import org.javaspec.runner.SpecRunner;"));
    }

    private CompiledSpecFixture compiledPassingFixture(String describedSimpleName) throws Exception {
        File basedir = temporaryFolder.newFolder(describedSimpleName + "-project");
        File specRoot = new File(basedir, "specs");
        File compilationSourceRoot = new File(basedir, "compilation-sources");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File productionSource = sourceFileFor(compilationSourceRoot, "com.example." + describedSimpleName);
        File specSource = sourceFileFor(specRoot, "spec.com.example." + describedSimpleName + "Spec");

        writeFile(productionSource,
                "package com.example;\n\n" +
                        "public class " + describedSimpleName + " {\n" +
                        "    public String value() {\n" +
                        "        return \"ok\";\n" +
                        "    }\n" +
                        "}\n");
        writeFile(specSource,
                "package spec.com.example;\n\n" +
                        "public class " + describedSimpleName + "Spec {\n" +
                        "    public void it_uses_injected_test_classpath() {\n" +
                        "        if (!\"ok\".equals(new com.example." + describedSimpleName + "().value())) {\n" +
                        "            throw new AssertionError(\"injected Maven test classpath was not used\");\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");
        compile(classesDirectory, productionSource, specSource);
        return new CompiledSpecFixture(basedir, specRoot, classesDirectory);
    }

    private CompiledSpecFixture compiledPendingFixture(String describedSimpleName) throws Exception {
        File basedir = temporaryFolder.newFolder(describedSimpleName + "-project");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File specSource = writeSpec(specRoot, "spec.com.example." + describedSimpleName + "Spec",
                "    public void it_is_pending() {\n" +
                        "        throw pending(\"plugin pending\");\n" +
                        "    }\n" +
                        "\n" +
                        "    private static RuntimeException pending(String reason) {\n" +
                        "        try {\n" +
                        "            Class<?> type = Class.forName(\"org.javaspec.api.PendingExampleException\");\n" +
                        "            return (RuntimeException) type.getConstructor(String.class).newInstance(reason);\n" +
                        "        } catch (RuntimeException ex) {\n" +
                        "            throw ex;\n" +
                        "        } catch (Exception ex) {\n" +
                        "            throw new RuntimeException(ex);\n" +
                        "        }\n" +
                        "    }\n");
        compile(classesDirectory, specSource);
        return new CompiledSpecFixture(basedir, specRoot, classesDirectory);
    }

    private CompiledSpecFixture compiledFailureFixture(String describedSimpleName) throws Exception {
        File basedir = temporaryFolder.newFolder(describedSimpleName + "-project");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File specSource = writeSpec(specRoot, "spec.com.example." + describedSimpleName + "Spec",
                "    public void it_fails() {\n" +
                        "        throw new AssertionError(\"plugin assertion failed\");\n" +
                        "    }\n" +
                        "\n" +
                        "    public void it_breaks() {\n" +
                        "        throw new IllegalStateException(\"plugin example broken\");\n" +
                        "    }\n");
        compile(classesDirectory, specSource);
        return new CompiledSpecFixture(basedir, specRoot, classesDirectory);
    }

    private void assertDuplicateReportAliasRejected(String reportConfiguration, String duplicateKey) throws Exception {
        File basedir = temporaryFolder.newFolder("duplicate-" + duplicateKey + "-project");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File configFile = new File(basedir, "javaspec.conf");
        writeFile(configFile, "suite.default.specDir=specs\n" + reportConfiguration);
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, null, classesDirectory, log);
        set(mojo, "configFile", configFile);

        try {
            mojo.execute();
            fail("Expected duplicate report alias to throw MojoExecutionException");
        } catch (MojoExecutionException expected) {
            assertContains(expected.getMessage(), "Invalid javaspec configuration:");
            assertContains(expected.getMessage(), "Duplicate configuration key '" + duplicateKey + "'");
        }
    }

    private static JavaspecRunMojo mojo(File basedir, File specRoot, File classesDirectory, CapturingLog log) throws Exception {
        JavaspecRunMojo mojo = new JavaspecRunMojo();
        mojo.setLog(log);
        set(mojo, "basedir", basedir);
        set(mojo, "testClasspathElements", Collections.singletonList(classesDirectory.getAbsolutePath()));
        set(mojo, "failOnFailure", Boolean.TRUE);
        if (specRoot != null) {
            set(mojo, "specDir", specRoot);
        }
        return mojo;
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static File writeSpec(File specRoot, String specQualifiedName, String body) throws Exception {
        int lastDot = specQualifiedName.lastIndexOf('.');
        String packageName = lastDot < 0 ? "" : specQualifiedName.substring(0, lastDot);
        String simpleName = lastDot < 0 ? specQualifiedName : specQualifiedName.substring(lastDot + 1);
        File specFile = sourceFileFor(specRoot, specQualifiedName);
        if (packageName.length() == 0) {
            writeFile(specFile, "public class " + simpleName + " {\n" + body + "}\n");
        } else {
            writeFile(specFile, "package " + packageName + ";\n\n" +
                    "public class " + simpleName + " {\n" + body + "}\n");
        }
        return specFile;
    }

    private static File sourceFileFor(File root, String qualifiedName) {
        return new File(root, qualifiedName.replace('.', File.separatorChar) + ".java");
    }

    private static void writeFile(File file, String content) throws Exception {
        File parent = file.getParentFile();
        assertTrue(parent == null || parent.isDirectory() || parent.mkdirs());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static File pluginProjectFile(String relativePath) {
        File direct = new File(relativePath);
        if (direct.isFile()) {
            return direct;
        }
        File fromRepositoryRoot = new File("javaspec-maven-plugin", relativePath);
        if (fromRepositoryRoot.isFile()) {
            return fromRepositoryRoot;
        }
        return direct;
    }

    private static Document readXml(File file) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
    }

    private static String directDependencyScope(Document pom, String groupId, String artifactId) {
        NodeList dependencies = pom.getDocumentElement().getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Node node = dependencies.item(i);
            if (node instanceof Element) {
                Element dependency = (Element) node;
                if (groupId.equals(directChildText(dependency, "groupId"))
                        && artifactId.equals(directChildText(dependency, "artifactId"))) {
                    return directChildText(dependency, "scope");
                }
            }
        }
        fail("Expected direct dependency " + groupId + ":" + artifactId + " in plugin POM");
        return null;
    }

    private static String directChildText(Element parent, String childName) {
        Element child = directChild(parent, childName);
        if (child == null) {
            return null;
        }
        return child.getTextContent().trim();
    }

    private static Element directChild(Element parent, String childName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && childName.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static void compile(File outputDirectory, File... sources) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("These tests require a JDK with javax.tools.JavaCompiler", compiler);
        List<String> arguments = new ArrayList<String>();
        arguments.add("-d");
        arguments.add(outputDirectory.getAbsolutePath());
        arguments.add("-source");
        arguments.add("1.8");
        arguments.add("-target");
        arguments.add("1.8");
        for (int i = 0; i < sources.length; i++) {
            arguments.add(sources[i].getAbsolutePath());
        }
        ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
        int exitCode = compiler.run(null, compilerOutput, compilerOutput, arguments.toArray(new String[arguments.size()]));
        assertEquals(new String(compilerOutput.toByteArray(), StandardCharsets.UTF_8), 0, exitCode);
    }

    private static void assertParsesAsXml(String xml) throws Exception {
        parseXml(xml);
    }

    private static void assertPluginTestcaseHasSource(String xml, File specFile, String methodName, int lineNumber) throws Exception {
        Element testcase = singleTestcase(xml);
        assertEquals("spec.com.example.PluginPassingSubjectSpec", testcase.getAttribute("classname"));
        assertEquals(methodName, testcase.getAttribute("name"));
        assertEquals("0", testcase.getAttribute("time"));
        assertEquals(specFile.getPath(), testcase.getAttribute("file"));
        assertEquals(String.valueOf(lineNumber), testcase.getAttribute("line"));
    }

    private static Element singleTestcase(String xml) throws Exception {
        Document document = parseXml(xml);
        NodeList testcases = document.getElementsByTagName("testcase");
        assertEquals(1, testcases.getLength());
        return (Element) testcases.item(0);
    }

    private static Document parseXml(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );
    }

    private static void assertPluginReportJsonMetadata(String json, File specFile, String methodName, int lineNumber) {
        String specName = "spec.com.example.PluginPassingSubjectSpec";
        String exampleId = specName + "#" + methodName;
        assertContains(json, "\"id\": " + jsonString(specName));
        assertContains(json, "\"stableId\": " + jsonString(specName));
        assertContains(json, "\"sourceFile\": " + jsonString(specFile.getPath()));
        assertContains(json, "\"id\": " + jsonString(exampleId));
        assertContains(json, "\"stableId\": " + jsonString(exampleId));
        assertContains(json, "\"fullName\": " + jsonString(exampleId));
        assertContains(json, "\"source\": {");
        assertContains(json, "\"file\": " + jsonString(specFile.getPath()));
        assertContains(json, "\"line\": " + lineNumber);
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void assertContains(String value, String expected) {
        assertTrue("Expected to contain: " + expected + "\nActual value:\n" + value, value.contains(expected));
    }

    private static final class CompiledSpecFixture {
        private final File basedir;
        private final File specRoot;
        private final File classesDirectory;

        private CompiledSpecFixture(File basedir, File specRoot, File classesDirectory) {
            this.basedir = basedir;
            this.specRoot = specRoot;
            this.classesDirectory = classesDirectory;
        }
    }

    private static final class CapturingLog implements Log {
        private final List<String> debugMessages = new ArrayList<String>();
        private final List<String> infoMessages = new ArrayList<String>();
        private final List<String> warnMessages = new ArrayList<String>();
        private final List<String> errorMessages = new ArrayList<String>();

        public boolean isDebugEnabled() {
            return true;
        }

        public void debug(CharSequence content) {
            debugMessages.add(stringValue(content));
        }

        public void debug(CharSequence content, Throwable error) {
            debugMessages.add(stringValue(content) + " " + stringValue(error));
        }

        public void debug(Throwable error) {
            debugMessages.add(stringValue(error));
        }

        public boolean isInfoEnabled() {
            return true;
        }

        public void info(CharSequence content) {
            infoMessages.add(stringValue(content));
        }

        public void info(CharSequence content, Throwable error) {
            infoMessages.add(stringValue(content) + " " + stringValue(error));
        }

        public void info(Throwable error) {
            infoMessages.add(stringValue(error));
        }

        public boolean isWarnEnabled() {
            return true;
        }

        public void warn(CharSequence content) {
            warnMessages.add(stringValue(content));
        }

        public void warn(CharSequence content, Throwable error) {
            warnMessages.add(stringValue(content) + " " + stringValue(error));
        }

        public void warn(Throwable error) {
            warnMessages.add(stringValue(error));
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public void error(CharSequence content) {
            errorMessages.add(stringValue(content));
        }

        public void error(CharSequence content, Throwable error) {
            errorMessages.add(stringValue(content) + " " + stringValue(error));
        }

        public void error(Throwable error) {
            errorMessages.add(stringValue(error));
        }

        private boolean containsDebug(String fragment) {
            return contains(debugMessages, fragment);
        }

        private boolean containsInfo(String fragment) {
            return contains(infoMessages, fragment);
        }

        private boolean containsWarn(String fragment) {
            return contains(warnMessages, fragment);
        }

        private static boolean contains(List<String> messages, String fragment) {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).contains(fragment)) {
                    return true;
                }
            }
            return false;
        }

        private static String stringValue(CharSequence content) {
            return content == null ? "null" : content.toString();
        }

        private static String stringValue(Throwable error) {
            return error == null ? "null" : error.toString();
        }
    }
}
