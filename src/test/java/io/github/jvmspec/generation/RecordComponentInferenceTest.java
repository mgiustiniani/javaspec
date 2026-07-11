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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
