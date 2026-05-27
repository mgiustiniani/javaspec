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

public class SpecFileGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writeCreatesParentDirectoriesAndFileContent() throws Exception {
        File specRoot = temporaryFolder.newFolder("spec-root");
        SpecGenerationPlan plan = SpecSkeletonGenerator.plan(DescribedClass.of("com.example.Generated"), specRoot);

        File written = SpecFileGenerator.write(plan);

        assertEquals(plan.targetFile(), written);
        assertTrue(new File(specRoot, "spec" + File.separator + "com" + File.separator + "example").isDirectory());
        assertTrue(written.isFile());
        assertEquals(plan.sourceContent(), readFile(written));
    }

    @Test
    public void writeRefusesToOverwriteExistingFiles() throws Exception {
        File specRoot = temporaryFolder.newFolder("overwrite-spec-root");
        final SpecGenerationPlan plan = SpecSkeletonGenerator.plan(DescribedClass.of("com.example.Generated"), specRoot);
        assertTrue(plan.targetFile().getParentFile().mkdirs());
        Files.write(plan.targetFile().toPath(), "existing\n".getBytes(StandardCharsets.UTF_8));

        assertThrows(FileAlreadyExistsException.class, new ThrowingRunnable() {
            public void run() throws Throwable {
                SpecFileGenerator.write(plan);
            }
        });
        assertEquals("existing\n", readFile(plan.targetFile()));
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
