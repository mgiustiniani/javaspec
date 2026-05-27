package org.javaspec.model;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DescribedClassTest {
    @Test
    public void acceptsSimpleNames() {
        DescribedClass describedClass = DescribedClass.of("Widget");

        assertEquals("Widget", describedClass.qualifiedName());
        assertEquals("", describedClass.packageName());
        assertEquals("Widget", describedClass.simpleName());
        assertFalse(describedClass.hasPackage());
        assertEquals("Widget.java", describedClass.sourceRelativePath());
        assertEquals("Widget", describedClass.toString());
    }

    @Test
    public void acceptsQualifiedNames() {
        DescribedClass describedClass = DescribedClass.of("com.example.Widget");

        assertEquals("com.example.Widget", describedClass.qualifiedName());
        assertEquals("com.example", describedClass.packageName());
        assertEquals("Widget", describedClass.simpleName());
        assertTrue(describedClass.hasPackage());
        assertEquals("com" + File.separator + "example" + File.separator + "Widget.java", describedClass.sourceRelativePath());
    }

    @Test
    public void aliasesValidateAndCreateTheSameDescribedClass() {
        DescribedClass expected = DescribedClass.of("org.example.Sample");

        assertEquals(expected, DescribedClass.from("org.example.Sample"));
        assertEquals(expected, DescribedClass.fromName("org.example.Sample"));
        assertEquals(expected, DescribedClass.named("org.example.Sample"));
        assertEquals(expected.hashCode(), DescribedClass.named("org.example.Sample").hashCode());
    }

    @Test
    public void rejectsNullNames() {
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            public void run() {
                DescribedClass.of(null);
            }
        });
    }

    @Test
    public void rejectsBlankAndEmptyNames() {
        assertInvalid("");
        assertInvalid(" ");
        assertInvalid("\t");
        assertInvalid(" com.example.Widget");
        assertInvalid("com.example.Widget ");
    }

    @Test
    public void rejectsEmptyNameSegments() {
        assertInvalid(".Widget");
        assertInvalid("com..Widget");
        assertInvalid("com.example.");
    }

    @Test
    public void rejectsInvalidJavaIdentifiers() {
        assertInvalid("1Widget");
        assertInvalid("com.9example.Widget");
        assertInvalid("com.example.Bad-Name");
        assertInvalid("com.example.Bad Name");
    }

    @Test
    public void rejectsJavaReservedWordsInAnySegment() {
        assertInvalid("class");
        assertInvalid("com.class.Widget");
        assertInvalid("com.example.true");
        assertInvalid("com.example.null");
    }

    private static void assertInvalid(final String className) {
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                DescribedClass.of(className);
            }
        });
    }
}
