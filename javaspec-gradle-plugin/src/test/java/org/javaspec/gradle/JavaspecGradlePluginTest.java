package org.javaspec.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JavaspecGradlePluginTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void registersJavaspecRunTaskInVerificationGroup() {
        Project project = ProjectBuilder.builder().build();

        project.getPluginManager().apply(JavaspecPlugin.class);
        project.getPluginManager().apply(JavaPlugin.class);

        Task task = project.getTasks().findByName(JavaspecPlugin.TASK_NAME);
        assertNotNull(task);
        assertTrue(task instanceof JavaspecRunTask);
        assertEquals("verification", task.getGroup());
        assertTrue(task.getDependsOn().contains("testClasses"));
    }

    @Test
    public void pluginIdRegistersTaskAndNoSpecRunSucceeds() throws Exception {
        File projectDir = newProject("no-spec-success");
        writeBuildFile(projectDir, javaPluginBuild(""));

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: no specifications found.");
        assertContains(result.getOutput(), "javaspec: Examples: 0 total, 0 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
    }

    @Test
    public void compiledPassingSpecRunsThroughDefaultTestSourceSetRuntimeClasspathAndWritesReports() throws Exception {
        File projectDir = newProject("passing-default-classpath");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    jsonReportFile = file('build/reports/javaspec/passing.json')\n" +
                        "    junitXmlReportFile = file('build/reports/javaspec/passing.xml')\n" +
                        "}\n"
        ));
        writeJavaSource(projectDir, "src/main/java/com/example/Greeter.java",
                "package com.example;\n\n" +
                        "public class Greeter {\n" +
                        "    public String message() {\n" +
                        "        return \"hello\";\n" +
                        "    }\n" +
                        "}\n");
        writeJavaSource(projectDir, "src/test/java/spec/com/example/GreeterSpec.java",
                "package spec.com.example;\n\n" +
                        "public class GreeterSpec {\n" +
                        "    public void it_uses_main_output_on_default_test_runtime_classpath() {\n" +
                        "        if (!\"hello\".equals(new com.example.Greeter().message())) {\n" +
                        "            throw new AssertionError(\"default test runtime classpath was not used\");\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: found 1 specification(s).");
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        File specFile = new File(projectDir, "src/test/java/spec/com/example/GreeterSpec.java");
        String json = readFile(new File(projectDir, "build/reports/javaspec/passing.json"));
        assertContains(json, "\"total\": 1");
        assertContains(json, "\"status\": \"PASSED\"");
        assertGradleReportJsonMetadata(json, specFile, "it_uses_main_output_on_default_test_runtime_classpath", 4);
        String xml = readFile(new File(projectDir, "build/reports/javaspec/passing.xml"));
        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"0\" time=\"0\">");
        assertGradleTestcaseHasSource(xml, specFile, "it_uses_main_output_on_default_test_runtime_classpath", 4);
        assertParsesAsXml(xml);
    }

    @Test
    public void pendingExamplesDoNotFailBuildAndAreWrittenToReports() throws Exception {
        File projectDir = newProject("pending-example");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    jsonReportFile = file('build/reports/javaspec/pending.json')\n" +
                        "    junitXmlReportFile = file('build/reports/javaspec/pending.xml')\n" +
                        "}\n"
        ));
        writeJavaSource(projectDir, "src/test/java/spec/com/example/PendingSpec.java",
                "package spec.com.example;\n\n" +
                        "public class PendingSpec {\n" +
                        "    public void it_is_pending() {\n" +
                        "        throw pending(\"gradle pending\");\n" +
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
                        "    }\n" +
                        "}\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 0 passed, 0 failed, 0 broken, 0 skipped, 1 pending.");
        assertContains(result.getOutput(), "javaspec: Pending examples:");
        assertContains(result.getOutput(), "javaspec:   PENDING spec.com.example.PendingSpec#it_is_pending (it is pending): gradle pending");
        String json = readFile(new File(projectDir, "build/reports/javaspec/pending.json"));
        assertContains(json, "\"pending\": 1");
        assertContains(json, "\"status\": \"PENDING\"");
        assertContains(json, "\"detail\": \"gradle pending\"");
        String xml = readFile(new File(projectDir, "build/reports/javaspec/pending.xml"));
        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"1\" time=\"0\">");
        assertContains(xml, "<skipped message=\"Pending: gradle pending\"/>");
        assertParsesAsXml(xml);
    }

    @Test
    public void failedAndBrokenExamplesFailByDefaultAfterReportsAreWritten() throws Exception {
        File projectDir = failingExamplesProject("failed-and-broken-default", "");

        BuildResult result = runGradleAndFail(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.FAILED, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec found failed or broken examples: failed=1, broken=1.");
        String json = readFile(new File(projectDir, "build/reports/javaspec/failures.json"));
        assertContains(json, "\"failed\": 1");
        assertContains(json, "\"broken\": 1");
        assertContains(json, "\"status\": \"FAILED\"");
        assertContains(json, "\"status\": \"BROKEN\"");
        String xml = readFile(new File(projectDir, "build/reports/javaspec/failures.xml"));
        assertContains(xml, "tests=\"2\" failures=\"1\" errors=\"1\"");
        assertContains(xml, "<failure type=\"java.lang.AssertionError\" message=\"expected failure\">");
        assertContains(xml, "<error type=\"java.lang.IllegalStateException\" message=\"expected broken\">");
        assertParsesAsXml(xml);
    }

    @Test
    public void failOnFailureFalseContinuesAfterFailedAndBrokenExamplesAndWritesReports() throws Exception {
        File projectDir = failingExamplesProject("fail-on-failure-false", "    failOnFailure = false\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec found failed or broken examples: failed=1, broken=1.");
        assertContains(result.getOutput(), "javaspec: failOnFailure=false, Gradle build will continue.");
        assertTrue(new File(projectDir, "build/reports/javaspec/failures.json").isFile());
        assertTrue(new File(projectDir, "build/reports/javaspec/failures.xml").isFile());
    }

    @Test
    public void skipTrueAvoidsExecutionAndReportWrites() throws Exception {
        File projectDir = newProject("skip-true");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    skip = true\n" +
                        "    jsonReportFile = file('build/reports/javaspec/skipped.json')\n" +
                        "    junitXmlReportFile = file('build/reports/javaspec/skipped.xml')\n" +
                        "}\n"
        ));
        writeFailingSpec(projectDir, "src/test/java/spec/com/example/SkipGuardSpec.java", "spec.com.example", "SkipGuardSpec");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: execution skipped.");
        assertFalse(new File(projectDir, "build/reports/javaspec/skipped.json").exists());
        assertFalse(new File(projectDir, "build/reports/javaspec/skipped.xml").exists());
    }

    @Test
    public void configSuiteClassAndExampleFiltersSelectOnlyMatchingExamplesFromCustomSpecRoot() throws Exception {
        File projectDir = newProject("config-suite-filters");
        writeBuildFile(projectDir, javaPluginBuild(
                "sourceSets {\n" +
                        "    test {\n" +
                        "        java.srcDirs = ['src/customSpec/java', 'src/ignoredSpec/java']\n" +
                        "    }\n" +
                        "}\n" +
                        "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        "    suite = 'selected'\n" +
                        "    classFilter = 'app.Target'\n" +
                        "    exampleFilter = 'it_selected'\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "formatter = progress\n" +
                        "defaultSuite = ignored\n" +
                        "suite.ignored.specDir = src/ignoredSpec/java\n" +
                        "suite.ignored.specPackagePrefix = ignored\n" +
                        "suite.ignored.packagePrefix = app\n" +
                        "suite.selected.specDir = src/customSpec/java\n" +
                        "suite.selected.specPackagePrefix = contract\n" +
                        "suite.selected.packagePrefix = app\n");
        writeJavaSource(projectDir, "src/customSpec/java/contract/TargetSpec.java",
                "package contract;\n\n" +
                        "public class TargetSpec {\n" +
                        "    public void it_selected() {\n" +
                        "    }\n" +
                        "    public void it_rejected_example_fails_if_filter_is_ignored() {\n" +
                        "        throw new AssertionError(\"example filter ignored\");\n" +
                        "    }\n" +
                        "}\n");
        writeJavaSource(projectDir, "src/customSpec/java/contract/OtherSpec.java",
                "package contract;\n\n" +
                        "public class OtherSpec {\n" +
                        "    public void it_selected() {\n" +
                        "        throw new AssertionError(\"class filter ignored\");\n" +
                        "    }\n" +
                        "}\n");
        writeJavaSource(projectDir, "src/ignoredSpec/java/ignored/IgnoredSuiteSpec.java",
                "package ignored;\n\n" +
                        "public class IgnoredSuiteSpec {\n" +
                        "    public void it_selected() {\n" +
                        "        throw new AssertionError(\"suite selection ignored\");\n" +
                        "    }\n" +
                        "}\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: running suite 'selected'");
        assertContains(result.getOutput(), "javaspec: found 1 specification(s).");
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertFalse(result.getOutput().contains("filter ignored"));
        assertFalse(result.getOutput().contains("suite selection ignored"));
    }

    @Test
    public void explicitSpecRootAliasRunsSpecsOutsideDefaultSpecDirectory() throws Exception {
        File projectDir = newProject("explicit-spec-root");
        writeBuildFile(projectDir, javaPluginBuild(
                "sourceSets {\n" +
                        "    test {\n" +
                        "        java.srcDir 'specifications'\n" +
                        "    }\n" +
                        "}\n" +
                        "javaspec {\n" +
                        "    specRoot = file('specifications')\n" +
                        "}\n"
        ));
        writeJavaSource(projectDir, "specifications/spec/com/example/ExternalSpec.java",
                "package spec.com.example;\n\n" +
                        "public class ExternalSpec {\n" +
                        "    public void it_runs_from_explicit_spec_root() {\n" +
                        "    }\n" +
                        "}\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: found 1 specification(s).");
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
    }

    @Test
    public void invalidConfigFileFailsWithDiagnostics() throws Exception {
        File projectDir = newProject("invalid-config");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    configFile = file('bad-javaspec.conf')\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "bad-javaspec.conf"), "unknown = value\n");

        BuildResult result = runGradleAndFail(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.FAILED, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "Invalid javaspec configuration:");
        assertContains(result.getOutput(), "Unknown configuration key: unknown.");
        assertContains(result.getOutput(), "bad-javaspec.conf");
    }

    @Test
    public void invalidReportPathFailsWithDiagnostics() throws Exception {
        File projectDir = newProject("invalid-report-path");
        File directoryReportPath = new File(projectDir, "build/reports/javaspec/directory-report.json");
        assertTrue(directoryReportPath.mkdirs());
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    jsonReportFile = file('build/reports/javaspec/directory-report.json')\n" +
                        "}\n"
        ));

        BuildResult result = runGradleAndFail(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.FAILED, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: no specifications found.");
        assertContains(result.getOutput(), "I/O error while writing javaspec JSON report:");
        assertContains(result.getOutput(), "directory-report.json");
    }

    @Test
    public void gradleTaskUsesCanonicalLauncherAndDoesNotCallSystemExit() throws Exception {
        String source = readFile(new File("src/main/java/org/javaspec/gradle/JavaspecRunTask.java"));

        assertContains(source, "import org.javaspec.invocation.JavaspecLauncher;");
        assertContains(source, "JavaspecLauncher.run(invocation)");
        assertFalse(source.contains("System.exit"));
    }

    private File failingExamplesProject(String projectName, String extraJavaspecConfiguration) throws Exception {
        File projectDir = newProject(projectName);
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        extraJavaspecConfiguration +
                        "    jsonReportFile = file('build/reports/javaspec/failures.json')\n" +
                        "    junitXmlReportFile = file('build/reports/javaspec/failures.xml')\n" +
                        "}\n"
        ));
        writeJavaSource(projectDir, "src/test/java/spec/com/example/FailureSpec.java",
                "package spec.com.example;\n\n" +
                        "public class FailureSpec {\n" +
                        "    public void it_fails() {\n" +
                        "        throw new AssertionError(\"expected failure\");\n" +
                        "    }\n" +
                        "    public void it_breaks() {\n" +
                        "        throw new IllegalStateException(\"expected broken\");\n" +
                        "    }\n" +
                        "}\n");
        return projectDir;
    }

    private static String javaPluginBuild(String extraConfiguration) {
        return "plugins {\n" +
                "    id 'org.javaspec'\n" +
                "    id 'java'\n" +
                "}\n" +
                "\n" +
                "tasks.withType(JavaCompile).configureEach {\n" +
                "    sourceCompatibility = '1.8'\n" +
                "    targetCompatibility = '1.8'\n" +
                "    options.encoding = 'UTF-8'\n" +
                "}\n" +
                "\n" +
                extraConfiguration;
    }

    private File newProject(String name) throws IOException {
        File projectDir = temporaryFolder.newFolder(name);
        writeFile(new File(projectDir, "settings.gradle"), "rootProject.name = '" + name + "'\n");
        return projectDir;
    }

    private static void writeBuildFile(File projectDir, String content) throws IOException {
        writeFile(new File(projectDir, "build.gradle"), content);
    }

    private static void writeFailingSpec(File projectDir, String relativePath, String packageName, String simpleName) throws IOException {
        writeJavaSource(projectDir, relativePath,
                "package " + packageName + ";\n\n" +
                        "public class " + simpleName + " {\n" +
                        "    public void it_would_fail_if_executed() {\n" +
                        "        throw new AssertionError(\"skip did not avoid execution\");\n" +
                        "    }\n" +
                        "}\n");
    }

    private static void writeJavaSource(File projectDir, String relativePath, String content) throws IOException {
        writeFile(new File(projectDir, relativePath), content);
    }

    private static void writeFile(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create directory " + parent.getAbsolutePath());
        }
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static BuildResult runGradle(File projectDir, String... arguments) {
        return gradleRunner(projectDir, arguments).build();
    }

    private static BuildResult runGradleAndFail(File projectDir, String... arguments) {
        return gradleRunner(projectDir, arguments).buildAndFail();
    }

    private static GradleRunner gradleRunner(File projectDir, String... arguments) {
        String[] effectiveArguments = new String[arguments.length + 1];
        System.arraycopy(arguments, 0, effectiveArguments, 0, arguments.length);
        effectiveArguments[arguments.length] = "--stacktrace";
        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments(effectiveArguments);
    }

    private static void assertParsesAsXml(String xml) throws Exception {
        parseXml(xml);
    }

    private static void assertGradleTestcaseHasSource(String xml, File specFile, String methodName, int lineNumber) throws Exception {
        Element testcase = singleTestcase(xml);
        assertEquals("spec.com.example.GreeterSpec", testcase.getAttribute("classname"));
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
                new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );
    }

    private static void assertGradleReportJsonMetadata(String json, File specFile, String methodName, int lineNumber) {
        String specName = "spec.com.example.GreeterSpec";
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
}
