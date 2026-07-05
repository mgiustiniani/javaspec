package io.github.jvmspec.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DescribedTypeConstructorTest {
    @Test
    public void createsDescribedTypeWithConstructors() {
        ConstructorDescriptor constructor = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Markdown",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(constructor)
        );

        assertTrue(type.hasConstructors());
        assertEquals(1, type.constructors().size());
        assertEquals(constructor, type.constructors().get(0));
    }

    @Test
    public void createsDescribedTypeWithoutConstructors() {
        DescribedType type = DescribedType.of("com.example.Markdown", JavaTypeKind.CLASS);

        assertFalse(type.hasConstructors());
        assertEquals(0, type.constructors().size());
    }

    @Test
    public void factoryMethodsPreserveEmptyConstructors() {
        DescribedType type = DescribedType.classNamed("com.example.Test");

        assertFalse(type.hasConstructors());
    }

    @Test
    public void equalsConsidersConstructors() {
        ConstructorDescriptor c1 = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                ""
        );
        DescribedType a = DescribedType.of(
                "com.example.Markdown",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(c1)
        );
        DescribedType b = DescribedType.of(
                "com.example.Markdown",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(c1)
        );
        DescribedType c = DescribedType.of("com.example.Markdown", JavaTypeKind.CLASS);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
    }

    @Test
    public void toStringContainsConstructorInfo() {
        ConstructorDescriptor constructor = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Markdown",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(constructor)
        );

        String str = type.toString();
        assertTrue(str.contains("class"));
        assertTrue(str.contains("com.example.Markdown"));
    }
}
