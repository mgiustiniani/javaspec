package org.javaspec.generation;

import org.javaspec.model.DescribedType;

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

    /**
     * Writes or updates a class file with constructor changes.
     * If the file doesn't exist, creates it with the plan's source content.
     * If the file exists, applies constructor updates from the spec.
     *
     * @param plan           the generation plan
     * @param policy         the constructor policy for handling old constructors
     * @return the target file
     * @throws IOException if writing fails
     */
    public static File writeOrUpdate(TypeGenerationPlan plan, ConstructorPolicy policy) throws IOException {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        File targetFile = plan.targetFile();
        File parent = targetFile.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }

        if (!targetFile.exists()) {
            // New file: write the generated source
            Path targetPath = targetFile.toPath();
            OutputStream output = Files.newOutputStream(targetPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                output.write(plan.sourceContent().getBytes(StandardCharsets.UTF_8));
            } finally {
                output.close();
            }
            return targetFile;
        }

        // Existing file: update constructors and missing methods.
        DescribedType describedType = plan.describedType();
        if (describedType.hasConstructors()) {
            ClassConstructorUpdater.updateFile(targetFile, describedType, policy);
        }
        if (describedType.hasMethods()) {
            ClassMethodUpdater.updateFile(targetFile, describedType);
        }
        return targetFile;
    }
}
