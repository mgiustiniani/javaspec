package org.javaspec.cli;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

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
    public void runCompileIncludesGeneratedSupportSourcesFromTarget() throws Exception {
        requireJdkCompiler();
        File generatedRoot = new File("target/generated-sources/javaspec");
        deleteRecursively(generatedRoot);
        File sourceRoot = temporaryFolder.newFolder("compile-generated-support-source-root");
        File specRoot = temporaryFolder.newFolder("compile-generated-support-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "compile-generated-support-classes");
        writeSource(sourceRoot, "com.phase29.SupportCompiledSubject",
                "public class SupportCompiledSubject {\n" +
                        "    public String message() { return \"compiled support\"; }\n" +
                        "}\n");
        writeSource(generatedRoot, "spec.com.phase29.SupportCompiledSubjectSpecSupport",
                "import com.phase29.SupportCompiledSubject;\n" +
                        "\n" +
                        "public class SupportCompiledSubjectSpecSupport {\n" +
                        "    protected String message() { return new SupportCompiledSubject().message(); }\n" +
                        "}\n");
        writeSource(specRoot, "spec.com.phase29.SupportCompiledSubjectSpec",
                "public class SupportCompiledSubjectSpec extends SupportCompiledSubjectSpecSupport {\n" +
                        "    public void it_compiles_with_generated_support() {\n" +
                        "        if (!\"compiled support\".equals(message())) {\n" +
                        "            throw new AssertionError(\"generated support was not compiled\");\n" +
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
        assertContains(result.out, "Compiled 2 source file(s) to " + compileOutput.getAbsolutePath() + ".");
        assertContains(result.out, "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertTrue(classFileFor(compileOutput, "spec.com.phase29.SupportCompiledSubjectSpecSupport").isFile());
        assertEquals("", result.err);
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
    public void runCompileUsesResolvePomDependenciesForCompilationAndExecution() throws Exception {
        requireJdkCompiler();
        File repo = temporaryFolder.newFolder("resolve-pom-repo");
        installFakeDependency(repo, "com.external", "helper", "1.0", "com.external.Helper",
                "public class Helper {\n" +
                        "    public static String message() { return \"from dependency\"; }\n" +
                        "}\n");
        File consumerProject = temporaryFolder.newFolder("consumer-project");
        File rootPom = new File(consumerProject, "pom.xml");
        writeFile(rootPom,
                "<project>\n" +
                        "  <groupId>consumer</groupId><artifactId>consumer</artifactId><version>1.0</version>\n" +
                        "  <dependencies>\n" +
                        "    <dependency>\n" +
                        "      <groupId>com.external</groupId><artifactId>helper</artifactId><version>1.0</version>\n" +
                        "    </dependency>\n" +
                        "  </dependencies>\n" +
                        "</project>\n");
        File sourceRoot = temporaryFolder.newFolder("resolve-pom-source-root");
        File specRoot = temporaryFolder.newFolder("resolve-pom-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "resolve-pom-classes");
        writeSource(specRoot, "spec.com.external.HelperSpec",
                "public class HelperSpec {\n" +
                        "    public void it_uses_external_dependency_from_resolved_pom() {\n" +
                        "        if (!\"from dependency\".equals(com.external.Helper.message())) {\n" +
                        "            throw new AssertionError(\"dependency was not resolved\");\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");
        String previousRepo = System.getProperty("maven.repo.local");
        try {
            System.setProperty("maven.repo.local", repo.getAbsolutePath());
            CommandResult result = run(
                    "run",
                    "--spec-dir", specRoot.getAbsolutePath(),
                    "--source-dir", sourceRoot.getAbsolutePath(),
                    "--resolve-pom", rootPom.getAbsolutePath(),
                    "--compile",
                    "--compile-output", compileOutput.getAbsolutePath()
            );
            assertEquals("stdout:\n" + result.out + "\nstderr:\n" + result.err, 0, result.exitCode);
            assertContains(result.out, "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
            assertEquals("", result.err);
        } finally {
            restoreProperty("maven.repo.local", previousRepo);
        }
    }

    @Test
    public void runCompileReleaseWritesExpectedClassFileMajorVersion() throws Exception {
        requireJdkCompiler();
        File sourceRoot = temporaryFolder.newFolder("release-source-root");
        File specRoot = temporaryFolder.newFolder("release-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "release-classes");
        writeSource(sourceRoot, "com.phase29.ReleaseSubject",
                "public class ReleaseSubject { public String message() { return \"release\"; } }\n");
        writeSource(specRoot, "spec.com.phase29.ReleaseSubjectSpec",
                "public class ReleaseSubjectSpec {\n" +
                        "    public void it_runs() { new com.phase29.ReleaseSubject().message(); }\n" +
                        "}\n");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--compile",
                "--release", "8",
                "--compile-output", compileOutput.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertEquals(52, classFileMajorVersion(classFileFor(compileOutput, "com.phase29.ReleaseSubject")));
    }

    @Test
    public void runCompileSecondRunUsesIncrementalCache() throws Exception {
        requireJdkCompiler();
        File sourceRoot = temporaryFolder.newFolder("cache-source-root");
        File specRoot = temporaryFolder.newFolder("cache-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "cache-classes");
        writeSource(sourceRoot, "com.phase29.CacheSubject",
                "public class CacheSubject { public String message() { return \"cache\"; } }\n");
        writeSource(specRoot, "spec.com.phase29.CacheSubjectSpec",
                "public class CacheSubjectSpec {\n" +
                        "    public void it_runs() { new com.phase29.CacheSubject().message(); }\n" +
                        "}\n");

        CommandResult first = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--compile",
                "--compile-output", compileOutput.getAbsolutePath()
        );
        CommandResult second = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--compile",
                "--compile-output", compileOutput.getAbsolutePath()
        );

        assertEquals(0, first.exitCode);
        assertContains(first.out, "Compiled 2 source file(s) to " + compileOutput.getAbsolutePath() + ".");
        assertEquals(0, second.exitCode);
        assertContains(second.out, "Compilation up to date (2 source file(s), "
                + compileOutput.getAbsolutePath() + ").");
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

    private static void installFakeDependency(
            File repo,
            String groupId,
            String artifactId,
            String version,
            String className,
            String sourceBody
    ) throws Exception {
        File work = new File(repo.getParentFile(), artifactId + "-work");
        File sourceRoot = new File(work, "src");
        File classes = new File(work, "classes");
        writeSource(sourceRoot, className, sourceBody);
        assertTrue(classes.mkdirs());
        int compileExit = ToolProvider.getSystemJavaCompiler().run(
                null, null, null,
                "-d", classes.getAbsolutePath(),
                sourceFileFor(sourceRoot, className).getAbsolutePath()
        );
        assertEquals(0, compileExit);

        File artifactDir = new File(repo,
                groupId.replace('.', File.separatorChar)
                        + File.separator + artifactId
                        + File.separator + version);
        assertTrue(artifactDir.mkdirs());
        File jarFile = new File(artifactDir, artifactId + "-" + version + ".jar");
        writeJar(jarFile, classes, className.replace('.', '/') + ".class");
        File pomFile = new File(artifactDir, artifactId + "-" + version + ".pom");
        writeFile(pomFile,
                "<project><groupId>" + groupId + "</groupId><artifactId>" + artifactId
                        + "</artifactId><version>" + version + "</version><dependencies/></project>");
    }

    private static void writeJar(File jarFile, File classesRoot, String classEntryName) throws Exception {
        JarOutputStream out = new JarOutputStream(new FileOutputStream(jarFile));
        try {
            JarEntry entry = new JarEntry(classEntryName);
            out.putNextEntry(entry);
            byte[] bytes = Files.readAllBytes(new File(classesRoot, classEntryName).toPath());
            out.write(bytes);
            out.closeEntry();
        } finally {
            out.close();
        }
    }

    private static int classFileMajorVersion(File classFile) throws Exception {
        FileInputStream in = new FileInputStream(classFile);
        try {
            byte[] header = new byte[8];
            int read = in.read(header);
            assertEquals(8, read);
            return ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
        } finally {
            in.close();
        }
    }

    private static void restoreProperty(String key, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previousValue);
        }
    }

    private static void deleteRecursively(File path) {
        if (path == null || !path.exists()) {
            return;
        }
        if (path.isDirectory()) {
            File[] children = path.listFiles();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    deleteRecursively(children[i]);
                }
            }
        }
        path.delete();
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
