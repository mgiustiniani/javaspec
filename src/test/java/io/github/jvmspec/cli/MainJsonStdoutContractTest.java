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
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class MainJsonStdoutContractTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void successfulRunWritesExactlyOneJsonDocumentToStdout() throws Exception {
        assumeTrue(ToolProvider.getSystemJavaCompiler() != null);
        File sourceRoot = temporaryFolder.newFolder("json-success-source");
        File specRoot = temporaryFolder.newFolder("json-success-spec");
        File classes = temporaryFolder.newFolder("json-success-classes");
        writeSource(sourceRoot, "com.example.Subject", "public class Subject { }\n");
        writeSource(specRoot, "spec.com.example.SubjectSpec",
                "public class SubjectSpec { public void it_passes() { } }\n");

        CommandResult result = run("run", "--formatter", "json", "--compile",
                "--compile-output", classes.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(0, result.exitCode);
        parseWholeJsonObject(result.out);
        assertTrue(result.out.contains("\"success\": true"));
        assertTrue(result.err.contains("Found 1 specification(s)"));
    }

    @Test
    public void generationStopStillWritesExactlyOneJsonDocumentToStdout() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("json-generation-stop-source");
        File specRoot = temporaryFolder.newFolder("json-generation-stop-spec");
        writeSource(specRoot, "spec.com.example.SubjectSpec",
                "public class SubjectSpec extends SubjectSpecSupport {\n" +
                        "  public void it_describes_an_ambiguous_record() {\n" +
                        "    shouldBeARecord();\n" +
                        "    beConstructedWith(false);\n" +
                        "  }\n" +
                        "}\n");

        CommandResult result = run("run", "--formatter", "json", "--generate",
                "--profile", "java17",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath());

        assertTrue("stdout:\n" + result.out + "\nstderr:\n" + result.err,
                result.exitCode != 0);
        parseWholeJsonObject(result.out);
        assertTrue(result.out.contains("\"success\": false"));
        assertTrue("stderr:\n" + result.err,
                result.err.contains("AMBIGUOUS_RECORD_COMPONENT_NAME"));
    }

    @Test
    public void compileFailureStillWritesExactlyOneJsonDocumentToStdout() throws Exception {
        assumeTrue(ToolProvider.getSystemJavaCompiler() != null);
        File sourceRoot = temporaryFolder.newFolder("json-compile-failure-source");
        File specRoot = temporaryFolder.newFolder("json-compile-failure-spec");
        File classes = temporaryFolder.newFolder("json-compile-failure-classes");
        writeSource(sourceRoot, "com.example.Broken", "public class Broken { invalid }\n");
        writeSource(specRoot, "spec.com.example.BrokenSpec",
                "public class BrokenSpec { public void it_is_discovered() { } }\n");

        CommandResult result = run("run", "--formatter", "json", "--compile",
                "--compile-output", classes.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(1, result.exitCode);
        parseWholeJsonObject(result.out);
        assertTrue(result.out.contains("\"success\": false"));
        assertTrue(result.err.contains("Compilation failed"));
    }

    private static void parseWholeJsonObject(String value) {
        String json = value.trim();
        assertTrue("Expected JSON object, got:\n" + value,
                json.length() >= 2 && json.charAt(0) == '{' && json.charAt(json.length() - 1) == '}');
        int objectDepth = 0;
        int arrayDepth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{') objectDepth++;
            else if (c == '}') objectDepth--;
            else if (c == '[') arrayDepth++;
            else if (c == ']') arrayDepth--;
            assertTrue("Invalid JSON nesting in:\n" + value, objectDepth >= 0 && arrayDepth >= 0);
            if (objectDepth == 0 && i < json.length() - 1) {
                throw new AssertionError("Trailing content after JSON document:\n" + value);
            }
        }
        assertEquals("Unterminated JSON string", false, inString);
        assertEquals("Unbalanced JSON object", 0, objectDepth);
        assertEquals("Unbalanced JSON array", 0, arrayDepth);
    }

    private static void writeSource(File root, String qualifiedName, String body) throws Exception {
        File file = new File(root, qualifiedName.replace('.', File.separatorChar) + ".java");
        assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
        int packageEnd = qualifiedName.lastIndexOf('.');
        String packageName = packageEnd < 0 ? "" : qualifiedName.substring(0, packageEnd);
        Files.write(file.toPath(), ("package " + packageName + ";\n" + body)
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
