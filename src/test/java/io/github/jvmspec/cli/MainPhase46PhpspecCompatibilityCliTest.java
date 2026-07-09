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

import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class MainPhase46PhpspecCompatibilityCliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runCompileExecutesCanonicalPhpspecStyleSubjectBehavior() throws Exception {
        requireJdkCompiler();
        File sourceRoot = temporaryFolder.newFolder("phase46-source-root");
        File specRoot = temporaryFolder.newFolder("phase46-spec-root");
        File compileOutput = new File(temporaryFolder.getRoot(), "phase46-classes");
        writeSource(sourceRoot, "com.phase46.Greeting",
                "public class Greeting {\n" +
                        "    private final String name;\n" +
                        "\n" +
                        "    public Greeting(String name) {\n" +
                        "        this.name = name;\n" +
                        "    }\n" +
                        "\n" +
                        "    public String message() {\n" +
                        "        return \"Hello \" + name;\n" +
                        "    }\n" +
                        "}\n");
        writeSource(specRoot, "spec.com.phase46.GreetingSpec",
                "import com.phase46.Greeting;\n" +
                        "import io.github.jvmspec.api.ObjectBehavior;\n" +
                        "\n" +
                        "public class GreetingSpec extends ObjectBehavior<Greeting> {\n" +
                        "    public GreetingSpec() {\n" +
                        "        super(Greeting.class);\n" +
                        "    }\n" +
                        "\n" +
                        "    public void let() {\n" +
                        "        beConstructedWith(\"Ada\");\n" +
                        "    }\n" +
                        "\n" +
                        "    public void it_greets_the_configured_subject() {\n" +
                        "        match(subject().message()).shouldReturn(\"Hello Ada\");\n" +
                        "    }\n" +
                        "\n" +
                        "    public void letGo() {\n" +
                        "        match(\"cleanup\").shouldReturn(\"cleanup\");\n" +
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
        assertContains(result.out, "Found 1 specification(s) in " + specRoot.getAbsolutePath() + ".");
        assertContains(result.out, "Compiled 2 source file(s) to " + compileOutput.getAbsolutePath() + ".");
        assertContains(result.out, "PASSED spec.com.phase46.GreetingSpec#it_greets_the_configured_subject");
        assertContains(result.out, "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending.");
        assertEquals("", result.err);
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
