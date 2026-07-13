package io.github.jvmspec.generation;

import io.github.jvmspec.model.DescribedClass;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JavaMethodSourceComponentsTest {
    @Test
    public void sourceEditorPreservesCrLfAndClosingBraceIndentation() {
        String source = "class Outer {\r\n"
                + "    class Inner {\r\n"
                + "    }\r\n"
                + "}\r\n";
        int closingBrace = source.indexOf("    }\r\n");

        String updated = JavaSourceEditor.insertBeforeClosingBraceKeepingIndent(
                source,
                closingBrace + 4,
                "        int value() {\n            return 0;\n        }\n"
        );

        assertTrue(updated.contains("        int value() {\r\n"
                + "            return 0;\r\n"
                + "        }\r\n"
                + "    }"));
        assertTrue(updated.endsWith("}\r\n"));
    }

    @Test
    public void inventoryIgnoresNestedMembersAndRecognizesRecordAccessors() {
        MethodDescriptor name = MethodDescriptor.of("name", "String");
        DescribedType classType = type(JavaTypeKind.CLASS, Arrays.asList(name));
        String nestedOnly = "package com.example;\n"
                + "public class Subject {\n"
                + "  static class Nested { public String name() { return \"nested\"; } }\n"
                + "}\n";

        List<MethodDescriptor> classMissing = JavaMethodInventory.missingMethods(nestedOnly, classType);
        assertEquals(Arrays.asList(name), classMissing);

        DescribedType recordType = type(JavaTypeKind.RECORD, Arrays.asList(name));
        String recordSource = "package com.example; public record Subject(String name) {}";
        assertTrue(JavaMethodInventory.missingMethods(recordSource, recordType).isEmpty());
    }

    @Test
    public void sealedSynchronizerUpdatesRootAndNestedPermittedTypeIdempotently() {
        MethodDescriptor area = MethodDescriptor.of("area", "double");
        DescribedType type = DescribedType.of(
                DescribedClass.of("com.example.Shape"),
                JavaTypeKind.SEALED_INTERFACE,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList("com.example.Shape.Circle"),
                Collections.emptyList(),
                Arrays.asList(area),
                Collections.<DescribedType.EnumConstantInfo>emptyList()
        );
        String source = "package com.example;\n"
                + "public sealed interface Shape permits Shape.Circle {\n"
                + "    final class Circle implements Shape { }\n"
                + "}\n";

        String updated = SealedInterfaceMethodSynchronizer.updateSource(source, type);

        assertTrue(updated.contains("double area();"));
        assertTrue(updated.contains("public double area()"));
        assertEquals(updated, SealedInterfaceMethodSynchronizer.updateSource(updated, type));
    }

    private static DescribedType type(JavaTypeKind kind, List<MethodDescriptor> methods) {
        return DescribedType.of(
                DescribedClass.of("com.example.Subject"),
                kind,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.emptyList(),
                methods,
                Collections.<DescribedType.EnumConstantInfo>emptyList()
        );
    }
}
