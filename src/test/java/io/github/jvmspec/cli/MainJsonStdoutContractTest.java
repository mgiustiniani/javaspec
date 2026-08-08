package io.github.jvmspec.cli;

import io.github.jvmspec.testing.CliProjectFixture;
import io.github.jvmspec.testing.CliProjectFixture.RunResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
        CliProjectFixture project = CliProjectFixture.create(temporaryFolder.newFolder("json-success"));
        project.source("com.example.Subject", "public class Subject { }\n");
        project.spec("spec.com.example.SubjectSpec",
                "public class SubjectSpec { public void it_passes() { } }\n");

        RunResult result = project.run("run", "--formatter", "json", "--compile",
                "--compile-output", project.classesRoot().getAbsolutePath());

        assertEquals(0, result.exitCode());
        parseWholeJsonObject(result.stdout());
        assertTrue(result.stdout().contains("\"success\": true"));
        assertTrue(result.stderr().contains("Found 1 specification(s)"));
    }

    @Test
    public void generationStopStillWritesExactlyOneJsonDocumentToStdout() throws Exception {
        CliProjectFixture project = CliProjectFixture.create(temporaryFolder.newFolder("json-generation-stop"));
        project.spec("spec.com.example.SubjectSpec",
                "public class SubjectSpec extends SubjectSpecSupport {\n" +
                        "  public void it_describes_an_ambiguous_record() {\n" +
                        "    shouldBeARecord();\n" +
                        "    beConstructedWith(false);\n" +
                        "  }\n" +
                        "}\n");

        RunResult result = project.run("run", "--formatter", "json", "--generate",
                "--profile", "java17");

        assertTrue("stdout:\n" + result.stdout() + "\nstderr:\n" + result.stderr(),
                result.exitCode() != 0);
        parseWholeJsonObject(result.stdout());
        assertTrue(result.stdout().contains("\"success\": false"));
        assertTrue("stderr:\n" + result.stderr(),
                result.stderr().contains("AMBIGUOUS_RECORD_COMPONENT_NAME"));
    }

    @Test
    public void compileFailureStillWritesExactlyOneJsonDocumentToStdout() throws Exception {
        assumeTrue(ToolProvider.getSystemJavaCompiler() != null);
        CliProjectFixture project = CliProjectFixture.create(temporaryFolder.newFolder("json-compile-failure"));
        project.source("com.example.Broken", "public class Broken { invalid }\n");
        project.spec("spec.com.example.BrokenSpec",
                "public class BrokenSpec { public void it_is_discovered() { } }\n");

        RunResult result = project.run("run", "--formatter", "json", "--compile",
                "--compile-output", project.classesRoot().getAbsolutePath());

        assertEquals(1, result.exitCode());
        parseWholeJsonObject(result.stdout());
        assertTrue(result.stdout().contains("\"success\": false"));
        assertTrue(result.stderr().contains("Compilation failed"));
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

}
