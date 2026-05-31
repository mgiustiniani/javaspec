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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainConfigurationIntegrationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void describeUsesConfiguredSuiteSpecDirectory() throws Exception {
        File specRoot = temporaryFolder.newFolder("describe-config-spec");
        File sourceRoot = temporaryFolder.newFolder("describe-config-source");
        File configFile = writeConfig("describe.conf", suiteConfig("custom", specRoot, sourceRoot));
        File specFile = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java");
        File supportFile = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpecSupport.java");

        CommandResult result = run("describe", "com.example.Book", "--config", configFile.getAbsolutePath(), "--suite", "custom");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Generated specification: " + specFile.getPath()));
        assertTrue(result.out.contains("Generated specification support: " + supportFile.getPath()));
        assertEquals("", result.err);
        assertTrue(specFile.isFile());
        assertTrue(supportFile.isFile());
        assertEquals(0, countFiles(sourceRoot));
    }

    @Test
    public void runUsesConfiguredSuiteDirectoriesForDiscoveryAndGeneration() throws Exception {
        File specRoot = temporaryFolder.newFolder("run-config-spec");
        File sourceRoot = temporaryFolder.newFolder("run-config-source");
        File configFile = writeConfig("run.conf", suiteConfig("custom", specRoot, sourceRoot));
        writeSpec(specRoot, "spec.com.example.GeneratedFromConfigSpec");
        File targetFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "GeneratedFromConfig.java");

        CommandResult result = run("run", "--config", configFile.getAbsolutePath(), "--suite", "custom", "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Found 1 specification(s) in " + specRoot.getPath()));
        assertTrue(result.out.contains("Generated class skeleton: " + targetFile.getPath()));
        assertEquals("", result.err);
        assertTrue(targetFile.isFile());
        assertEquals("package com.example;\n\npublic class GeneratedFromConfig { }\n", readFile(targetFile));
    }

    @Test
    public void commandLineDirectoriesOverrideConfiguredSuiteDirectories() throws Exception {
        File configuredSpecRoot = temporaryFolder.newFolder("configured-spec-root");
        File configuredSourceRoot = temporaryFolder.newFolder("configured-source-root");
        File overrideSpecRoot = temporaryFolder.newFolder("override-spec-root");
        File overrideSourceRoot = temporaryFolder.newFolder("override-source-root");
        File configFile = writeConfig("overrides.conf", suiteConfig("custom", configuredSpecRoot, configuredSourceRoot));
        writeSpec(overrideSpecRoot, "spec.com.example.OverrideGeneratedSpec");
        File overrideTargetFile = new File(overrideSourceRoot, "com" + File.separator + "example" + File.separator + "OverrideGenerated.java");
        File configuredTargetFile = new File(configuredSourceRoot, "com" + File.separator + "example" + File.separator + "OverrideGenerated.java");

        CommandResult result = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--spec-dir", overrideSpecRoot.getAbsolutePath(),
                "--source-dir", overrideSourceRoot.getAbsolutePath(),
                "--generate"
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Found 1 specification(s) in " + overrideSpecRoot.getPath()));
        assertTrue(result.out.contains("Generated class skeleton: " + overrideTargetFile.getPath()));
        assertEquals("", result.err);
        assertTrue(overrideTargetFile.isFile());
        assertFalse(configuredTargetFile.exists());
        assertEquals(0, countFiles(configuredSourceRoot));
    }

    @Test
    public void constructorPolicyComesFromConfigurationAndCommandLineOverrideWins() throws Exception {
        File deleteSpecRoot = temporaryFolder.newFolder("delete-policy-spec");
        File deleteSourceRoot = temporaryFolder.newFolder("delete-policy-source");
        File deleteConfigFile = writeConfig("delete-policy.conf",
                "constructorPolicy=delete\n" + suiteConfig("custom", deleteSpecRoot, deleteSourceRoot));
        writeConstructorSpec(deleteSpecRoot, "Book");
        File bookSourceFile = writeLegacySource(deleteSourceRoot, "Book");

        CommandResult deleteResult = run("run", "--config", deleteConfigFile.getAbsolutePath(), "--suite", "custom", "--generate");

        assertEquals(0, deleteResult.exitCode);
        assertTrue(deleteResult.out.contains("Updated constructors in " + bookSourceFile.getPath() + " (policy: delete)"));
        assertEquals("", deleteResult.err);
        String deletedSource = readFile(bookSourceFile);
        assertTrue(deletedSource.contains("public Book(String title)"));
        assertFalse(deletedSource.contains("public Book(int legacy)"));
        assertFalse(deletedSource.contains("this.legacy = legacy;"));

        File preserveSpecRoot = temporaryFolder.newFolder("preserve-policy-spec");
        File preserveSourceRoot = temporaryFolder.newFolder("preserve-policy-source");
        File preserveConfigFile = writeConfig("preserve-policy.conf",
                "constructorPolicy=delete\n" + suiteConfig("custom", preserveSpecRoot, preserveSourceRoot));
        writeConstructorSpec(preserveSpecRoot, "Movie");
        File movieSourceFile = writeLegacySource(preserveSourceRoot, "Movie");

        CommandResult preserveResult = run(
                "run",
                "--config", preserveConfigFile.getAbsolutePath(),
                "--suite", "custom",
                "--constructor-policy", "preserve",
                "--generate"
        );

        assertEquals(0, preserveResult.exitCode);
        assertTrue(preserveResult.out.contains("Updated constructors in " + movieSourceFile.getPath() + " (policy: preserve)"));
        assertEquals("", preserveResult.err);
        String preservedSource = readFile(movieSourceFile);
        assertTrue(preservedSource.contains("public Movie(String title)"));
        assertTrue(preservedSource.contains("public Movie(int legacy)"));
        assertTrue(preservedSource.contains("this.legacy = legacy;"));
    }

    @Test
    public void invalidConfigSyntaxExitsUsageAndPrintsInvalidConfiguration() throws Exception {
        File configFile = writeConfig("invalid.conf", "profile java8\n");

        CommandResult result = run("run", "--config", configFile.getAbsolutePath());

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Invalid configuration"));
        assertTrue(result.err.contains("Line 1"));
    }

    @Test
    public void missingConfigFileExitsIoErrorAndPrintsConfigPath() {
        File missingConfigFile = new File(temporaryFolder.getRoot(), "missing.conf");

        CommandResult result = run("run", "--config", missingConfigFile.getAbsolutePath());

        assertEquals(70, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Config path"));
        assertTrue(result.err.contains(missingConfigFile.getPath()));
    }

    @Test
    public void missingSelectedSuiteExitsUsage() throws Exception {
        File configFile = writeConfig("missing-suite.conf", "formatter=progress\n");

        CommandResult result = run("run", "--config", configFile.getAbsolutePath(), "--suite", "missing");

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Invalid configuration"));
        assertTrue(result.err.contains("Suite 'missing'"));
        assertTrue(result.err.contains("Available suites"));
    }

    @Test
    public void describeAllowsConfiguredSourceDirectoryButRejectsCommandLineSourceDirectory() throws Exception {
        File specRoot = temporaryFolder.newFolder("describe-source-config-spec");
        File sourceRoot = temporaryFolder.newFolder("describe-source-config-source");
        File configFile = writeConfig("describe-source.conf", suiteConfig("custom", specRoot, sourceRoot));

        CommandResult configuredSourceResult = run("describe", "com.example.Configured", "--config", configFile.getAbsolutePath(), "--suite", "custom");
        CommandResult commandLineSourceResult = run(
                "describe",
                "com.example.Rejected",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--source-dir", sourceRoot.getAbsolutePath()
        );

        assertEquals(0, configuredSourceResult.exitCode);
        assertEquals("", configuredSourceResult.err);
        assertTrue(new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "ConfiguredSpec.java").isFile());
        assertEquals(64, commandLineSourceResult.exitCode);
        assertEquals("", commandLineSourceResult.out);
        assertTrue(commandLineSourceResult.err.contains("source directory is used by run"));
    }

    @Test
    public void helpListsConfigurationOptions() {
        CommandResult result = run("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("--config <file>"));
        assertTrue(result.out.contains("--suite <name>"));
        assertEquals("", result.err);
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

    private static File writeSpec(File specRoot, String specQualifiedName) throws Exception {
        int lastDot = specQualifiedName.lastIndexOf('.');
        String packageName = lastDot < 0 ? "" : specQualifiedName.substring(0, lastDot);
        String simpleName = lastDot < 0 ? specQualifiedName : specQualifiedName.substring(lastDot + 1);
        String relativePath;
        if (packageName.length() == 0) {
            relativePath = simpleName + ".java";
        } else {
            relativePath = packageName.replace('.', File.separatorChar) + File.separator + simpleName + ".java";
        }
        File specFile = new File(specRoot, relativePath);
        File parent = specFile.getParentFile();
        assertTrue(parent == null || parent.isDirectory() || parent.mkdirs());
        String content;
        if (packageName.length() == 0) {
            content = "public class " + simpleName + " {\n}\n";
        } else {
            content = "package " + packageName + "; public class " + simpleName + " {\n}\n";
        }
        writeFile(specFile, content);
        return specFile;
    }

    private static File writeConstructorSpec(File specRoot, String simpleName) throws Exception {
        File specFile = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + simpleName + "Spec.java");
        String content =
                "package spec.com.example;\n\n" +
                "import com.example." + simpleName + ";\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class " + simpleName + "Spec extends ObjectBehavior<" + simpleName + "> {\n" +
                "    public void let(String title) {\n" +
                "        beConstructedWith(title);\n" +
                "    }\n" +
                "}\n";
        writeFile(specFile, content);
        return specFile;
    }

    private static File writeLegacySource(File sourceRoot, String simpleName) throws Exception {
        File sourceFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + simpleName + ".java");
        String content =
                "package com.example;\n\n" +
                "public class " + simpleName + " {\n" +
                "    private int legacy;\n" +
                "\n" +
                "    public " + simpleName + "(int legacy) {\n" +
                "        this.legacy = legacy;\n" +
                "    }\n" +
                "}\n";
        writeFile(sourceFile, content);
        return sourceFile;
    }

    private static void writeFile(File file, String content) throws Exception {
        File parent = file.getParentFile();
        assertTrue(parent == null || parent.isDirectory() || parent.mkdirs());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
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
