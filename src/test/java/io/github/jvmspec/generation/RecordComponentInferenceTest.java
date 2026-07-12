package io.github.jvmspec.generation;

import io.github.jvmspec.cli.run.GenerationOrchestrator;
import io.github.jvmspec.cli.run.GenerationOrchestratorResult;
import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.discovery.SpecDiscovery;
import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RecordComponentInferenceTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void falseLiteralUsesMatchingBooleanAccessorAsComponentName() throws Exception {
        DescribedType type = discoverRecord(
                "beConstructedWith(false);\n" +
                "        ca().shouldReturn(false);");

        assertEquals(Arrays.asList("boolean"), constructor(type).parameterTypes());
        assertEquals(Arrays.asList("ca"), constructor(type).parameterNames());
        assertTrue(skeleton(type).contains("record Example(boolean ca)"));
        assertFalse(skeleton(type).contains("boolean false"));
        assertFalse("record component accessors are implicit and must not become pending stubs",
                skeleton(type).contains("javaspec:stub"));
    }

    @Test
    public void trueLiteralUsesMatchingBooleanAccessorAsComponentName() throws Exception {
        DescribedType type = discoverRecord(
                "beConstructedWith(true);\n" +
                "        ca().shouldReturn(true);");

        assertEquals(Arrays.asList("ca"), constructor(type).parameterNames());
        assertTrue(skeleton(type).contains("record Example(boolean ca)"));
    }

    @Test
    public void multipleBooleanValuesMapToTheirMatchingAccessorsInConstructorOrder() throws Exception {
        DescribedType type = discoverRecord(
                "beConstructedWith(false, true);\n" +
                "        ca().shouldReturn(false);\n" +
                "        critical().shouldReturn(true);");

        assertEquals(Arrays.asList("boolean", "boolean"), constructor(type).parameterTypes());
        assertEquals(Arrays.asList("ca", "critical"), constructor(type).parameterNames());
        assertTrue(skeleton(type).contains("record Example(boolean ca, boolean critical)"));
    }

    @Test
    public void nonIdentifierLiteralValuesNeverBecomeComponentNames() throws Exception {
        DescribedType type = discoverRecord(
                "beConstructedWith(397, \"serverAuth\", ExampleToken.ACTIVE, -2, null);\n" +
                "        serialNumber().shouldReturn(397);\n" +
                "        purpose().shouldReturn(\"serverAuth\");\n" +
                "        token().shouldReturn(ExampleToken.ACTIVE);\n" +
                "        offset().shouldReturn(-2);\n" +
                "        comment().shouldReturn(null);");

        assertEquals(Arrays.asList("serialNumber", "purpose", "token", "offset", "comment"),
                constructor(type).parameterNames());
        String generated = skeleton(type);
        assertFalse(generated.contains(" 397"));
        assertFalse(generated.contains(" serverAuth"));
        assertFalse(generated.contains(" ACTIVE"));
        assertFalse(generated.contains(" -2"));
    }

    @Test
    public void accessorEvidenceOverridesSixLocalNamesAndPreservesGenericTypes() throws Exception {
        File specRoot = temporaryFolder.newFolder("aggregate-spec");
        File specFile = new File(specRoot, "spec/com/example/CertificateProfileSpec.java");
        assertTrue(specFile.getParentFile().mkdirs());
        String source = "package spec.com.example;\n" +
                "import com.example.BasicConstraints;\n" +
                "import com.example.ExtendedKeyUsage;\n" +
                "import com.example.KeyUsage;\n" +
                "import com.example.SubjectPublicKeyProfile;\n" +
                "import com.example.SubjectTemplate;\n" +
                "import com.example.ValidityDays;\n" +
                "import java.util.List;\n" +
                "public class CertificateProfileSpec extends CertificateProfileSpecSupport {\n" +
                "    public void it_forms_the_profile() {\n" +
                "        SubjectTemplate subject = null;\n" +
                "        List<SubjectPublicKeyProfile> publicKeys = null;\n" +
                "        ValidityDays validity = null;\n" +
                "        List<KeyUsage> keyUsages = null;\n" +
                "        List<ExtendedKeyUsage> extendedKeyUsages = null;\n" +
                "        BasicConstraints constraints = null;\n" +
                "        shouldBeARecord();\n" +
                "        beConstructedWith(subject, publicKeys, validity, keyUsages, extendedKeyUsages, constraints);\n" +
                "        subjectTemplate().shouldReturn(subject);\n" +
                "        publicKeyAlgorithms().shouldReturn(publicKeys);\n" +
                "        validityDays().shouldReturn(validity);\n" +
                "        keyUsages().shouldReturn(keyUsages);\n" +
                "        extendedKeyUsages().shouldReturn(extendedKeyUsages);\n" +
                "        basicConstraints().shouldReturn(constraints);\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), source.getBytes(StandardCharsets.UTF_8));

        DescribedType type = SpecDiscovery.discover(specRoot).get(0).describedType();

        assertEquals(Arrays.asList(
                "subjectTemplate", "publicKeyAlgorithms", "validityDays",
                "keyUsages", "extendedKeyUsages", "basicConstraints"),
                constructor(type).parameterNames());
        assertEquals(Arrays.asList(
                "com.example.SubjectTemplate",
                "java.util.List<com.example.SubjectPublicKeyProfile>",
                "com.example.ValidityDays",
                "java.util.List<com.example.KeyUsage>",
                "java.util.List<com.example.ExtendedKeyUsage>",
                "com.example.BasicConstraints"), constructor(type).parameterTypes());
        String skeleton = TypeSkeletonGenerator.render(type);
        assertTrue(skeleton, skeleton.contains("SubjectTemplate subjectTemplate"));
        assertTrue(skeleton, skeleton.contains("List<SubjectPublicKeyProfile> publicKeyAlgorithms"));
        assertFalse(skeleton, skeleton.contains("javaspec:stub"));
        String support = SpecSkeletonGenerator.renderSupport(type);
        assertTrue(support, support.contains("import com.example.SubjectPublicKeyProfile;"));
        assertTrue(support, support.contains("Matchable<List<SubjectPublicKeyProfile>> publicKeyAlgorithms()"));
        File supportRoot = temporaryFolder.newFolder("aggregate-support");
        SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(
                type, specRoot, supportRoot, SpecNamingConvention.defaults());
        SpecSupportFileGenerator.SupportWriteResult firstWrite =
                SpecSupportFileGenerator.writeOrUpdateResult(supportPlan);
        String firstSupport = new String(Files.readAllBytes(firstWrite.file().toPath()), StandardCharsets.UTF_8);
        SpecSupportFileGenerator.SupportWriteResult secondWrite =
                SpecSupportFileGenerator.writeOrUpdateResult(supportPlan);
        assertTrue(firstWrite.changed());
        assertFalse(secondWrite.changed());
        assertEquals(firstSupport,
                new String(Files.readAllBytes(secondWrite.file().toPath()), StandardCharsets.UTF_8));
        if (javaSpecificationVersion() >= 17) {
            compileAndRunAggregate(specFile, skeleton, support);
        }
    }

    @Test
    public void duplicateAccessorEvidenceCannotMapToTwoConstructorPositions() throws Exception {
        DescribedType type = discoverRecord(
                "beConstructedWith(false, false);\n" +
                "        ca().shouldReturn(false);");

        assertAmbiguousName(type, 1, "boolean", "not unique or mapped to exactly one constructor position");
    }

    @Test
    public void enumLiteralWithoutAccessorEvidenceFailsInsteadOfUsingTypeDerivedName() throws Exception {
        DescribedType type = discoverRecord("beConstructedWith(ExampleToken.ACTIVE);");

        assertAmbiguousName(type, 0, "com.example.ExampleToken", "no matching accessor expectation");
    }

    @Test
    public void missingNamingEvidenceFailsBeforeRenderingARecordSkeleton() throws Exception {
        DescribedType type = discoverRecord("beConstructedWith(false);");

        assertAmbiguousName(type, 0, "boolean", "no matching accessor expectation");
    }

    @Test
    public void generationOrchestratorReportsAmbiguityBeforeAnyProductionOrSupportWrite() throws Exception {
        DiscoveredSpec spec = discoverSpec("beConstructedWith(false);");
        File sourceRoot = temporaryFolder.newFolder("ambiguous-source");
        File generatedRoot = new File(temporaryFolder.getRoot(), "ambiguous-generated");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        GenerationOrchestratorResult result = GenerationOrchestrator.execute(
                Arrays.asList(spec),
                spec.specFile().getParentFile().getParentFile().getParentFile().getParentFile(),
                sourceRoot,
                new BufferedReader(new StringReader("")),
                new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"),
                new PrintStream(errors, true, "UTF-8"),
                true,
                false,
                SpecNamingConvention.defaults(),
                getClass().getClassLoader(),
                ConstructorPolicy.PRESERVE,
                generatedRoot
        );

        String diagnostic = errors.toString("UTF-8");
        assertFalse(result.shouldProceed());
        assertEquals(1, result.exitCode());
        assertTrue(diagnostic, diagnostic.contains("AMBIGUOUS_RECORD_COMPONENT_NAME"));
        assertTrue(diagnostic, diagnostic.contains("component index 0"));
        assertFalse(new File(sourceRoot, "com/example/Example.java").exists());
        assertFalse(generatedRoot.exists());
    }

    @Test
    public void javaKeywordNamingEvidenceFailsBeforeRenderingARecordSkeleton() throws Exception {
        DescribedType type = DescribedType.of(
                "com.example.Example",
                JavaTypeKind.RECORD,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("boolean"), Arrays.asList("class"), "")),
                Collections.emptyList()
        );

        assertAmbiguousName(type, 0, "boolean", "illegal Java identifier");
    }

    private void compileAndRunAggregate(File specFile, String subjectSource, String supportSource) throws Exception {
        File sourceRoot = temporaryFolder.newFolder("aggregate-production");
        File generatedRoot = temporaryFolder.newFolder("aggregate-generated");
        List<File> sources = new ArrayList<File>();
        sources.add(writeSource(sourceRoot, "com/example/CertificateProfile.java", subjectSource));
        sources.add(writeSource(generatedRoot, "spec/com/example/CertificateProfileSpecSupport.java", supportSource));
        sources.add(specFile);
        String[] names = {
                "SubjectTemplate", "SubjectPublicKeyProfile", "ValidityDays",
                "KeyUsage", "ExtendedKeyUsage", "BasicConstraints"
        };
        for (int i = 0; i < names.length; i++) {
            sources.add(writeSource(sourceRoot, "com/example/" + names[i] + ".java",
                    "package com.example; public final class " + names[i] + " { }\n"));
        }
        File classes = temporaryFolder.newFolder("aggregate-classes");
        List<String> arguments = new ArrayList<String>();
        arguments.add("--release");
        arguments.add("17");
        arguments.add("-classpath");
        arguments.add(System.getProperty("java.class.path"));
        arguments.add("-d");
        arguments.add(classes.getAbsolutePath());
        for (int i = 0; i < sources.size(); i++) arguments.add(sources.get(i).getAbsolutePath());
        ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int exit = compiler.run(null, compilerOutput, compilerOutput,
                arguments.toArray(new String[arguments.size()]));
        assertEquals(new String(compilerOutput.toByteArray(), StandardCharsets.UTF_8), 0, exit);
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()}, getClass().getClassLoader());
        try {
            Class<?> specClass = Class.forName("spec.com.example.CertificateProfileSpec", true, loader);
            Object spec = specClass.newInstance();
            specClass.getMethod("it_forms_the_profile").invoke(spec);
        } finally {
            loader.close();
        }
    }

    private static File writeSource(File root, String relativePath, String source) throws Exception {
        File file = new File(root, relativePath);
        assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static int javaSpecificationVersion() {
        String value = System.getProperty("java.specification.version", "8");
        if (value.startsWith("1.")) value = value.substring(2);
        int dot = value.indexOf('.');
        return Integer.parseInt(dot < 0 ? value : value.substring(0, dot));
    }

    private void assertAmbiguousName(DescribedType type, int index, String componentType, String reason) {
        try {
            TypeSkeletonGenerator.plan(type, temporaryFolder.getRoot());
            fail("Expected ambiguous record component name failure");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("AMBIGUOUS_RECORD_COMPONENT_NAME"));
            assertTrue(ex.getMessage(), ex.getMessage().contains(type.qualifiedName()));
            assertTrue(ex.getMessage(), ex.getMessage().contains("component index " + index));
            assertTrue(ex.getMessage(), ex.getMessage().contains("inferred type " + componentType));
            assertTrue(ex.getMessage(), ex.getMessage().contains(reason));
        }
    }

    private DescribedType discoverRecord(String body) throws Exception {
        return discoverSpec(body).describedType();
    }

    private DiscoveredSpec discoverSpec(String body) throws Exception {
        File specRoot = temporaryFolder.newFolder("spec-" + System.nanoTime());
        File specFile = new File(specRoot, "spec/com/example/ExampleSpec.java");
        assertTrue(specFile.getParentFile().mkdirs());
        String source = "package spec.com.example;\n" +
                "public class ExampleSpec extends ExampleSpecSupport {\n" +
                "    public void it_describes_components() {\n" +
                "        shouldBeARecord();\n" +
                "        " + body + "\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), source.getBytes(StandardCharsets.UTF_8));
        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);
        assertEquals(1, specs.size());
        return specs.get(0);
    }

    private ConstructorDescriptor constructor(DescribedType type) {
        assertEquals(1, type.constructors().size());
        return type.constructors().get(0);
    }

    private String skeleton(DescribedType type) {
        return TypeSkeletonGenerator.plan(type, temporaryFolder.getRoot()).sourceContent();
    }
}
