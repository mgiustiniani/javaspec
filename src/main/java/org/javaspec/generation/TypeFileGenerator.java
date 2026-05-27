package org.javaspec.generation;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Explicit writer for class-like type generation plans.
 */
public final class TypeFileGenerator {
    private TypeFileGenerator() {
    }

    public static File write(TypeGenerationPlan plan) throws IOException {
        Objects.requireNonNull(plan, "plan must not be null");

        File targetFile = plan.targetFile();
        if (targetFile.exists()) {
            throw new FileAlreadyExistsException(targetFile.getPath(), null, "Target source file already exists");
        }

        File parent = targetFile.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }

        Path targetPath = targetFile.toPath();
        OutputStream output = Files.newOutputStream(targetPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try {
            output.write(plan.sourceContent().getBytes(StandardCharsets.UTF_8));
        } finally {
            output.close();
        }

        return targetFile;
    }
}
