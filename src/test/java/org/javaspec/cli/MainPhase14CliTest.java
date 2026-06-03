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

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainPhase14CliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void classpathOptionRunsCompiledSpecsFromExplicitPathListAndVerbosePrintsEntries() throws Exception {
        CompiledSpecFixture fixture = compiledSpecFixture("ExplicitSubject", "it_uses_explicit_classpath");
        File unusedEntry = temporaryFolder.newFolder("unused-classpath-entry");
        String pathList = unusedEntry.getAbsolutePath() + File.pathSeparator + fixture.classesDirectory.getAbsolutePath();

        CommandResult result = run(
                "run",
                "--spec-dir", fixture.specRoot.getAbsolutePath(),
                "--source-dir", fixture.sourceRoot.getAbsolutePath(),
                "--classpath", pathList,
                "--verbose"
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "Run configuration:");
        assertContains(result.out, "  Explicit classpath entries:");
        assertContains(result.out, "    " + unusedEntry.getAbsolutePath());
        assertContains(result.out, "    " + fixture.classesDirectory.getAbsolutePath());
        assertContains(result.out, "Classpath: present");
        assertContains(result.out, "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped.");
        assertEquals("", result.err);
    }

    @Test
    public void classpathFileReadsUtf8EntriesAndIgnoresBlankAndCommentLines() throws Exception {
        CompiledSpecFixture fixture = compiledSpecFixture("ClasspathFileSubject", "it_uses_classpath_file");
        File classpathFile = new File(temporaryFolder.getRoot(), "explicit-classpath.txt");
        writeFile(classpathFile, "# UTF-8 comment: café\n\n  # indented comment\n  "
                + fixture.classesDirectory.getAbsolutePath() + "  \n");

        CommandResult result = run(
                "run",
                "--spec-dir", fixture.specRoot.getAbsolutePath(),
                "--source-dir", fixture.sourceRoot.getAbsolutePath(),
                "--classpath-file", classpathFile.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "Classpath: present");
        assertContains(result.out, "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped.");
        assertEquals("", result.err);
    }

    @Test
    public void describeRejectsRunOnlyClasspathOptionsWithUsageExit() throws Exception {
        File specRoot = temporaryFolder.newFolder("describe-classpath-spec-root");
        File classpathFile = new File(temporaryFolder.getRoot(), "describe-classpath.txt");
        writeFile(classpathFile, temporaryFolder.getRoot().getAbsolutePath() + "\n");

        CommandResult classpath = run(
                "describe",
                "com.example.DescribeClasspath",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--classpath", temporaryFolder.getRoot().getAbsolutePath()
        );
        CommandResult classpathFileResult = run(
                "describe",
                "com.example.DescribeClasspathFile",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--classpath-file", classpathFile.getAbsolutePath()
        );

        assertEquals(64, classpath.exitCode);
        assertEquals("", classpath.out);
        assertContains(classpath.err, "The --classpath option belongs to run; describe does not execute examples.");
        assertContains(classpath.err, "Usage:");
        assertEquals(64, classpathFileResult.exitCode);
        assertEquals("", classpathFileResult.out);
        assertContains(classpathFileResult.err, "The --classpath-file option belongs to run; describe does not execute examples.");
        assertContains(classpathFileResult.err, "Usage:");
        assertEquals(0, countFiles(specRoot));
    }

    @Test
    public void invalidClasspathFileReturnsIoExitAndPathDiagnostics() throws Exception {
        File specRoot = temporaryFolder.newFolder("invalid-classpath-file-spec-root");
        File sourceRoot = temporaryFolder.newFolder("invalid-classpath-file-source-root");
        File missingClasspathFile = new File(temporaryFolder.getRoot(), "missing-classpath.txt");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--classpath-file", missingClasspathFile.getAbsolutePath()
        );

        assertEquals(70, result.exitCode);
        assertEquals("", result.out);
        assertContains(result.err, "I/O error while reading classpath file:");
        assertContains(result.err, "Classpath file: " + missingClasspathFile.getAbsolutePath());
    }

    @Test
    public void junitXmlOptionWritesNoSpecReport() throws Exception {
        File specRoot = temporaryFolder.newFolder("empty-junit-xml-spec-root");
        File sourceRoot = temporaryFolder.newFolder("empty-junit-xml-source-root");
        File xmlFile = new File(temporaryFolder.getRoot(), "empty-junit.xml");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--junit-xml", xmlFile.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "No specifications found in " + specRoot.getAbsolutePath() + ".");
        assertEquals("", result.err);
        assertTrue(xmlFile.isFile());
        assertEquals(emptyJUnitXml(), readFile(xmlFile));
        assertParsesAsXml(readFile(xmlFile));
    }

    @Test
    public void junitXmlFileAliasWritesNormalRunReport() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("alias-junit-xml-source-root");
        File xmlFile = new File(temporaryFolder.getRoot(), "passing-alias-junit.xml");

        CommandResult result = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_passes",
                "--junit-xml-file", xmlFile.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped.");
        assertEquals("", result.err);
        String xml = readFile(xmlFile);
        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"0\" time=\"0\">");
        assertContains(xml, "<testcase classname=\"spec.org.javaspec.fixtures.cli.FailingSubjectSpec\" name=\"it_passes\" time=\"0\"/>");
        assertParsesAsXml(xml);
    }

    @Test
    public void junitXmlReportIsWrittenBeforeFailureExitCodeIsReturned() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("failing-junit-xml-source-root");
        File xmlFile = new File(temporaryFolder.getRoot(), "failing-junit.xml");

        CommandResult result = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_fails",
                "--junit-xml", xmlFile.getAbsolutePath()
        );

        assertEquals(1, result.exitCode);
        assertContains(result.out, "Examples: 1 total, 0 passed, 1 failed, 0 broken, 0 skipped.");
        assertEquals("", result.err);
        String xml = readFile(xmlFile);
        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"1\" failures=\"1\" errors=\"0\" skipped=\"0\" time=\"0\">");
        assertContains(xml, "<failure type=\"java.lang.AssertionError\" message=\"cli failure\">Assertion failed");
        assertParsesAsXml(xml);
    }

    @Test
    public void junitXmlReportWriteFailureReturnsIoExit() throws Exception {
        File specRoot = temporaryFolder.newFolder("write-failure-junit-xml-spec-root");
        File sourceRoot = temporaryFolder.newFolder("write-failure-junit-xml-source-root");
        File directoryInsteadOfFile = temporaryFolder.newFolder("junit-xml-directory-report");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--junit-xml", directoryInsteadOfFile.getAbsolutePath()
        );

        assertEquals(70, result.exitCode);
        assertContains(result.out, "No specifications found in " + specRoot.getAbsolutePath() + ".");
        assertContains(result.err, "I/O error while writing JUnit XML report:");
        assertContains(result.err, "JUnit XML path: " + directoryInsteadOfFile.getAbsolutePath());
    }

    @Test
    public void describeRejectsRunOnlyJunitXmlReportOptions() throws Exception {
        File specRoot = temporaryFolder.newFolder("describe-junit-xml-spec-root");
        File xmlFile = new File(temporaryFolder.getRoot(), "describe-junit.xml");
        File aliasFile = new File(temporaryFolder.getRoot(), "describe-junit-alias.xml");

        CommandResult junitXml = run(
                "describe",
                "com.example.Reported",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--junit-xml", xmlFile.getAbsolutePath()
        );
        CommandResult junitXmlAlias = run(
                "describe",
                "com.example.ReportedAlias",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--junit-xml-file", aliasFile.getAbsolutePath()
        );

        assertEquals(64, junitXml.exitCode);
        assertEquals("", junitXml.out);
        assertContains(junitXml.err, "The --junit-xml option belongs to run; describe does not execute examples.");
        assertContains(junitXml.err, "Usage:");
        assertEquals(64, junitXmlAlias.exitCode);
        assertEquals("", junitXmlAlias.out);
        assertContains(junitXmlAlias.err, "The --junit-xml-file option belongs to run; describe does not execute examples.");
        assertContains(junitXmlAlias.err, "Usage:");
        assertFalse(xmlFile.exists());
        assertFalse(aliasFile.exists());
        assertEquals(0, countFiles(specRoot));
    }

    @Test
    public void dryRunWithPendingGenerationDoesNotWriteJunitXmlReport() throws Exception {
        File specRoot = temporaryFolder.newFolder("dry-run-junit-xml-spec-root");
        File sourceRoot = temporaryFolder.newFolder("dry-run-junit-xml-source-root");
        File xmlFile = new File(temporaryFolder.getRoot(), "dry-run-junit.xml");
        writeSpec(specRoot, "spec.com.example.DryRunJunitXmlSpec", "");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--dry-run",
                "--junit-xml", xmlFile.getAbsolutePath()
        );

        assertEquals(1, result.exitCode);
        assertContains(result.out, "Dry-run found pending generation/update work; no files were written.");
        assertEquals("", result.err);
        assertFalse(xmlFile.exists());
    }

    @Test
    public void jsonAndJunitXmlReportsCanBeRequestedTogether() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("combined-report-source-root");
        File jsonFile = new File(temporaryFolder.getRoot(), "combined-report.json");
        File xmlFile = new File(temporaryFolder.getRoot(), "combined-report.xml");

        CommandResult result = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_passes",
                "--report", jsonFile.getAbsolutePath(),
                "--junit-xml", xmlFile.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertEquals("", result.err);
        assertTrue(jsonFile.isFile());
        assertTrue(xmlFile.isFile());
        assertContains(readFile(jsonFile), "\"status\": \"PASSED\"");
        assertContains(readFile(xmlFile), "<testcase classname=\"spec.org.javaspec.fixtures.cli.FailingSubjectSpec\" name=\"it_passes\" time=\"0\"/>");
        assertParsesAsXml(readFile(xmlFile));
    }

    private CompiledSpecFixture compiledSpecFixture(String describedSimpleName, String exampleMethodName) throws Exception {
        File specRoot = temporaryFolder.newFolder(describedSimpleName + "-spec-root");
        File sourceRoot = temporaryFolder.newFolder(describedSimpleName + "-source-root");
        File compilationSourceRoot = temporaryFolder.newFolder(describedSimpleName + "-compilation-sources");
        File classesDirectory = temporaryFolder.newFolder(describedSimpleName + "-classes");
        File productionSource = sourceFileFor(compilationSourceRoot, "com.example." + describedSimpleName);
        File specSource = sourceFileFor(specRoot, "spec.com.example." + describedSimpleName + "Spec");

        writeFile(productionSource, "package com.example;\n\n" +
                "public class " + describedSimpleName + " {\n" +
                "    public String value() {\n" +
                "        return \"ok\";\n" +
                "    }\n" +
                "}\n");
        writeFile(specSource, "package spec.com.example;\n\n" +
                "public class " + describedSimpleName + "Spec {\n" +
                "    public void " + exampleMethodName + "() {\n" +
                "        if (!\"ok\".equals(new com.example." + describedSimpleName + "().value())) {\n" +
                "            throw new AssertionError(\"explicit classpath was not used\");\n" +
                "        }\n" +
                "    }\n" +
                "}\n");
        compile(classesDirectory, productionSource, specSource);
        return new CompiledSpecFixture(specRoot, sourceRoot, classesDirectory);
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

    private static File testJavaRoot() {
        return new File(System.getProperty("user.dir"), "src/test/java");
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

    private static String emptyJUnitXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<testsuite name=\"javaspec\" tests=\"0\" failures=\"0\" errors=\"0\" skipped=\"0\" time=\"0\">\n" +
                "</testsuite>\n";
    }

    private static void assertParsesAsXml(String xml) throws Exception {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );
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

    private static final class CompiledSpecFixture {
        private final File specRoot;
        private final File sourceRoot;
        private final File classesDirectory;

        private CompiledSpecFixture(File specRoot, File sourceRoot, File classesDirectory) {
            this.specRoot = specRoot;
            this.sourceRoot = sourceRoot;
            this.classesDirectory = classesDirectory;
        }
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
