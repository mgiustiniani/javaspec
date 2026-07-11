package io.github.jvmspec.generation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
}
