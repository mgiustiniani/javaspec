package io.github.jvmspec.generation;

import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
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

        AtomicFileWriter.writeUtf8(targetFile, plan.sourceContent());

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
            AtomicFileWriter.writeUtf8(targetFile, plan.sourceContent());
            return targetFile;
        }

        // Existing file: update constructors and missing methods.
        DescribedType describedType = plan.describedType();
        if (JavaTypeKind.RECORD.equals(describedType.kind())) {
            // For records, insert missing accessor stubs before constructor-driven header evolution.
            // This keeps constructor-driven component slices from becoming BROKEN while still
            // preserving assertion-level RED behavior for newly described accessors.
            if (describedType.hasMethods()) {
                ClassMethodUpdater.updateFile(targetFile, describedType);
            }
            if (describedType.hasConstructors()) {
                ClassConstructorUpdater.updateFile(targetFile, describedType, policy);
            }
            return targetFile;
        }
        if (describedType.hasConstructors()) {
            ClassConstructorUpdater.updateFile(targetFile, describedType, policy);
        }
        if (describedType.hasMethods()) {
            ClassMethodUpdater.updateFile(targetFile, describedType);
        }
        return targetFile;
    }
}
