package org.javaspec.generation;

import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.model.MethodDescriptor;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClassMethodUpdaterTest {
    @Test
    public void insertsOnlyMissingMethodsAndDoesNotDuplicateExistingSignatures() {
        String source = "package com.example;\n\n" +
                "public class Book {\n" +
                "    public int getRating() {\n" +
                "        return 5;\n" +
                "    }\n" +
                "}\n";
        DescribedType type = DescribedType.of(
                "com.example.Book",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("getRating", "int"),
                        MethodDescriptor.of("getTitle", "String"),
                        MethodDescriptor.voidMethod("setRating", Arrays.asList("int"), Arrays.asList("rating"))
                )
        );

        String updated = ClassMethodUpdater.updateSource(source, type);
        String updatedAgain = ClassMethodUpdater.updateSource(updated, type);

        assertEquals(updated, updatedAgain);
        assertEquals(1, countOccurrences(updated, "public int getRating()"));
        assertEquals(1, countOccurrences(updated, "public String getTitle()"));
        assertEquals(1, countOccurrences(updated, "public void setRating(int rating)"));
        assertTrue(updated.contains("return 5;"));
        assertTrue(updated.contains("return null;"));
    }

    @Test
    public void insertsMissingInterfaceDeclarationsAndDoesNotDuplicateOnSecondPass() {
        String source = "package com.example;\n\n" +
                "public interface PaymentGateway {\n" +
                "    String status();\n" +
                "}\n";
        DescribedType type = DescribedType.of(
                "com.example.PaymentGateway",
                JavaTypeKind.INTERFACE,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("status", "String"),
                        MethodDescriptor.of(
                                "charge",
                                "boolean",
                                Arrays.asList("String", "int"),
                                Arrays.asList("accountId", "cents")
                        ),
                        MethodDescriptor.staticMethod("named", "com.example.PaymentGateway")
                )
        );

        String updated = ClassMethodUpdater.updateSource(source, type);
        String updatedAgain = ClassMethodUpdater.updateSource(updated, type);

        assertEquals(updated, updatedAgain);
        assertEquals(1, countOccurrences(updated, "String status()"));
        assertEquals(1, countOccurrences(updated, "boolean charge(String accountId, int cents)"));
        assertTrue(!updated.contains("named()"));
        assertTrue(!updated.contains("return false;"));
        assertTrue(!ClassMethodUpdater.hasMissingMethods(updated, type));
    }

    @Test
    public void skipsSealedInterfaceExistingSourceUpdatesEvenWhenMethodsWouldBeMissing() {
        String source = "package com.example;\n\n" +
                "public sealed interface Shape permits Shape.Circle {\n" +
                "    final class Circle implements Shape { }\n" +
                "}\n";
        DescribedType type = DescribedType.of(
                "com.example.Shape",
                JavaTypeKind.SEALED_INTERFACE,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList("com.example.Circle"),
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("sides", "int"),
                        MethodDescriptor.of("name", "String")
                )
        );

        assertEquals(source, ClassMethodUpdater.updateSource(source, type));
        assertTrue(!ClassMethodUpdater.hasMissingMethods(source, type));
    }

    @Test
    public void insertsMissingStaticFactoryMethodsAndDoesNotDuplicateExistingSignatures() {
        String source = "package com.example;\n\n" +
                "public class Book {\n" +
                "    public static Book existing(String title) {\n" +
                "        return new Book(\"existing:\" + title);\n" +
                "    }\n" +
                "}\n";
        DescribedType type = DescribedType.of(
                "com.example.Book",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.staticMethod(
                                "existing",
                                "com.example.Book",
                                Arrays.asList("String"),
                                Arrays.asList("title")
                        ),
                        MethodDescriptor.staticMethod(
                                "create",
                                "com.example.Book",
                                Arrays.asList("String", "int"),
                                Arrays.asList("title", "rating")
                        )
                )
        );

        String updated = ClassMethodUpdater.updateSource(source, type);
        String updatedAgain = ClassMethodUpdater.updateSource(updated, type);

        assertEquals(updated, updatedAgain);
        assertEquals(1, countOccurrences(updated, "public static Book existing(String title)"));
        assertEquals(1, countOccurrences(updated, "public static Book create(String title, int rating)"));
        assertTrue(updated.contains("return new Book(\"existing:\" + title);"));
        assertTrue(updated.contains("return new Book();"));
    }

    @Test
    public void insertsMissingAnnotationElementsAndDoesNotDuplicateOnSecondPass() {
        String source = "package com.example;\n\n" +
                "public @interface GeneratedTag {\n" +
                "    String value();\n" +
                "}\n";
        DescribedType type = DescribedType.of(
                "com.example.GeneratedTag",
                JavaTypeKind.ANNOTATION,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("value", "String"),
                        MethodDescriptor.of("enabled", "boolean"),
                        MethodDescriptor.of("tags", "String[]")
                )
        );

        String updated = ClassMethodUpdater.updateSource(source, type);
        String updatedAgain = ClassMethodUpdater.updateSource(updated, type);

        assertEquals(updated, updatedAgain);
        assertEquals(1, countOccurrences(updated, "String value()"));
        assertEquals(1, countOccurrences(updated, "boolean enabled()"));
        assertEquals(1, countOccurrences(updated, "String[] tags()"));
        assertTrue(!updated.contains("return false;"));
        assertTrue(!ClassMethodUpdater.hasMissingMethods(updated, type));
    }

    @Test
    public void leavesAnnotationSourceUnchangedWhenOnlyIncompatibleDescriptorsAreMissing() {
        String source = "package com.example;\n\n" +
                "public @interface GeneratedTag { }\n";
        DescribedType type = DescribedType.of(
                "com.example.GeneratedTag",
                JavaTypeKind.ANNOTATION,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of(
                                "withParameter",
                                "String",
                                Arrays.asList("String"),
                                Arrays.asList("value")
                        ),
                        MethodDescriptor.staticMethod("staticValue", "String"),
                        MethodDescriptor.of("objectValue", "Object"),
                        MethodDescriptor.voidMethod("nothing")
                )
        );

        assertEquals(source, ClassMethodUpdater.updateSource(source, type));
        assertTrue(!ClassMethodUpdater.hasMissingMethods(source, type));
    }

    @Test
    public void reportsNoMissingMethodsWhenAllSignaturesExist() {
        String source = "package com.example;\n\n" +
                "public class Book {\n" +
                "    public String getTitle() {\n" +
                "        return \"Wizard\";\n" +
                "    }\n" +
                "\n" +
                "    public void setRating(int rating) {\n" +
                "    }\n" +
                "}\n";
        DescribedType type = DescribedType.of(
                "com.example.Book",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("getTitle", "String"),
                        MethodDescriptor.voidMethod("setRating", Arrays.asList("int"), Arrays.asList("rating"))
                )
        );

        assertEquals(source, ClassMethodUpdater.updateSource(source, type));
        assertTrue(!ClassMethodUpdater.hasMissingMethods(source, type));
    }

    private static int countOccurrences(String text, String fragment) {
        int count = 0;
        int index = 0;
        while (true) {
            int found = text.indexOf(fragment, index);
            if (found < 0) {
                return count;
            }
            count++;
            index = found + fragment.length();
        }
    }
}
