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

public class MainExecutionDiagnosticsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runPrintsExecutionDiagnosticsAndCurrentClassloaderHintOnlyWhenNoExplicitClasspathIsProvided() throws Exception {
        File specRoot = temporaryFolder.newFolder("implicit-spec-root");
        File sourceRoot = temporaryFolder.newFolder("implicit-source-root");
        writeSourceClass(sourceRoot, "com.example.ImplicitMissing");
        writeSpec(specRoot, "spec.com.example.ImplicitMissingSpec", "it_is_discovered_but_not_compiled");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Execution diagnostics:"));
        assertTrue(result.out.contains("Specification spec.com.example.ImplicitMissingSpec is not executable"));
        assertTrue(result.out.contains("Specification class not found: spec.com.example.ImplicitMissingSpec"));
        assertTrue(result.out.contains("Compile the spec/test sources"));
        assertTrue(result.out.contains("No explicit classpath entries were provided; javaspec used the current process classloader"));
        assertTrue(result.out.contains("Use --classpath or --classpath-file with compiled test/spec output and dependencies"));
        assertEquals("", result.err);
    }

    @Test
    public void runPrintsExplicitClasspathDiagnosticHintWithEntryCount() throws Exception {
        File specRoot = temporaryFolder.newFolder("explicit-spec-root");
        File sourceRoot = temporaryFolder.newFolder("explicit-source-root");
        File firstClasspathEntry = temporaryFolder.newFolder("first-classpath-entry");
        File secondClasspathEntry = temporaryFolder.newFolder("second-classpath-entry");
        writeSourceClass(sourceRoot, "com.example.ExplicitMissing");
        writeSpec(specRoot, "spec.com.example.ExplicitMissingSpec", "it_is_discovered_but_not_compiled");
        String explicitClasspath = firstClasspathEntry.getAbsolutePath()
                + File.pathSeparator
                + secondClasspathEntry.getAbsolutePath();

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--classpath", explicitClasspath
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Execution diagnostics:"));
        assertTrue(result.out.contains("Explicit classpath entries provided: 2"));
        assertTrue(result.out.contains("Verify these entries contain compiled spec classes and required dependencies"));
        assertFalse(result.out.contains("javaspec used the current process classloader"));
        assertEquals("", result.err);
    }

    @Test
    public void runOmitsExecutionDiagnosticBlockWhenDiagnosticsAreEmpty() throws Exception {
        File specRoot = temporaryFolder.newFolder("empty-spec-root");
        File sourceRoot = temporaryFolder.newFolder("empty-source-root");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath()
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("No specifications found"));
        assertFalse(result.out.contains("Execution diagnostics:"));
        assertFalse(result.out.contains("current process classloader"));
        assertFalse(result.out.contains("Explicit classpath entries provided"));
        assertEquals("", result.err);
    }

    private static void writeSourceClass(File sourceRoot, String qualifiedName) throws Exception {
        int lastDot = qualifiedName.lastIndexOf('.');
        String packageName = qualifiedName.substring(0, lastDot);
        String simpleName = qualifiedName.substring(lastDot + 1);
        File sourceFile = new File(sourceRoot, qualifiedName.replace('.', File.separatorChar) + ".java");
        writeFile(sourceFile,
                "package " + packageName + ";\n\n" +
                        "public class " + simpleName + " { }\n");
    }

    private static void writeSpec(File specRoot, String specQualifiedName, String exampleMethodName) throws Exception {
        int lastDot = specQualifiedName.lastIndexOf('.');
        String packageName = specQualifiedName.substring(0, lastDot);
        String simpleName = specQualifiedName.substring(lastDot + 1);
        File specFile = new File(specRoot, specQualifiedName.replace('.', File.separatorChar) + ".java");
        writeFile(specFile,
                "package " + packageName + ";\n\n" +
                        "public class " + simpleName + " {\n" +
                        "    public void " + exampleMethodName + "() {\n" +
                        "    }\n" +
                        "}\n");
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
