package org.javaspec.generation;

import org.javaspec.model.DescribedClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ClassFileGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writeCreatesParentDirectoriesAndFileContent() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("source-root");
        ClassGenerationPlan plan = ClassSkeletonGenerator.plan(DescribedClass.of("com.example.Generated"), sourceRoot);

        File written = ClassFileGenerator.write(plan);

        assertEquals(plan.targetFile(), written);
        assertTrue(new File(sourceRoot, "com" + File.separator + "example").isDirectory());
        assertTrue(written.isFile());
        assertEquals(plan.sourceContent(), readFile(written));
    }

    @Test
    public void writeRefusesToOverwriteExistingFiles() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("overwrite-source-root");
        final ClassGenerationPlan plan = ClassSkeletonGenerator.plan(DescribedClass.of("com.example.Generated"), sourceRoot);
        assertTrue(plan.targetFile().getParentFile().mkdirs());
        Files.write(plan.targetFile().toPath(), "existing\n".getBytes(StandardCharsets.UTF_8));

        assertThrows(FileAlreadyExistsException.class, new ThrowingRunnable() {
            public void run() throws Throwable {
                ClassFileGenerator.write(plan);
            }
        });
        assertEquals("existing\n", readFile(plan.targetFile()));
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
