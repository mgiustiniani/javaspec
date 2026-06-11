package org.javaspec.cli;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainPhase11ReportCliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void reportOptionWritesUtf8JsonForPassingExamples() throws Exception {
        File reportFile = new File(temporaryFolder.getRoot(), "passing-report.json");
        File sourceRoot = temporaryFolder.newFolder("passing-report-source-root");

        CommandResult result = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_passes",
                "--report", reportFile.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
        assertEquals("", result.err);
        assertTrue(reportFile.isFile());
        String json = readFile(reportFile);
        assertContains(json, "\"schemaVersion\": 1");
        assertDefaultReportMetadata(json);
        assertContains(json, "\"total\": 1");
        assertContains(json, "\"passed\": 1");
        assertContains(json, "\"successful\": true");
        String specName = "spec.org.javaspec.fixtures.cli.FailingSubjectSpec";
        String exampleId = specName + "#it_passes";
        String specFilePath = failingSubjectSpecFile().getPath();
        assertContains(json, "\"name\": \"spec.org.javaspec.fixtures.cli.FailingSubjectSpec\"");
        assertContains(json, "\"id\": " + jsonString(specName));
        assertContains(json, "\"stableId\": " + jsonString(specName));
        assertContains(json, "\"sourceFile\": " + jsonString(specFilePath));
        assertContains(json, "\"id\": " + jsonString(exampleId));
        assertContains(json, "\"stableId\": " + jsonString(exampleId));
        assertContains(json, "\"fullName\": " + jsonString(exampleId));
        assertContains(json, "\"method\": \"it_passes\"");
        assertContains(json, "\"source\": {");
        assertContains(json, "\"file\": " + jsonString(specFilePath));
        assertContains(json, "\"line\": 4");
        assertContains(json, "\"status\": \"PASSED\"");
        assertContains(json, "\"failure\": null");
    }

    @Test
    public void reportOptionWritesUtf8JsonBeforeReturningFailureExitCode() throws Exception {
        File reportFile = new File(temporaryFolder.getRoot(), "failing-report.json");
        File sourceRoot = temporaryFolder.newFolder("failing-report-source-root");

        CommandResult result = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_fails",
                "--report", reportFile.getAbsolutePath()
        );

        assertEquals(1, result.exitCode);
        assertTrue(result.out.contains("Examples: 1 total, 0 passed, 1 failed, 0 broken, 0 skipped, 0 pending."));
        assertEquals("", result.err);
        assertTrue(reportFile.isFile());
        String json = readFile(reportFile);
        assertContains(json, "\"successful\": false");
        assertContains(json, "\"failed\": 1");
        assertContains(json, "\"status\": \"FAILED\"");
        assertContains(json, "\"detail\": \"Assertion failed\"");
        assertContains(json, "\"throwableClassName\": \"java.lang.AssertionError\"");
        assertContains(json, "\"message\": \"cli failure\"");
    }

    @Test
    public void reportFileAliasWritesReport() throws Exception {
        File reportFile = new File(temporaryFolder.getRoot(), "alias-report.json");
        File sourceRoot = temporaryFolder.newFolder("alias-report-source-root");

        CommandResult result = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_passes",
                "--report-file", reportFile.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertEquals("", result.err);
        assertTrue(reportFile.isFile());
        assertContains(readFile(reportFile), "\"status\": \"PASSED\"");
    }

    @Test
    public void noSpecRunWithReportWritesValidEmptyReport() throws Exception {
        File specRoot = temporaryFolder.newFolder("empty-report-spec-root");
        File sourceRoot = temporaryFolder.newFolder("empty-report-source-root");
        File reportFile = new File(temporaryFolder.getRoot(), "empty-report.json");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--report", reportFile.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("No specifications found in " + specRoot.getAbsolutePath() + "."));
        assertEquals("", result.err);
        assertTrue(reportFile.isFile());
        assertEmptyReportJson(readFile(reportFile));
    }

    @Test
    public void dryRunWithPendingGenerationDoesNotWriteReport() throws Exception {
        File specRoot = temporaryFolder.newFolder("dry-run-report-spec-root");
        File sourceRoot = temporaryFolder.newFolder("dry-run-report-source-root");
        File reportFile = new File(temporaryFolder.getRoot(), "dry-run-report.json");
        writeSpec(specRoot, "spec.com.example.DryRunReportSpec", "");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--dry-run",
                "--report", reportFile.getAbsolutePath()
        );

        assertEquals(1, result.exitCode);
        assertTrue(result.out.contains("Dry-run found pending generation/update work; no files were written."));
        assertEquals("", result.err);
        assertFalse(reportFile.exists());
    }

    @Test
    public void verboseRunPrintsReportPathWhenSpecified() throws Exception {
        File specRoot = temporaryFolder.newFolder("verbose-report-spec-root");
        File sourceRoot = temporaryFolder.newFolder("verbose-report-source-root");
        File reportFile = new File(temporaryFolder.getRoot(), "verbose-report.json");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--verbose",
                "--report", reportFile.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Run configuration:"));
        assertTrue(result.out.contains("  Report path: " + reportFile.getAbsolutePath()));
        assertEquals("", result.err);
        assertTrue(reportFile.isFile());
    }

    @Test
    public void describeRejectsReportAsRunOnlyOption() throws Exception {
        File specRoot = temporaryFolder.newFolder("describe-report-spec-root");
        File reportFile = new File(temporaryFolder.getRoot(), "describe-report.json");

        CommandResult result = run(
                "describe",
                "com.example.Reported",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--report", reportFile.getAbsolutePath()
        );

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("The --report option belongs to run; describe does not execute examples."));
        assertTrue(result.err.contains("Usage:"));
        assertFalse(reportFile.exists());
        assertEquals(0, countFiles(specRoot));
    }

    @Test
    public void helpDocumentsReportOptions() {
        CommandResult result = run("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("--report <file>"));
        assertTrue(result.out.contains("--report-file <file>"));
        assertEquals("", result.err);
    }

    @Test
    public void invalidFormatterMessageStillListsBuiltInNames() {
        CommandResult result = run("run", "--formatter", "dots");

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Invalid formatter: dots. Valid values: progress, pretty."));
        assertTrue(result.err.contains("Usage:"));
    }

    private static void assertEmptyReportJson(String json) {
        assertContains(json, "\"schemaVersion\": 1");
        assertDefaultReportMetadata(json);
        assertContains(json, "\"summary\": {\n    \"total\": 0");
        assertContains(json, "\"passed\": 0");
        assertContains(json, "\"failed\": 0");
        assertContains(json, "\"broken\": 0");
        assertContains(json, "\"skipped\": 0");
        assertContains(json, "\"pending\": 0");
        assertContains(json, "\"successful\": true");
        assertContains(json, "\"specs\": []");
    }

    private static void assertDefaultReportMetadata(String json) {
        assertContains(json, "\"metadata\": {");
        assertContains(json, "\"timestamp\": \"");
        assertContains(json, "\"hostname\": \"");
        assertContains(json, "\"time\": 0");
        assertContains(json, "\"javaspec.report.schemaVersion\": \"1\"");
        assertContains(json, "\"javaspec.report.tool\": \"javaspec\"");
    }

    private static File testJavaRoot() {
        return new File(System.getProperty("user.dir"), "src/test/java");
    }

    private static File failingSubjectSpecFile() {
        return sourceFileFor(testJavaRoot(), "spec.org.javaspec.fixtures.cli.FailingSubjectSpec");
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static File writeSpec(File specRoot, String specQualifiedName, String body) throws Exception {
        int lastDot = specQualifiedName.lastIndexOf('.');
        String packageName = lastDot < 0 ? "" : specQualifiedName.substring(0, lastDot);
        String simpleName = lastDot < 0 ? specQualifiedName : specQualifiedName.substring(lastDot + 1);
        File specFile = sourceFileFor(specRoot, specQualifiedName);
        String content;
        if (packageName.length() == 0) {
            content = "public class " + simpleName + " {\n" + body + "}\n";
        } else {
            content = "package " + packageName + ";\n\n" +
                    "public class " + simpleName + " {\n" + body + "}\n";
        }
        writeFile(specFile, content);
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

    private static int countFiles(File root) {
        File[] files = root.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                count += countFiles(file);
            } else {
                count++;
            }
        }
        return count;
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
