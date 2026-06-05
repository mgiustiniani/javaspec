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

import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MainPhase29CompileCliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runCompileCompilesTemporarySourceAndSpecFilesAndExecutesThem() throws Exception {
        requireJdkCompiler();
        File sourceRoot = temporaryFolder.newFolder("compile-run-source-root");
        File specRoot = temporaryFolder.newFolder("compile-run-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "compile-run-classes");
        writeSource(sourceRoot, "com.phase29.CompiledSubject",
                "public class CompiledSubject {\n" +
                        "    public String message() {\n" +
                        "        return \"compiled\";\n" +
                        "    }\n" +
                        "}\n");
        writeSource(specRoot, "spec.com.phase29.CompiledSubjectSpec",
                "public class CompiledSubjectSpec {\n" +
                        "    public void it_executes_after_cli_compilation() {\n" +
                        "        if (!\"compiled\".equals(new com.phase29.CompiledSubject().message())) {\n" +
                        "            throw new AssertionError(\"compiled source was not executed\");\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--compile",
                "--compile-output", compileOutput.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "Found 1 specification(s) in " + specRoot.getAbsolutePath() + ".");
        assertContains(result.out, "Compiled 2 source file(s) to " + compileOutput.getAbsolutePath() + ".");
        assertContains(result.out, "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertEquals("", result.err);
        assertTrue(sourceFileFor(compileOutput, "com.phase29.CompiledSubject").getPath(),
                classFileFor(compileOutput, "com.phase29.CompiledSubject").isFile());
        assertTrue(sourceFileFor(compileOutput, "spec.com.phase29.CompiledSubjectSpec").getPath(),
                classFileFor(compileOutput, "spec.com.phase29.CompiledSubjectSpec").isFile());
    }

    @Test
    public void runCompileFailureExitsOneAndDoesNotWriteReports() throws Exception {
        requireJdkCompiler();
        File sourceRoot = temporaryFolder.newFolder("compile-failure-source-root");
        File specRoot = temporaryFolder.newFolder("compile-failure-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "compile-failure-classes");
        File jsonReport = new File(temporaryFolder.getRoot(), "compile-failure-report.json");
        File junitReport = new File(temporaryFolder.getRoot(), "compile-failure-report.xml");
        writeFile(sourceFileFor(sourceRoot, "com.phase29.BrokenSubject"),
                "package com.phase29;\n\n" +
                        "public class BrokenSubject {\n" +
                        "    public String message() {\n" +
                        "        return ;\n" +
                        "    }\n" +
                        "}\n");
        writeSource(specRoot, "spec.com.phase29.BrokenSubjectSpec",
                "public class BrokenSubjectSpec {\n" +
                        "    public void it_would_execute_after_compilation() {\n" +
                        "        new com.phase29.BrokenSubject().message();\n" +
                        "    }\n" +
                        "}\n");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--compile",
                "--compile-output", compileOutput.getAbsolutePath(),
                "--report", jsonReport.getAbsolutePath(),
                "--junit-xml", junitReport.getAbsolutePath()
        );

        assertEquals(1, result.exitCode);
        assertContains(result.out, "Found 1 specification(s) in " + specRoot.getAbsolutePath() + ".");
        assertContains(result.err, "Compilation failed:");
        assertContains(result.err, "BrokenSubject.java");
        assertFalse(result.out.contains("Examples:"));
        assertFalse(jsonReport.exists());
        assertFalse(junitReport.exists());
    }

    @Test
    public void noSpecRunCompileSkipsCompilationAndWritesReports() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("no-spec-compile-source-root");
        File specRoot = temporaryFolder.newFolder("no-spec-compile-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "no-spec-compile-classes");
        File jsonReport = new File(temporaryFolder.getRoot(), "no-spec-compile-report.json");
        File junitReport = new File(temporaryFolder.getRoot(), "no-spec-compile-report.xml");
        writeSource(sourceRoot, "com.phase29.NoSpecSubject",
                "public class NoSpecSubject {\n" +
                        "    public String message() { return \"unused\"; }\n" +
                        "}\n");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--compile",
                "--compile-output", compileOutput.getAbsolutePath(),
                "--report", jsonReport.getAbsolutePath(),
                "--junit-xml", junitReport.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "No specifications found in " + specRoot.getAbsolutePath() + ".");
        assertFalse(result.out.contains("Compiled "));
        assertEquals("", result.err);
        assertFalse(compileOutput.exists());
        assertTrue(jsonReport.isFile());
        assertTrue(junitReport.isFile());
        assertContains(readFile(jsonReport), "\"total\": 0");
        assertContains(readFile(junitReport), "<testsuite name=\"javaspec\" tests=\"0\"");
    }

    @Test
    public void dryRunCompileDoesNotCreateOutputDirectory() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("dry-run-compile-source-root");
        File specRoot = temporaryFolder.newFolder("dry-run-compile-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "dry-run-compile-classes");
        writeSource(sourceRoot, "com.phase29.DryRunSubject",
                "public class DryRunSubject {\n" +
                        "    public String message() { return \"dry-run\"; }\n" +
                        "}\n");
        writeSource(specRoot, "spec.com.phase29.DryRunSubjectSpec",
                "public class DryRunSubjectSpec {\n" +
                        "    public void it_is_not_compiled_during_dry_run() {\n" +
                        "        new com.phase29.DryRunSubject().message();\n" +
                        "    }\n" +
                        "}\n");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--compile",
                "--compile-output", compileOutput.getAbsolutePath(),
                "--dry-run"
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "Found 1 specification(s) in " + specRoot.getAbsolutePath() + ".");
        assertContains(result.out, "Dry-run found no pending generation/update work.");
        assertFalse(result.out.contains("Compiled "));
        assertFalse(compileOutput.exists());
        assertEquals("", result.err);
    }

    @Test
    public void describeRejectsCompileOptions() throws Exception {
        File specRoot = temporaryFolder.newFolder("describe-compile-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "describe-compile-classes");

        CommandResult compile = run(
                "describe",
                "com.phase29.DescribeCompile",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--compile"
        );
        CommandResult compileOutputResult = run(
                "describe",
                "com.phase29.DescribeCompileOutput",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--compile-output", compileOutput.getAbsolutePath()
        );

        assertEquals(64, compile.exitCode);
        assertEquals("", compile.out);
        assertContains(compile.err, "The --compile option belongs to run; describe does not execute examples.");
        assertContains(compile.err, "Usage:");
        assertEquals(64, compileOutputResult.exitCode);
        assertEquals("", compileOutputResult.out);
        assertContains(compileOutputResult.err, "The --compile-output option belongs to run; describe does not execute examples.");
        assertContains(compileOutputResult.err, "Usage:");
        assertFalse(compileOutput.exists());
        assertEquals(0, countFiles(specRoot));
    }

    private static void requireJdkCompiler() {
        assertNotNull("These tests require a JDK with javax.tools.JavaCompiler", ToolProvider.getSystemJavaCompiler());
    }

    private static File writeSource(File root, String qualifiedName, String typeBody) throws Exception {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = lastDot < 0 ? "" : qualifiedName.substring(0, lastDot);
        File sourceFile = sourceFileFor(root, qualifiedName);
        String content;
        if (packageName.length() == 0) {
            content = typeBody;
        } else {
            content = "package " + packageName + ";\n\n" + typeBody;
        }
        writeFile(sourceFile, content);
        return sourceFile;
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
