package io.github.jvmspec.cli;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class MainPhase47ExampleDataCliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runCompileExecutesPhpspecStyleExampleDataRows() throws Exception {
        requireJdkCompiler();
        File sourceRoot = temporaryFolder.newFolder("phase47-source-root");
        File specRoot = temporaryFolder.newFolder("phase47-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "phase47-classes");
        writeSource(sourceRoot, "com.phase47.NameNormalizer",
                "public class NameNormalizer {\n" +
                        "    public String normalize(String value) {\n" +
                        "        return value.trim();\n" +
                        "    }\n" +
                        "}\n");
        writeSource(specRoot, "spec.com.phase47.NameNormalizerSpec",
                "import com.phase47.NameNormalizer;\n" +
                        "import io.github.jvmspec.api.Example2;\n" +
                        "import io.github.jvmspec.api.ObjectBehavior;\n" +
                        "\n" +
                        "public class NameNormalizerSpec extends ObjectBehavior<NameNormalizer> {\n" +
                        "    public NameNormalizerSpec() {\n" +
                        "        super(NameNormalizer.class);\n" +
                        "    }\n" +
                        "\n" +
                        "    public void it_normalizes_known_inputs() {\n" +
                        "        examples(row(\"  Alice  \", \"Alice\"), row(\"Bob\", \"Bob\"))\n" +
                        "            .verify(new Example2<String, String>() {\n" +
                        "                @Override\n" +
                        "                public void run(String input, String expected) {\n" +
                        "                    match(subject().normalize(input)).shouldReturn(expected);\n" +
                        "                }\n" +
                        "            });\n" +
                        "    }\n" +
                        "}\n");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--compile",
                "--compile-output", compileOutput.getAbsolutePath(),
                "--formatter", "pretty"
        );

        assertEquals(0, result.exitCode);
        assertContains(result.out, "PASSED spec.com.phase47.NameNormalizerSpec#it_normalizes_known_inputs");
        assertContains(result.out, "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertEquals("", result.err);
    }

    @Test
    public void runCompileReportsFailingExampleDataRowContext() throws Exception {
        requireJdkCompiler();
        File sourceRoot = temporaryFolder.newFolder("phase47-failing-source-root");
        File specRoot = temporaryFolder.newFolder("phase47-failing-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "phase47-failing-classes");
        File jsonReport = new File(temporaryFolder.getRoot(), "phase47-failing-report.json");
        File junitReport = new File(temporaryFolder.getRoot(), "phase47-failing-report.xml");
        writeSource(sourceRoot, "com.phase47.FailingNameNormalizer",
                "public class FailingNameNormalizer {\n" +
                        "    public String normalize(String value) {\n" +
                        "        return value.trim();\n" +
                        "    }\n" +
                        "}\n");
        writeSource(specRoot, "spec.com.phase47.FailingNameNormalizerSpec",
                "import com.phase47.FailingNameNormalizer;\n" +
                        "import io.github.jvmspec.api.Example2;\n" +
                        "import io.github.jvmspec.api.ObjectBehavior;\n" +
                        "\n" +
                        "public class FailingNameNormalizerSpec extends ObjectBehavior<FailingNameNormalizer> {\n" +
                        "    public FailingNameNormalizerSpec() { super(FailingNameNormalizer.class); }\n" +
                        "\n" +
                        "    public void it_reports_the_failing_row() {\n" +
                        "        examples(row(\"Alice\", \"Alice\"), row(\" Bob \", \"Robert\"))\n" +
                        "            .verify(new Example2<String, String>() {\n" +
                        "                @Override\n" +
                        "                public void run(String input, String expected) {\n" +
                        "                    match(subject().normalize(input)).shouldReturn(expected);\n" +
                        "                }\n" +
                        "            });\n" +
                        "    }\n" +
                        "}\n");

        CommandResult result = run(
                "run",
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--compile",
                "--compile-output", compileOutput.getAbsolutePath(),
                "--formatter", "pretty",
                "--report", jsonReport.getAbsolutePath(),
                "--junit-xml", junitReport.getAbsolutePath()
        );

        assertEquals(1, result.exitCode);
        assertContains(result.out, "FAILED spec.com.phase47.FailingNameNormalizerSpec#it_reports_the_failing_row");
        assertContains(result.out, "Example data row 2 [ Bob , Robert] failed");
        assertContains(result.out, "Expected equality(Robert) but got Bob");
        assertEquals("", result.err);
        assertTrue(jsonReport.isFile());
        assertTrue(junitReport.isFile());
        String json = readFile(jsonReport);
        assertContains(json, "\"exampleDataRows\": [");
        assertContains(json, "\"index\": 1");
        assertContains(json, "\"description\": \"[Alice, Alice]\"");
        assertContains(json, "\"status\": \"PASSED\"");
        assertContains(json, "\"index\": 2");
        assertContains(json, "\"description\": \"[ Bob , Robert]\"");
        assertContains(json, "\"status\": \"FAILED\"");
        assertContains(json, "Example data row 2 [ Bob , Robert] failed");
        assertContains(json, "Expected equality(Robert) but got Bob");
        assertContains(readFile(junitReport), "Example data row 2 [ Bob , Robert] failed");
        assertContains(readFile(junitReport), "Expected equality(Robert) but got Bob");
    }

    private static void requireJdkCompiler() {
        assumeTrue("JDK compiler is required", ToolProvider.getSystemJavaCompiler() != null);
    }

    private static void writeSource(File root, String qualifiedName, String body) throws Exception {
        String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
        String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        File sourceFile = sourceFileFor(root, qualifiedName);
        writeFile(sourceFile, "package " + packageName + ";\n\n" + body);
        assertTrue(simpleName + " source file should exist", sourceFile.isFile());
    }

    private static File sourceFileFor(File root, String qualifiedName) {
        return new File(root, qualifiedName.replace('.', File.separatorChar) + ".java");
    }

    private static void writeFile(File file, String content) throws Exception {
        File parent = file.getParentFile();
        if (parent != null) {
            assertTrue(parent.isDirectory() || parent.mkdirs());
        }
        FileOutputStream output = new FileOutputStream(file);
        try {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        } finally {
            output.close();
        }
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void assertContains(String value, String expected) {
        assertTrue("Expected to find <" + expected + "> in:\n" + value, value.contains(expected));
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
