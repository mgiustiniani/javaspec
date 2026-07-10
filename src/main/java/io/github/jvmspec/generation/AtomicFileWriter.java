package io.github.jvmspec.generation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes generated source content through a same-directory temporary file and atomic rename when
 * the filesystem supports it.
 */
public final class AtomicFileWriter {
    private AtomicFileWriter() {
    }

    public static void writeUtf8(File targetFile, String content) throws IOException {
        writeUtf8(targetFile.toPath(), content);
    }

    public static void writeUtf8(Path targetPath, String content) throws IOException {
        File parent = targetPath.toFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent.getPath());
        }
        Path directory = parent == null ? targetPath.toAbsolutePath().getParent() : parent.toPath();
        if (directory == null) {
            directory = new File(".").toPath();
        }
        Path tempPath = Files.createTempFile(directory, targetPath.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            Files.write(tempPath, content.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException ignored) {
                    // Preserve the original failure.
                }
            }
        }
    }
}
