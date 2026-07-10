package io.github.jvmspec.gradle;

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
    public void configurationReportDestinationsAreUsedWhenTaskAndExtensionReportsAreAbsent() throws Exception {
        File projectDir = newProject("config-report-destinations");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "suite.default.specDir = src/test/java\n" +
                        "report = build/reports/javaspec/configured.json\n" +
                        "junitXml = build/reports/javaspec/configured.xml\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: no specifications found.");
        File jsonReport = new File(projectDir, "build/reports/javaspec/configured.json");
        File junitXmlReport = new File(projectDir, "build/reports/javaspec/configured.xml");
        assertTrue(jsonReport.isFile());
        assertTrue(junitXmlReport.isFile());
        assertContains(readFile(jsonReport), "\"total\": 0");
        assertContains(readFile(junitXmlReport), "tests=\"0\" failures=\"0\" errors=\"0\"");
    }

    @Test
    public void extensionReportSettingsOverrideConfiguredReportDestinations() throws Exception {
        File projectDir = newProject("extension-report-overrides-config");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        "    jsonReportFile = file('build/reports/javaspec/extension.json')\n" +
                        "    junitXmlReportFile = file('build/reports/javaspec/extension.xml')\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "suite.default.specDir = src/test/java\n" +
                        "jsonReportFile = build/reports/javaspec/configured.json\n" +
                        "junitXmlReportFile = build/reports/javaspec/configured.xml\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertTrue(new File(projectDir, "build/reports/javaspec/extension.json").isFile());
        assertTrue(new File(projectDir, "build/reports/javaspec/extension.xml").isFile());
        assertFalse(new File(projectDir, "build/reports/javaspec/configured.json").exists());
        assertFalse(new File(projectDir, "build/reports/javaspec/configured.xml").exists());
    }

    @Test
    public void taskReportSettingsOverrideExtensionAndConfiguredReportDestinations() throws Exception {
        File projectDir = newProject("task-report-overrides-extension");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        "    reportFile = file('build/reports/javaspec/extension.json')\n" +
                        "    junitXmlFile = file('build/reports/javaspec/extension.xml')\n" +
                        "}\n" +
                        "tasks.named('javaspecRun') {\n" +
                        "    reportFile = file('build/reports/javaspec/task.json')\n" +
                        "    junitXmlFile = file('build/reports/javaspec/task.xml')\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "suite.default.specDir = src/test/java\n" +
                        "report = build/reports/javaspec/configured.json\n" +
                        "junit-xml = build/reports/javaspec/configured.xml\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertTrue(new File(projectDir, "build/reports/javaspec/task.json").isFile());
        assertTrue(new File(projectDir, "build/reports/javaspec/task.xml").isFile());
        assertFalse(new File(projectDir, "build/reports/javaspec/extension.json").exists());
        assertFalse(new File(projectDir, "build/reports/javaspec/extension.xml").exists());
        assertFalse(new File(projectDir, "build/reports/javaspec/configured.json").exists());
        assertFalse(new File(projectDir, "build/reports/javaspec/configured.xml").exists());
    }

    @Test
    public void duplicateReportAliasesInConfigurationFailGradleTask() throws Exception {
        assertDuplicateReportAliasFailsGradleBuild(
                "duplicate-json-report-alias",
                "report = build/reports/one.json\n" +
                        "jsonReportFile = build/reports/two.json\n",
                "jsonReportFile"
        );
        assertDuplicateReportAliasFailsGradleBuild(
                "duplicate-junit-report-alias",
                "junitXml = build/reports/one.xml\n" +
                        "junitXmlReportFile = build/reports/two.xml\n",
                "junitXmlReportFile"
        );
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
        assertReportJsonDefaultMetadata(json);
        assertGradleReportJsonMetadata(json, specFile, "it_uses_main_output_on_default_test_runtime_classpath", 4);
        String xml = readFile(new File(projectDir, "build/reports/javaspec/passing.xml"));
        assertJunitXmlSuiteMetadata(xml, 1, 0, 0, 0);
        assertGradleTestcaseHasSource(xml, specFile, "it_uses_main_output_on_default_test_runtime_classpath", 4);
        assertParsesAsXml(xml);
    }

    @Test
    public void canonicalPhpspecStyleSpecRunsThroughDefaultTestSourceSetRuntimeClasspath() throws Exception {
        File projectDir = newProject("phase46-canonical-phpspec-style");
        writeBuildFile(projectDir, javaPluginBuild(
                "repositories {\n" +
                        "    mavenLocal()\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "dependencies {\n" +
                        "    testImplementation 'io.github.jvmspec:javaspec:1.0.0-RC1'\n" +
                        "}\n" +
                        "javaspec {\n" +
                        "    jsonReportFile = file('build/reports/javaspec/phase46.json')\n" +
                        "    junitXmlReportFile = file('build/reports/javaspec/phase46.xml')\n" +
                        "}\n"
        ));
        writeJavaSource(projectDir, "src/main/java/com/example/Phase46Greeting.java",
                "package com.example;\n\n" +
                        "public class Phase46Greeting {\n" +
                        "    private final String name;\n" +
                        "\n" +
                        "    public Phase46Greeting(String name) { this.name = name; }\n" +
                        "\n" +
                        "    public String message() { return \"Hello \" + name; }\n" +
                        "}\n");
        writeJavaSource(projectDir, "src/test/java/spec/com/example/Phase46GreetingSpec.java",
                "package spec.com.example;\n\n" +
                        "import com.example.Phase46Greeting;\n" +
                        "import io.github.jvmspec.api.ObjectBehavior;\n" +
                        "\n" +
                        "public class Phase46GreetingSpec extends ObjectBehavior<Phase46Greeting> {\n" +
                        "    public Phase46GreetingSpec() { super(Phase46Greeting.class); }\n" +
                        "\n" +
                        "    public void let() { beConstructedWith(\"Ada\"); }\n" +
                        "\n" +
                        "    public void it_greets_the_configured_subject() {\n" +
                        "        match(subject().message()).shouldReturn(\"Hello Ada\");\n" +
                        "    }\n" +
                        "}\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: found 1 specification(s).");
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        String json = readFile(new File(projectDir, "build/reports/javaspec/phase46.json"));
        assertContains(json, "\"stableId\": \"spec.com.example.Phase46GreetingSpec#it_greets_the_configured_subject\"");
        String xml = readFile(new File(projectDir, "build/reports/javaspec/phase46.xml"));
        assertContains(xml, "classname=\"spec.com.example.Phase46GreetingSpec\"");
        assertContains(xml, "name=\"it_greets_the_configured_subject\"");
    }

    @Test
    public void uncompiledSpecSourceLogsExecutionDiagnosticsForDefaultTestRuntimeClasspath() throws Exception {
        File projectDir = newProject("uncompiled-spec-diagnostics");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    specDir = file('specifications')\n" +
                        "}\n"
        ));
        writeJavaSource(projectDir, "specifications/spec/com/example/UncompiledSpec.java",
                "package spec.com.example;\n\n" +
                        "public class UncompiledSpec {\n" +
                        "    public void it_is_discovered_but_not_compiled() {\n" +
                        "    }\n" +
                        "}\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: found 1 specification(s).");
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 0 passed, 0 failed, 0 broken, 1 skipped, 0 pending.");
        assertContains(result.getOutput(), "javaspec: Execution diagnostics:");
        assertContains(result.getOutput(), "Specification spec.com.example.UncompiledSpec is not executable");
        assertContains(result.getOutput(), "Specification class not found: spec.com.example.UncompiledSpec");
        assertContains(result.getOutput(), "Gradle classpath contains");
        assertContains(result.getOutput(), "element(s); this task needs compiled spec classes and dependencies on its configured/default test runtime classpath.");
    }

    @Test
    public void dslTaskAndProjectPropertyCompileOptInsCompileAndExecuteSourceOnlySpec() throws Exception {
        assertGradleCompilationOptIn(
                "phase34-dsl-compile",
                "Phase34DslCompileSubject",
                "    compile = true\n",
                ""
        );
        assertGradleCompilationOptIn(
                "phase34-task-compile",
                "Phase34TaskCompileSubject",
                "",
                "tasks.named('javaspecRun') {\n" +
                        "    compile = true\n" +
                        "}\n"
        );
        assertGradleCompilationOptIn(
                "phase34-property-compile",
                "Phase34PropertyCompileSubject",
                "",
                "",
                "-Pjavaspec.compile=true"
        );
    }

    @Test
    public void compileOutputOverridesImplyCompilationAndUseTaskExtensionProjectPrecedence() throws Exception {
        File extensionProject = sourceOnlyCompilationProject(
                "phase34-extension-compile-output",
                "Phase34ExtensionOutputSubject",
                "    compileOutput = file('extension-javaspec-classes')\n",
                ""
        );

        BuildResult extensionResult = runGradle(extensionProject, "javaspecRun");

        File extensionOutput = new File(extensionProject, "extension-javaspec-classes");
        assertEquals(TaskOutcome.SUCCESS, extensionResult.task(":javaspecRun").getOutcome());
        assertContains(extensionResult.getOutput(), "javaspec: compiled 2 source file(s) to " + extensionOutput.getPath() + ".");
        assertTrue(classFileFor(extensionOutput, "spec.com.example.Phase34ExtensionOutputSubjectSpec").isFile());

        File propertyProject = sourceOnlyCompilationProject(
                "phase34-property-compile-output",
                "Phase34PropertyOutputSubject",
                "",
                ""
        );

        BuildResult propertyResult = runGradle(
                propertyProject,
                "javaspecRun",
                "-Pjavaspec.compileOutput=property-javaspec-classes"
        );

        File propertyOutput = new File(propertyProject, "property-javaspec-classes");
        assertEquals(TaskOutcome.SUCCESS, propertyResult.task(":javaspecRun").getOutcome());
        assertContains(propertyResult.getOutput(), "javaspec: compiled 2 source file(s) to " + propertyOutput.getPath() + ".");
        assertTrue(classFileFor(propertyOutput, "spec.com.example.Phase34PropertyOutputSubjectSpec").isFile());

        File precedenceProject = sourceOnlyCompilationProject(
                "phase34-compile-output-precedence",
                "Phase34PrecedenceOutputSubject",
                "    compileOutput = file('extension-javaspec-classes')\n",
                "tasks.named('javaspecRun') {\n" +
                        "    compileOutput = file('task-javaspec-classes')\n" +
                        "}\n"
        );

        BuildResult precedenceResult = runGradle(
                precedenceProject,
                "javaspecRun",
                "-Pjavaspec.compileOutput=property-javaspec-classes"
        );

        File taskOutput = new File(precedenceProject, "task-javaspec-classes");
        assertEquals(TaskOutcome.SUCCESS, precedenceResult.task(":javaspecRun").getOutcome());
        assertContains(precedenceResult.getOutput(), "javaspec: compiled 2 source file(s) to " + taskOutput.getPath() + ".");
        assertTrue(classFileFor(taskOutput, "spec.com.example.Phase34PrecedenceOutputSubjectSpec").isFile());
        assertFalse(new File(precedenceProject, "extension-javaspec-classes").exists());
        assertFalse(new File(precedenceProject, "property-javaspec-classes").exists());
    }

    @Test
    public void noSpecCompileOptInSkipsOutputCreation() throws Exception {
        File projectDir = newProject("phase34-no-spec-compile");
        File compileOutput = new File(projectDir, "build/no-spec-javaspec-classes");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        "    compile = true\n" +
                        "    compileOutput = file('build/no-spec-javaspec-classes')\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "suite.default.specDir = empty-specs\n" +
                        "suite.default.sourceDir = sources\n");
        assertTrue(new File(projectDir, "empty-specs").mkdirs());
        writeJavaSource(projectDir, "sources/com/example/BrokenNoSpecGradleSource.java",
                "package com.example;\n\n" +
                        "public class BrokenNoSpecGradleSource {\n" +
                        "    public void broken( {\n" +
                        "}\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: no specifications found.");
        assertFalse(result.getOutput().contains("javaspec: compiled "));
        assertFalse(compileOutput.exists());
    }

    @Test
    public void compilationFailureFailsBeforeReportsAndShowsDiagnostics() throws Exception {
        File projectDir = newProject("phase34-compilation-failure");
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        "    compile = true\n" +
                        "    jsonReportFile = file('build/reports/javaspec/compile-failure.json')\n" +
                        "    junitXmlReportFile = file('build/reports/javaspec/compile-failure.xml')\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "suite.default.specDir = specs\n" +
                        "suite.default.sourceDir = sources\n");
        writeJavaSource(projectDir, "sources/com/example/BrokenPhase34GradleSubject.java",
                "package com.example;\n\n" +
                        "public class BrokenPhase34GradleSubject {\n" +
                        "    public String message() {\n" +
                        "        return missingSymbol;\n" +
                        "    }\n" +
                        "}\n");
        writeJavaSource(projectDir, "specs/spec/com/example/BrokenPhase34GradleSubjectSpec.java",
                "package spec.com.example;\n\n" +
                        "public class BrokenPhase34GradleSubjectSpec {\n" +
                        "    public void it_would_run_after_successful_compilation() {\n" +
                        "    }\n" +
                        "}\n");

        BuildResult result = runGradleAndFail(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.FAILED, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec compilation failed: Compilation failed");
        assertContains(result.getOutput(), "BrokenPhase34GradleSubject.java");
        assertContains(result.getOutput(), "missingSymbol");
        assertFalse(result.getOutput().contains("javaspec: Examples:"));
        assertFalse(new File(projectDir, "build/reports/javaspec/compile-failure.json").exists());
        assertFalse(new File(projectDir, "build/reports/javaspec/compile-failure.xml").exists());
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
                        "            Class<?> type = Class.forName(\"io.github.jvmspec.api.PendingExampleException\");\n" +
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
        assertReportJsonDefaultMetadata(json);
        String xml = readFile(new File(projectDir, "build/reports/javaspec/pending.xml"));
        assertJunitXmlSuiteMetadata(xml, 1, 0, 0, 1);
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
    public void configurationBootstrapHooksExecuteBeforeExamplesAndCanChangeObservedBehavior() throws Exception {
        File projectDir = newProject("configuration-bootstrap-hooks");
        writeBuildFile(projectDir, javaPluginBuild(
                "repositories {\n" +
                        "    mavenLocal()\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    testImplementation 'io.github.jvmspec:javaspec:1.0.0-RC1'\n" +
                        "}\n" +
                        "\n" +
                        "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        "    suite = 'custom'\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "defaultSuite = custom\n" +
                        "suite.custom.specDir = src/test/java\n" +
                        "bootstrap = support.TopLevelBootstrapHook\n" +
                        "suite.custom.bootstrap = support.SuiteBootstrapHook\n");
        writeBootstrapSupportSources(projectDir);
        writeJavaSource(projectDir, "src/test/java/spec/com/example/BootstrapSubjectSpec.java",
                "package spec.com.example;\n\n" +
                        "public class BootstrapSubjectSpec {\n" +
                        "    public void it_observes_bootstrap_marker() {\n" +
                        "        support.BootstrapState.record(\"example:\" + support.BootstrapState.marker());\n" +
                        "        if (!\"top-level>suite\".equals(support.BootstrapState.marker())) {\n" +
                        "            throw new AssertionError(\"bootstrap marker was not applied before example: \" + support.BootstrapState.marker());\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: running suite 'custom'");
        assertContains(result.getOutput(), "javaspec: found 1 specification(s).");
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertEquals("top-level:1\nsuite:1\nexample:top-level>suite\n", readFile(bootstrapEventFile(projectDir)));
    }

    @Test
    public void bootstrapFailureFailsJavaspecRunTaskWithBootstrapWording() throws Exception {
        File projectDir = newProject("bootstrap-failure");
        writeBuildFile(projectDir, javaPluginBuild(
                "repositories {\n" +
                        "    mavenLocal()\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    testImplementation 'io.github.jvmspec:javaspec:1.0.0-RC1'\n" +
                        "}\n" +
                        "\n" +
                        "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        "    jsonReportFile = file('build/reports/javaspec/bootstrap-failure.json')\n" +
                        "    junitXmlReportFile = file('build/reports/javaspec/bootstrap-failure.xml')\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "suite.default.specDir = src/test/java\n" +
                        "bootstrap = support.FailingBootstrapHook\n");
        writeBootstrapSupportSources(projectDir);
        writeJavaSource(projectDir, "src/test/java/spec/com/example/FailingBootstrapSubjectSpec.java",
                "package spec.com.example;\n\n" +
                        "public class FailingBootstrapSubjectSpec {\n" +
                        "    public void it_would_run_after_bootstrap() {\n" +
                        "    }\n" +
                        "}\n");

        BuildResult result = runGradleAndFail(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.FAILED, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec bootstrap execution failed:");
        assertContains(result.getOutput(), "Bootstrap hook 'support.FailingBootstrapHook' threw an exception");
        assertContains(result.getOutput(), "phase 27 Gradle bootstrap failure");
        assertFalse(result.getOutput().contains("javaspec: Examples:"));
        assertEquals("failing:1\n", readFile(bootstrapEventFile(projectDir)));
        assertFalse(new File(projectDir, "build/reports/javaspec/bootstrap-failure.json").exists());
        assertFalse(new File(projectDir, "build/reports/javaspec/bootstrap-failure.xml").exists());
    }

    @Test
    public void dslTaskAndProjectPropertyBootstrapDiscoveryOptInsExecuteServiceLoaderHook() throws Exception {
        assertBootstrapDiscoveryOptIn(
                "dsl-bootstrap-discovery",
                "javaspec {\n" +
                        "    bootstrapDiscovery = true\n" +
                        "}\n"
        );
        assertBootstrapDiscoveryOptIn(
                "task-bootstrap-discovery",
                "tasks.named('javaspecRun') {\n" +
                        "    bootstrapDiscovery = true\n" +
                        "}\n"
        );
        assertBootstrapDiscoveryOptIn(
                "property-bootstrap-discovery",
                "",
                "-Pjavaspec.bootstrapDiscovery=true"
        );
    }

    @Test
    public void defaultBootstrapDiscoveryDoesNotExecuteServiceLoaderHook() throws Exception {
        File projectDir = bootstrapDiscoveryProject("default-bootstrap-discovery", "", "");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertEquals("example:\n", readFile(bootstrapDiscoveryEventFile(projectDir)));
    }

    @Test
    public void invalidBootstrapDiscoveryProjectPropertyFailsClearly() throws Exception {
        File projectDir = newProject("invalid-bootstrap-discovery-property");
        writeBuildFile(projectDir, javaPluginBuild(""));

        BuildResult result = runGradleAndFail(projectDir, "javaspecRun", "-Pjavaspec.bootstrapDiscovery=maybe");

        assertEquals(TaskOutcome.FAILED, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "Invalid javaspec.bootstrapDiscovery: expected true or false but was 'maybe'.");
    }

    @Test
    public void dslConfiguredExtensionRegistersSelectedFormatter() throws Exception {
        File projectDir = phase32ConfiguredExtensionProject(
                "dsl-configured-extension-formatter",
                "javaspec {\n" +
                        "    formatter = 'phase32-dsl'\n" +
                        "    extensions 'support.DslPhase32Extension'\n" +
                        "}\n"
        );
        writeGradleFormatterExtension(projectDir, "support.DslPhase32Extension", "phase32-dsl",
                "gradle dsl extension formatter");

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: formatter phase32-dsl.");
        assertContains(result.getOutput(), "javaspec: gradle dsl extension formatter total=1 passed=1 failed=0");
        assertFalse(result.getOutput().contains("javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
    }

    @Test
    public void projectPropertyConfiguredExtensionRegistersSelectedFormatter() throws Exception {
        File projectDir = phase32ConfiguredExtensionProject("project-property-configured-extension-formatter", "");
        writeGradleFormatterExtension(projectDir, "support.PropertyPhase32Extension", "phase32-property",
                "gradle property extension formatter");

        BuildResult result = runGradle(
                projectDir,
                "javaspecRun",
                "-Pjavaspec.extensions=support.PropertyPhase32Extension",
                "-Pjavaspec.formatter=phase32-property"
        );

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: formatter phase32-property.");
        assertContains(result.getOutput(), "javaspec: gradle property extension formatter total=1 passed=1 failed=0");
    }

    @Test
    public void configuredExtensionActivationFailureFailsBeforeReports() throws Exception {
        File projectDir = phase32ConfiguredExtensionProject(
                "configured-extension-activation-failure",
                "javaspec {\n" +
                        "    extensions 'support.FailingPhase32Extension'\n" +
                        "    jsonReportFile = file('build/reports/javaspec/extension-failure.json')\n" +
                        "    junitXmlReportFile = file('build/reports/javaspec/extension-failure.xml')\n" +
                        "}\n"
        );
        writeGradleFailingExtension(projectDir, "support.FailingPhase32Extension", "phase 32 Gradle extension failure");

        BuildResult result = runGradleAndFail(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.FAILED, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec extension activation failed:");
        assertContains(result.getOutput(), "support.FailingPhase32Extension");
        assertContains(result.getOutput(), "phase 32 Gradle extension failure");
        assertFalse(result.getOutput().contains("javaspec: Examples:"));
        assertFalse(new File(projectDir, "build/reports/javaspec/extension-failure.json").exists());
        assertFalse(new File(projectDir, "build/reports/javaspec/extension-failure.xml").exists());
    }

    @Test
    public void pluginRuntimeClasspathDeclarationStaysCoreOnly() throws Exception {
        String build = readFile(new File("build.gradle"));

        assertContains(build, "implementation \"io.github.jvmspec:javaspec:${javaspecCoreVersion}\"");
        assertFalse(build.contains("runtimeOnly"));
        assertFalse(build.contains("implementation gradleTestKit()"));
        assertFalse(build.contains("implementation \"junit:junit"));
    }

    @Test
    public void externalExtensionProviderOnTaskClasspathRendersSelectedFormatter() throws Exception {
        File projectDir = externalFormatterExtensionProject(
                "external-extension-formatter",
                "    formatter = 'external'\n"
        );

        BuildResult result = runGradle(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: formatter external.");
        assertContains(result.getOutput(), "javaspec: found 1 specification(s).");
        assertContains(result.getOutput(), "javaspec: external gradle formatter total=1 passed=1 failed=0");
        assertFalse(result.getOutput().contains("javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
    }

    @Test
    public void invalidFormatterListsExternalNamesWhenProviderIsOnTaskClasspath() throws Exception {
        File projectDir = externalFormatterExtensionProject(
                "invalid-external-formatter-list",
                "    formatter = 'missing'\n"
        );

        BuildResult result = runGradleAndFail(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.FAILED, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "Invalid javaspec formatter: missing. Valid values: progress, pretty, json, external.");
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
        String source = readFile(new File("src/main/java/io/github/jvmspec/gradle/JavaspecRunTask.java"));

        assertContains(source, "import io.github.jvmspec.invocation.JavaspecLauncher;");
        assertContains(source, "JavaspecLauncher.run(invocation)");
        assertFalse(source.contains("System.exit"));
    }

    private void assertGradleCompilationOptIn(
            String projectName,
            String subjectSimpleName,
            String javaspecBlock,
            String extraBuildConfiguration,
            String... additionalArguments
    ) throws Exception {
        File projectDir = sourceOnlyCompilationProject(
                projectName,
                subjectSimpleName,
                javaspecBlock,
                extraBuildConfiguration
        );
        String[] arguments = new String[additionalArguments.length + 1];
        arguments[0] = "javaspecRun";
        System.arraycopy(additionalArguments, 0, arguments, 1, additionalArguments.length);

        BuildResult result = runGradle(projectDir, arguments);

        File defaultOutput = new File(projectDir, "build/javaspec-classes");
        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: compiled 2 source file(s) to " + defaultOutput.getPath() + ".");
        assertContains(result.getOutput(), "javaspec: found 1 specification(s).");
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertTrue(classFileFor(defaultOutput, "com.example." + subjectSimpleName).isFile());
        assertTrue(classFileFor(defaultOutput, "spec.com.example." + subjectSimpleName + "Spec").isFile());
    }

    private File sourceOnlyCompilationProject(
            String projectName,
            String subjectSimpleName,
            String javaspecBlock,
            String extraBuildConfiguration
    ) throws Exception {
        File projectDir = newProject(projectName);
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        javaspecBlock +
                        "}\n" +
                        extraBuildConfiguration
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "suite.default.specDir = specs\n" +
                        "suite.default.sourceDir = sources\n");
        writePhase34CompilationSources(projectDir, subjectSimpleName);
        return projectDir;
    }

    private static void writePhase34CompilationSources(File projectDir, String subjectSimpleName) throws IOException {
        writeJavaSource(projectDir, "sources/com/example/" + subjectSimpleName + ".java",
                "package com.example;\n\n" +
                        "public class " + subjectSimpleName + " {\n" +
                        "    public String message() {\n" +
                        "        return \"compiled-by-gradle-plugin\";\n" +
                        "    }\n" +
                        "}\n");
        writeJavaSource(projectDir, "specs/spec/com/example/" + subjectSimpleName + "Spec.java",
                "package spec.com.example;\n\n" +
                        "public class " + subjectSimpleName + "Spec {\n" +
                        "    public void it_runs_after_gradle_plugin_compilation() {\n" +
                        "        if (!\"compiled-by-gradle-plugin\".equals(new com.example." + subjectSimpleName + "().message())) {\n" +
                        "            throw new AssertionError(\"Gradle plugin compilation output was not used\");\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");
    }

    private void assertDuplicateReportAliasFailsGradleBuild(String projectName, String reportConfiguration, String duplicateKey) throws Exception {
        File projectDir = newProject(projectName);
        writeBuildFile(projectDir, javaPluginBuild(
                "javaspec {\n" +
                        "    configFile = file('javaspec.conf')\n" +
                        "}\n"
        ));
        writeFile(new File(projectDir, "javaspec.conf"),
                "suite.default.specDir = src/test/java\n" + reportConfiguration);

        BuildResult result = runGradleAndFail(projectDir, "javaspecRun");

        assertEquals(TaskOutcome.FAILED, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "Invalid javaspec configuration:");
        assertContains(result.getOutput(), "Duplicate configuration key '" + duplicateKey + "'");
    }

    private void assertBootstrapDiscoveryOptIn(
            String projectName,
            String javaspecConfiguration,
            String... additionalArguments
    ) throws Exception {
        File projectDir = bootstrapDiscoveryProject(projectName, javaspecConfiguration, "discovered");
        String[] arguments = new String[additionalArguments.length + 1];
        arguments[0] = "javaspecRun";
        System.arraycopy(additionalArguments, 0, arguments, 1, additionalArguments.length);

        BuildResult result = runGradle(projectDir, arguments);

        assertEquals(TaskOutcome.SUCCESS, result.task(":javaspecRun").getOutcome());
        assertContains(result.getOutput(), "javaspec: found 1 specification(s).");
        assertContains(result.getOutput(), "javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertEquals("discovered:1\nexample:discovered\n", readFile(bootstrapDiscoveryEventFile(projectDir)));
    }

    private File bootstrapDiscoveryProject(
            String projectName,
            String javaspecConfiguration,
            String expectedMarker
    ) throws Exception {
        File projectDir = newProject(projectName);
        writeBuildFile(projectDir, javaPluginBuild(
                "repositories {\n" +
                        "    mavenLocal()\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    testImplementation 'io.github.jvmspec:javaspec:1.0.0-RC1'\n" +
                        "}\n" +
                        "\n" +
                        javaspecConfiguration
        ));
        writeBootstrapDiscoverySupportSources(projectDir);
        writeFile(new File(projectDir, "src/test/resources/META-INF/services/io.github.jvmspec.bootstrap.BootstrapHook"),
                "support.DiscoveredBootstrapHook\n");
        writeJavaSource(projectDir, "src/test/java/spec/com/example/BootstrapDiscoverySpec.java",
                "package spec.com.example;\n\n" +
                        "public class BootstrapDiscoverySpec {\n" +
                        "    public void it_observes_bootstrap_discovery_marker() {\n" +
                        "        String marker = support.DiscoveryState.marker();\n" +
                        "        support.DiscoveryState.record(\"example:\" + marker);\n" +
                        "        if (!\"" + expectedMarker + "\".equals(marker)) {\n" +
                        "            throw new AssertionError(\"unexpected bootstrap discovery marker: \" + marker);\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");
        return projectDir;
    }

    private static void writeBootstrapDiscoverySupportSources(File projectDir) throws IOException {
        String eventFilePath = new File(projectDir, "build/bootstrap-discovery-events.txt")
                .getAbsolutePath()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        writeJavaSource(projectDir, "src/test/java/support/DiscoveryState.java",
                "package support;\n\n" +
                        "import java.io.File;\n" +
                        "import java.io.IOException;\n" +
                        "import java.nio.charset.StandardCharsets;\n" +
                        "import java.nio.file.Files;\n" +
                        "import java.nio.file.StandardOpenOption;\n\n" +
                        "public final class DiscoveryState {\n" +
                        "    private static String marker = \"\";\n" +
                        "    private static final File EVENT_FILE = new File(\"" + eventFilePath + "\");\n\n" +
                        "    private DiscoveryState() {\n" +
                        "    }\n\n" +
                        "    public static synchronized void reset() {\n" +
                        "        marker = \"\";\n" +
                        "        if (EVENT_FILE.exists() && !EVENT_FILE.delete()) {\n" +
                        "            throw new IllegalStateException(\"could not reset bootstrap discovery event file: \" + EVENT_FILE);\n" +
                        "        }\n" +
                        "    }\n\n" +
                        "    public static synchronized void appendMarker(String value) {\n" +
                        "        if (marker.length() == 0) {\n" +
                        "            marker = value;\n" +
                        "        } else {\n" +
                        "            marker = marker + \">\" + value;\n" +
                        "        }\n" +
                        "    }\n\n" +
                        "    public static synchronized String marker() {\n" +
                        "        return marker;\n" +
                        "    }\n\n" +
                        "    public static synchronized void record(String event) {\n" +
                        "        File parent = EVENT_FILE.getParentFile();\n" +
                        "        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {\n" +
                        "            throw new IllegalStateException(\"could not create event directory: \" + parent);\n" +
                        "        }\n" +
                        "        try {\n" +
                        "            Files.write(EVENT_FILE.toPath(), (event + \"\\n\").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);\n" +
                        "        } catch (IOException ex) {\n" +
                        "            throw new IllegalStateException(ex);\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");
        writeJavaSource(projectDir, "src/test/java/support/DiscoveredBootstrapHook.java",
                "package support;\n\n" +
                        "import io.github.jvmspec.bootstrap.BootstrapContext;\n" +
                        "import io.github.jvmspec.bootstrap.BootstrapHook;\n\n" +
                        "public final class DiscoveredBootstrapHook implements BootstrapHook {\n" +
                        "    public void bootstrap(BootstrapContext context) {\n" +
                        "        DiscoveryState.reset();\n" +
                        "        DiscoveryState.appendMarker(\"discovered\");\n" +
                        "        DiscoveryState.record(\"discovered:\" + context.specs().size());\n" +
                        "    }\n" +
                        "}\n");
    }

    private static File bootstrapDiscoveryEventFile(File projectDir) {
        return new File(projectDir, "build/bootstrap-discovery-events.txt");
    }

    private File phase32ConfiguredExtensionProject(String projectName, String javaspecConfiguration) throws Exception {
        File projectDir = newProject(projectName);
        writeBuildFile(projectDir, javaPluginBuild(
                "repositories {\n" +
                        "    mavenLocal()\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    testImplementation 'io.github.jvmspec:javaspec:1.0.0-RC1'\n" +
                        "}\n" +
                        "\n" +
                        javaspecConfiguration
        ));
        writeJavaSource(projectDir, "src/test/java/spec/com/example/Phase32GradleSpec.java",
                "package spec.com.example;\n\n" +
                        "public class Phase32GradleSpec {\n" +
                        "    public void it_passes_with_configured_extension() {\n" +
                        "    }\n" +
                        "}\n");
        return projectDir;
    }

    private static void writeGradleFormatterExtension(
            File projectDir,
            String qualifiedName,
            String formatterName,
            String linePrefix
    ) throws IOException {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        writeJavaSource(projectDir, "src/test/java/" + qualifiedName.replace('.', '/') + ".java",
                "package " + packageName + ";\n\n" +
                        "import io.github.jvmspec.extension.ExtensionContext;\n" +
                        "import io.github.jvmspec.extension.JavaspecExtension;\n" +
                        "import io.github.jvmspec.formatter.RunFormatter;\n" +
                        "import io.github.jvmspec.runner.RunResult;\n\n" +
                        "import java.io.PrintStream;\n\n" +
                        "public final class " + simpleName + " implements JavaspecExtension {\n" +
                        "    public void configure(ExtensionContext context) {\n" +
                        "        context.runFormatters().register(\"" + formatterName + "\", new RunFormatter() {\n" +
                        "            public void format(RunResult runResult, PrintStream out) {\n" +
                        "                out.println(\"" + linePrefix + " total=\" + runResult.totalExamples()\n" +
                        "                        + \" passed=\" + runResult.passedCount()\n" +
                        "                        + \" failed=\" + runResult.failedCount());\n" +
                        "            }\n" +
                        "        });\n" +
                        "    }\n" +
                        "}\n");
    }

    private static void writeGradleFailingExtension(File projectDir, String qualifiedName, String message) throws IOException {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        writeJavaSource(projectDir, "src/test/java/" + qualifiedName.replace('.', '/') + ".java",
                "package " + packageName + ";\n\n" +
                        "import io.github.jvmspec.extension.ExtensionContext;\n" +
                        "import io.github.jvmspec.extension.JavaspecExtension;\n\n" +
                        "public final class " + simpleName + " implements JavaspecExtension {\n" +
                        "    public void configure(ExtensionContext context) {\n" +
                        "        throw new IllegalStateException(\"" + message + "\");\n" +
                        "    }\n" +
                        "}\n");
    }

    private File externalFormatterExtensionProject(String projectName, String javaspecConfiguration) throws Exception {
        File projectDir = newProject(projectName);
        writeBuildFile(projectDir, javaPluginBuild(
                "repositories {\n" +
                        "    mavenLocal()\n" +
                        "    mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "    testImplementation 'io.github.jvmspec:javaspec:1.0.0-RC1'\n" +
                        "}\n" +
                        "\n" +
                        "javaspec {\n" +
                        javaspecConfiguration +
                        "}\n"
        ));
        writeJavaSource(projectDir, "src/test/java/com/example/ExternalFormatterExtension.java",
                "package com.example;\n\n" +
                        "import io.github.jvmspec.extension.ExtensionContext;\n" +
                        "import io.github.jvmspec.extension.JavaspecExtension;\n" +
                        "import io.github.jvmspec.formatter.RunFormatter;\n" +
                        "import io.github.jvmspec.runner.RunResult;\n\n" +
                        "import java.io.PrintStream;\n\n" +
                        "public class ExternalFormatterExtension implements JavaspecExtension {\n" +
                        "    public void configure(ExtensionContext context) {\n" +
                        "        context.runFormatters().register(\"external\", new RunFormatter() {\n" +
                        "            public void format(RunResult runResult, PrintStream out) {\n" +
                        "                out.println(\"external gradle formatter total=\" + runResult.totalExamples()\n" +
                        "                        + \" passed=\" + runResult.passedCount()\n" +
                        "                        + \" failed=\" + runResult.failedCount());\n" +
                        "            }\n" +
                        "        });\n" +
                        "    }\n" +
                        "}\n");
        writeFile(new File(projectDir, "src/test/resources/META-INF/services/io.github.jvmspec.extension.JavaspecExtension"),
                "com.example.ExternalFormatterExtension\n");
        writeJavaSource(projectDir, "src/test/java/spec/com/example/ExternalFormatterSpec.java",
                "package spec.com.example;\n\n" +
                        "public class ExternalFormatterSpec {\n" +
                        "    public void it_passes_with_external_formatter() {\n" +
                        "    }\n" +
                        "}\n");
        return projectDir;
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

    private static void writeBootstrapSupportSources(File projectDir) throws IOException {
        writeJavaSource(projectDir, "src/test/java/support/BootstrapState.java",
                "package support;\n\n" +
                        "import io.github.jvmspec.bootstrap.BootstrapContext;\n" +
                        "\n" +
                        "import java.io.File;\n" +
                        "import java.io.IOException;\n" +
                        "import java.nio.charset.StandardCharsets;\n" +
                        "import java.nio.file.Files;\n" +
                        "import java.nio.file.StandardOpenOption;\n" +
                        "\n" +
                        "public final class BootstrapState {\n" +
                        "    private static String marker = \"\";\n" +
                        "    private static File eventFile;\n" +
                        "\n" +
                        "    private BootstrapState() {\n" +
                        "    }\n" +
                        "\n" +
                        "    public static synchronized void bind(BootstrapContext context) {\n" +
                        "        if (context.specs().isEmpty()) {\n" +
                        "            throw new IllegalStateException(\"bootstrap context did not receive discovered specs\");\n" +
                        "        }\n" +
                        "        eventFile = new File(context.specs().get(0).specFile().getParentFile(), \"bootstrap-events.txt\");\n" +
                        "        if (eventFile.exists() && !eventFile.delete()) {\n" +
                        "            throw new IllegalStateException(\"could not reset bootstrap event file: \" + eventFile);\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    public static synchronized void appendMarker(String value) {\n" +
                        "        if (marker.length() == 0) {\n" +
                        "            marker = value;\n" +
                        "        } else {\n" +
                        "            marker = marker + \">\" + value;\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    public static synchronized String marker() {\n" +
                        "        return marker;\n" +
                        "    }\n" +
                        "\n" +
                        "    public static synchronized void record(String event) {\n" +
                        "        if (eventFile == null) {\n" +
                        "            throw new IllegalStateException(\"bootstrap event file was not bound\");\n" +
                        "        }\n" +
                        "        try {\n" +
                        "            Files.write(eventFile.toPath(), (event + \"\\n\").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);\n" +
                        "        } catch (IOException ex) {\n" +
                        "            throw new IllegalStateException(ex);\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");
        writeJavaSource(projectDir, "src/test/java/support/TopLevelBootstrapHook.java",
                "package support;\n\n" +
                        "import io.github.jvmspec.bootstrap.BootstrapContext;\n" +
                        "import io.github.jvmspec.bootstrap.BootstrapHook;\n" +
                        "\n" +
                        "public final class TopLevelBootstrapHook implements BootstrapHook {\n" +
                        "    public void bootstrap(BootstrapContext context) {\n" +
                        "        BootstrapState.bind(context);\n" +
                        "        BootstrapState.appendMarker(\"top-level\");\n" +
                        "        BootstrapState.record(\"top-level:\" + context.specs().size());\n" +
                        "    }\n" +
                        "}\n");
        writeJavaSource(projectDir, "src/test/java/support/SuiteBootstrapHook.java",
                "package support;\n\n" +
                        "import io.github.jvmspec.bootstrap.BootstrapContext;\n" +
                        "import io.github.jvmspec.bootstrap.BootstrapHook;\n" +
                        "\n" +
                        "public final class SuiteBootstrapHook implements BootstrapHook {\n" +
                        "    public void bootstrap(BootstrapContext context) {\n" +
                        "        BootstrapState.appendMarker(\"suite\");\n" +
                        "        BootstrapState.record(\"suite:\" + context.specs().size());\n" +
                        "    }\n" +
                        "}\n");
        writeJavaSource(projectDir, "src/test/java/support/FailingBootstrapHook.java",
                "package support;\n\n" +
                        "import io.github.jvmspec.bootstrap.BootstrapContext;\n" +
                        "import io.github.jvmspec.bootstrap.BootstrapHook;\n" +
                        "\n" +
                        "public final class FailingBootstrapHook implements BootstrapHook {\n" +
                        "    public void bootstrap(BootstrapContext context) {\n" +
                        "        BootstrapState.bind(context);\n" +
                        "        BootstrapState.record(\"failing:\" + context.specs().size());\n" +
                        "        throw new IllegalStateException(\"phase 27 Gradle bootstrap failure\");\n" +
                        "    }\n" +
                        "}\n");
    }

    private static File bootstrapEventFile(File projectDir) {
        return new File(projectDir, "src/test/java/spec/com/example/bootstrap-events.txt");
    }

    private static File classFileFor(File root, String qualifiedName) {
        return new File(root, qualifiedName.replace('.', File.separatorChar) + ".class");
    }

    private static String javaPluginBuild(String extraConfiguration) {
        return "plugins {\n" +
                "    id 'io.github.jvmspec'\n" +
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

    private static void assertJunitXmlSuiteMetadata(String xml, int tests, int failures, int errors, int skipped) {
        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"" + tests + "\" failures=\"" + failures
                + "\" errors=\"" + errors + "\" skipped=\"" + skipped + "\" timestamp=\"");
        assertContains(xml, "\" time=\"0\">");
        assertContains(xml, "<properties>");
        assertContains(xml, "<property name=\"javaspec.report.schemaVersion\" value=\"1\"/>");
        assertContains(xml, "<property name=\"javaspec.report.tool\" value=\"javaspec\"/>");
    }

    private static void assertReportJsonDefaultMetadata(String json) {
        assertContains(json, "\"metadata\": {");
        assertContains(json, "\"time\": 0");
        assertContains(json, "\"javaspec.report.schemaVersion\": \"1\"");
        assertContains(json, "\"javaspec.report.tool\": \"javaspec\"");
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
