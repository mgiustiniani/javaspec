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

import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class MainGenerationReportTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void dryRunWritesDeterministicPlannedReportWithoutWritingProduction() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("generation-report-dry-source");
        File specRoot = temporaryFolder.newFolder("generation-report-dry-spec");
        File report = new File(temporaryFolder.getRoot(), "generation-report-dry.json");
        writeSource(specRoot, "spec.com.example.ProfileSpec",
                "public class ProfileSpec extends ProfileSpecSupport {\n" +
                        "  public void it_is_a_record() {\n" +
                        "    shouldBeARecord();\n" +
                        "    beConstructedWith(false);\n" +
                        "    enabled().shouldReturn(false);\n" +
                        "  }\n" +
                        "}\n");

        CommandResult first = run("run", "--dry-run", "--profile", "java17",
                "--generation-report", report.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath());
        byte[] firstBytes = Files.readAllBytes(report.toPath());
        CommandResult second = run("run", "--dry-run", "--profile", "java17",
                "--generation-report", report.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(1, first.exitCode);
        assertEquals(1, second.exitCode);
        assertEquals(new String(firstBytes, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8));
        String json = new String(firstBytes, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"schemaVersion\": 1"));
        assertTrue(json.contains("\"outcome\": \"PLANNED\""));
        assertTrue(json.contains("\"exitCode\": 1"));
        assertTrue(json.contains("\"proceed\": false"));
        assertTrue(json.contains("\"actions\": []"));
        assertTrue(json.contains("\"pendingGenerationWork\": 1"));
        assertFalse(new File(sourceRoot, "com/example/Profile.java").exists());
    }

    @Test
    public void compileFailureStillWritesGenerationReport() throws Exception {
        assumeTrue(ToolProvider.getSystemJavaCompiler() != null);
        File sourceRoot = temporaryFolder.newFolder("generation-report-failure-source");
        File specRoot = temporaryFolder.newFolder("generation-report-failure-spec");
        File classes = temporaryFolder.newFolder("generation-report-failure-classes");
        File report = new File(temporaryFolder.getRoot(), "generation-report-failure.json");
        writeSource(sourceRoot, "com.example.Broken", "public class Broken { invalid }\n");
        writeSource(specRoot, "spec.com.example.BrokenSpec",
                "public class BrokenSpec { public void it_is_discovered() { } }\n");

        CommandResult result = run("run", "--compile",
                "--compile-output", classes.getAbsolutePath(),
                "--generation-report", report.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(1, result.exitCode);
        String json = new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"outcome\": \"FAILED\""));
        assertTrue(json.contains("\"exitCode\": 1"));
        assertTrue(json.contains("\"proceed\": true"));
    }

    @Test
    public void pendingStubsAreSortedAndUseSourceRelativePathsWithoutCompile() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("generation-report-stubs-source");
        File specRoot = temporaryFolder.newFolder("generation-report-stubs-spec");
        File report = new File(temporaryFolder.getRoot(), "generation-report-stubs.json");
        writeSource(sourceRoot, "com.example.Zed",
                "public class Zed {\n  public void pending() {\n    // javaspec:stub\n  }\n}\n");
        writeSource(sourceRoot, "com.example.Alpha",
                "public class Alpha {\n  public void pending() {\n    // javaspec:stub\n  }\n}\n");
        writeSource(specRoot, "spec.com.example.AlphaSpec",
                "public class AlphaSpec { public void it_is_discovered() { } }\n");

        CommandResult result = run("run", "--generation-report", report.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(1, result.exitCode);
        String json = new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8);
        int alpha = json.indexOf("com/example/Alpha.java");
        int zed = json.indexOf("com/example/Zed.java");
        assertTrue(json, alpha >= 0 && zed > alpha);
        assertFalse(json, json.contains(sourceRoot.getAbsolutePath()));
    }

    private static void writeSource(File root, String qualifiedName, String body) throws Exception {
        File file = new File(root, qualifiedName.replace('.', File.separatorChar) + ".java");
        assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
        int packageEnd = qualifiedName.lastIndexOf('.');
        Files.write(file.toPath(), ("package " + qualifiedName.substring(0, packageEnd) + ";\n" + body)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static CommandResult run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = Main.run(args, new ByteArrayInputStream(new byte[0]),
                new PrintStream(out), new PrintStream(err));
        return new CommandResult(exitCode,
                new String(out.toByteArray(), StandardCharsets.UTF_8),
                new String(err.toByteArray(), StandardCharsets.UTF_8));
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
