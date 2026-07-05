package io.github.jvmspec.cli;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainPhase26ProfileEnforcementCliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String TEST_GENERATED_SOURCES = "target/generated-sources/javaspec";

    @Test
    public void runGenerateWithJava8ProfileRejectsRecordSpecBeforeWritingFiles() throws Exception {
        File specRoot = temporaryFolder.newFolder("java8-record-spec-root");
        File sourceRoot = temporaryFolder.newFolder("java8-record-source-root");
        writeRecordSpec(specRoot, "spec.com.example.ProfiledRecordSpec");
        File targetFile = sourceFileFor(sourceRoot, "com.example.ProfiledRecord");
        File supportFile = sourceFileFor(new File(TEST_GENERATED_SOURCES), "spec.com.example.ProfiledRecordSpecSupport");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--profile", "java8",
                "--generate"
        );

        assertEquals(64, result.exitCode);
        assertContains(result.out, "Found 1 specification(s) in " + specRoot.getAbsolutePath() + ".");
        assertContains(result.err, "Profile compatibility error");
        assertContains(result.err, "record requires Java 17");
        assertContains(result.err, "Selected profile: java8 (Java 8)");
        assertContains(result.err, "spec.com.example.ProfiledRecordSpec -> record com.example.ProfiledRecord");
        assertFalse(result.out.contains("Generated record skeleton"));
        assertFalse(result.out.contains("Updated specification support"));
        assertFalse(targetFile.exists());
        assertFalse(supportFile.exists());
        assertEquals(0, countFiles(sourceRoot));
        assertEquals(1, countFiles(specRoot));
    }

    @Test
    public void runGenerateWithJava17ProfileAllowsRecordSkeletonGeneration() throws Exception {
        File specRoot = temporaryFolder.newFolder("java17-record-spec-root");
        File sourceRoot = temporaryFolder.newFolder("java17-record-source-root");
        writeRecordSpec(specRoot, "spec.com.example.AllowedRecordSpec");
        File targetFile = sourceFileFor(sourceRoot, "com.example.AllowedRecord");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--profile", "java17",
                "--generate"
        );

        assertEquals(0, result.exitCode);
        assertEquals("", result.err);
        assertContains(result.out, "spec.com.example.AllowedRecordSpec describes missing record com.example.AllowedRecord.");
        assertContains(result.out, "Generated record skeleton: " + targetFile.getPath());
        assertTrue(targetFile.isFile());
        assertEquals("package com.example;\n\npublic record AllowedRecord() { }\n", readFile(targetFile));
    }

    @Test
    public void configuredProfileEnforcesAndCommandLineProfileOverrideWins() throws Exception {
        File specRoot = temporaryFolder.newFolder("config-profile-spec-root");
        File sourceRoot = temporaryFolder.newFolder("config-profile-source-root");
        File configFile = temporaryFolder.newFile("profile.conf");
        writeFile(configFile,
                "profile=java8\n" +
                "defaultSuite=custom\n" +
                "suite.custom.specDir=" + specRoot.getAbsolutePath() + "\n" +
                "suite.custom.sourceDir=" + sourceRoot.getAbsolutePath() + "\n");
        writeRecordSpec(specRoot, "spec.com.example.ConfiguredRecordSpec");
        File targetFile = sourceFileFor(sourceRoot, "com.example.ConfiguredRecord");

        CommandResult configuredJava8 = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--generate"
        );

        assertEquals(64, configuredJava8.exitCode);
        assertContains(configuredJava8.err, "Profile compatibility error");
        assertContains(configuredJava8.err, "Selected profile: java8 (Java 8)");
        assertFalse(targetFile.exists());
        assertEquals(0, countFiles(sourceRoot));

        CommandResult overriddenJava17 = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--profile", "java17",
                "--generate"
        );

        assertEquals(0, overriddenJava17.exitCode);
        assertEquals("", overriddenJava17.err);
        assertContains(overriddenJava17.out, "Generated record skeleton: " + targetFile.getPath());
        assertTrue(targetFile.isFile());
        assertEquals("package com.example;\n\npublic record ConfiguredRecord() { }\n", readFile(targetFile));
    }

    @Test
    public void dryRunWithIncompatibleProfileRejectsBeforeWritingReportsOrFiles() throws Exception {
        File specRoot = temporaryFolder.newFolder("dry-run-profile-spec-root");
        File sourceRoot = temporaryFolder.newFolder("dry-run-profile-source-root");
        File jsonReport = new File(temporaryFolder.getRoot(), "dry-run-profile-report.json");
        File junitXmlReport = new File(temporaryFolder.getRoot(), "dry-run-profile-junit.xml");
        writeRecordSpec(specRoot, "spec.com.example.DryRunRecordSpec");
        File targetFile = sourceFileFor(sourceRoot, "com.example.DryRunRecord");
        File supportFile = sourceFileFor(new File(TEST_GENERATED_SOURCES), "spec.com.example.DryRunRecordSpecSupport");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--dry-run",
                "--profile", "java8",
                "--report", jsonReport.getAbsolutePath(),
                "--junit-xml", junitXmlReport.getAbsolutePath()
        );

        assertEquals(64, result.exitCode);
        assertContains(result.err, "Profile compatibility error");
        assertFalse(result.out.contains("Would generate"));
        assertFalse(result.out.contains("Dry-run found pending generation/update work"));
        assertFalse(targetFile.exists());
        assertFalse(supportFile.exists());
        assertFalse(jsonReport.exists());
        assertFalse(junitXmlReport.exists());
        assertEquals(0, countFiles(sourceRoot));
    }

    @Test
    public void missingTypeWithNewerApiMethodSignatureIsRejectedBeforeSupportOrTypeGeneration() throws Exception {
        File specRoot = temporaryFolder.newFolder("api-signature-spec-root");
        File sourceRoot = temporaryFolder.newFolder("api-signature-source-root");
        writeSpec(
                specRoot,
                "spec.com.example.SequencedConsumerSpec",
                "    public void it_accepts_sequenced_collection(java.util.SequencedCollection<String> items) {\n" +
                "        subject().accept(items);\n" +
                "    }\n"
        );
        File targetFile = sourceFileFor(sourceRoot, "com.example.SequencedConsumer");
        File supportFile = sourceFileFor(new File(TEST_GENERATED_SOURCES), "spec.com.example.SequencedConsumerSpecSupport");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--profile", "java17",
                "--generate"
        );

        assertEquals(64, result.exitCode);
        assertContains(result.err, "Profile compatibility error");
        assertContains(result.err, "java.util.SequencedCollection requires Java 21");
        assertContains(result.err, "Spec/type: spec.com.example.SequencedConsumerSpec -> com.example.SequencedConsumer");
        assertFalse(result.out.contains("Updated specification support"));
        assertFalse(result.out.contains("Generated class skeleton"));
        assertFalse(supportFile.exists());
        assertFalse(targetFile.exists());
        assertEquals(0, countFiles(sourceRoot));
        assertEquals(1, countFiles(specRoot));
    }

    @Test
    public void runGenerateWithJava8ProfileRejectsPostProfileRelationshipTypesBeforeWritingReports() throws Exception {
        File specRoot = temporaryFolder.newFolder("phase36-relationship-spec-root");
        File sourceRoot = temporaryFolder.newFolder("phase36-relationship-source-root");
        File jsonReport = new File(temporaryFolder.getRoot(), "phase36-relationship-report.json");
        File junitXmlReport = new File(temporaryFolder.getRoot(), "phase36-relationship-junit.xml");
        writeSpec(
                specRoot,
                "spec.com.example.HttpProfileSpec",
                "    public void it_declares_post_profile_relationships() {\n" +
                "        shouldExtend(java.net.http.HttpRequest.class);\n" +
                "        shouldImplement(java.net.http.HttpClient.class);\n" +
                "    }\n"
        );
        File targetFile = sourceFileFor(sourceRoot, "com.example.HttpProfile");
        File supportFile = sourceFileFor(new File(TEST_GENERATED_SOURCES), "spec.com.example.HttpProfileSpecSupport");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--profile", "java8",
                "--generate",
                "--report", jsonReport.getAbsolutePath(),
                "--junit-xml", junitXmlReport.getAbsolutePath()
        );

        assertEquals(64, result.exitCode);
        assertContains(result.out, "Found 1 specification(s) in " + specRoot.getAbsolutePath() + ".");
        assertContains(result.err, "Profile compatibility error");
        assertContains(result.err, "java.net.http.HttpRequest requires Java 11");
        assertContains(result.err, "Selected profile: java8 (Java 8)");
        assertContains(result.err, "Spec/type: spec.com.example.HttpProfileSpec -> com.example.HttpProfile");
        assertContains(result.err, "super type: java.net.http.HttpRequest requires Java 11");
        assertContains(result.err, "implemented type 1: java.net.http.HttpClient requires Java 11");
        assertFalse(result.out.contains("Generated class skeleton"));
        assertFalse(result.out.contains("Updated specification support"));
        assertFalse(targetFile.exists());
        assertFalse(supportFile.exists());
        assertFalse(jsonReport.exists());
        assertFalse(junitXmlReport.exists());
        assertEquals(0, countFiles(sourceRoot));
        assertEquals(1, countFiles(specRoot));
    }

    @Test
    public void describeWithConfiguredProfileStillGeneratesSpecificationOnly() throws Exception {
        File specRoot = temporaryFolder.newFolder("describe-config-profile-spec-root");
        File sourceRoot = temporaryFolder.newFolder("describe-config-profile-source-root");
        File configFile = temporaryFolder.newFile("describe-profile.conf");
        writeFile(configFile,
                "profile=java8\n" +
                "defaultSuite=custom\n" +
                "suite.custom.specDir=" + specRoot.getAbsolutePath() + "\n" +
                "suite.custom.sourceDir=" + sourceRoot.getAbsolutePath() + "\n");
        File specFile = sourceFileFor(specRoot, "spec.com.example.DescribedWithProfileSpec");
        File supportFile = sourceFileFor(new File(TEST_GENERATED_SOURCES), "spec.com.example.DescribedWithProfileSpecSupport");

        CommandResult result = run(
                "describe",
                "com.example.DescribedWithProfile",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom"
        );

        assertEquals(0, result.exitCode);
        assertEquals("", result.err);
        assertContains(result.out, "Generated specification: " + specFile.getPath());
        assertContains(result.out, "Generated specification support: " + supportFile.getPath());
        assertContains(result.out, "No production class was generated");
        assertTrue(specFile.isFile());
        assertTrue(supportFile.isFile());
        assertFalse(sourceFileFor(specRoot, "spec.com.example.DescribedWithProfileSpecSupport").exists());
        assertEquals(0, countFiles(sourceRoot));
    }

    private static File writeRecordSpec(File specRoot, String specQualifiedName) throws Exception {
        return writeSpec(
                specRoot,
                specQualifiedName,
                "    public void it_is_declared_as_record() {\n" +
                "        shouldBeARecord();\n" +
                "    }\n"
        );
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
