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

public class MainPhase9CliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void dryRunReportsMissingProductionWithoutWritingOrPrompting() throws Exception {
        File specRoot = temporaryFolder.newFolder("dry-run-missing-spec-root");
        File sourceRoot = temporaryFolder.newFolder("dry-run-missing-source-root");
        writeSpec(specRoot, "spec.com.example.DryRunMissingSpec", "");
        File targetFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "DryRunMissing.java");

        CommandResult result = runWithInput(
                "n\n",
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--dry-run"
        );

        assertEquals(1, result.exitCode);
        assertTrue(result.out.contains("spec.com.example.DryRunMissingSpec describes missing class com.example.DryRunMissing."));
        assertTrue(result.out.contains("Would generate class skeleton: " + targetFile.getPath()));
        assertTrue(result.out.contains("Dry-run found pending generation/update work; no files were written."));
        assertFalse(result.out.contains("Do you want me to create"));
        assertEquals("", result.err);
        assertFalse(targetFile.exists());
        assertEquals(0, countFiles(sourceRoot));
    }

    @Test
    public void dryRunReportsConstructorAndMethodUpdatesWithoutChangingSourceOrPrompting() throws Exception {
        File specRoot = temporaryFolder.newFolder("dry-run-update-spec-root");
        File sourceRoot = temporaryFolder.newFolder("dry-run-update-source-root");
        writeSpec(specRoot, "spec.com.example.BookSpec",
                "    public void let(String title) {\n" +
                "        beConstructedWith(title);\n" +
                "    }\n" +
                "\n" +
                "    public void it_has_title() {\n" +
                "        getTitle().shouldReturn(\"Wizard\");\n" +
                "    }\n");
        File sourceFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Book.java");
        File supportFile = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpecSupport.java");
        String originalSource =
                "package com.example;\n" +
                "\n" +
                "public class Book {\n" +
                "    private int legacy;\n" +
                "\n" +
                "    public Book(int legacy) {\n" +
                "        this.legacy = legacy;\n" +
                "    }\n" +
                "}\n";
        writeFile(sourceFile, originalSource);

        CommandResult result = runWithInput(
                "y\n",
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--dry-run"
        );

        assertEquals(1, result.exitCode);
        assertTrue(result.out.contains("Would generate specification support: " + supportFile.getPath()));
        assertTrue(result.out.contains("Would update constructors in " + sourceFile.getPath() + " (policy: comment)"));
        assertTrue(result.out.contains("Would update methods in " + sourceFile.getPath()));
        assertTrue(result.out.contains("Dry-run found pending generation/update work; no files were written."));
        assertFalse(result.out.contains("Do you want me to add missing method skeletons"));
        assertEquals("", result.err);
        assertEquals(originalSource, readFile(sourceFile));
        assertFalse(supportFile.exists());
    }

    @Test
    public void dryRunReturnsOkWhenNoPendingWorkAndExamplesPassOrSkip() throws Exception {
        File passSourceRoot = temporaryFolder.newFolder("dry-run-pass-source-root");
        CommandResult passingResult = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", passSourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_passes",
                "--dry-run"
        );

        assertEquals(0, passingResult.exitCode);
        assertTrue(passingResult.out.contains("Dry-run found no pending generation/update work."));
        assertTrue(passingResult.out.contains("Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
        assertEquals("", passingResult.err);

        File skipSpecRoot = temporaryFolder.newFolder("dry-run-skip-spec-root");
        File skipSourceRoot = temporaryFolder.newFolder("dry-run-skip-source-root");
        writeSpec(skipSpecRoot, "spec.com.example.ExistingSkipSpec",
                "    public void it_is_skipped() {\n" +
                "    }\n");
        writeFile(new File(skipSourceRoot, "com" + File.separator + "example" + File.separator + "ExistingSkip.java"),
                "package com.example;\n\npublic class ExistingSkip { }\n");

        CommandResult skippedResult = run(
                "run",
                "--spec-dir", skipSpecRoot.getAbsolutePath(),
                "--source-dir", skipSourceRoot.getAbsolutePath(),
                "--dry-run"
        );

        assertEquals(0, skippedResult.exitCode);
        assertTrue(skippedResult.out.contains("Dry-run found no pending generation/update work."));
        assertTrue(skippedResult.out.contains("Examples: 1 total, 0 passed, 0 failed, 0 broken, 1 skipped, 0 pending."));
        assertEquals("", skippedResult.err);
    }

    @Test
    public void stopOnFailureStopsAfterFailedOrBrokenExampleAndDefaultRunsAllExamples() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("stop-on-failure-source-root");

        CommandResult stopAfterFailureResult = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.Phase9FailureStopSubject",
                "--formatter", "pretty",
                "--stop-on-failure"
        );

        assertEquals(1, stopAfterFailureResult.exitCode);
        assertTrue(stopAfterFailureResult.out.contains("Examples: 1 total, 0 passed, 1 failed, 0 broken, 0 skipped, 0 pending."));
        assertTrue(stopAfterFailureResult.out.contains("FAILED spec.org.javaspec.fixtures.cli.Phase9FailureStopSubjectSpec#it_fails_first"));
        assertFalse(stopAfterFailureResult.out.contains("it_passes_after_failure"));
        assertFalse(stopAfterFailureResult.out.contains("it_is_broken_after_failure"));
        assertEquals("", stopAfterFailureResult.err);

        CommandResult defaultFailureResult = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.Phase9FailureStopSubject",
                "--formatter", "pretty"
        );

        assertEquals(1, defaultFailureResult.exitCode);
        assertTrue(defaultFailureResult.out.contains("Examples: 3 total, 1 passed, 1 failed, 1 broken, 0 skipped, 0 pending."));
        assertTrue(defaultFailureResult.out.contains("FAILED spec.org.javaspec.fixtures.cli.Phase9FailureStopSubjectSpec#it_fails_first"));
        assertTrue(defaultFailureResult.out.contains("PASSED spec.org.javaspec.fixtures.cli.Phase9FailureStopSubjectSpec#it_passes_after_failure"));
        assertTrue(defaultFailureResult.out.contains("BROKEN spec.org.javaspec.fixtures.cli.Phase9FailureStopSubjectSpec#it_is_broken_after_failure"));
        assertEquals("", defaultFailureResult.err);

        CommandResult stopAfterBrokenResult = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.Phase9BrokenStopSubject",
                "--formatter", "pretty",
                "--stop-on-failure"
        );

        assertEquals(1, stopAfterBrokenResult.exitCode);
        assertTrue(stopAfterBrokenResult.out.contains("Examples: 1 total, 0 passed, 0 failed, 1 broken, 0 skipped, 0 pending."));
        assertTrue(stopAfterBrokenResult.out.contains("BROKEN spec.org.javaspec.fixtures.cli.Phase9BrokenStopSubjectSpec#it_breaks_first"));
        assertFalse(stopAfterBrokenResult.out.contains("it_passes_after_broken"));
        assertEquals("", stopAfterBrokenResult.err);
    }

    @Test
    public void progressFormatterIsConciseAndPrettyFormatterPrintsPerExampleStatuses() throws Exception {
        File progressSourceRoot = temporaryFolder.newFolder("progress-formatter-source-root");
        CommandResult progressResult = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", progressSourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_passes",
                "--formatter", "progress"
        );

        assertEquals(0, progressResult.exitCode);
        assertTrue(progressResult.out.contains("Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
        assertFalse(progressResult.out.contains("Example results:"));
        assertFalse(progressResult.out.contains("PASSED spec.org.javaspec.fixtures.cli.FailingSubjectSpec#it_passes"));
        assertEquals("", progressResult.err);

        File prettySourceRoot = temporaryFolder.newFolder("pretty-formatter-source-root");
        CommandResult prettyResult = run(
                "run",
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", prettySourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_passes",
                "--formatter", "pretty"
        );

        assertEquals(0, prettyResult.exitCode);
        assertTrue(prettyResult.out.contains("Example results:"));
        assertTrue(prettyResult.out.contains("PASSED spec.org.javaspec.fixtures.cli.FailingSubjectSpec#it_passes"));
        assertTrue(prettyResult.out.contains("Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
        assertEquals("", prettyResult.err);
    }

    @Test
    public void invalidFormatterExitsUsage() {
        CommandResult result = run("run", "--formatter", "dots");

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Invalid formatter: dots. Valid values: progress, pretty."));
        assertTrue(result.err.contains("Usage:"));
    }

    @Test
    public void profileOptionAcceptsAllSupportedProfiles() throws Exception {
        String[] profiles = new String[] {"java8", "java11", "java17", "java21", "java25"};
        for (int i = 0; i < profiles.length; i++) {
            File specRoot = temporaryFolder.newFolder("profile-" + profiles[i] + "-spec-root");
            File sourceRoot = temporaryFolder.newFolder("profile-" + profiles[i] + "-source-root");

            CommandResult result = run(
                    "run",
                    "--spec-dir", specRoot.getAbsolutePath(),
                    "--source-dir", sourceRoot.getAbsolutePath(),
                    "--profile", profiles[i]
            );

            assertEquals("Profile should be accepted: " + profiles[i], 0, result.exitCode);
            assertTrue(result.out.contains("No specifications found in " + specRoot.getAbsolutePath() + "."));
            assertEquals("", result.err);
        }
    }

    @Test
    public void invalidProfileExitsUsage() {
        CommandResult result = run("run", "--profile", "java26");

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Invalid profile: java26. Valid profiles: java8, java11, java17, java21, java25."));
        assertTrue(result.err.contains("Usage:"));
    }

    @Test
    public void verbosePrintsEffectiveRunConfigurationAndCliProfileOverride() throws Exception {
        File specRoot = temporaryFolder.newFolder("verbose-spec-root");
        File sourceRoot = temporaryFolder.newFolder("verbose-source-root");
        File configFile = temporaryFolder.newFile("verbose.conf");
        writeFile(configFile,
                "profile=java8\n" +
                "formatter=progress\n" +
                "constructorPolicy=delete\n" +
                "defaultSuite=phase9\n" +
                "suite.phase9.specDir=" + specRoot.getAbsolutePath() + "\n" +
                "suite.phase9.sourceDir=" + sourceRoot.getAbsolutePath() + "\n" +
                "suite.phase9.specPackagePrefix=behavior\n" +
                "suite.phase9.packagePrefix=com.acme\n");

        CommandResult result = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "phase9",
                "--constructor-policy", "preserve",
                "--profile", "java21",
                "--formatter", "pretty",
                "--dry-run",
                "--stop-on-failure",
                "--verbose"
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Run configuration:"));
        assertTrue(result.out.contains("  Selected suite: phase9"));
        assertTrue(result.out.contains("  Spec root: " + specRoot.getAbsolutePath()));
        assertTrue(result.out.contains("  Source root: " + sourceRoot.getAbsolutePath()));
        assertTrue(result.out.contains("  Spec package prefix: behavior"));
        assertTrue(result.out.contains("  Production package prefix: com.acme"));
        assertTrue(result.out.contains("  Constructor policy: preserve"));
        assertTrue(result.out.contains("  Profile: java21"));
        assertTrue(result.out.contains("  Formatter: pretty"));
        assertTrue(result.out.contains("  Dry-run: true"));
        assertTrue(result.out.contains("  Stop-on-failure: true"));
        assertTrue(result.out.contains("No specifications found in " + specRoot.getAbsolutePath() + "."));
        assertEquals("", result.err);
    }

    @Test
    public void describeRejectsRunOnlyPhase9FlagsWithUsageErrors() throws Exception {
        assertDescribeRunOnlyFlagRejected("The --dry-run option belongs to run", "--dry-run");
        assertDescribeRunOnlyFlagRejected("The --stop-on-failure option belongs to run", "--stop-on-failure");
        assertDescribeRunOnlyFlagRejected("The --formatter option belongs to run", "--formatter", "pretty");
        assertDescribeRunOnlyFlagRejected("The --profile option belongs to run", "--profile", "java17");
        assertDescribeRunOnlyFlagRejected("The --verbose option belongs to run", "--verbose");
    }

    private void assertDescribeRunOnlyFlagRejected(String expectedMessage, String... extraArgs) throws Exception {
        File specRoot = temporaryFolder.newFolder("describe-reject-" + sanitize(expectedMessage));
        List<String> args = new ArrayList<String>();
        args.add("describe");
        args.add("com.example.Book");
        args.add("--spec-dir");
        args.add(specRoot.getAbsolutePath());
        for (int i = 0; i < extraArgs.length; i++) {
            args.add(extraArgs[i]);
        }

        CommandResult result = run(args.toArray(new String[args.size()]));

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains(expectedMessage));
        assertTrue(result.err.contains("Usage:"));
        assertEquals(0, countFiles(specRoot));
    }

    private static String sanitize(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if ((character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9')) {
                builder.append(character);
            }
        }
        return builder.toString();
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

    private static CommandResult run(String... args) {
        return runWithInput("", args);
    }

    private static CommandResult runWithInput(String input, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = Main.run(
                args,
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
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
