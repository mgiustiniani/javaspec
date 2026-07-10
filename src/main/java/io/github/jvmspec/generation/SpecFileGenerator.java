package io.github.jvmspec.generation;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Explicit writer for specification generation plans.
 */
public final class SpecFileGenerator {
    private SpecFileGenerator() {
    }

    public static File write(SpecGenerationPlan plan) throws IOException {
        Objects.requireNonNull(plan, "plan must not be null");

        File targetFile = plan.targetFile();
        if (targetFile.exists()) {
            throw new FileAlreadyExistsException(targetFile.getPath(), null, "Target spec file already exists");
        }

        File parent = targetFile.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }

        AtomicFileWriter.writeUtf8(targetFile, plan.sourceContent());

        return targetFile;
    }
}
