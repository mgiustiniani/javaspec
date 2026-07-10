package io.github.jvmspec.generation;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Explicit writer for class generation plans.
 */
public final class ClassFileGenerator {
    public static File write(ClassGenerationPlan plan) throws IOException {
        Objects.requireNonNull(plan, "plan must not be null");

        File targetFile = plan.targetFile();
        if (targetFile.exists()) {
            throw new FileAlreadyExistsException(targetFile.getPath(), null, "Target source file already exists");
        }

        File parent = targetFile.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }

        AtomicFileWriter.writeUtf8(targetFile, plan.sourceContent());

        return targetFile;
    }
}
