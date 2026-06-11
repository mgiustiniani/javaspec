package org.javaspec.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.javaspec.bootstrap.BootstrapContext;
import org.javaspec.bootstrap.BootstrapHook;
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
        assertReportJsonDefaultMetadata(json);
        assertPluginReportJsonMetadata(json, specFile, "it_uses_injected_test_classpath", 4);
        String xml = readFile(xmlFile);
        assertJunitXmlSuiteMetadata(xml, 1, 0, 0, 0);
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
    public void compileTrueCompilesSourceAndSpecRootsAndRunsSourceOnlySpec() throws Exception {
        requireJdkCompiler();
        SourceOnlySpecFixture fixture = sourceOnlySpecFixture("PluginPhase34CompileSubject");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, fixture.specRoot, fixture.classesDirectory, log);
        set(mojo, "compile", Boolean.TRUE);

        mojo.execute();

        File defaultOutput = new File(fixture.basedir, "target/javaspec-classes");
        assertTrue(log.containsInfo("javaspec: compiled 2 source file(s) to " + defaultOutput.getPath() + "."));
        assertTrue(log.containsInfo("javaspec: found 1 specification(s)."));
        assertTrue(log.containsInfo("javaspec: examples total=1, passed=1, failed=0, broken=0, skipped=0, pending=0."));
        assertTrue(classFileFor(defaultOutput, "com.example.PluginPhase34CompileSubject").isFile());
        assertTrue(classFileFor(defaultOutput, "spec.com.example.PluginPhase34CompileSubjectSpec").isFile());
    }

    @Test
    public void compileOutputParametersImplyCompilationAndCompileOutputTakesPrecedenceOverAlias() throws Exception {
        requireJdkCompiler();
        SourceOnlySpecFixture aliasFixture = sourceOnlySpecFixture("PluginPhase34AliasOutputSubject");
        File aliasOutput = new File(aliasFixture.basedir, "alias-javaspec-classes");
        CapturingLog aliasLog = new CapturingLog();
        JavaspecRunMojo aliasMojo = mojo(aliasFixture.basedir, aliasFixture.specRoot, aliasFixture.classesDirectory, aliasLog);
        set(aliasMojo, "compileOutputDirectory", aliasOutput);

        aliasMojo.execute();

        assertTrue(aliasLog.containsInfo("javaspec: compiled 2 source file(s) to " + aliasOutput.getPath() + "."));
        assertTrue(classFileFor(aliasOutput, "spec.com.example.PluginPhase34AliasOutputSubjectSpec").isFile());

        SourceOnlySpecFixture precedenceFixture = sourceOnlySpecFixture("PluginPhase34PrecedenceOutputSubject");
        File primaryOutput = new File(precedenceFixture.basedir, "primary-javaspec-classes");
        File ignoredAliasOutput = new File(precedenceFixture.basedir, "ignored-alias-javaspec-classes");
        CapturingLog precedenceLog = new CapturingLog();
        JavaspecRunMojo precedenceMojo = mojo(precedenceFixture.basedir, precedenceFixture.specRoot,
                precedenceFixture.classesDirectory, precedenceLog);
        set(precedenceMojo, "compileOutput", primaryOutput);
        set(precedenceMojo, "compileOutputDirectory", ignoredAliasOutput);

        precedenceMojo.execute();

        assertTrue(precedenceLog.containsInfo("javaspec: compiled 2 source file(s) to " + primaryOutput.getPath() + "."));
        assertTrue(classFileFor(primaryOutput, "spec.com.example.PluginPhase34PrecedenceOutputSubjectSpec").isFile());
        assertFalse(ignoredAliasOutput.exists());
    }

    @Test
    public void noSpecCompileOptInSkipsCompilationLogAndOutputCreation() throws Exception {
        File basedir = temporaryFolder.newFolder("phase34-no-spec-compile-project");
        File sourceRoot = new File(basedir, "src/main/java");
        File specRoot = new File(basedir, "empty-specs");
        File classesDirectory = new File(basedir, "classes");
        File compileOutput = new File(basedir, "target/no-spec-javaspec-classes");
        assertTrue(specRoot.mkdirs());
        assertTrue(classesDirectory.mkdirs());
        writeFile(sourceFileFor(sourceRoot, "com.example.BrokenNoSpecMavenSource"),
                "package com.example;\n\n" +
                        "public class BrokenNoSpecMavenSource {\n" +
                        "    public void broken( {\n" +
                        "}\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, specRoot, classesDirectory, log);
        set(mojo, "compile", Boolean.TRUE);
        set(mojo, "compileOutput", compileOutput);

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: no specifications found."));
        assertTrue(log.containsInfo("javaspec: examples total=0, passed=0, failed=0, broken=0, skipped=0, pending=0."));
        assertFalse(log.containsInfo("javaspec: compiled "));
        assertFalse(compileOutput.exists());
    }

    @Test
    public void compilationFailureFailsBeforeReportsAndLogsDiagnostics() throws Exception {
        requireJdkCompiler();
        File basedir = temporaryFolder.newFolder("phase34-compilation-failure-project");
        File sourceRoot = new File(basedir, "src/main/java");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        File jsonReport = new File(basedir, "reports/compile-failure.json");
        File xmlReport = new File(basedir, "reports/compile-failure.xml");
        assertTrue(classesDirectory.mkdirs());
        writeFile(sourceFileFor(sourceRoot, "com.example.BrokenPhase34MavenSubject"),
                "package com.example;\n\n" +
                        "public class BrokenPhase34MavenSubject {\n" +
                        "    public String message() {\n" +
                        "        return missingSymbol;\n" +
                        "    }\n" +
                        "}\n");
        writeSpec(specRoot, "spec.com.example.BrokenPhase34MavenSubjectSpec",
                "    public void it_would_run_after_successful_compilation() {\n" +
                        "    }\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, specRoot, classesDirectory, log);
        set(mojo, "compile", Boolean.TRUE);
        set(mojo, "jsonReportFile", jsonReport);
        set(mojo, "junitXmlReportFile", xmlReport);

        try {
            mojo.execute();
            fail("Expected compilation failure to throw MojoFailureException");
        } catch (MojoFailureException expected) {
            assertContains(expected.getMessage(), "javaspec compilation failed: Compilation failed");
        }

        assertTrue(log.containsError("BrokenPhase34MavenSubject.java"));
        assertTrue(log.containsError("missingSymbol"));
        assertFalse(log.containsInfo("javaspec: examples total="));
        assertFalse(jsonReport.exists());
        assertFalse(xmlReport.exists());
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
        assertReportJsonDefaultMetadata(json);
        String xml = readFile(xmlFile);
        assertJunitXmlSuiteMetadata(xml, 1, 0, 0, 1);
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
        assertReportJsonDefaultMetadata(json);
        String xml = readFile(xmlFile);
        assertJunitXmlSuiteMetadata(xml, 2, 1, 1, 0);
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
    public void configurationBootstrapHooksExecuteBeforeExamplesAndCanChangeObservedBehavior() throws Exception {
        BootstrapState.reset();
        File basedir = temporaryFolder.newFolder("bootstrap-project");
        File specRoot = new File(basedir, "bootstrap-specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File configFile = new File(basedir, "javaspec.conf");
        writeFile(configFile,
                "defaultSuite=custom\n" +
                        "suite.custom.specDir=bootstrap-specs\n" +
                        "bootstrap=" + TopLevelBootstrapHook.class.getName() + "\n" +
                        "suite.custom.bootstrap=" + SuiteBootstrapHook.class.getName() + "\n");
        File specSource = writeSpec(specRoot, "spec.com.example.PluginBootstrapSubjectSpec",
                "    public void it_observes_bootstrap_marker() {\n" +
                        "        org.javaspec.maven.JavaspecRunMojoTest.BootstrapState.record(\"example:\" + org.javaspec.maven.JavaspecRunMojoTest.BootstrapState.marker());\n" +
                        "        if (!\"top-level>suite\".equals(org.javaspec.maven.JavaspecRunMojoTest.BootstrapState.marker())) {\n" +
                        "            throw new AssertionError(\"bootstrap marker was not applied before example: \" + org.javaspec.maven.JavaspecRunMojoTest.BootstrapState.marker());\n" +
                        "        }\n" +
                        "    }\n");
        compile(classesDirectory, specSource);
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, null, classesDirectory, log);
        set(mojo, "configFile", configFile);
        set(mojo, "suite", "custom");

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: running suite 'custom' from " + specRoot.getPath() + "."));
        assertTrue(log.containsInfo("javaspec: found 1 specification(s)."));
        assertTrue(log.containsInfo("javaspec: examples total=1, passed=1, failed=0, broken=0, skipped=0, pending=0."));
        assertTrue(log.warnMessages.isEmpty());
        assertEquals("top-level>suite", BootstrapState.marker());
        assertEquals(list("top-level:1", "suite:1", "example:top-level>suite"), BootstrapState.events());
    }

    @Test
    public void configBootstrapDiscoveryExecutesServiceLoaderHookBeforeExamples() throws Exception {
        BootstrapState.reset();
        CompiledSpecFixture fixture = compiledBootstrapDiscoverySubjectFixture(
                "PluginConfigBootstrapDiscoverySubject",
                "discovered"
        );
        writeBootstrapService(fixture.classesDirectory, DiscoveredServiceLoaderBootstrapHook.class);
        File configFile = new File(fixture.basedir, "javaspec.conf");
        writeFile(configFile,
                "suite.default.specDir=specs\n" +
                        "bootstrapDiscovery=true\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, null, fixture.classesDirectory, log);
        set(mojo, "configFile", configFile);

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: found 1 specification(s)."));
        assertTrue(log.containsInfo("javaspec: examples total=1, passed=1, failed=0, broken=0, skipped=0, pending=0."));
        assertTrue(log.warnMessages.isEmpty());
        assertEquals(list("discovered:1", "example:discovered"), BootstrapState.events());
    }

    @Test
    public void bootstrapDiscoveryPropertyExecutesServiceLoaderHookBeforeExamples() throws Exception {
        BootstrapState.reset();
        CompiledSpecFixture fixture = compiledBootstrapDiscoverySubjectFixture(
                "PluginPropertyBootstrapDiscoverySubject",
                "discovered"
        );
        writeBootstrapService(fixture.classesDirectory, DiscoveredServiceLoaderBootstrapHook.class);
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, fixture.specRoot, fixture.classesDirectory, log);
        set(mojo, "bootstrapDiscovery", "true");

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: found 1 specification(s)."));
        assertTrue(log.containsInfo("javaspec: examples total=1, passed=1, failed=0, broken=0, skipped=0, pending=0."));
        assertEquals(list("discovered:1", "example:discovered"), BootstrapState.events());
    }

    @Test
    public void defaultBootstrapDiscoveryLeavesServiceLoaderHookUnexecuted() throws Exception {
        BootstrapState.reset();
        CompiledSpecFixture fixture = compiledBootstrapDiscoverySubjectFixture(
                "PluginDefaultBootstrapDiscoverySubject",
                ""
        );
        writeBootstrapService(fixture.classesDirectory, DiscoveredServiceLoaderBootstrapHook.class);
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, fixture.specRoot, fixture.classesDirectory, log);

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: examples total=1, passed=1, failed=0, broken=0, skipped=0, pending=0."));
        assertEquals(list("example:"), BootstrapState.events());
    }

    @Test
    public void invalidBootstrapDiscoveryPropertyFailsClearly() throws Exception {
        File basedir = temporaryFolder.newFolder("invalid-bootstrap-discovery-property-project");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(specRoot.mkdirs());
        assertTrue(classesDirectory.mkdirs());
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, specRoot, classesDirectory, log);
        set(mojo, "bootstrapDiscovery", "sometimes");

        try {
            mojo.execute();
            fail("Expected invalid bootstrapDiscovery property to throw MojoExecutionException");
        } catch (MojoExecutionException expected) {
            assertContains(expected.getMessage(), "Invalid javaspec.bootstrapDiscovery:");
            assertContains(expected.getMessage(), "expected true or false");
            assertContains(expected.getMessage(), "sometimes");
        }
    }

    @Test
    public void formatterParameterOverridesConfiguredFormatter() throws Exception {
        File basedir = temporaryFolder.newFolder("formatter-override-project");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(specRoot.mkdirs());
        assertTrue(classesDirectory.mkdirs());
        File configFile = new File(basedir, "javaspec.conf");
        writeFile(configFile,
                "suite.default.specDir=specs\n" +
                        "formatter=pretty\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(basedir, null, classesDirectory, log);
        set(mojo, "configFile", configFile);
        set(mojo, "formatter", "progress");

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: formatter progress."));
        assertTrue(log.containsInfo("javaspec: Examples: 0 total, 0 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
        assertFalse(log.containsInfo("javaspec: Example results:"));
    }

    @Test
    public void configuredAndPropertyExtensionsRegisterFormatterWithPropertyAppliedLast() throws Exception {
        CompiledSpecFixture fixture = compiledPassingFixture("PluginPhase32ExtensionSubject");
        File extensionSourceRoot = new File(fixture.basedir, "extension-sources");
        File topExtension = writeMavenTopExtension(extensionSourceRoot);
        File suiteExtension = writeMavenSuiteExtension(extensionSourceRoot);
        File propertyExtension = writeMavenPropertyExtension(extensionSourceRoot);
        compile(fixture.classesDirectory, topExtension, suiteExtension, propertyExtension);
        File configFile = new File(fixture.basedir, "javaspec.conf");
        writeFile(configFile,
                "suite.default.specDir=specs\n" +
                        "formatter=phase32-maven\n" +
                        "extensions=support.TopPhase32Extension\n" +
                        "suite.default.extensions=support.SuitePhase32Extension\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, null, fixture.classesDirectory, log);
        set(mojo, "configFile", configFile);
        set(mojo, "extensionsProperty", "support.PropertyPhase32Extension");

        mojo.execute();

        assertTrue(log.containsInfo("javaspec: formatter phase32-maven."));
        assertTrue(log.containsInfo("javaspec: maven property extension after top and suite total=1 passed=1 failed=0"));
        assertFalse(log.containsInfo("javaspec: Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
    }

    @Test
    public void invalidFormatterListsExtensionRegisteredFormatterNames() throws Exception {
        CompiledSpecFixture fixture = compiledPassingFixture("PluginPhase32InvalidFormatterSubject");
        File extensionSourceRoot = new File(fixture.basedir, "extension-sources");
        File extension = writeMavenNamedFormatterExtension(
                extensionSourceRoot,
                "support.AvailablePhase32Extension",
                "phase32-available",
                "available maven formatter"
        );
        compile(fixture.classesDirectory, extension);
        File configFile = new File(fixture.basedir, "javaspec.conf");
        writeFile(configFile,
                "suite.default.specDir=specs\n" +
                        "extensions=support.AvailablePhase32Extension\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, null, fixture.classesDirectory, log);
        set(mojo, "configFile", configFile);
        set(mojo, "formatter", "missing");

        try {
            mojo.execute();
            fail("Expected invalid formatter to throw MojoFailureException");
        } catch (MojoFailureException expected) {
            assertContains(expected.getMessage(), "Invalid javaspec formatter: missing.");
            assertContains(expected.getMessage(), "Valid values: progress, pretty, phase32-available.");
        }
    }

    @Test
    public void configuredExtensionActivationFailureFailsBeforeReports() throws Exception {
        CompiledSpecFixture fixture = compiledPassingFixture("PluginPhase32FailingExtensionSubject");
        File extensionSourceRoot = new File(fixture.basedir, "extension-sources");
        File failingExtension = writeMavenFailingExtension(
                extensionSourceRoot,
                "support.FailingPhase32Extension",
                "phase 32 Maven extension failure"
        );
        compile(fixture.classesDirectory, failingExtension);
        File configFile = new File(fixture.basedir, "javaspec.conf");
        File jsonFile = new File(fixture.basedir, "reports/extension-failure.json");
        File xmlFile = new File(fixture.basedir, "reports/extension-failure.xml");
        writeFile(configFile,
                "suite.default.specDir=specs\n" +
                        "extensions=support.FailingPhase32Extension\n" +
                        "report=reports/extension-failure.json\n" +
                        "junitXml=reports/extension-failure.xml\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, null, fixture.classesDirectory, log);
        set(mojo, "configFile", configFile);

        try {
            mojo.execute();
            fail("Expected extension activation failure to throw MojoFailureException");
        } catch (MojoFailureException expected) {
            assertContains(expected.getMessage(), "javaspec extension activation failed:");
            assertContains(expected.getMessage(), "support.FailingPhase32Extension");
            assertContains(expected.getMessage(), "phase 32 Maven extension failure");
        }

        assertFalse(log.containsInfo("javaspec: examples total="));
        assertFalse(jsonFile.exists());
        assertFalse(xmlFile.exists());
    }

    @Test
    public void bootstrapFailureThrowsMojoExecutionExceptionWithBootstrapWordingBeforeReports() throws Exception {
        BootstrapState.reset();
        CompiledSpecFixture fixture = compiledBootstrapSubjectFixture("PluginFailingBootstrapSubject");
        File configFile = new File(fixture.basedir, "javaspec.conf");
        File jsonFile = new File(fixture.basedir, "reports/bootstrap-failure.json");
        File xmlFile = new File(fixture.basedir, "reports/bootstrap-failure.xml");
        writeFile(configFile,
                "suite.default.specDir=specs\n" +
                        "bootstrap=" + FailingBootstrapHook.class.getName() + "\n");
        CapturingLog log = new CapturingLog();
        JavaspecRunMojo mojo = mojo(fixture.basedir, null, fixture.classesDirectory, log);
        set(mojo, "configFile", configFile);
        set(mojo, "jsonReportFile", jsonFile);
        set(mojo, "junitXmlReportFile", xmlFile);

        try {
            mojo.execute();
            fail("Expected bootstrap failure to throw MojoExecutionException");
        } catch (MojoExecutionException expected) {
            assertContains(expected.getMessage(), "javaspec bootstrap execution failed:");
            assertContains(expected.getMessage(), "Bootstrap hook '");
            assertContains(expected.getMessage(), FailingBootstrapHook.class.getName());
            assertContains(expected.getMessage(), "phase 27 Maven bootstrap failure");
        }

        assertFalse(log.containsInfo("javaspec: examples total="));
        assertEquals(list("failing:1"), BootstrapState.events());
        assertFalse(jsonFile.exists());
        assertFalse(xmlFile.exists());
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

    private SourceOnlySpecFixture sourceOnlySpecFixture(String describedSimpleName) throws Exception {
        File basedir = temporaryFolder.newFolder(describedSimpleName + "-source-only-project");
        File specRoot = new File(basedir, "specs");
        File sourceRoot = new File(basedir, "src/main/java");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File productionSource = sourceFileFor(sourceRoot, "com.example." + describedSimpleName);
        writeFile(productionSource,
                "package com.example;\n\n" +
                        "public class " + describedSimpleName + " {\n" +
                        "    public String message() {\n" +
                        "        return \"compiled-by-maven-plugin\";\n" +
                        "    }\n" +
                        "}\n");
        writeSpec(specRoot, "spec.com.example." + describedSimpleName + "Spec",
                "    public void it_runs_after_plugin_compilation() {\n" +
                        "        if (!\"compiled-by-maven-plugin\".equals(new com.example." + describedSimpleName + "().message())) {\n" +
                        "            throw new AssertionError(\"Maven plugin compilation output was not used\");\n" +
                        "        }\n" +
                        "    }\n");
        return new SourceOnlySpecFixture(basedir, specRoot, classesDirectory);
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

    private CompiledSpecFixture compiledBootstrapSubjectFixture(String describedSimpleName) throws Exception {
        File basedir = temporaryFolder.newFolder(describedSimpleName + "-project");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File specSource = writeSpec(specRoot, "spec.com.example." + describedSimpleName + "Spec",
                "    public void it_runs_after_bootstrap() {\n" +
                        "    }\n");
        compile(classesDirectory, specSource);
        return new CompiledSpecFixture(basedir, specRoot, classesDirectory);
    }

    private CompiledSpecFixture compiledBootstrapDiscoverySubjectFixture(
            String describedSimpleName,
            String expectedMarker
    ) throws Exception {
        File basedir = temporaryFolder.newFolder(describedSimpleName + "-project");
        File specRoot = new File(basedir, "specs");
        File classesDirectory = new File(basedir, "classes");
        assertTrue(classesDirectory.mkdirs());
        File specSource = writeSpec(specRoot, "spec.com.example." + describedSimpleName + "Spec",
                "    public void it_runs_after_discovered_bootstrap() {\n" +
                        "        String marker = org.javaspec.maven.JavaspecRunMojoTest.BootstrapState.marker();\n" +
                        "        org.javaspec.maven.JavaspecRunMojoTest.BootstrapState.record(\"example:\" + marker);\n" +
                        "        if (!\"" + expectedMarker + "\".equals(marker)) {\n" +
                        "            throw new AssertionError(\"unexpected bootstrap discovery marker: \" + marker);\n" +
                        "        }\n" +
                        "    }\n");
        compile(classesDirectory, specSource);
        return new CompiledSpecFixture(basedir, specRoot, classesDirectory);
    }

    private static File writeMavenTopExtension(File sourceRoot) throws Exception {
        File source = sourceFileFor(sourceRoot, "support.TopPhase32Extension");
        writeFile(source,
                "package support;\n\n" +
                        "import org.javaspec.extension.ExtensionContext;\n" +
                        "import org.javaspec.extension.JavaspecExtension;\n" +
                        "import org.javaspec.formatter.RunFormatter;\n" +
                        "import org.javaspec.runner.RunResult;\n\n" +
                        "import java.io.PrintStream;\n\n" +
                        "public final class TopPhase32Extension implements JavaspecExtension {\n" +
                        "    public void configure(ExtensionContext context) {\n" +
                        "        context.runFormatters().register(\"phase32-top\", new RunFormatter() {\n" +
                        "            public void format(RunResult runResult, PrintStream out) {\n" +
                        "                out.println(\"top\");\n" +
                        "            }\n" +
                        "        });\n" +
                        "    }\n" +
                        "}\n");
        return source;
    }

    private static File writeMavenSuiteExtension(File sourceRoot) throws Exception {
        File source = sourceFileFor(sourceRoot, "support.SuitePhase32Extension");
        writeFile(source,
                "package support;\n\n" +
                        "import org.javaspec.extension.ExtensionContext;\n" +
                        "import org.javaspec.extension.JavaspecExtension;\n" +
                        "import org.javaspec.formatter.RunFormatter;\n" +
                        "import org.javaspec.runner.RunResult;\n\n" +
                        "import java.io.PrintStream;\n\n" +
                        "public final class SuitePhase32Extension implements JavaspecExtension {\n" +
                        "    public void configure(ExtensionContext context) {\n" +
                        "        if (!context.runFormatters().contains(\"phase32-top\")) {\n" +
                        "            return;\n" +
                        "        }\n" +
                        "        context.runFormatters().register(\"phase32-suite\", new RunFormatter() {\n" +
                        "            public void format(RunResult runResult, PrintStream out) {\n" +
                        "                out.println(\"suite\");\n" +
                        "            }\n" +
                        "        });\n" +
                        "    }\n" +
                        "}\n");
        return source;
    }

    private static File writeMavenPropertyExtension(File sourceRoot) throws Exception {
        File source = sourceFileFor(sourceRoot, "support.PropertyPhase32Extension");
        writeFile(source,
                "package support;\n\n" +
                        "import org.javaspec.extension.ExtensionContext;\n" +
                        "import org.javaspec.extension.JavaspecExtension;\n" +
                        "import org.javaspec.formatter.RunFormatter;\n" +
                        "import org.javaspec.runner.RunResult;\n\n" +
                        "import java.io.PrintStream;\n\n" +
                        "public final class PropertyPhase32Extension implements JavaspecExtension {\n" +
                        "    public void configure(ExtensionContext context) {\n" +
                        "        final String prefix = context.runFormatters().contains(\"phase32-top\")\n" +
                        "                && context.runFormatters().contains(\"phase32-suite\")\n" +
                        "                ? \"maven property extension after top and suite\"\n" +
                        "                : \"maven property extension missing prior extension\";\n" +
                        "        context.runFormatters().register(\"phase32-maven\", new RunFormatter() {\n" +
                        "            public void format(RunResult runResult, PrintStream out) {\n" +
                        "                out.println(prefix + \" total=\" + runResult.totalExamples()\n" +
                        "                        + \" passed=\" + runResult.passedCount()\n" +
                        "                        + \" failed=\" + runResult.failedCount());\n" +
                        "            }\n" +
                        "        });\n" +
                        "    }\n" +
                        "}\n");
        return source;
    }

    private static File writeMavenNamedFormatterExtension(
            File sourceRoot,
            String qualifiedName,
            String formatterName,
            String linePrefix
    ) throws Exception {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        File source = sourceFileFor(sourceRoot, qualifiedName);
        writeFile(source,
                "package " + packageName + ";\n\n" +
                        "import org.javaspec.extension.ExtensionContext;\n" +
                        "import org.javaspec.extension.JavaspecExtension;\n" +
                        "import org.javaspec.formatter.RunFormatter;\n" +
                        "import org.javaspec.runner.RunResult;\n\n" +
                        "import java.io.PrintStream;\n\n" +
                        "public final class " + simpleName + " implements JavaspecExtension {\n" +
                        "    public void configure(ExtensionContext context) {\n" +
                        "        context.runFormatters().register(\"" + formatterName + "\", new RunFormatter() {\n" +
                        "            public void format(RunResult runResult, PrintStream out) {\n" +
                        "                out.println(\"" + linePrefix + " total=\" + runResult.totalExamples());\n" +
                        "            }\n" +
                        "        });\n" +
                        "    }\n" +
                        "}\n");
        return source;
    }

    private static File writeMavenFailingExtension(File sourceRoot, String qualifiedName, String message) throws Exception {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        File source = sourceFileFor(sourceRoot, qualifiedName);
        writeFile(source,
                "package " + packageName + ";\n\n" +
                        "import org.javaspec.extension.ExtensionContext;\n" +
                        "import org.javaspec.extension.JavaspecExtension;\n\n" +
                        "public final class " + simpleName + " implements JavaspecExtension {\n" +
                        "    public void configure(ExtensionContext context) {\n" +
                        "        throw new IllegalStateException(\"" + message + "\");\n" +
                        "    }\n" +
                        "}\n");
        return source;
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

    private static File classFileFor(File root, String qualifiedName) {
        return new File(root, qualifiedName.replace('.', File.separatorChar) + ".class");
    }

    private static void writeFile(File file, String content) throws Exception {
        File parent = file.getParentFile();
        assertTrue(parent == null || parent.isDirectory() || parent.mkdirs());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBootstrapService(File classesDirectory, Class<?>... providerTypes) throws Exception {
        File serviceFile = new File(new File(classesDirectory, "META-INF" + File.separator + "services"),
                BootstrapHook.class.getName());
        File parent = serviceFile.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < providerTypes.length; i++) {
            content.append(providerTypes[i].getName()).append('\n');
        }
        Files.write(serviceFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
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

    private static void requireJdkCompiler() {
        assertNotNull("These tests require a JDK with javax.tools.JavaCompiler", ToolProvider.getSystemJavaCompiler());
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
        arguments.add("-classpath");
        arguments.add(System.getProperty("java.class.path"));
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

    private static List<String> list(String first) {
        List<String> values = new ArrayList<String>();
        values.add(first);
        return values;
    }

    private static List<String> list(String first, String second) {
        List<String> values = new ArrayList<String>();
        values.add(first);
        values.add(second);
        return values;
    }

    private static List<String> list(String first, String second, String third) {
        List<String> values = new ArrayList<String>();
        values.add(first);
        values.add(second);
        values.add(third);
        return values;
    }

    private static void assertContains(String value, String expected) {
        assertTrue("Expected to contain: " + expected + "\nActual value:\n" + value, value.contains(expected));
    }

    public static final class BootstrapState {
        private static final List<String> EVENTS = new ArrayList<String>();
        private static String marker = "";

        public static synchronized void reset() {
            EVENTS.clear();
            marker = "";
        }

        public static synchronized void appendMarker(String value) {
            if (marker.length() == 0) {
                marker = value;
            } else {
                marker = marker + ">" + value;
            }
        }

        public static synchronized String marker() {
            return marker;
        }

        public static synchronized void record(String event) {
            EVENTS.add(event);
        }

        public static synchronized List<String> events() {
            return new ArrayList<String>(EVENTS);
        }
    }

    public static final class TopLevelBootstrapHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            BootstrapState.appendMarker("top-level");
            BootstrapState.record("top-level:" + context.specs().size());
        }
    }

    public static final class SuiteBootstrapHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            BootstrapState.appendMarker("suite");
            BootstrapState.record("suite:" + context.specs().size());
        }
    }

    public static final class FailingBootstrapHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            BootstrapState.record("failing:" + context.specs().size());
            throw new IllegalStateException("phase 27 Maven bootstrap failure");
        }
    }

    public static final class DiscoveredServiceLoaderBootstrapHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            BootstrapState.appendMarker("discovered");
            BootstrapState.record("discovered:" + context.specs().size());
        }
    }

    public static final class FailingServiceLoaderBootstrapHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            BootstrapState.record("discovered-failing:" + context.specs().size());
            throw new IllegalStateException("phase 33 Maven discovered bootstrap failure");
        }
    }

    private static final class SourceOnlySpecFixture {
        private final File basedir;
        private final File specRoot;
        private final File classesDirectory;

        private SourceOnlySpecFixture(File basedir, File specRoot, File classesDirectory) {
            this.basedir = basedir;
            this.specRoot = specRoot;
            this.classesDirectory = classesDirectory;
        }
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

        private boolean containsError(String fragment) {
            return contains(errorMessages, fragment);
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
