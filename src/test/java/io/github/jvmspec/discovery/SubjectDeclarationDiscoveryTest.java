package io.github.jvmspec.discovery;

import io.github.jvmspec.model.JavaTypeKind;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class SubjectDeclarationDiscoveryTest {
    @Test
    public void discoversKindRelationshipsAndQualifiedTypesInSourceOrder() {
        String source = "package spec.com.example;\n"
                + "import com.acme.BaseType;\n"
                + "import com.acme.FirstPort;\n"
                + "public class SubjectSpec {\n"
                + "  public void it_describes_the_type() {\n"
                + "    shouldBeASealedClass();\n"
                + "    shouldExtend(BaseType.class);\n"
                + "    shouldImplement(FirstPort.class, com.acme.SecondPort.class);\n"
                + "    shouldPermit(com.example.First.class, com.example.Second.class);\n"
                + "  }\n"
                + "}\n";

        SubjectDeclarationDiscovery.Declaration declaration =
                SubjectDeclarationDiscovery.discover(source, "com.example");

        assertEquals(JavaTypeKind.SEALED_CLASS, declaration.kind);
        assertEquals(Arrays.asList("com.acme.BaseType"), declaration.extendedTypes);
        assertEquals(Arrays.asList("com.acme.FirstPort", "com.acme.SecondPort"),
                declaration.implementedTypes);
        assertEquals(Arrays.asList("com.example.First", "com.example.Second"),
                declaration.permittedTypes);
    }

    @Test
    public void discoversEnumConstantValuesAndOneCanonicalConstructor() {
        String source = "package spec.com.example;\n"
                + "public class StatusSpec {\n"
                + "  public void it_describes_constants() {\n"
                + "    shouldBeAnEnum();\n"
                + "    shouldHaveConstant(\"READY\", 1, \"ready\");\n"
                + "    shouldHaveConstant(\"STOPPED\", 2, \"stopped\");\n"
                + "  }\n"
                + "}\n";

        SubjectDeclarationDiscovery.Declaration declaration =
                SubjectDeclarationDiscovery.discover(source, "com.example");

        assertEquals(JavaTypeKind.ENUM, declaration.kind);
        assertEquals(2, declaration.enumConstants.size());
        assertEquals("READY", declaration.enumConstants.get(0).name());
        assertEquals(Arrays.asList("int", "String"),
                declaration.enumConstants.get(0).parameterTypes());
        assertEquals(Arrays.asList("1", "\"ready\""),
                declaration.enumConstants.get(0).parameterValues());
        assertEquals(1, declaration.enumConstructors.size());
        assertEquals(Arrays.asList("int", "String"),
                declaration.enumConstructors.get(0).parameterTypes());
    }
}
