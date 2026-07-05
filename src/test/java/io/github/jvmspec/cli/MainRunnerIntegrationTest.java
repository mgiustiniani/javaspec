package io.github.jvmspec.cli;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MainRunnerIntegrationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runPrintsRunnerSummaryAndReturnsFailureExitCodeForFilteredFailingExample() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("source-root");
        File specRoot = new File(System.getProperty("user.dir"), "src/test/java");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "io.github.jvmspec.fixtures.cli.FailingSubject",
                "--example", "it_fails"
        );

        assertEquals(1, result.exitCode);
        assertTrue(result.out.contains("Found 1 specification(s) in " + specRoot.getAbsolutePath() + "."));
        assertTrue(result.out.contains("spec.io.github.jvmspec.fixtures.cli.FailingSubjectSpec describes io.github.jvmspec.fixtures.cli.FailingSubject; class exists."));
        assertTrue(result.out.contains("Examples: 1 total, 0 passed, 1 failed, 0 broken, 0 skipped, 0 pending."));
        assertTrue(result.out.contains("Failed examples:"));
        assertTrue(result.out.contains("FAILED spec.io.github.jvmspec.fixtures.cli.FailingSubjectSpec#it_fails"));
        assertTrue(result.out.contains("Assertion failed"));
        assertTrue(result.out.contains("java.lang.AssertionError: cli failure"));
        assertEquals("", result.err);
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
