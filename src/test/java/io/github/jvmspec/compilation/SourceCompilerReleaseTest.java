package io.github.jvmspec.compilation;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SourceCompiler}'s Java release detection logic.
 */
public class SourceCompilerReleaseTest {

    @Test
    public void java8SpecVersionIsNotJava9OrLater() {
        // Mimic java.specification.version = "1.8" (Java 8).
        String saved = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "1.8");
            assertFalse(SourceCompiler.isJava9OrLater());
        } finally {
            restoreProperty("java.specification.version", saved);
        }
    }

    @Test
    public void java9SpecVersionIsJava9OrLater() {
        String saved = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "9");
            assertTrue(SourceCompiler.isJava9OrLater());
        } finally {
            restoreProperty("java.specification.version", saved);
        }
    }

    @Test
    public void java11SpecVersionIsJava9OrLater() {
        String saved = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "11");
            assertTrue(SourceCompiler.isJava9OrLater());
        } finally {
            restoreProperty("java.specification.version", saved);
        }
    }

    @Test
    public void java21SpecVersionIsJava9OrLater() {
        String saved = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "21");
            assertTrue(SourceCompiler.isJava9OrLater());
        } finally {
            restoreProperty("java.specification.version", saved);
        }
    }

    @Test
    public void unknownSpecVersionDefaultsToNotJava9OrLater() {
        String saved = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "bogus");
            assertFalse(SourceCompiler.isJava9OrLater());
        } finally {
            restoreProperty("java.specification.version", saved);
        }
    }

    private static void restoreProperty(String key, String saved) {
        if (saved == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, saved);
        }
    }
}
