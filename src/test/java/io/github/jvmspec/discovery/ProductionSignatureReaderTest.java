package io.github.jvmspec.discovery;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeTrue;

/**
 * When the production class already exists, its real signatures are the truth: discovery
 * inference must be refined with the declared return types, parameter types and parameter
 * names read from the production source.
 */
public class ProductionSignatureReaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File writeProductionSource(String relativePath, String source) throws Exception {
        File sourceRoot = temporaryFolder.newFolder("production-source-root");
        File file = new File(sourceRoot, relativePath);
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("cannot create " + parent);
        }
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        return sourceRoot;
    }

    private static DescribedType describedKey(List<ConstructorDescriptor> constructors, List<MethodDescriptor> methods) {
        return DescribedType.of(
                "com.example.Key",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                constructors,
                methods
        );
    }

    @Test
    public void refinesInferredSignaturesFromProductionSource() throws Exception {
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "import java.util.UUID;\n\n" +
                "public class Key {\n" +
                "    public UUID id() {\n" +
                "        return null;\n" +
                "    }\n\n" +
                "    public KeyHandle toKeyHandle() {\n" +
                "        return null;\n" +
                "    }\n\n" +
                "    public void rotate(int graceDays) {\n" +
                "    }\n" +
                "}\n");
        DescribedType described = describedKey(
                Collections.<ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("id", "Object"),
                        MethodDescriptor.of("toKeyHandle", "Object"),
                        MethodDescriptor.voidMethod("rotate",
                                Arrays.asList("Object"), Arrays.asList("arg0"))
                                .withUnknownParameterTypes(Arrays.asList(Boolean.TRUE)),
                        MethodDescriptor.of("missingInProduction", "String")
                ));

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(Arrays.asList(
                MethodDescriptor.of("id", "java.util.UUID"),
                MethodDescriptor.of("toKeyHandle", "com.example.KeyHandle"),
                MethodDescriptor.voidMethod("rotate",
                        Arrays.asList("int"), Arrays.asList("graceDays")),
                MethodDescriptor.of("missingInProduction", "String")
        ), refined.methods());
    }

    @Test
    public void refinesConstructorParameterTypesAndNames() throws Exception {
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "public class Key {\n" +
                "    private final Algorithm algorithm;\n" +
                "    private final String label;\n\n" +
                "    public Key(Algorithm algorithm, String label) {\n" +
                "        this.algorithm = algorithm;\n" +
                "        this.label = label;\n" +
                "    }\n" +
                "}\n");
        DescribedType described = describedKey(
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("Object", "String"),
                        Arrays.asList("arg0", "label"),
                        "")),
                Collections.<MethodDescriptor>emptyList());

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(1, refined.constructors().size());
        assertEquals(Arrays.asList("com.example.Algorithm", "String"),
                refined.constructors().get(0).parameterTypes());
        assertEquals(Arrays.asList("algorithm", "label"),
                refined.constructors().get(0).parameterNames());
    }

    @Test
    public void refinesUnknownMethodArgumentsToBestProductionOverload() throws Exception {
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "public class Key {\n" +
                "    public boolean isCanonicalText(String value) { return true; }\n" +
                "}\n");
        DescribedType described = describedKey(
                Collections.<ConstructorDescriptor>emptyList(),
                Arrays.asList(MethodDescriptor.of(
                        "isCanonicalText", "boolean", Arrays.asList("Object"), Arrays.asList("arg0"))
                        .withUnknownParameterTypes(Arrays.asList(Boolean.TRUE))));

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(Arrays.asList(MethodDescriptor.of(
                "isCanonicalText", "boolean", Arrays.asList("String"), Arrays.asList("value"))), refined.methods());
    }

    @Test
    public void doesNotRefineKnownObjectArgumentToStringOverload() throws Exception {
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "public class Key {\n" +
                "    public boolean isCanonicalText(String value) { return true; }\n" +
                "}\n");
        DescribedType described = describedKey(
                Collections.<ConstructorDescriptor>emptyList(),
                Arrays.asList(MethodDescriptor.of(
                        "isCanonicalText", "boolean", Arrays.asList("Object"), Arrays.asList("value"))));

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(Arrays.asList(MethodDescriptor.of(
                "isCanonicalText", "boolean", Arrays.asList("Object"), Arrays.asList("value"))), refined.methods());
    }

    @Test
    public void keepsUnknownMethodArgumentWhenProductionOverloadMatchIsAmbiguous() throws Exception {
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "public class Key {\n" +
                "    public boolean isCanonicalText(String value) { return true; }\n" +
                "    public boolean isCanonicalText(Integer value) { return false; }\n" +
                "}\n");
        MethodDescriptor unknownNullCall = MethodDescriptor.of(
                "isCanonicalText", "boolean", Arrays.asList("Object"), Arrays.asList("arg0"))
                .withUnknownParameterTypes(Arrays.asList(Boolean.TRUE));
        DescribedType described = describedKey(
                Collections.<ConstructorDescriptor>emptyList(),
                Arrays.asList(unknownNullCall));

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(Arrays.asList(unknownNullCall), refined.methods());
    }

    @Test
    public void keepsUnknownConstructorArgumentWhenProductionOverloadMatchIsAmbiguous() throws Exception {
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "public class Key {\n" +
                "    public Key(String value) { }\n" +
                "    public Key(Integer value) { }\n" +
                "}\n");
        ConstructorDescriptor unknownConstructor = ConstructorDescriptor.of(
                Arrays.asList("Object"), Arrays.asList("arg0"), "");
        DescribedType described = describedKey(
                Arrays.asList(unknownConstructor),
                Collections.<MethodDescriptor>emptyList());

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(Arrays.asList(unknownConstructor), refined.constructors());
    }

    @Test
    public void preservesRealObjectAndStringProductionOverloads() throws Exception {
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "public class Key {\n" +
                "    public boolean isCanonicalText(Object value) { return false; }\n" +
                "    public boolean isCanonicalText(String value) { return true; }\n" +
                "}\n");
        DescribedType described = describedKey(
                Collections.<ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("Object"), Arrays.asList("arg0")),
                        MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("String"), Arrays.asList("arg0"))));

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(Arrays.asList(
                MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("Object"), Arrays.asList("value")),
                MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("String"), Arrays.asList("value"))
        ), refined.methods());
    }

    @Test
    public void refinesKindFromExistingRecordSource() throws Exception {
        assumeTrue(supportsJavaSpecificationVersion(17));
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "public record Key(String value) {\n" +
                "}\n");
        DescribedType described = describedKey(
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String"), Arrays.asList("value"), "")),
                Collections.<MethodDescriptor>emptyList());

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(JavaTypeKind.RECORD, refined.kind());
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

    @Test
    public void returnsSameDescribedTypeWhenProductionSourceIsAbsent() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("empty-source-root");
        DescribedType described = describedKey(
                Collections.<ConstructorDescriptor>emptyList(),
                Arrays.asList(MethodDescriptor.of("id", "Object")));

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertSame(described, refined);
    }

    @Test
    public void keepsInferredConstructorWhenProductionTypesConflict() throws Exception {
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "public class Key {\n" +
                "    public Key(int legacy) {\n" +
                "    }\n" +
                "}\n");
        DescribedType described = describedKey(
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String"), Arrays.asList("title"), "")),
                Collections.<MethodDescriptor>emptyList());

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(Arrays.asList("String"), refined.constructors().get(0).parameterTypes());
        assertEquals(Arrays.asList("title"), refined.constructors().get(0).parameterNames());
    }

    @Test
    public void doesNotRefineWhenArityDiffers() throws Exception {
        File sourceRoot = writeProductionSource("com/example/Key.java",
                "package com.example;\n\n" +
                "public class Key {\n" +
                "    public String label(int index) {\n" +
                "        return null;\n" +
                "    }\n" +
                "}\n");
        DescribedType described = describedKey(
                Collections.<ConstructorDescriptor>emptyList(),
                Arrays.asList(MethodDescriptor.of("label", "Object")));

        DescribedType refined = ProductionSignatureReader.refine(described, sourceRoot);

        assertEquals(Arrays.asList(MethodDescriptor.of("label", "Object")), refined.methods());
    }
}
