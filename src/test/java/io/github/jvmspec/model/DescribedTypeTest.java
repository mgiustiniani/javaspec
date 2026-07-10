package io.github.jvmspec.model;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DescribedTypeTest {
    @Test
    public void defaultsToClassKind() {
        DescribedType describedType = DescribedType.of("com.example.Widget");

        assertEquals(JavaTypeKind.CLASS, describedType.kind());
        assertEquals("com.example.Widget", describedType.qualifiedName());
        assertEquals("com.example", describedType.packageName());
        assertEquals("Widget", describedType.simpleName());
        assertTrue(describedType.hasPackage());
        assertTrue(describedType.isClass());
        assertFalse(describedType.isInterface());
        assertEquals("com" + File.separator + "example" + File.separator + "Widget.java", describedType.sourceRelativePath());
    }

    @Test
    public void acceptsClassLikeKinds() {
        assertEquals(JavaTypeKind.INTERFACE, DescribedType.of("com.example.Port", JavaTypeKind.INTERFACE).kind());
        assertEquals(JavaTypeKind.ENUM, DescribedType.of("com.example.Status", JavaTypeKind.ENUM).kind());
        assertEquals(JavaTypeKind.ANNOTATION, DescribedType.of("com.example.Experimental", JavaTypeKind.ANNOTATION).kind());
        assertEquals(JavaTypeKind.RECORD, DescribedType.of("com.example.User", JavaTypeKind.RECORD).kind());
        assertEquals(JavaTypeKind.SEALED_CLASS, DescribedType.of("com.example.Shape", JavaTypeKind.SEALED_CLASS).kind());
        assertEquals(JavaTypeKind.SEALED_INTERFACE, DescribedType.of("com.example.Port", JavaTypeKind.SEALED_INTERFACE).kind());
    }

    @Test
    public void storesPermittedTypeNamesForSealedTypes() {
        DescribedType describedType = DescribedType.of(
                "com.example.Shape",
                JavaTypeKind.SEALED_CLASS,
                Arrays.asList("com.example.Circle", "com.example.Rectangle")
        );

        assertEquals(Arrays.asList("com.example.Circle", "com.example.Rectangle"), describedType.permittedTypeNames());
        assertTrue(describedType.hasPermittedTypes());
    }

    @Test
    public void equalityIncludesKind() {
        DescribedType classType = DescribedType.of("com.example.PaymentGateway", JavaTypeKind.CLASS);
        DescribedType interfaceType = DescribedType.of("com.example.PaymentGateway", JavaTypeKind.INTERFACE);

        assertFalse(classType.equals(interfaceType));
        assertEquals(classType, DescribedType.classNamed("com.example.PaymentGateway"));
        assertFalse(DescribedType.of("com.example.Shape", JavaTypeKind.SEALED_CLASS)
                .equals(DescribedType.of("com.example.Shape", JavaTypeKind.SEALED_CLASS, Arrays.asList("com.example.Circle"))));
    }

    @Test
    public void deduplicatesEquivalentMethodSignaturesUsingNormalizedParameterTypes() {
        DescribedType describedType = DescribedType.of(
                "com.example.CanonicalText",
                JavaTypeKind.CLASS,
                Arrays.<String>asList(),
                Arrays.<String>asList(),
                Arrays.<String>asList(),
                Arrays.<ConstructorDescriptor>asList(),
                Arrays.asList(
                        MethodDescriptor.of("isCanonicalText", "Object", Arrays.asList("java.lang.String"), Arrays.asList("arg0")),
                        MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("String"), Arrays.asList("arg0"))
                )
        );

        assertEquals(Arrays.asList(
                MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("String"), Arrays.asList("arg0"))
        ), describedType.methods());
    }

    @Test
    public void preservesObjectAndConcreteOverloadSignatures() {
        DescribedType describedType = DescribedType.of(
                "com.example.CanonicalText",
                JavaTypeKind.CLASS,
                Arrays.<String>asList(),
                Arrays.<String>asList(),
                Arrays.<String>asList(),
                Arrays.<ConstructorDescriptor>asList(),
                Arrays.asList(
                        MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("Object"), Arrays.asList("arg0")),
                        MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("String"), Arrays.asList("arg0"))
                )
        );

        assertEquals(Arrays.asList(
                MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("Object"), Arrays.asList("arg0")),
                MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("String"), Arrays.asList("arg0"))
        ), describedType.methods());
    }

    @Test
    public void rejectsNullKind() {
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            public void run() {
                DescribedType.of("com.example.Widget", null);
            }
        });
    }
}
