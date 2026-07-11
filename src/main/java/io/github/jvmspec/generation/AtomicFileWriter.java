package io.github.jvmspec.generation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Writes generated source content through a same-directory temporary file and atomic rename when
 * the filesystem supports it.
 */
public final class AtomicFileWriter {
    interface MoveOperation {
        void move(Path temporaryPath, Path targetPath) throws IOException;
    }

    private static final MoveOperation DEFAULT_MOVE = new MoveOperation() {
        @Override
        public void move(Path temporaryPath, Path targetPath) throws IOException {
            try {
                Files.move(temporaryPath, targetPath,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    };

    private AtomicFileWriter() {
    }

    public static void writeUtf8(File targetFile, String content) throws IOException {
        writeUtf8(targetFile.toPath(), content);
    }

    public static void writeUtf8(Path targetPath, String content) throws IOException {
        writeUtf8(targetPath, content, DEFAULT_MOVE);
    }

    static void writeUtf8(Path targetPath, String content, MoveOperation moveOperation) throws IOException {
        Objects.requireNonNull(targetPath, "targetPath must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(moveOperation, "moveOperation must not be null");
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
            moveOperation.move(tempPath, targetPath);
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
