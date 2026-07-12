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
        assertTrue(json.contains("\"status\": \"PROPOSED\""));
        assertTrue(json.contains("\"appliedWrites\": 0"));
        assertTrue(json.contains("\"pendingGenerationWork\": 2"));
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

    @Test
    public void repeatedSameSignatureConstructionGeneratesOneCompilableConstructorAndReachesRed() throws Exception {
        assumeTrue(ToolProvider.getSystemJavaCompiler() != null);
        File sourceRoot = temporaryFolder.newFolder("duplicate-constructor-source");
        File specRoot = temporaryFolder.newFolder("duplicate-constructor-spec");
        File classes = temporaryFolder.newFolder("duplicate-constructor-classes");
        File report = new File(temporaryFolder.getRoot(), "duplicate-constructor-report.json");
        writeSource(specRoot, "spec.com.example.SemanticPolicyHashSpec",
                "import io.github.jvmspec.api.ObjectBehavior;\n" +
                        "import java.util.LinkedHashMap;\n" +
                        "import java.util.Map;\n" +
                        "public class SemanticPolicyHashSpec extends ObjectBehavior<com.example.SemanticPolicyHash> {\n" +
                        "  public SemanticPolicyHashSpec() { super(com.example.SemanticPolicyHash.class); }\n" +
                        "  public void it_hashes_normalized_fields() {\n" +
                        "    Map<String, String> firstMap = new LinkedHashMap<String, String>();\n" +
                        "    Map<String, String> secondMap = new LinkedHashMap<String, String>();\n" +
                        "    beConstructedWith(firstMap);\n" +
                        "    beConstructedWith(secondMap);\n" +
                        "    match(subject().toString()).shouldReturn(\"expected semantic hash\");\n" +
                        "  }\n" +
                        "}\n");

        CommandResult result = run("run", "--generate", "--compile",
                "--compile-output", classes.getAbsolutePath(),
                "--generation-report", report.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals("stdout:\n" + result.out + "\nstderr:\n" + result.err, 1, result.exitCode);
        File source = new File(sourceRoot, "com/example/SemanticPolicyHash.java");
        String generated = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertEquals(1, countOccurrences(generated, "public SemanticPolicyHash("));
        assertTrue(generated, generated.contains("Map<String, String> firstMap"));
        assertTrue(result.out, result.out.contains("FAILED spec.com.example.SemanticPolicyHashSpec"));
        assertTrue(result.out, result.out.contains("Compiled 2 source file(s)"));
    }

    @Test
    public void nonGenerativeCompileLeavesRequiredClassConstructorSynchronizationByteIdentical() throws Exception {
        assumeTrue(ToolProvider.getSystemJavaCompiler() != null);
        File sourceRoot = temporaryFolder.newFolder("readonly-class-source");
        File specRoot = temporaryFolder.newFolder("readonly-class-spec");
        File classes = temporaryFolder.newFolder("readonly-class-classes");
        File report = new File(temporaryFolder.getRoot(), "readonly-class-report.json");
        File source = writeSource(sourceRoot, "com.example.Service",
                "public class Service {\n" +
                        "  public Service(String normalizedFields) {\n" +
                        "    this.value = normalizedFields;\n" +
                        "  }\n" +
                        "  private String value;\n" +
                        "}\n");
        writeSource(specRoot, "spec.com.example.ServiceSpec",
                "import io.github.jvmspec.api.ObjectBehavior;\n" +
                        "public class ServiceSpec extends ObjectBehavior<com.example.Service> {\n" +
                        "  public ServiceSpec() { super(com.example.Service.class); }\n" +
                        "  public void it_requires_an_extended_constructor() {\n" +
                        "    beConstructedWith(\"value\", 7);\n" +
                        "  }\n" +
                        "}\n");
        byte[] before = Files.readAllBytes(source.toPath());
        long modifiedBefore = source.lastModified();

        CommandResult result = run("run", "--compile", "--constructor-policy", "preserve",
                "--compile-output", classes.getAbsolutePath(),
                "--generation-report", report.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(1, result.exitCode);
        assertEquals(new String(before, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8));
        assertEquals(modifiedBefore, source.lastModified());
        String json = new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8);
        assertTrue(json, json.contains("\"status\": \"PROPOSED\""));
        assertTrue(json, json.contains("\"appliedWrites\": 0"));
        assertTrue(result.out, result.out.contains("No production files were written."));
    }

    @Test
    public void explicitGenerationAppliesConstructorSynchronizationOnceAndThenIsIdempotent() throws Exception {
        assumeTrue(ToolProvider.getSystemJavaCompiler() != null);
        File sourceRoot = temporaryFolder.newFolder("generate-class-source");
        File specRoot = temporaryFolder.newFolder("generate-class-spec");
        File classes = temporaryFolder.newFolder("generate-class-classes");
        File dryReport = new File(temporaryFolder.getRoot(), "generate-class-dry.json");
        File firstReport = new File(temporaryFolder.getRoot(), "generate-class-first.json");
        File secondReport = new File(temporaryFolder.getRoot(), "generate-class-second.json");
        File source = writeSource(sourceRoot, "com.example.Service",
                "public class Service {\n" +
                        "  public Service(String normalizedFields) { this.value = normalizedFields; }\n" +
                        "  private String value;\n" +
                        "}\n");
        writeSource(specRoot, "spec.com.example.ServiceSpec",
                "import io.github.jvmspec.api.ObjectBehavior;\n" +
                        "public class ServiceSpec extends ObjectBehavior<com.example.Service> {\n" +
                        "  public ServiceSpec() { super(com.example.Service.class); }\n" +
                        "  public void it_requires_an_extended_constructor() {\n" +
                        "    beConstructedWith(\"value\", 7);\n" +
                        "  }\n" +
                        "}\n");

        byte[] original = Files.readAllBytes(source.toPath());
        CommandResult dryRun = run("run", "--dry-run", "--constructor-policy", "preserve",
                "--generation-report", dryReport.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());
        assertEquals(1, dryRun.exitCode);
        assertEquals(new String(original, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8));

        CommandResult first = run("run", "--generate", "--compile",
                "--constructor-policy", "preserve", "--compile-output", classes.getAbsolutePath(),
                "--generation-report", firstReport.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());
        byte[] generated = Files.readAllBytes(source.toPath());
        long generatedModified = source.lastModified();
        CommandResult second = run("run", "--generate", "--compile",
                "--constructor-policy", "preserve", "--compile-output", classes.getAbsolutePath(),
                "--generation-report", secondReport.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(0, first.exitCode);
        assertEquals(0, second.exitCode);
        String generatedSource = new String(generated, StandardCharsets.UTF_8);
        assertEquals(1, countOccurrences(generatedSource, "Service(String arg0, int arg1)"));
        assertEquals(generatedSource,
                new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8));
        assertEquals(generatedModified, source.lastModified());
        String dryJson = new String(Files.readAllBytes(dryReport.toPath()), StandardCharsets.UTF_8);
        String firstJson = new String(Files.readAllBytes(firstReport.toPath()), StandardCharsets.UTF_8);
        String secondJson = new String(Files.readAllBytes(secondReport.toPath()), StandardCharsets.UTF_8);
        assertTrue(dryJson, dryJson.contains("CONSTRUCTOR_SYNCHRONIZATION"));
        assertTrue(dryJson, dryJson.contains("\"status\": \"PROPOSED\""));
        assertTrue(dryJson, dryJson.contains("\"appliedWrites\": 0"));
        assertEquals(1, countOccurrences(firstJson, "SOURCE_SYNCHRONIZATION"));
        assertTrue(firstJson, firstJson.contains("\"status\": \"APPLIED\""));
        assertTrue(secondJson, secondJson.contains("\"appliedWrites\": 0"));
        assertTrue(secondJson, secondJson.contains("\"outcome\": \"NO_CHANGES\""));
    }

    @Test
    public void nonGenerativeRecordSynchronizationIsReadOnly() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("readonly-record-source");
        File specRoot = temporaryFolder.newFolder("readonly-record-spec");
        File report = new File(temporaryFolder.getRoot(), "readonly-record-report.json");
        File source = writeSource(sourceRoot, "com.example.Profile",
                "public record Profile(String value) { }\n");
        writeSource(specRoot, "spec.com.example.ProfileSpec",
                "import io.github.jvmspec.api.ObjectBehavior;\n" +
                        "public class ProfileSpec extends ObjectBehavior<com.example.Profile> {\n" +
                        "  public ProfileSpec() { super(com.example.Profile.class); }\n" +
                        "  public void it_requires_two_components() {\n" +
                        "    String value = \"value\"; int count = 7;\n" +
                        "    beConstructedWith(value, count);\n" +
                        "  }\n" +
                        "}\n");
        byte[] before = Files.readAllBytes(source.toPath());
        long modifiedBefore = source.lastModified();

        CommandResult result = run("run", "--profile", "java17",
                "--generation-report", report.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(1, result.exitCode);
        assertEquals(new String(before, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8));
        assertEquals(modifiedBefore, source.lastModified());
        String json = new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8);
        assertTrue(json, json.contains("\"appliedWrites\": 0"));
        assertTrue(json, json.contains("CONSTRUCTOR_SYNCHRONIZATION"));
    }

    @Test
    public void explicitRecordSynchronizationAppliesOnceAndThenIsIdempotent() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("generate-record-source");
        File specRoot = temporaryFolder.newFolder("generate-record-spec");
        File firstReport = new File(temporaryFolder.getRoot(), "generate-record-first.json");
        File secondReport = new File(temporaryFolder.getRoot(), "generate-record-second.json");
        File source = writeSource(sourceRoot, "com.example.Profile",
                "public record Profile(String value) { }\n");
        writeSource(specRoot, "spec.com.example.ProfileSpec",
                "import io.github.jvmspec.api.ObjectBehavior;\n" +
                        "public class ProfileSpec extends ObjectBehavior<com.example.Profile> {\n" +
                        "  public ProfileSpec() { super(com.example.Profile.class); }\n" +
                        "  public void it_requires_two_components() {\n" +
                        "    String value = \"value\"; int count = 7;\n" +
                        "    beConstructedWith(value, count);\n" +
                        "  }\n" +
                        "}\n");

        CommandResult first = run("run", "--generate", "--profile", "java17",
                "--generation-report", firstReport.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());
        byte[] generated = Files.readAllBytes(source.toPath());
        long modified = source.lastModified();
        CommandResult second = run("run", "--generate", "--profile", "java17",
                "--generation-report", secondReport.getAbsolutePath(),
                "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(0, first.exitCode);
        assertEquals(0, second.exitCode);
        String generatedSource = new String(generated, StandardCharsets.UTF_8);
        assertTrue(generatedSource, generatedSource.contains("record Profile(String value, int count)"));
        assertEquals(generatedSource,
                new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8));
        assertEquals(modified, source.lastModified());
        String firstJson = new String(Files.readAllBytes(firstReport.toPath()), StandardCharsets.UTF_8);
        String secondJson = new String(Files.readAllBytes(secondReport.toPath()), StandardCharsets.UTF_8);
        assertEquals(1, countOccurrences(firstJson, "SOURCE_SYNCHRONIZATION"));
        assertTrue(firstJson, firstJson.contains("\"status\": \"APPLIED\""));
        assertTrue(secondJson, secondJson.contains("\"appliedWrites\": 0"));
    }

    private static int countOccurrences(String value, String token) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static File writeSource(File root, String qualifiedName, String body) throws Exception {
        File file = new File(root, qualifiedName.replace('.', File.separatorChar) + ".java");
        assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
        int packageEnd = qualifiedName.lastIndexOf('.');
        Files.write(file.toPath(), ("package " + qualifiedName.substring(0, packageEnd) + ";\n" + body)
                .getBytes(StandardCharsets.UTF_8));
        return file;
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
