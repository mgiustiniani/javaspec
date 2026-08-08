package io.github.jvmspec.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JavaspecNativePrepareMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generatesDeterministicBuildLinkedLauncherAndReflectionMetadata() throws Exception {
        File basedir = temporaryFolder.newFolder("native-consumer");
        File specRoot = new File(basedir, "src/test/java");
        File generatedRoot = new File(basedir, "target/generated-test-sources/javaspec-native");
        File configRoot = new File(
                basedir,
                "target/test-classes/META-INF/native-image/io.github.jvmspec/javaspec-native"
        );
        write(specRoot, "spec/com/example/CalculatorSpec.java",
                "package spec.com.example;\n\n" +
                "public class CalculatorSpec extends CalculatorSpecSupport {\n" +
                "    public void it_adds_two_numbers() {\n" +
                "        add(2, 3).shouldReturn(5);\n" +
                "    }\n" +
                "}\n");
        MavenProject project = new MavenProject();
        JavaspecNativePrepareMojo mojo = mojo(basedir, specRoot, generatedRoot, configRoot, project);

        mojo.execute();

        File launcher = new File(
                generatedRoot,
                "io/github/jvmspec/generated/JavaspecNativeMain.java"
        );
        File reflection = new File(configRoot, "reflect-config.json");
        assertTrue(launcher.isFile());
        assertTrue(reflection.isFile());
        assertTrue(project.getTestCompileSourceRoots().contains(generatedRoot.getAbsolutePath()));

        String source = read(launcher);
        assertTrue(source, source.contains("spec.com.example.CalculatorSpec.class"));
        assertTrue(source, source.contains("com.example.Calculator.class"));
        assertTrue(source, source.contains("NativeImageLauncher.run"));
        assertTrue(source, source.contains("SpecExample.of(\"it_adds_two_numbers\", 0, 4)"));
        assertTrue(source, source.contains("src/test/java/spec/com/example/CalculatorSpec.java"));

        String metadata = read(reflection);
        assertTrue(metadata, metadata.contains("\"name\": \"spec.com.example.CalculatorSpec\""));
        assertTrue(metadata, metadata.contains("\"name\": \"com.example.Calculator\""));
        assertTrue(metadata, metadata.contains("\"allDeclaredConstructors\": true"));
        assertTrue(metadata, metadata.contains("\"allPublicMethods\": true"));

        String launcherHash = sha256(launcher);
        String metadataHash = sha256(reflection);
        mojo.execute();
        assertEquals(launcherHash, sha256(launcher));
        assertEquals(metadataHash, sha256(reflection));
    }

    @Test(expected = MojoFailureException.class)
    public void rejectsDefaultPackageTypesThatCannotBeLinkedFromTheGeneratedPackage() throws Exception {
        File basedir = temporaryFolder.newFolder("default-package-native-consumer");
        File specRoot = new File(basedir, "src/test/java");
        File generatedRoot = new File(basedir, "target/generated-test-sources/javaspec-native");
        File configRoot = new File(basedir, "target/test-classes/META-INF/native-image/test");
        write(specRoot, "CalculatorSpec.java",
                "public class CalculatorSpec {\n" +
                "    public void it_adds_two_numbers() { }\n" +
                "}\n");

        mojo(basedir, specRoot, generatedRoot, configRoot, new MavenProject()).execute();
    }

    @Test(expected = MojoFailureException.class)
    public void rejectsNativePreparationWithoutExecutableExamples() throws Exception {
        File basedir = temporaryFolder.newFolder("empty-native-consumer");
        File specRoot = new File(basedir, "src/test/java");
        File generatedRoot = new File(basedir, "target/generated-test-sources/javaspec-native");
        File configRoot = new File(basedir, "target/test-classes/META-INF/native-image/test");
        assertTrue(specRoot.mkdirs());

        mojo(basedir, specRoot, generatedRoot, configRoot, new MavenProject()).execute();
    }

    private JavaspecNativePrepareMojo mojo(
            File basedir,
            File specRoot,
            File generatedRoot,
            File configRoot,
            MavenProject project
    ) throws Exception {
        JavaspecNativePrepareMojo mojo = new JavaspecNativePrepareMojo();
        set(mojo, "basedir", basedir);
        set(mojo, "specDir", specRoot);
        set(mojo, "generatedNativeSourcesDir", generatedRoot);
        set(mojo, "nativeImageConfigDir", configRoot);
        set(mojo, "project", project);
        return mojo;
    }

    private static File write(File root, String relativePath, String content) throws Exception {
        File file = new File(root, relativePath);
        assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = Files.readAllBytes(file.toPath());
        byte[] hash = digest.digest(bytes);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            result.append(String.format("%02x", hash[i] & 0xff));
        }
        return result.toString();
    }
}
