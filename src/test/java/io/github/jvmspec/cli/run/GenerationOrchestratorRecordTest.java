package io.github.jvmspec.cli.run;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class GenerationOrchestratorRecordTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void existingRecordSourceEvolvesHeaderWithoutOverwritingJavadocs() throws Exception {
        assumeTrue(supportsJavaSpecificationVersion(17));
        File sourceRoot = temporaryFolder.newFolder("source-root");
        File specRoot = temporaryFolder.newFolder("spec-root");
        File generatedSourcesRoot = temporaryFolder.newFolder("generated-sources-root");
        File sourceFile = writeSource(sourceRoot, "com/example/CertificateProfileId.java",
                "package com.example;\n\n" +
                "/** Existing type documentation. */\n" +
                "public record CertificateProfileId() {\n" +
                "    // Existing body comment must stay near the record body.\n" +
                "\n" +
                "    /** Existing method documentation. */\n" +
                "    public String algorithm() {\n" +
                "        return \"SHA-256\";\n" +
                "    }\n" +
                "}\n");
        File specFile = writeSource(specRoot, "spec/com/example/CertificateProfileIdSpec.java",
                "package spec.com.example;\n\n" +
                "public class CertificateProfileIdSpec extends CertificateProfileIdSpecSupport {\n" +
                "    public void it_uses_existing_no_arg_example_after_record_evolution() {\n" +
                "        subject().algorithm();\n" +
                "    }\n" +
                "}\n");
        DescribedType inferredClassDescription = DescribedType.of(
                "com.example.CertificateProfileId",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String"),
                        Arrays.asList("arg0"),
                        "")),
                Arrays.asList(MethodDescriptor.of("value", "String"))
        );

        GenerationOrchestratorResult result = GenerationOrchestrator.execute(
                Arrays.asList(DiscoveredSpec.of(
                        specFile,
                        "spec.com.example.CertificateProfileIdSpec",
                        inferredClassDescription)),
                specRoot,
                sourceRoot,
                new BufferedReader(new StringReader("")),
                new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"),
                new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"),
                true,
                false,
                SpecNamingConvention.defaults(),
                Thread.currentThread().getContextClassLoader(),
                ConstructorPolicy.COMMENT,
                generatedSourcesRoot
        );

        File supportFile = new File(generatedSourcesRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CertificateProfileIdSpecSupport.java");
        String updated = readFile(sourceFile);
        String support = readFile(supportFile);
        assertEquals(0, result.exitCode());
        assertTrue(result.shouldProceed());
        assertTrue(updated.contains("public record CertificateProfileId(String value)"));
        assertFalse(updated.contains("public record CertificateProfileId()"));
        assertFalse("record evolution must not insert a class-style constructor",
                updated.contains("public CertificateProfileId(String"));
        assertTrue("existing type Javadoc must be preserved exactly",
                updated.contains("/** Existing type documentation. */"));
        assertTrue("existing body comment must be preserved",
                updated.contains("    // Existing body comment must stay near the record body."));
        assertTrue("existing method Javadoc must be preserved exactly",
                updated.contains("    /** Existing method documentation. */"));
        assertTrue("existing method body must be preserved",
                updated.contains("public String algorithm() {\n" +
                        "        return \"SHA-256\";\n" +
                        "    }"));
        assertFalse("the planned value component must supply its implicit accessor",
                updated.contains("public String value()"));
        assertTrue("generated support must default record construction for already-described examples",
                support.contains("super(CertificateProfileId.class);\n" +
                        "        beConstructedWith((String) null);"));
        compileAndRunNoArgExample(sourceFile, supportFile, specFile,
                "it_uses_existing_no_arg_example_after_record_evolution");
    }

    @Test
    public void plannedRecordComponentSuppliesRequestedAccessorWithoutDuplicateStub() throws Exception {
        assumeTrue(supportsJavaSpecificationVersion(17));
        File sourceRoot = temporaryFolder.newFolder("planned-accessor-source-root");
        File specRoot = temporaryFolder.newFolder("planned-accessor-spec-root");
        File generatedSourcesRoot = temporaryFolder.newFolder("planned-accessor-generated-sources-root");
        File sourceFile = writeSource(sourceRoot, "com/example/CertificateProfileId.java",
                "package com.example;\n\n" +
                "public record CertificateProfileId(\n" +
                "        String eventId,\n" +
                "        String profileId,\n" +
                "        long sequence,\n" +
                "        String eventType,\n" +
                "        String actor) {\n" +
                "}\n");
        File specFile = writeSource(specRoot, "spec/com/example/CertificateProfileIdSpec.java",
                "package spec.com.example;\n\n" +
                "import java.time.Instant;\n\n" +
                "public class CertificateProfileIdSpec extends CertificateProfileIdSpecSupport {\n" +
                "    public void it_exposes_when_the_event_occurred() {\n" +
                "        Instant occurredAt = Instant.parse(\"2026-07-10T12:00:00Z\");\n" +
                "        beConstructedWith(\"event-id\", \"profile-id\", 7L, \"created\", \"actor\", occurredAt);\n" +
                "        occurredAt().shouldReturn(occurredAt);\n" +
                "    }\n" +
                "}\n");
        DescribedType inferredClassDescription = DescribedType.of(
                "com.example.CertificateProfileId",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String", "String", "long", "String", "String", "java.time.Instant"),
                        Arrays.asList("arg0", "arg1", "arg2", "arg3", "arg4", "occurredAt"),
                        "")),
                Arrays.asList(
                        MethodDescriptor.of("occurredAt", "Object"),
                        MethodDescriptor.of("summary", "String"))
        );

        GenerationOrchestratorResult result = GenerationOrchestrator.execute(
                Arrays.asList(DiscoveredSpec.of(
                        specFile,
                        "spec.com.example.CertificateProfileIdSpec",
                        inferredClassDescription)),
                specRoot,
                sourceRoot,
                new BufferedReader(new StringReader("")),
                new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"),
                new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"),
                true,
                false,
                SpecNamingConvention.defaults(),
                Thread.currentThread().getContextClassLoader(),
                ConstructorPolicy.COMMENT,
                generatedSourcesRoot
        );

        File supportFile = new File(generatedSourcesRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CertificateProfileIdSpecSupport.java");
        String updated = readFile(sourceFile);
        assertEquals(0, result.exitCode());
        assertTrue(result.shouldProceed());
        assertTrue(updated.contains("String actor, java.time.Instant occurredAt)"));
        assertFalse("a planned record component must supply its implicit accessor",
                updated.contains("public Object occurredAt()"));
        assertTrue("a genuine missing record method must remain an explicit javaspec stub",
                updated.contains("public String summary() {\n" +
                        "        // javaspec:stub\n" +
                        "        return null;\n" +
                        "    }"));
        compileAndRunNoArgExample(sourceFile, supportFile, specFile,
                "it_exposes_when_the_event_occurred");
    }

    @Test
    public void matcherOnlyEnumSpecGeneratesSupportFromCleanOutputDirectory() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("matcher-only-enum-source-root");
        File specRoot = temporaryFolder.newFolder("matcher-only-enum-spec-root");
        File generatedSourcesRoot = temporaryFolder.newFolder("matcher-only-enum-generated-root");
        File sourceFile = writeSource(sourceRoot, "com/example/SubjectPublicKeyProfile.java",
                "package com.example;\n\n" +
                "public enum SubjectPublicKeyProfile { EC_P256, RSA_3072 }\n");
        File specFile = writeSource(specRoot, "spec/com/example/SubjectPublicKeyProfileSpec.java",
                "package spec.com.example;\n\n" +
                "public class SubjectPublicKeyProfileSpec extends SubjectPublicKeyProfileSpecSupport {\n" +
                "    public void it_is_an_enum() {\n" +
                "        shouldBeAnEnum();\n" +
                "    }\n" +
                "}\n");
        DescribedType matcherOnlyEnum = DescribedType.of(
                "com.example.SubjectPublicKeyProfile",
                JavaTypeKind.ENUM,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<ConstructorDescriptor>emptyList(),
                Collections.<MethodDescriptor>emptyList()
        );

        GenerationOrchestratorResult result = GenerationOrchestrator.execute(
                Arrays.asList(DiscoveredSpec.of(
                        specFile,
                        "spec.com.example.SubjectPublicKeyProfileSpec",
                        matcherOnlyEnum)),
                specRoot,
                sourceRoot,
                new BufferedReader(new StringReader("")),
                new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"),
                new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"),
                true,
                false,
                SpecNamingConvention.defaults(),
                Thread.currentThread().getContextClassLoader(),
                ConstructorPolicy.COMMENT,
                generatedSourcesRoot
        );

        File supportFile = new File(generatedSourcesRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator
                        + "SubjectPublicKeyProfileSpecSupport.java");
        assertEquals(0, result.exitCode());
        assertTrue(result.shouldProceed());
        assertTrue("Matcher-only specs still require their generated superclass", supportFile.isFile());
        compileAndRunNoArgExample(sourceFile, supportFile, specFile, "it_is_an_enum");
    }

    @Test
    public void constructorOnlyRecordEvolutionStillUpdatesGeneratedSupport() throws Exception {
        assumeTrue(supportsJavaSpecificationVersion(17));
        File sourceRoot = temporaryFolder.newFolder("constructor-only-source-root");
        File specRoot = temporaryFolder.newFolder("constructor-only-spec-root");
        File generatedSourcesRoot = temporaryFolder.newFolder("constructor-only-generated-sources-root");
        File sourceFile = writeSource(sourceRoot, "com/example/CertificateProfileId.java",
                "package com.example;\n\n" +
                "public record CertificateProfileId() {\n" +
                "}\n");
        File specFile = writeSource(specRoot, "spec/com/example/CertificateProfileIdSpec.java",
                "package spec.com.example;\n\n" +
                "public class CertificateProfileIdSpec extends CertificateProfileIdSpecSupport {\n" +
                "    public void it_constructs_after_record_evolution() {\n" +
                "        subject();\n" +
                "    }\n" +
                "}\n");
        DescribedType inferredClassDescription = DescribedType.of(
                "com.example.CertificateProfileId",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String"),
                        Arrays.asList("arg0"),
                        "")),
                Collections.<MethodDescriptor>emptyList()
        );

        GenerationOrchestratorResult result = GenerationOrchestrator.execute(
                Arrays.asList(DiscoveredSpec.of(
                        specFile,
                        "spec.com.example.CertificateProfileIdSpec",
                        inferredClassDescription)),
                specRoot,
                sourceRoot,
                new BufferedReader(new StringReader("")),
                new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"),
                new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"),
                true,
                false,
                SpecNamingConvention.defaults(),
                Thread.currentThread().getContextClassLoader(),
                ConstructorPolicy.COMMENT,
                generatedSourcesRoot
        );

        File supportFile = new File(generatedSourcesRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CertificateProfileIdSpecSupport.java");
        String updated = readFile(sourceFile);
        String support = readFile(supportFile);
        assertEquals(0, result.exitCode());
        assertTrue(result.shouldProceed());
        assertTrue(updated.contains("public record CertificateProfileId(String arg0)"));
        assertTrue("generated support must be updated even when only constructor data is described",
                support.contains("super(CertificateProfileId.class);\n" +
                        "        beConstructedWith((String) null);"));
        compileAndRunNoArgExample(sourceFile, supportFile, specFile,
                "it_constructs_after_record_evolution");
    }

    private static boolean supportsJavaSpecificationVersion(int minimumVersion) {
        String version = System.getProperty("java.specification.version");
        if (version == null) {
            return false;
        }
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dot = version.indexOf('.');
        if (dot >= 0) {
            version = version.substring(0, dot);
        }
        try {
            return Integer.parseInt(version) >= minimumVersion;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void compileAndRunNoArgExample(
            File sourceFile,
            File supportFile,
            File specFile,
            String exampleMethodName
    ) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue("A JDK compiler is required to verify generated record support", compiler != null);
        File classesDirectory = temporaryFolder.newFolder("compiled-record-support");
        ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
        int exitCode = compiler.run(null, compilerOutput, compilerOutput,
                "--release", "17",
                "-classpath", System.getProperty("java.class.path"),
                "-d", classesDirectory.getAbsolutePath(),
                sourceFile.getAbsolutePath(),
                supportFile.getAbsolutePath(),
                specFile.getAbsolutePath());
        assertEquals(new String(compilerOutput.toByteArray(), StandardCharsets.UTF_8), 0, exitCode);

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{classesDirectory.toURI().toURL()},
                Thread.currentThread().getContextClassLoader());
        try {
            String specSimpleName = specFile.getName().substring(0, specFile.getName().length() - ".java".length());
            Class<?> specClass = Class.forName("spec.com.example." + specSimpleName, true, classLoader);
            Object spec = specClass.newInstance();
            Method example = specClass.getMethod(exampleMethodName);
            example.invoke(spec);
        } finally {
            classLoader.close();
        }
    }

    private static File writeSource(File root, String relativePath, String source) throws Exception {
        File file = new File(root, relativePath);
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("cannot create " + parent);
        }
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
