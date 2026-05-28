package org.javaspec.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConstructorDescriptorTest {
    @Test
    public void createsDescriptorWithParameters() {
        ConstructorDescriptor descriptor = ConstructorDescriptor.of(
                Arrays.asList("Writer", "boolean"),
                Arrays.asList("writer", "flag"),
                ""
        );

        assertEquals(Arrays.asList("Writer", "boolean"), descriptor.parameterTypes());
        assertEquals(Arrays.asList("writer", "flag"), descriptor.parameterNames());
        assertEquals("", descriptor.bodyContent());
        assertFalse(descriptor.hasBody());
        assertTrue(descriptor.hasParameters());
    }

    @Test
    public void createsDescriptorWithBody() {
        ConstructorDescriptor descriptor = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                "this.writer = writer;"
        );

        assertTrue(descriptor.hasBody());
        assertEquals("this.writer = writer;", descriptor.bodyContent());
    }

    @Test
    public void createsEmptyDescriptor() {
        ConstructorDescriptor descriptor = ConstructorDescriptor.empty();

        assertFalse(descriptor.hasParameters());
        assertFalse(descriptor.hasBody());
        assertEquals(0, descriptor.parameterTypes().size());
        assertEquals(0, descriptor.parameterNames().size());
    }

    @Test
    public void createsNoArgDescriptor() {
        ConstructorDescriptor descriptor = ConstructorDescriptor.noArg("this.value = 42;");

        assertFalse(descriptor.hasParameters());
        assertTrue(descriptor.hasBody());
        assertEquals("this.value = 42;", descriptor.bodyContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMismatchedParameterSizes() {
        ConstructorDescriptor.of(
                Arrays.asList("String"),
                Arrays.asList("name", "extra"),
                ""
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullParameterTypes() {
        ConstructorDescriptor.of(null, Arrays.asList("name"), "");
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullParameterNames() {
        ConstructorDescriptor.of(Arrays.asList("String"), null, "");
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullBodyContent() {
        ConstructorDescriptor.of(Arrays.asList("String"), Arrays.asList("name"), null);
    }

    @Test
    public void equalsAndHashCode() {
        ConstructorDescriptor a = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                "this.writer = writer;"
        );
        ConstructorDescriptor b = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                "this.writer = writer;"
        );
        ConstructorDescriptor c = ConstructorDescriptor.of(
                Arrays.asList("Reader"),
                Arrays.asList("reader"),
                "this.reader = reader;"
        );

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("string"));
    }

    @Test
    public void toStringContainsParameterInfo() {
        ConstructorDescriptor descriptor = ConstructorDescriptor.of(
                Arrays.asList("Writer", "boolean"),
                Arrays.asList("writer", "flag"),
                ""
        );

        String str = descriptor.toString();
        assertTrue(str.contains("Writer writer"));
        assertTrue(str.contains("boolean flag"));
        assertTrue(str.contains("hasBody=false"));
    }
}
