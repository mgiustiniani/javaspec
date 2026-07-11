package io.github.jvmspec.generation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AtomicFileWriterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writesUtf8ContentAndCreatesParentDirectories() throws Exception {
        File target = new File(temporaryFolder.getRoot(), "nested/Example.java");

        AtomicFileWriter.writeUtf8(target, "class Example { String value = \"✓\"; }\n");

        assertTrue(target.isFile());
        assertEquals("class Example { String value = \"✓\"; }\n",
                new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void replacesExistingContent() throws Exception {
        File target = temporaryFolder.newFile("Existing.java");
        Files.write(target.toPath(), "old".getBytes(StandardCharsets.UTF_8));

        AtomicFileWriter.writeUtf8(target, "new");

        assertEquals("new", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void failedMovePreservesOriginalAndDeletesTemporaryFile() throws Exception {
        File target = temporaryFolder.newFile("Preserved.java");
        Files.write(target.toPath(), "original".getBytes(StandardCharsets.UTF_8));

        try {
            AtomicFileWriter.writeUtf8(target.toPath(), "replacement", new AtomicFileWriter.MoveOperation() {
                @Override
                public void move(Path temporaryPath, Path targetPath) throws IOException {
                    throw new IOException("injected move failure");
                }
            });
            fail("Expected injected move failure");
        } catch (IOException expected) {
            assertEquals("injected move failure", expected.getMessage());
        }

        assertEquals("original", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
        assertEquals("Temporary file must be deleted after failed move",
                1, temporaryFolder.getRoot().listFiles().length);
        assertTrue(target.isFile());
    }
}
