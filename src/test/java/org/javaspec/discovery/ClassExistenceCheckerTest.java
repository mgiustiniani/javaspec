package org.javaspec.discovery;

import org.javaspec.model.DescribedClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassExistenceCheckerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void detectsSourceFileUnderTemporarySourceRoot() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("source-root");
        File sourceFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Existing.java");
        assertTrue(sourceFile.getParentFile().mkdirs());
        Files.write(sourceFile.toPath(), "package com.example; public class Existing { }\n".getBytes(StandardCharsets.UTF_8));

        ClassCheckResult result = ClassExistenceChecker.check(
                DescribedClass.of("com.example.Existing"),
                sourceRoot,
                getClass().getClassLoader()
        );

        assertTrue(result.isPresent());
        assertTrue(result.sourceFilePresent());
        assertFalse(result.classpathPresent());
        assertEquals(sourceFile, result.sourceFile());
        assertEquals(sourceRoot, result.sourceRoot());
    }

    @Test
    public void detectsBootstrapClasspathPresentClass() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("bootstrap-source-root");

        ClassCheckResult result = ClassExistenceChecker.check(
                DescribedClass.of("java.lang.String"),
                sourceRoot,
                null
        );

        assertTrue(result.isPresent());
        assertTrue(result.classpathPresent());
        assertFalse(result.sourceFilePresent());
    }

    @Test
    public void detectsClasspathPresentClassWithoutInitializingIt() throws Exception {
        InitializationTracker.initialized = false;
        File sourceRoot = temporaryFolder.newFolder("classpath-source-root");

        ClassCheckResult result = ClassExistenceChecker.check(
                DescribedClass.of(InitializingMarker.class.getName()),
                sourceRoot,
                getClass().getClassLoader()
        );

        assertTrue(result.isPresent());
        assertTrue(result.classpathPresent());
        assertFalse("Class.forName must use initialize=false", InitializationTracker.initialized);
    }

    @Test
    public void reportsMissingWhenClassIsInNeitherSourceRootNorClasspath() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("missing-source-root");

        ClassCheckResult result = ClassExistenceChecker.check(
                DescribedClass.of("org.javaspec.missing.DoesNotExist"),
                sourceRoot,
                getClass().getClassLoader()
        );

        assertFalse(result.isPresent());
        assertFalse(result.classpathPresent());
        assertFalse(result.sourceFilePresent());
        assertEquals(new File(sourceRoot, "org" + File.separator + "javaspec" + File.separator + "missing" + File.separator + "DoesNotExist.java"), result.sourceFile());
    }

    private static final class InitializationTracker {
        private static boolean initialized;
    }

    public static final class InitializingMarker {
        static {
            InitializationTracker.initialized = true;
        }

        private InitializingMarker() {
        }
    }
}
