package io.github.jvmspec.maven;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class JavaspecGenerateMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void cleanGenerationCreatesRecordAndMatcherOnlyEnumSupportBeforeCompilation() throws Exception {
        assumeTrue(javaSpecificationVersion() >= 21);
        File basedir = temporaryFolder.newFolder("consumer");
        File sourceRoot = new File(basedir, "src/main/java");
        File specRoot = new File(basedir, "src/test/java");
        File generatedRoot = new File(basedir, "target/generated-sources/javaspec");
        File recordSource = write(sourceRoot, "com/example/CertificateProfileId.java",
                "package com.example;\n\n" +
                "public record CertificateProfileId(String value) {\n" +
                "}\n");
        File enumSource = write(sourceRoot, "com/example/SubjectPublicKeyProfile.java",
                "package com.example;\n\n" +
                "public enum SubjectPublicKeyProfile {\n" +
                "    EC_P256(\"EC_P256\"), RSA_3072(\"RSA_3072\");\n" +
                "    private final String wireToken;\n" +
                "    SubjectPublicKeyProfile(String wireToken) { this.wireToken = wireToken; }\n" +
                "    public String wireToken() { return wireToken; }\n" +
                "}\n");
        File recordSpec = write(specRoot, "spec/com/example/CertificateProfileIdSpec.java",
                "package spec.com.example;\n\n" +
                "public class CertificateProfileIdSpec extends CertificateProfileIdSpecSupport {\n" +
                "    public void it_exposes_the_complete_identifier() {\n" +
                "        String value = \"x509-profile-v1-sha256-0123456789abcdef\";\n" +
                "        beConstructedWith(value);\n" +
                "        match(subject().value()).shouldReturn(value);\n" +
                "    }\n" +
                "}\n");
        File enumSpec = write(specRoot, "spec/com/example/SubjectPublicKeyProfileSpec.java",
                "package spec.com.example;\n\n" +
                "import com.example.SubjectPublicKeyProfile;\n" +
                "import java.lang.reflect.Method;\n" +
                "import java.util.Arrays;\n" +
                "public class SubjectPublicKeyProfileSpec extends SubjectPublicKeyProfileSpecSupport {\n" +
                "    public void it_preserves_the_enriched_enum_contract() throws Exception {\n" +
                "        shouldBeAnEnum();\n" +
                "        shouldEqual(Arrays.asList(SubjectPublicKeyProfile.values()).size(), 2);\n" +
                "        shouldEqual(SubjectPublicKeyProfile.EC_P256.wireToken(), \"EC_P256\");\n" +
                "        Method method = SubjectPublicKeyProfile.class.getDeclaredMethod(\"wireToken\");\n" +
                "        shouldEqual(method.getReturnType(), String.class);\n" +
                "    }\n" +
                "}\n");

        String recordSourceHash = sha256(recordSource);
        String enumSourceHash = sha256(enumSource);
        String recordSpecHash = sha256(recordSpec);
        String enumSpecHash = sha256(enumSpec);
        assertFalse(generatedRoot.exists());
        assertCompilationFails(recordSource, enumSource, recordSpec, enumSpec);
        MavenProject project = new MavenProject();
        JavaspecGenerateMojo mojo = mojo(basedir, sourceRoot, specRoot, generatedRoot, project);

        mojo.execute();

        File recordSupport = new File(generatedRoot, "spec/com/example/CertificateProfileIdSpecSupport.java");
        File enumSupport = new File(generatedRoot, "spec/com/example/SubjectPublicKeyProfileSpecSupport.java");
        assertTrue(recordSupport.isFile());
        assertTrue(enumSupport.isFile());
        assertTrue(project.getTestCompileSourceRoots().contains(generatedRoot.getAbsolutePath()));
        String recordSupportHash = sha256(recordSupport);
        String enumSupportHash = sha256(enumSupport);
        assertNoStub(recordSource, enumSource, recordSupport, enumSupport);
        compileAndRun(recordSource, enumSource, recordSupport, enumSupport, recordSpec, enumSpec);

        mojo.execute();

        assertEquals(recordSupportHash, sha256(recordSupport));
        assertEquals(enumSupportHash, sha256(enumSupport));
        assertEquals(recordSourceHash, sha256(recordSource));
        assertEquals(enumSourceHash, sha256(enumSource));
        assertEquals(recordSpecHash, sha256(recordSpec));
        assertEquals(enumSpecHash, sha256(enumSpec));
        assertNoStub(recordSource, enumSource, recordSupport, enumSupport);
    }

    private JavaspecGenerateMojo mojo(
            File basedir,
            File sourceRoot,
            File specRoot,
            File generatedRoot,
            MavenProject project
    ) throws Exception {
        JavaspecGenerateMojo mojo = new JavaspecGenerateMojo();
        set(mojo, "basedir", basedir);
        set(mojo, "sourceDir", sourceRoot);
        set(mojo, "specDir", specRoot);
        set(mojo, "generatedSourcesDir", generatedRoot);
        set(mojo, "project", project);
        set(mojo, "profile", "java21");
        mojo.setLog(new QuietLog());
        return mojo;
    }

    private void assertCompilationFails(File... sources) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<String> arguments = compilerArguments(temporaryFolder.newFolder("red-classes"), sources);
        int exit = compiler.run(null, output, output, arguments.toArray(new String[arguments.size()]));
        assertTrue("Specs must initially fail only because generated support is absent", exit != 0);
        String diagnostic = new String(output.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(diagnostic.contains("CertificateProfileIdSpecSupport"));
        assertTrue(diagnostic.contains("SubjectPublicKeyProfileSpecSupport"));
    }

    private void compileAndRun(File... sources) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        File outputDirectory = temporaryFolder.newFolder("green-classes");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<String> arguments = compilerArguments(outputDirectory, sources);
        int exit = compiler.run(null, output, output, arguments.toArray(new String[arguments.size()]));
        assertEquals(new String(output.toByteArray(), StandardCharsets.UTF_8), 0, exit);
        URLClassLoader loader = new URLClassLoader(
                new URL[] {outputDirectory.toURI().toURL()}, getClass().getClassLoader());
        try {
            runExample(loader, "spec.com.example.CertificateProfileIdSpec", "it_exposes_the_complete_identifier");
            runExample(loader, "spec.com.example.SubjectPublicKeyProfileSpec", "it_preserves_the_enriched_enum_contract");
        } finally {
            loader.close();
        }
    }

    private List<String> compilerArguments(File outputDirectory, File... sources) {
        List<String> arguments = new ArrayList<String>();
        arguments.add("--release");
        arguments.add("21");
        arguments.add("-classpath");
        arguments.add(System.getProperty("java.class.path"));
        arguments.add("-d");
        arguments.add(outputDirectory.getAbsolutePath());
        for (int i = 0; i < sources.length; i++) {
            arguments.add(sources[i].getAbsolutePath());
        }
        return arguments;
    }

    private static void runExample(URLClassLoader loader, String className, String methodName) throws Exception {
        Class<?> type = Class.forName(className, true, loader);
        Object spec = type.newInstance();
        type.getMethod(methodName).invoke(spec);
    }

    private static void assertNoStub(File... files) throws Exception {
        for (int i = 0; i < files.length; i++) {
            String source = new String(Files.readAllBytes(files[i].toPath()), StandardCharsets.UTF_8);
            assertFalse(files[i].getPath(), source.contains("javaspec:stub"));
        }
    }

    private static File write(File root, String relativePath, String source) throws Exception {
        File file = new File(root, relativePath);
        assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String sha256(File file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()));
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            text.append(String.format("%02x", Integer.valueOf(digest[i] & 0xff)));
        }
        return text.toString();
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static int javaSpecificationVersion() {
        String value = System.getProperty("java.specification.version", "8");
        if (value.startsWith("1.")) {
            value = value.substring(2);
        }
        int dot = value.indexOf('.');
        return Integer.parseInt(dot < 0 ? value : value.substring(0, dot));
    }

    private static final class QuietLog implements Log {
        public boolean isDebugEnabled() { return false; }
        public void debug(CharSequence content) { }
        public void debug(CharSequence content, Throwable error) { }
        public void debug(Throwable error) { }
        public boolean isInfoEnabled() { return false; }
        public void info(CharSequence content) { }
        public void info(CharSequence content, Throwable error) { }
        public void info(Throwable error) { }
        public boolean isWarnEnabled() { return false; }
        public void warn(CharSequence content) { }
        public void warn(CharSequence content, Throwable error) { }
        public void warn(Throwable error) { }
        public boolean isErrorEnabled() { return false; }
        public void error(CharSequence content) { }
        public void error(CharSequence content, Throwable error) { }
        public void error(Throwable error) { }
        public boolean isFatalErrorEnabled() { return false; }
        public void fatalError(CharSequence content) { }
        public void fatalError(CharSequence content, Throwable error) { }
        public void fatalError(Throwable error) { }
    }
}
