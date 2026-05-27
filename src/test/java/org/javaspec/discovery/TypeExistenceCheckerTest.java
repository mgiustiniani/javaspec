package org.javaspec.discovery;

import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TypeExistenceCheckerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void detectsClasspathInterfaceKindWithoutInitializing() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("interface-source-root");

        TypeCheckResult result = TypeExistenceChecker.check(
                DescribedType.of(SampleInterface.class.getName(), JavaTypeKind.INTERFACE),
                sourceRoot,
                getClass().getClassLoader()
        );

        assertTrue(result.isPresent());
        assertTrue(result.classpathPresent());
        assertEquals(JavaTypeKind.INTERFACE, result.classpathKind());
        assertTrue(result.classpathKindMatches());
    }

    @Test
    public void detectsClasspathEnumKind() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("enum-source-root");

        TypeCheckResult result = TypeExistenceChecker.check(
                DescribedType.of(SampleEnum.class.getName(), JavaTypeKind.ENUM),
                sourceRoot,
                getClass().getClassLoader()
        );

        assertTrue(result.isPresent());
        assertEquals(JavaTypeKind.ENUM, result.classpathKind());
        assertTrue(result.classpathKindMatches());
    }

    @Test
    public void detectsClasspathAnnotationKind() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("annotation-source-root");

        TypeCheckResult result = TypeExistenceChecker.check(
                DescribedType.of(SampleAnnotation.class.getName(), JavaTypeKind.ANNOTATION),
                sourceRoot,
                getClass().getClassLoader()
        );

        assertTrue(result.isPresent());
        assertEquals(JavaTypeKind.ANNOTATION, result.classpathKind());
        assertTrue(result.classpathKindMatches());
    }

    @Test
    public void reportsClasspathKindMismatch() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("mismatch-source-root");

        TypeCheckResult result = TypeExistenceChecker.check(
                DescribedType.of(String.class.getName(), JavaTypeKind.INTERFACE),
                sourceRoot,
                getClass().getClassLoader()
        );

        assertTrue(result.isPresent());
        assertEquals(JavaTypeKind.CLASS, result.classpathKind());
        assertFalse(result.classpathKindMatches());
    }

    public interface SampleInterface {
    }

    public enum SampleEnum {
        VALUE
    }

    public @interface SampleAnnotation {
    }
}
