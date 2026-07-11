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

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClassMethodUpdaterRecordAndSealedClassTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void treatsRecordComponentsAsExistingAccessorsAndPreservesCompactConstructor() throws Exception {
        String source = "package com.example;\n\n" +
                "public record UserId(String value, int version) {\n" +
                "    public UserId {\n" +
                "        if (value == null) {\n" +
                "            throw new IllegalArgumentException(\"value\");\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        DescribedType type = recordType(
                "com.example.UserId",
                Arrays.asList(
                        MethodDescriptor.of("value", "String"),
                        MethodDescriptor.of("version", "int"),
                        MethodDescriptor.of("normalized", "String")
                )
        );

        String updated = ClassMethodUpdater.updateSource(source, type);
        String updatedAgain = ClassMethodUpdater.updateSource(updated, type);

        assertEquals(updated, updatedAgain);
        assertEquals(0, countOccurrences(updated, "public String value()"));
        assertEquals(0, countOccurrences(updated, "public int version()"));
        assertEquals(1, countOccurrences(updated, "public String normalized()"));
        assertTrue(updated.contains("public UserId {"));
        assertTrue(!ClassMethodUpdater.hasMissingMethods(updated, type));
        assertCompilesAsJava17Source(updated, "com/example/UserId.java");
    }

    @Test
    public void recordComponentAccessorDetectionToleratesAnnotationsGenericsAndComments() {
        String source = "package com.example;\n\n" +
                "public record Box<T>(\n" +
                "        @Deprecated java.util.List<T> values,\n" +
                "        T item\n" +
                ") {\n" +
                "    // public int size() { return 1; }\n" +
                "    private String text = \"public boolean empty() { return true; }\";\n" +
                "}\n";
        DescribedType type = recordType(
                "com.example.Box",
                Arrays.asList(
                        MethodDescriptor.of("values", "java.util.List<T>"),
                        MethodDescriptor.of("item", "T"),
                        MethodDescriptor.of("size", "int"),
                        MethodDescriptor.of("empty", "boolean")
                )
        );

        String updated = ClassMethodUpdater.updateSource(source, type);

        assertEquals(updated, ClassMethodUpdater.updateSource(updated, type));
        assertEquals(0, countOccurrences(updated, "public java.util.List<T> values()"));
        assertEquals(0, countOccurrences(updated, "public T item()"));
        assertEquals(1, countOccurrences(updated, "\n    public int size()"));
        assertEquals(1, countOccurrences(updated, "\n    public boolean empty()"));
        assertTrue(!ClassMethodUpdater.hasMissingMethods(updated, type));
    }

    @Test
    public void updatesExistingSealedClassWithMultilinePermitsAndNestedPermittedTypes() throws Exception {
        String source = "package com.example;\n\n" +
                "public abstract sealed class Shape\n" +
                "        permits Shape.Circle,\n" +
                "                Shape.Open {\n" +
                "    static final class Circle extends Shape {\n" +
                "    }\n" +
                "\n" +
                "    static non-sealed class Open extends Shape {\n" +
                "    }\n" +
                "}\n";
        DescribedType type = DescribedType.of(
                "com.example.Shape",
                JavaTypeKind.SEALED_CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList("com.example.Shape.Circle", "com.example.Shape.Open"),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(MethodDescriptor.of("area", "int"))
        );

        String updated = ClassMethodUpdater.updateSource(source, type);

        assertEquals(updated, ClassMethodUpdater.updateSource(updated, type));
        assertEquals(1, countOccurrences(updated, "public int area()"));
        assertTrue(updated.indexOf("public int area()") > updated.indexOf("static non-sealed class Open"));
        assertTrue(!ClassMethodUpdater.hasMissingMethods(updated, type));
        assertCompilesAsJava17Source(updated, "com/example/Shape.java");
    }

    private static DescribedType recordType(String qualifiedName, List<MethodDescriptor> methods) {
        return DescribedType.of(
                qualifiedName,
                JavaTypeKind.RECORD,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                methods
        );
    }

    private void assertCompilesAsJava17Source(String source, String sourceRelativePath) throws Exception {
        if (!supportsJavaSpecificationVersion(17)) {
            return;
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("A JDK compiler is required to verify Java 17 generated source", compiler);

        File sourceRoot = temporaryFolder.newFolder("java17-source");
        File sourceFile = new File(sourceRoot, sourceRelativePath);
        File parent = sourceFile.getParentFile();
        assertTrue(parent.mkdirs() || parent.isDirectory());
        Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));

        File classOutput = temporaryFolder.newFolder("java17-classes");
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        try {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile));
            List<String> options = Arrays.asList("--release", "17", "-d", classOutput.getAbsolutePath());
            Boolean success = compiler.getTask(null, fileManager, null, options, null, units).call();
            assertTrue("Updated source did not compile as Java 17:\n" + source, success.booleanValue());
        } finally {
            fileManager.close();
        }
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
