package org.javaspec.cli;

import org.javaspec.bootstrap.BootstrapContext;
import org.javaspec.bootstrap.BootstrapHook;
import org.javaspec.fixtures.cli.BootstrapEventLog;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainPhase27BootstrapCliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void resetBootstrapEvents() {
        BootstrapEventLog.reset();
    }

    @Test
    public void runExecutesTopLevelAndSuiteBootstrapHooksInOrderWithDuplicatesBeforeExamples() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("ordered-bootstrap-source-root");
        File configFile = writeConfig("ordered-bootstrap.conf",
                suiteConfig("custom", testJavaRoot(), sourceRoot) +
                        "bootstrap = " + TopLevelBootstrapHook.class.getName() + ", " + DuplicateBootstrapHook.class.getName() + "\n" +
                        "suite.custom.bootstrap = " + SuiteBootstrapHook.class.getName() + ", " + DuplicateBootstrapHook.class.getName() + "\n");

        CommandResult result = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--class", "org.javaspec.fixtures.cli.BootstrapSubject",
                "--example", "it_runs_after_bootstrap"
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "Found 1 specification(s) in " + testJavaRoot().getPath() + ".");
        assertContains(result.out, "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertEquals("", result.err);
        assertEquals(Arrays.asList(
                "top-level:1",
                "duplicate:1",
                "suite:1",
                "duplicate:1",
                "example"
        ), BootstrapEventLog.events());
    }

    @Test
    public void verboseRunShowsEffectiveBootstrapHooksFromTopLevelAndSuite() throws Exception {
        File specRoot = temporaryFolder.newFolder("verbose-bootstrap-spec-root");
        File sourceRoot = temporaryFolder.newFolder("verbose-bootstrap-source-root");
        File configFile = writeConfig("verbose-bootstrap.conf",
                suiteConfig("custom", specRoot, sourceRoot) +
                        "bootstrap = " + TopLevelBootstrapHook.class.getName() + ", " + DuplicateBootstrapHook.class.getName() + "\n" +
                        "suite.custom.bootstrap = " + SuiteBootstrapHook.class.getName() + ", " + DuplicateBootstrapHook.class.getName() + "\n");

        CommandResult result = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--verbose"
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "Run configuration:");
        assertContains(result.out, "  Bootstrap hooks: " + TopLevelBootstrapHook.class.getName()
                + ", " + DuplicateBootstrapHook.class.getName()
                + ", " + SuiteBootstrapHook.class.getName()
                + ", " + DuplicateBootstrapHook.class.getName());
        assertContains(result.out, "No specifications found in " + specRoot.getAbsolutePath() + ".");
        assertEquals("", result.err);
        assertTrue(BootstrapEventLog.events().isEmpty());
    }

    @Test
    public void noSpecRunSkipsBootstrapHooksAndWritesRequestedAndConfiguredReports() throws Exception {
        File specRoot = temporaryFolder.newFolder("no-spec-bootstrap-spec-root");
        File sourceRoot = temporaryFolder.newFolder("no-spec-bootstrap-source-root");
        File requestedJsonReport = new File(temporaryFolder.getRoot(), "requested-no-spec-report.json");
        File configuredJunitXmlReport = new File(temporaryFolder.getRoot(), "configured-no-spec-junit.xml");
        File configFile = writeConfig("no-spec-bootstrap.conf",
                suiteConfig("custom", specRoot, sourceRoot) +
                        "bootstrap = " + FailingBootstrapHook.class.getName() + "\n" +
                        "junitXml = " + configuredJunitXmlReport.getAbsolutePath() + "\n");

        CommandResult result = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--report", requestedJsonReport.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "No specifications found in " + specRoot.getAbsolutePath() + ".");
        assertEquals("", result.err);
        assertTrue(BootstrapEventLog.events().isEmpty());
        assertTrue(requestedJsonReport.isFile());
        assertTrue(configuredJunitXmlReport.isFile());
        assertEmptyReportJson(readFile(requestedJsonReport));
        assertEmptyJUnitXml(readFile(configuredJunitXmlReport));
    }

    @Test
    public void bootstrapFailureExitsUsagePrintsBootstrapErrorAndDoesNotWriteReports() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("failing-bootstrap-source-root");
        File jsonReport = new File(temporaryFolder.getRoot(), "failing-bootstrap-report.json");
        File junitXmlReport = new File(temporaryFolder.getRoot(), "failing-bootstrap-junit.xml");
        File configFile = writeConfig("failing-bootstrap.conf",
                suiteConfig("custom", testJavaRoot(), sourceRoot) +
                        "bootstrap = " + FailingBootstrapHook.class.getName() + "\n");

        CommandResult result = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--class", "org.javaspec.fixtures.cli.BootstrapSubject",
                "--example", "it_runs_after_bootstrap",
                "--report", jsonReport.getAbsolutePath(),
                "--junit-xml", junitXmlReport.getAbsolutePath()
        );

        assertEquals(64, result.exitCode);
        assertContains(result.err, "Error: Bootstrap execution failed");
        assertContains(result.err, "phase 27 bootstrap failure");
        assertFalse(result.out.contains("Examples:"));
        assertEquals(Arrays.asList("failing"), BootstrapEventLog.events());
        assertFalse(jsonReport.exists());
        assertFalse(junitXmlReport.exists());
    }

    @Test
    public void describeWithBootstrapConfigurationDoesNotExecuteHooks() throws Exception {
        File specRoot = temporaryFolder.newFolder("describe-bootstrap-spec-root");
        File sourceRoot = temporaryFolder.newFolder("describe-bootstrap-source-root");
        File configFile = writeConfig("describe-bootstrap.conf",
                suiteConfig("custom", specRoot, sourceRoot) +
                        "bootstrap = " + FailingBootstrapHook.class.getName() + "\n" +
                        "suite.custom.bootstrap = " + SuiteBootstrapHook.class.getName() + "\n");
        File specFile = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "DescribedWithBootstrapSpec.java");

        CommandResult result = run(
                "describe",
                "com.example.DescribedWithBootstrap",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom"
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "Generated specification: " + specFile.getPath());
        assertContains(result.out, "No production class was generated");
        assertEquals("", result.err);
        assertTrue(BootstrapEventLog.events().isEmpty());
        assertTrue(specFile.isFile());
    }

    public static final class TopLevelBootstrapHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            BootstrapEventLog.record("top-level:" + context.specs().size());
        }
    }

    public static final class DuplicateBootstrapHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            BootstrapEventLog.record("duplicate:" + context.specs().size());
        }
    }

    public static final class SuiteBootstrapHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            BootstrapEventLog.record("suite:" + context.specs().size());
        }
    }

    public static final class FailingBootstrapHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            BootstrapEventLog.record("failing");
            throw new IllegalStateException("phase 27 bootstrap failure");
        }
    }

    private File writeConfig(String fileName, String content) throws Exception {
        File configFile = temporaryFolder.newFile(fileName);
        writeFile(configFile, content);
        return configFile;
    }

    private static String suiteConfig(String suiteName, File specRoot, File sourceRoot) {
        return "defaultSuite=" + suiteName + "\n" +
                "suite." + suiteName + ".specDir=" + specRoot.getAbsolutePath() + "\n" +
                "suite." + suiteName + ".sourceDir=" + sourceRoot.getAbsolutePath() + "\n";
    }

    private static File testJavaRoot() {
        return new File(System.getProperty("user.dir"), "src/test/java");
    }

    private static void assertEmptyReportJson(String json) {
        assertContains(json, "\"schemaVersion\": 1");
        assertContains(json, "\"metadata\": {");
        assertContains(json, "\"timestamp\": \"");
        assertContains(json, "\"hostname\": \"");
        assertContains(json, "\"time\": 0");
        assertContains(json, "\"javaspec.report.schemaVersion\": \"1\"");
        assertContains(json, "\"javaspec.report.tool\": \"javaspec\"");
        assertContains(json, "\"summary\": {\n    \"total\": 0");
        assertContains(json, "\"passed\": 0");
        assertContains(json, "\"failed\": 0");
        assertContains(json, "\"broken\": 0");
        assertContains(json, "\"skipped\": 0");
        assertContains(json, "\"pending\": 0");
        assertContains(json, "\"successful\": true");
        assertContains(json, "\"specs\": []");
    }

    private static void assertEmptyJUnitXml(String xml) {
        assertContains(xml, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"0\" failures=\"0\" errors=\"0\" skipped=\"0\" timestamp=\"");
        assertContains(xml, "\" hostname=\"");
        assertContains(xml, "\" time=\"0\">\n  <properties>\n");
        assertContains(xml, "    <property name=\"javaspec.report.schemaVersion\" value=\"1\"/>\n");
        assertContains(xml, "    <property name=\"javaspec.report.tool\" value=\"javaspec\"/>\n");
        assertContains(xml, "  </properties>\n</testsuite>\n");
    }

    private static CommandResult run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = Main.run(
                args,
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(out),
                new PrintStream(err)
        );
        return new CommandResult(
                exitCode,
                new String(out.toByteArray(), StandardCharsets.UTF_8),
                new String(err.toByteArray(), StandardCharsets.UTF_8)
        );
    }

    private static void writeFile(File file, String content) throws Exception {
        File parent = file.getParentFile();
        assertTrue(parent == null || parent.isDirectory() || parent.mkdirs());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void assertContains(String value, String expected) {
        assertTrue("Expected to contain: " + expected + "\nActual value:\n" + value, value.contains(expected));
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String out;
        private final String err;

        private CommandResult(int exitCode, String out, String err) {
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }
    }
}
