package io.github.jvmspec.api;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SubjectLifecycleRecordCompatibilityTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void explicitRecordConstructorPrefixIsPaddedWithTrailingDefaults() throws Exception {
        Class<?> recordType = compileAndLoad("com.example.LegacyEnvelope",
                "package com.example;\n" +
                "public record LegacyEnvelope(String id, int version, String status, boolean active) { }\n");

        SubjectLifecycle<Object> lifecycle = lifecycleFor(recordType);
        lifecycle.beConstructedWith("abc", Integer.valueOf(7));

        Object subject = lifecycle.subject();

        assertEquals("abc", invoke(subject, "id"));
        assertEquals(Integer.valueOf(7), invoke(subject, "version"));
        assertEquals(null, invoke(subject, "status"));
        assertEquals(Boolean.FALSE, invoke(subject, "active"));
    }

    @Test
    public void fullArityRecordConstructionIsUnchanged() throws Exception {
        Class<?> recordType = compileAndLoad("com.example.FullEnvelope",
                "package com.example;\n" +
                "public record FullEnvelope(String id, int version, String status, boolean active) { }\n");

        SubjectLifecycle<Object> lifecycle = lifecycleFor(recordType);
        lifecycle.beConstructedWith("abc", Integer.valueOf(7), "ready", Boolean.TRUE);

        Object subject = lifecycle.subject();

        assertEquals("ready", invoke(subject, "status"));
        assertEquals(Boolean.TRUE, invoke(subject, "active"));
    }

    @Test
    public void exactAuxiliaryRecordConstructorWinsOverPrefixPadding() throws Exception {
        Class<?> recordType = compileAndLoad("com.example.AuxiliaryEnvelope",
                "package com.example;\n" +
                "public record AuxiliaryEnvelope(String id, int version, String status) {\n" +
                "  public AuxiliaryEnvelope(String id, int version) { this(id, version, \"auxiliary\"); }\n" +
                "}\n");

        SubjectLifecycle<Object> lifecycle = lifecycleFor(recordType);
        lifecycle.beConstructedWith("abc", Integer.valueOf(7));

        Object subject = lifecycle.subject();

        assertEquals("auxiliary", invoke(subject, "status"));
    }

    @Test
    public void compactRecordConstructorValidationStillSurfaces() throws Exception {
        Class<?> recordType = compileAndLoad("com.example.ValidatedEnvelope",
                "package com.example;\n" +
                "public record ValidatedEnvelope(String id, String status) {\n" +
                "  public ValidatedEnvelope {\n" +
                "    if (status == null) { throw new IllegalArgumentException(\"status required\"); }\n" +
                "  }\n" +
                "}\n");

        SubjectLifecycle<Object> lifecycle = lifecycleFor(recordType);
        lifecycle.beConstructedWith("abc");

        IllegalArgumentException error = (IllegalArgumentException) expect(IllegalArgumentException.class, new ThrowingCall() {
            @Override
            public void run() throws Throwable {
                lifecycle.subject();
            }
        });
        assertTrue(error.getMessage().contains("status required"));
    }

    @Test
    public void recordPrefixPaddingRequiresMatchingPrefixTypes() throws Exception {
        Class<?> recordType = compileAndLoad("com.example.TypedEnvelope",
                "package com.example;\n" +
                "public record TypedEnvelope(String id, int version, String status) { }\n");

        SubjectLifecycle<Object> lifecycle = lifecycleFor(recordType);
        lifecycle.beConstructedWith(Integer.valueOf(7));

        IllegalStateException error = (IllegalStateException) expect(IllegalStateException.class, new ThrowingCall() {
            @Override
            public void run() throws Throwable {
                lifecycle.subject();
            }
        });
        assertTrue(error.getMessage().contains("No matching constructor found"));
    }

    @Test
    public void nonRecordConstructorArityStillRequiresExactMatch() {
        SubjectLifecycle<FourArgumentClass> lifecycle = new SubjectLifecycle<FourArgumentClass>(FourArgumentClass.class);
        lifecycle.beConstructedWith("abc", Integer.valueOf(7));

        IllegalStateException error = (IllegalStateException) expect(IllegalStateException.class, new ThrowingCall() {
            @Override
            public void run() throws Throwable {
                lifecycle.subject();
            }
        });
        assertTrue(error.getMessage().contains("No matching constructor found"));
        assertFalse(lifecycle.isInstantiated());
    }

    @SuppressWarnings("unchecked")
    private static SubjectLifecycle<Object> lifecycleFor(Class<?> subjectType) {
        return new SubjectLifecycle<Object>((Class<? extends Object>) subjectType);
    }

    private Class<?> compileAndLoad(String qualifiedName, String source) throws Exception {
        assumeRecordCapableCompiler();
        File sourceRoot = temporaryFolder.newFolder("record-src");
        File classesRoot = temporaryFolder.newFolder("record-classes");
        File sourceFile = new File(sourceRoot, qualifiedName.replace('.', File.separatorChar) + ".java");
        File parent = sourceFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create " + parent);
        }
        Writer writer = new OutputStreamWriter(new FileOutputStream(sourceFile), StandardCharsets.UTF_8);
        try {
            writer.write(source);
        } finally {
            writer.close();
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int exit = compiler.run(null, null, null,
                "--release", "17",
                "-d", classesRoot.getAbsolutePath(),
                sourceFile.getAbsolutePath());
        assertEquals("record source should compile", 0, exit);
        URLClassLoader loader = new URLClassLoader(new URL[] {classesRoot.toURI().toURL()},
                SubjectLifecycleRecordCompatibilityTest.class.getClassLoader());
        return Class.forName(qualifiedName, true, loader);
    }

    private static void assumeRecordCapableCompiler() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assume.assumeTrue("A JDK compiler is required for record compatibility tests", compiler != null);
        Assume.assumeTrue("Record compatibility tests require a Java 17+ test runtime", javaSpecificationAtLeast(17));
    }

    private static boolean javaSpecificationAtLeast(int major) {
        String version = System.getProperty("java.specification.version", "0");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dot = version.indexOf('.');
        if (dot >= 0) {
            version = version.substring(0, dot);
        }
        try {
            return Integer.parseInt(version) >= major;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static Throwable expect(Class<? extends Throwable> expectedType, ThrowingCall call) {
        try {
            call.run();
        } catch (Throwable thrown) {
            if (expectedType.isAssignableFrom(thrown.getClass())) {
                return thrown;
            }
            AssertionError error = new AssertionError("Expected " + expectedType.getName()
                    + " but got " + thrown.getClass().getName());
            error.initCause(thrown);
            throw error;
        }
        fail("Expected " + expectedType.getName());
        return null;
    }

    private interface ThrowingCall {
        void run() throws Throwable;
    }

    public static final class FourArgumentClass {
        public FourArgumentClass(String id, int version, String status, boolean active) {
        }
    }
}
