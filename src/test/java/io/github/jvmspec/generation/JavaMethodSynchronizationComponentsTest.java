package io.github.jvmspec.generation;

import io.github.jvmspec.internal.type.JavaIdentifiers;
import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedClass;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JavaMethodSynchronizationComponentsTest {
    @Test
    public void rendererProducesDeterministicFactoryAndInterfaceSkeletons() {
        ConstructorDescriptor constructor = ConstructorDescriptor.of(
                Arrays.asList("String"), Arrays.asList("name"), "");
        MethodDescriptor factory = MethodDescriptor.staticMethod(
                "create", "com.example.Subject", Arrays.asList("String"), Arrays.asList("name"));
        DescribedType owner = type(JavaTypeKind.CLASS, Arrays.asList(constructor), Arrays.asList(factory));

        String renderedFactory = JavaMethodRenderer.renderMissingMethods(
                Arrays.asList(factory), owner, "    ");
        String renderedInterface = JavaMethodRenderer.renderMissingMethods(
                Arrays.asList(MethodDescriptor.of(
                        "find", "String", Arrays.asList("int"), Arrays.asList("index"))),
                type(JavaTypeKind.INTERFACE, Collections.<ConstructorDescriptor>emptyList(),
                        Collections.<MethodDescriptor>emptyList()),
                "    "
        );

        assertEquals("    public static Subject create(String name) {\n"
                + "        // javaspec:stub\n"
                + "        return new Subject(name);\n"
                + "    }\n", renderedFactory);
        assertEquals("    String find(int index);\n", renderedInterface);
    }

    @Test
    public void eligibilityFiltersImplicitEnumStaticInterfaceAndInvalidAnnotationMethods() {
        List<MethodDescriptor> enumMethods = Arrays.asList(
                MethodDescriptor.of("values", "Subject[]"),
                MethodDescriptor.of("valueOf", "Subject", Arrays.asList("String"), Arrays.asList("name")),
                MethodDescriptor.of("name", "String"),
                MethodDescriptor.of("custom", "String")
        );
        List<MethodDescriptor> eligibleEnum = JavaMethodEligibility.eligibleMethods(
                type(JavaTypeKind.ENUM, Collections.<ConstructorDescriptor>emptyList(), enumMethods));
        assertEquals(1, eligibleEnum.size());
        assertEquals("custom", eligibleEnum.get(0).methodName());

        MethodDescriptor instance = MethodDescriptor.of("run", "void");
        MethodDescriptor staticMethod = MethodDescriptor.staticMethod(
                "create", "com.example.Subject", Collections.<String>emptyList(), Collections.<String>emptyList());
        List<MethodDescriptor> eligibleInterface = JavaMethodEligibility.interfaceMethods(
                type(JavaTypeKind.INTERFACE, Collections.<ConstructorDescriptor>emptyList(),
                        Arrays.asList(instance, staticMethod)));
        assertEquals(Arrays.asList(instance), eligibleInterface);

        List<MethodDescriptor> annotationMethods = Arrays.asList(
                MethodDescriptor.of("count", "int"),
                MethodDescriptor.of("names", "String[]"),
                MethodDescriptor.of("invalidObject", "Object"),
                MethodDescriptor.of("invalidNestedArray", "String[][]"),
                MethodDescriptor.of("invalidParameters", "String",
                        Arrays.asList("int"), Arrays.asList("index"))
        );
        List<MethodDescriptor> eligibleAnnotation = JavaMethodEligibility.annotationElementMethods(
                type(JavaTypeKind.ANNOTATION, Collections.<ConstructorDescriptor>emptyList(),
                        annotationMethods));
        assertEquals(Arrays.asList(annotationMethods.get(0), annotationMethods.get(1)), eligibleAnnotation);
    }

    @Test
    public void sharedIdentifierChecksSupportUnicodeAndRejectInvalidSegments() {
        assertTrue(JavaIdentifiers.isIdentifier("valore"));
        assertTrue(JavaIdentifiers.isIdentifier("πValue"));
        assertFalse(JavaIdentifiers.isIdentifier("9value"));
        assertFalse(JavaIdentifiers.isIdentifier("with-dash"));
        assertFalse(JavaIdentifiers.isIdentifier(""));
    }

    private static DescribedType type(
            JavaTypeKind kind,
            List<ConstructorDescriptor> constructors,
            List<MethodDescriptor> methods
    ) {
        return DescribedType.of(
                DescribedClass.of("com.example.Subject"),
                kind,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                constructors,
                methods,
                new ArrayList<DescribedType.EnumConstantInfo>()
        );
    }
}
