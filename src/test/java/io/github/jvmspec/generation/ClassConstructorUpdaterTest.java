package io.github.jvmspec.generation;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassConstructorUpdaterTest {
    private static final String SIMPLE_CLASS = "package com.example;\n\npublic class Service { }\n";

    private static final String CLASS_WITH_EMPTY_CONSTRUCTOR =
            "package com.example;\n\npublic class Service {\n" +
            "    public Service() { }\n" +
            "}\n";

    private static final String CLASS_WITH_PARAM_CONSTRUCTOR =
            "package com.example;\n\npublic class Service {\n" +
            "    public Service(String name, int count) { }\n" +
            "}\n";

    private static final String CLASS_WITH_BODY_CONSTRUCTOR =
            "package com.example;\n\npublic class Service {\n" +
            "    public Service(String name) {\n" +
            "        this.name = name;\n" +
            "    }\n" +
            "}\n";

    private static final String CLASS_WITH_MULTIPLE_CONSTRUCTORS =
            "package com.example;\n\npublic class Service {\n" +
            "    public Service() { }\n" +
            "    public Service(String name) {\n" +
            "        this.name = name;\n" +
            "    }\n" +
            "}\n";

    private static final String CLASS_WITH_SUBSET_CONSTRUCTOR =
            "package com.example;\n\npublic class Service {\n" +
            "    public Service(String name) {\n" +
            "        this.name = name;\n" +
            "    }\n" +
            "}\n";

    // --- Rule 1: Empty constructors not in spec are always deleted ---

    @Test
    public void deletesEmptyConstructorNotInSpecRegardlessOfPolicy() {
        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("String"),
                Arrays.asList("name"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        // Test with PRESERVE policy
        String updatedPreserve = ClassConstructorUpdater.updateSource(
                CLASS_WITH_EMPTY_CONSTRUCTOR, type, ConstructorPolicy.PRESERVE);
        assertTrue(updatedPreserve.contains("public Service(String name) {"));
        assertFalse(updatedPreserve.contains("public Service() { }"));

        // Test with DELETE policy
        String updatedDelete = ClassConstructorUpdater.updateSource(
                CLASS_WITH_EMPTY_CONSTRUCTOR, type, ConstructorPolicy.DELETE);
        assertTrue(updatedDelete.contains("public Service(String name) {"));
        assertFalse(updatedDelete.contains("public Service() { }"));

        // Test with COMMENT policy
        String updatedComment = ClassConstructorUpdater.updateSource(
                CLASS_WITH_EMPTY_CONSTRUCTOR, type, ConstructorPolicy.COMMENT);
        assertTrue(updatedComment.contains("public Service(String name) {"));
        assertFalse(updatedComment.contains("public Service() { }"));
    }

    // --- Rule 2: Non-empty constructors not in spec follow policy ---

    @Test
    public void preservesNonEmptyConstructorWithPreservePolicy() {
        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("int"),
                Arrays.asList("id"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_BODY_CONSTRUCTOR, type, ConstructorPolicy.PRESERVE);

        assertTrue(updated.contains("public Service(int id) {"));
        assertTrue(updated.contains("public Service(String name) {"));
        assertTrue(updated.contains("this.name = name;"));
    }

    @Test
    public void deletesNonEmptyConstructorWithDeletePolicy() {
        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("int"),
                Arrays.asList("id"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_BODY_CONSTRUCTOR, type, ConstructorPolicy.DELETE);

        assertTrue(updated.contains("public Service(int id) {"));
        assertFalse(updated.contains("public Service(String name) {"));
        assertFalse(updated.contains("this.name = name;"));
    }

    @Test
    public void commentsOutNonEmptyConstructorWithCommentPolicy() {
        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("int"),
                Arrays.asList("id"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_BODY_CONSTRUCTOR, type, ConstructorPolicy.COMMENT);

        assertTrue(updated.contains("public Service(int id) {"));
        assertTrue(updated.contains("/*"));
        assertTrue(updated.contains("public Service(String name) {"));
        assertTrue(updated.contains("this.name = name;"));
        assertTrue(updated.contains("*/"));
    }

    // --- Rule 3: Extend existing constructor when spec adds params ---

    @Test
    public void extendsExistingConstructorWhenSpecAddsNewParams() {
        // Existing constructor: Service(String name) { this.name = name; }
        // Spec constructor: Service(String name, int count) { }
        // Result: Service(String name, int count) { this.name = name; }
        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("String", "int"),
                Arrays.asList("name", "count"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_SUBSET_CONSTRUCTOR, type, ConstructorPolicy.DELETE);

        // Should extend the existing constructor, not add a separate one
        assertTrue(updated.contains("public Service(String name, int count) {"));
        assertTrue(updated.contains("this.name = name;"));
        // The old constructor should NOT appear separately
        assertFalse(updated.contains("public Service(String name) {"));
    }

    @Test
    public void extendsExistingConstructorAndAddsNewParamsOnly() {
        // Existing: Service(String name, int id) { this.name = name; this.id = id; }
        // Spec: Service(String name, int id, long timestamp) { }
        // Result: Service(String name, int id, long timestamp) { this.name = name; this.id = id; }
        String classWithTwoParams =
                "package com.example;\n\npublic class Service {\n" +
                "    public Service(String name, int id) {\n" +
                "        this.name = name;\n" +
                "        this.id = id;\n" +
                "    }\n" +
                "}\n";

        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("String", "int", "long"),
                Arrays.asList("name", "id", "timestamp"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                classWithTwoParams, type, ConstructorPolicy.DELETE);

        assertTrue(updated.contains("public Service(String name, int id, long timestamp) {"));
        assertTrue(updated.contains("this.name = name;"));
        assertTrue(updated.contains("this.id = id;"));
    }

    @Test
    public void doesNotExtendWhenExistingHasParamsNotInSpec() {
        // Existing: Service(int id) { this.id = id; }
        // Spec: Service(String name) { }
        // No extension because params differ
        String classWithDifferentParam =
                "package com.example;\n\npublic class Service {\n" +
                "    public Service(int id) {\n" +
                "        this.id = id;\n" +
                "    }\n" +
                "}\n";

        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("String"),
                Arrays.asList("name"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                classWithDifferentParam, type, ConstructorPolicy.DELETE);

        // Both constructors should exist (spec one is new, existing is deleted by policy)
        assertTrue(updated.contains("public Service(String name) {"));
        assertFalse(updated.contains("public Service(int id)"));
    }

    @Test
    public void doesNotExtendWhenExistingHasSameNumberOfParams() {
        // Existing: Service(String name) { this.name = name; }
        // Spec: Service(String name) { }
        // Same params, no extension needed, they match
        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("String"),
                Arrays.asList("name"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_SUBSET_CONSTRUCTOR, type, ConstructorPolicy.DELETE);

        // Should keep the constructor with its body
        assertTrue(updated.contains("public Service(String name) {"));
        assertTrue(updated.contains("this.name = name;"));
    }

    // --- Policy edge cases ---

    @Test
    public void removesAllConstructorsWhenNoSpecAndDeletePolicy() {
        DescribedType type = DescribedType.of("com.example.Service", JavaTypeKind.CLASS);

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_EMPTY_CONSTRUCTOR, type, ConstructorPolicy.DELETE);

        assertFalse(updated.contains("public Service()"));
    }

    @Test
    public void keepsNonEmptyConstructorWhenNoSpecAndPreservePolicy() {
        DescribedType type = DescribedType.of("com.example.Service", JavaTypeKind.CLASS);

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_BODY_CONSTRUCTOR, type, ConstructorPolicy.PRESERVE);

        assertTrue(updated.contains("public Service(String name) {"));
        assertTrue(updated.contains("this.name = name;"));
    }

    @Test
    public void deletesEmptyConstructorWhenNoSpecAndPreservePolicy() {
        DescribedType type = DescribedType.of("com.example.Service", JavaTypeKind.CLASS);

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_EMPTY_CONSTRUCTOR, type, ConstructorPolicy.PRESERVE);

        assertFalse(updated.contains("public Service() { }"));
        // Class should still have the class declaration
        assertTrue(updated.contains("class Service"));
    }

    @Test
    public void commentsOutNonEmptyConstructorWhenNoSpecAndCommentPolicy() {
        DescribedType type = DescribedType.of("com.example.Service", JavaTypeKind.CLASS);

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_BODY_CONSTRUCTOR, type, ConstructorPolicy.COMMENT);

        assertTrue(updated.contains("/*"));
        assertTrue(updated.contains("public Service(String name) {"));
        assertTrue(updated.contains("this.name = name;"));
        assertTrue(updated.contains("*/"));
    }

    @Test
    public void deletesEmptyConstructorWhenNoSpecAndCommentPolicy() {
        DescribedType type = DescribedType.of("com.example.Service", JavaTypeKind.CLASS);

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_EMPTY_CONSTRUCTOR, type, ConstructorPolicy.COMMENT);

        assertFalse(updated.contains("public Service() { }"));
        assertFalse(updated.contains("/*"));
    }

    @Test
    public void matchesGenericConstructorRegardlessOfParameterNameAndPreservesBodyByteForByte() {
        String source = "package com.example;\n\n" +
                "import java.util.Map;\n\n" +
                "public class Service {\n" +
                "    public Service(Map<String, String> normalizedFields) {\n" +
                "        this.value = normalizedFields.toString();\n" +
                "    }\n" +
                "}\n";
        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("java.util.Map<java.lang.String, java.lang.String>"),
                Arrays.asList("fields"), "");
        DescribedType type = DescribedType.of(
                "com.example.Service", JavaTypeKind.CLASS,
                Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Arrays.asList(spec));

        String updated = ClassConstructorUpdater.updateSource(source, type, ConstructorPolicy.COMMENT);

        assertEquals(source, updated);
        assertFalse(updated.contains("/*"));
        assertEquals(1, countOccurrences(updated, "public Service("));
    }

    @Test
    public void parsesNestedGenericAnnotatedArrayVarargsAndMultipleParameters() {
        String source = "package com.example;\n\n" +
                "import java.util.List;\n" +
                "import java.util.Map;\n\n" +
                "public class Service {\n" +
                "    public Service(\n" +
                "            final String first,\n" +
                "            @Deprecated Map<String, List<Integer>> second,\n" +
                "            Map<String, List<Map<String, Integer>>> third,\n" +
                "            String... values) {\n" +
                "        this.value = first;\n" +
                "    }\n" +
                "}\n";
        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList(
                        "String",
                        "java.util.Map<String, java.util.List<Integer>>",
                        "java.util.Map<String, java.util.List<java.util.Map<String, Integer>>>",
                        "String[]"),
                Arrays.asList("a", "b", "c", "d"), "");
        DescribedType type = DescribedType.of(
                "com.example.Service", JavaTypeKind.CLASS,
                Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Arrays.asList(spec));

        String updated = ClassConstructorUpdater.updateSource(source, type, ConstructorPolicy.COMMENT);

        assertEquals(source, updated);
        assertEquals(1, countOccurrences(updated, "public Service("));
    }

    @Test
    public void addsMultipleConstructorsFromSpec() {
        ConstructorDescriptor spec1 = ConstructorDescriptor.of(
                Arrays.asList("String"),
                Arrays.asList("name"),
                ""
        );
        ConstructorDescriptor spec2 = ConstructorDescriptor.of(
                Arrays.asList("int"),
                Arrays.asList("id"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec1, spec2)
        );

        String updated = ClassConstructorUpdater.updateSource(
                SIMPLE_CLASS, type, ConstructorPolicy.DELETE);

        assertTrue(updated.contains("public Service(String name) {"));
        assertTrue(updated.contains("public Service(int id) {"));
    }

    // --- Regression: non-constructor members must never be discarded ---

    @Test
    public void preservesFieldsAndMethodBodiesUntouchedWhenUpdatingMatchedConstructor() {
        // A matched constructor update must not discard fields or method bodies that are
        // not part of the tracked constructor model. Only constructor declarations are
        // this updater's concern.
        String classWithFieldAndMethod =
                "package com.example;\n\npublic class Service {\n" +
                "    private final String name;\n\n" +
                "    public Service(String name) {\n" +
                "        this.name = name;\n" +
                "    }\n\n" +
                "    public String name() {\n" +
                "        return name;\n" +
                "    }\n" +
                "}\n";

        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("String"),
                Arrays.asList("name"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                classWithFieldAndMethod, type, ConstructorPolicy.COMMENT);

        assertTrue("field declaration must survive a constructor update",
                updated.contains("private final String name;"));
        assertTrue("method declaration must survive a constructor update",
                updated.contains("public String name() {"));
        assertTrue("method body must survive a constructor update",
                updated.contains("return name;"));
        assertTrue("matched constructor body must be preserved",
                updated.contains("this.name = name;"));
    }

    @Test
    public void updateFileIfChangedReturnsFalseAndLeavesContentWhenNothingChanges() throws Exception {
        // Truthful reporting: when the source already satisfies the spec constructors,
        // the file must not be rewritten and the caller must learn that nothing changed.
        java.io.File classFile = java.io.File.createTempFile("Service", ".java");
        classFile.deleteOnExit();
        java.nio.file.Files.write(classFile.toPath(),
                CLASS_WITH_BODY_CONSTRUCTOR.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("String"),
                Arrays.asList("name"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        boolean changed = ClassConstructorUpdater.updateFileIfChanged(classFile, type, ConstructorPolicy.COMMENT);

        assertFalse("no-op update must report no change", changed);
        String content = new String(java.nio.file.Files.readAllBytes(classFile.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("file content must be untouched", CLASS_WITH_BODY_CONSTRUCTOR, content);

        // And when a change IS needed, it must report true and write it.
        DescribedType differentType = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("int"), Arrays.asList("id"), ""))
        );
        boolean changedNow = ClassConstructorUpdater.updateFileIfChanged(classFile, differentType, ConstructorPolicy.COMMENT);
        assertTrue("real update must report a change", changedNow);
        String rewritten = new String(java.nio.file.Files.readAllBytes(classFile.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(rewritten.contains("public Service(int id) {"));
    }

    @Test
    public void matchesConstructorRegardlessOfParameterNamesAndTypeQualification() {
        // Discovery often infers synthetic parameter names (arg0, arg1) and fully-qualified
        // parameter types, while hand-written production code uses meaningful names and simple
        // type names. Parameter names are an implementation detail of the production class and
        // must not break constructor matching; type comparison is simple-name based.
        String classWithNamedParams =
                "package com.example;\n\npublic class Key {\n" +
                "    private final String label;\n\n" +
                "    public Key(Algorithm algorithm, KeyUsage usage, String label) {\n" +
                "        this.label = label;\n" +
                "    }\n" +
                "}\n";

        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("com.example.Algorithm", "com.example.KeyUsage", "String"),
                Arrays.asList("arg0", "arg1", "arg2"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Key",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                classWithNamedParams, type, ConstructorPolicy.COMMENT);

        assertTrue("constructor with meaningful parameter names must be treated as matched",
                updated.contains("public Key(Algorithm algorithm, KeyUsage usage, String label) {"));
        assertTrue("matched constructor body must be preserved",
                updated.contains("this.label = label;"));
        assertFalse("no synthetic stub constructor must be inserted",
                updated.contains("arg0"));
        assertFalse("matched constructor must not be commented out",
                updated.contains("/*"));
    }

    @Test
    public void preservesFieldsAndMethodsWhenUnmatchedConstructorIsCommented() {
        // Same as above, but the existing constructor does NOT match any spec constructor,
        // forcing the COMMENT policy path. Fields/methods must still survive untouched.
        String classWithFieldAndMethod =
                "package com.example;\n\npublic class Service {\n" +
                "    private final int count;\n\n" +
                "    public Service(String name) {\n" +
                "        this.count = 0;\n" +
                "    }\n\n" +
                "    public int count() {\n" +
                "        return count;\n" +
                "    }\n" +
                "}\n";

        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("int"),
                Arrays.asList("id"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                classWithFieldAndMethod, type, ConstructorPolicy.COMMENT);

        assertTrue("field declaration must survive even when its constructor is commented",
                updated.contains("private final int count;"));
        assertTrue("method declaration must survive even when its constructor is commented",
                updated.contains("public int count() {"));
        assertTrue("method body must survive even when its constructor is commented",
                updated.contains("return count;"));
    }

    @Test
    public void updatesRecordHeaderAndPreservesCompactConstructor() {
        String recordWithCompactConstructor =
                "package com.example;\n\npublic record UserId() {\n" +
                "    public UserId {\n" +
                "        if (value == null) {\n" +
                "            throw new IllegalArgumentException(\"value\");\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        DescribedType type = DescribedType.of(
                "com.example.UserId",
                JavaTypeKind.RECORD,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String"),
                        Arrays.asList("arg0"),
                        "")),
                Arrays.asList(MethodDescriptor.of("value", "String"))
        );

        String updated = ClassConstructorUpdater.updateSource(
                recordWithCompactConstructor, type, ConstructorPolicy.COMMENT);

        assertTrue(updated.contains("public record UserId(String value)"));
        assertTrue(updated.contains("public UserId {"));
        assertTrue(updated.contains("if (value == null)"));
        assertFalse(updated.contains("public record UserId()"));
    }

    @Test
    public void handlesMultipleConstructorsWithExtendAndPreserve() {
        // Existing: Service() { } (empty) and Service(String name) { this.name = name; }
        // Spec: Service(String name, int count) { }
        // Empty constructor deleted (always), non-empty extended
        ConstructorDescriptor spec = ConstructorDescriptor.of(
                Arrays.asList("String", "int"),
                Arrays.asList("name", "count"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(spec)
        );

        String updated = ClassConstructorUpdater.updateSource(
                CLASS_WITH_MULTIPLE_CONSTRUCTORS, type, ConstructorPolicy.PRESERVE);

        // Empty constructor deleted
        assertFalse(updated.contains("public Service() { }"));
        // Extended constructor with preserved body
        assertTrue(updated.contains("public Service(String name, int count) {"));
        assertTrue(updated.contains("this.name = name;"));
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
}
