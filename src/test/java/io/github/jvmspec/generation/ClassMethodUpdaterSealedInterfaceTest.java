package io.github.jvmspec.generation;

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
import static org.junit.Assert.assertTrue;

/**
 * Phase 31 coverage: source-preserving updates of existing sealed interfaces (ADR 0023,
 * superseding the deferral half of ADR 0009).
 */
public class ClassMethodUpdaterSealedInterfaceTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void insertsMissingNonStaticDeclarationsIntoSealedRootBodyAndSkipsStaticDescriptors() {
        String updated = ClassMethodUpdater.updateSource(shapeSource(), shapeType());

        assertEquals(1, countOccurrences(updated, "int sides();"));
        assertEquals(1, countOccurrences(updated, "String name();"));
        assertTrue(!updated.contains("unit("));
    }

    @Test
    public void insertsDefaultReturnBodiesIntoNestedPermittedClassImplementations() {
        String updated = ClassMethodUpdater.updateSource(shapeSource(), shapeType());

        // Circle is empty and receives both bodies; Square already implements sides().
        assertEquals(2, countOccurrences(updated, "public int sides()"));
        assertEquals(2, countOccurrences(updated, "public String name()"));
        assertEquals(1, countOccurrences(updated, "return 0;"));
        assertEquals(1, countOccurrences(updated, "return 4;"));
        assertEquals(2, countOccurrences(updated, "return null;"));
    }

    @Test
    public void insertsBodiesIntoGeneratedDefaultNestedPermittedImplementation() {
        String source = "package com.example;\n\n" +
                "public sealed interface Command permits Command.Special {\n" +
                "    final class Permitted implements Command {\n" +
                "    }\n" +
                "}\n";
        DescribedType type = sealedInterfaceType(
                "com.example.Command",
                Collections.<String>emptyList(),
                Arrays.asList(MethodDescriptor.of("execute", "boolean"))
        );

        String updated = ClassMethodUpdater.updateSource(source, type);

        // The generated default "Permitted" implementation is targeted even when the permits
        // clause and the described permitted type names do not mention it.
        assertEquals(1, countOccurrences(updated, "public boolean execute()"));
        assertEquals(1, countOccurrences(updated, "return false;"));
        assertEquals(1, countOccurrences(updated, "boolean execute();"));
    }

    @Test
    public void insertsDeclarationsNotBodiesIntoNestedPermittedInterface() {
        String source = "package com.example;\n\n" +
                "public sealed interface Event permits Event.Special {\n" +
                "    non-sealed interface Special extends Event {\n" +
                "    }\n" +
                "}\n";
        DescribedType type = sealedInterfaceType(
                "com.example.Event",
                Arrays.asList("com.example.Event.Special"),
                Arrays.asList(MethodDescriptor.of("payload", "String"))
        );

        String updated = ClassMethodUpdater.updateSource(source, type);

        // One declaration in the nested interface, one in the sealed root, never a body.
        assertEquals(2, countOccurrences(updated, "String payload();"));
        assertTrue(!updated.contains("public String payload()"));
        assertTrue(!updated.contains("return null;"));
    }

    @Test
    public void deduplicatesSignaturesPerScopeAcrossRootAndNestedImplementations() {
        String source = "package com.example;\n\n" +
                "public sealed interface Shape permits Shape.Circle {\n" +
                "    String name();\n" +
                "\n" +
                "    final class Circle implements Shape {\n" +
                "        public int sides() {\n" +
                "            return 0;\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        DescribedType type = sealedInterfaceType(
                "com.example.Shape",
                Arrays.asList("com.example.Shape.Circle"),
                Arrays.asList(
                        MethodDescriptor.of("sides", "int"),
                        MethodDescriptor.of("name", "String")
                )
        );

        String updated = ClassMethodUpdater.updateSource(source, type);

        // name() already declared in the root is added only to the nested body; sides() already
        // implemented in the nested body is added only to the root.
        assertEquals(1, countOccurrences(updated, "String name();"));
        assertEquals(1, countOccurrences(updated, "public String name()"));
        assertEquals(1, countOccurrences(updated, "int sides();"));
        assertEquals(1, countOccurrences(updated, "public int sides()"));
        assertEquals(updated, ClassMethodUpdater.updateSource(updated, type));
    }

    @Test
    public void leavesOutOfFilePermittedTypesUntouchedAndUpdatesInFileScopesDeterministically() {
        String source = "package com.example;\n\n" +
                "public sealed interface Vehicle permits Car, Vehicle.Bike {\n" +
                "    final class Bike implements Vehicle {\n" +
                "    }\n" +
                "}\n";
        DescribedType type = sealedInterfaceType(
                "com.example.Vehicle",
                Arrays.asList("com.example.Car", "com.example.Vehicle.Bike"),
                Arrays.asList(MethodDescriptor.of("wheels", "int"))
        );

        String updated = ClassMethodUpdater.updateSource(source, type);

        // The out-of-file permitted Car stays a permits-clause mention only.
        assertEquals(1, countOccurrences(updated, "Car"));
        assertEquals(1, countOccurrences(updated, "int wheels();"));
        assertEquals(1, countOccurrences(updated, "public int wheels()"));
        assertEquals(updated, ClassMethodUpdater.updateSource(source, type));
        assertEquals(updated, ClassMethodUpdater.updateSource(updated, type));
    }

    @Test
    public void updateSourceIsIdempotentAndLeavesCompleteSealedInterfaceUnchanged() {
        String updated = ClassMethodUpdater.updateSource(shapeSource(), shapeType());

        assertEquals(updated, ClassMethodUpdater.updateSource(updated, shapeType()));
        assertEquals(completeShapeSource(),
                ClassMethodUpdater.updateSource(completeShapeSource(), completeShapeType()));
    }

    @Test
    public void updateFileWritesUpdatedSealedInterfaceSource() throws Exception {
        File classFile = temporaryFolder.newFile("Shape.java");
        Files.write(classFile.toPath(), shapeSource().getBytes(StandardCharsets.UTF_8));

        String updated = ClassMethodUpdater.updateFile(classFile, shapeType());

        assertEquals(ClassMethodUpdater.updateSource(shapeSource(), shapeType()), updated);
        assertEquals(updated, readFile(classFile));
    }

    @Test
    public void updateFileDoesNotRewriteSealedInterfaceFileWithoutMissingMethods() throws Exception {
        File classFile = temporaryFolder.newFile("CompleteShape.java");
        Files.write(classFile.toPath(), completeShapeSource().getBytes(StandardCharsets.UTF_8));
        long oldTimestamp = 1000000L;
        assertTrue(classFile.setLastModified(oldTimestamp));

        String updated = ClassMethodUpdater.updateFile(classFile, completeShapeType());

        assertEquals(completeShapeSource(), updated);
        assertEquals(completeShapeSource(), readFile(classFile));
        assertEquals(oldTimestamp, classFile.lastModified());
    }

    @Test
    public void hasMissingMethodsIsConsistentForSealedInterfacesBeforeAndAfterUpdate() {
        assertTrue(ClassMethodUpdater.hasMissingMethods(shapeSource(), shapeType()));

        String updated = ClassMethodUpdater.updateSource(shapeSource(), shapeType());

        assertTrue(!ClassMethodUpdater.hasMissingMethods(updated, shapeType()));
        assertTrue(!ClassMethodUpdater.hasMissingMethods(completeShapeSource(), completeShapeType()));
    }

    private static String shapeSource() {
        return "package com.example;\n\n" +
                "public sealed interface Shape permits Shape.Circle, Shape.Square {\n" +
                "    final class Circle implements Shape {\n" +
                "    }\n" +
                "\n" +
                "    final class Square implements Shape {\n" +
                "        public int sides() {\n" +
                "            return 4;\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private static DescribedType shapeType() {
        return sealedInterfaceType(
                "com.example.Shape",
                Arrays.asList("com.example.Shape.Circle", "com.example.Shape.Square"),
                Arrays.asList(
                        MethodDescriptor.of("sides", "int"),
                        MethodDescriptor.of("name", "String"),
                        MethodDescriptor.staticMethod("unit", "com.example.Shape")
                )
        );
    }

    private static String completeShapeSource() {
        return "package com.example;\n\n" +
                "public sealed interface Shape permits Shape.Circle {\n" +
                "    int sides();\n" +
                "\n" +
                "    final class Circle implements Shape {\n" +
                "        public int sides() {\n" +
                "            return 0;\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private static DescribedType completeShapeType() {
        return sealedInterfaceType(
                "com.example.Shape",
                Arrays.asList("com.example.Shape.Circle"),
                Arrays.asList(MethodDescriptor.of("sides", "int"))
        );
    }

    private static DescribedType sealedInterfaceType(
            String typeName,
            List<String> permittedTypeNames,
            List<MethodDescriptor> methods
    ) {
        return DescribedType.of(
                typeName,
                JavaTypeKind.SEALED_INTERFACE,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                permittedTypeNames,
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                methods
        );
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
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
