package org.javaspec.generation;

import org.javaspec.model.ConstructorDescriptor;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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
}
