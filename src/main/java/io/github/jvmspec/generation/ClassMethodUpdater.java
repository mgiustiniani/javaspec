package io.github.jvmspec.generation;

import static io.github.jvmspec.generation.JavaSourceEditor.ensureEnumConstantsTerminated;
import static io.github.jvmspec.generation.JavaSourceEditor.findPrimaryTypeClosingBrace;
import static io.github.jvmspec.generation.JavaSourceEditor.insertBeforeClosingBrace;
import static io.github.jvmspec.generation.JavaSourceEditor.memberIndentationBefore;

import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

/**
 * Inserts missing public method skeletons into existing class-like source without rewriting the body.
 *
 * <p>For sealed interfaces, missing non-static method declarations are inserted into the sealed
 * root body, and missing method implementations are inserted into nested permitted implementations
 * declared in the same source file. Permitted types declared in other source files are
 * deliberately left untouched: the updater deterministically modifies only the sealed root and its
 * in-file nested permitted implementations.</p>
 */
public final class ClassMethodUpdater {
    private ClassMethodUpdater() {
    }

    public static String updateSource(String existingSource, DescribedType describedType) {
        Objects.requireNonNull(existingSource, "existingSource must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");

        if (!describedType.hasMethods()) {
            return existingSource;
        }
        if (JavaTypeKind.SEALED_INTERFACE.equals(describedType.kind())) {
            return SealedInterfaceMethodSynchronizer.updateSource(existingSource, describedType);
        }

        List<MethodDescriptor> missingMethods = JavaMethodInventory.missingMethods(existingSource, describedType);
        if (missingMethods.isEmpty()) {
            return existingSource;
        }

        // Enum constant lists must be terminated with ';' before any member declaration.
        // When inserting the first method(s) into an enum whose constants are not yet
        // semicolon-terminated, add the terminator so the result stays valid Java.
        if (JavaTypeKind.ENUM.equals(describedType.kind())) {
            existingSource = ensureEnumConstantsTerminated(existingSource, describedType.simpleName());
        }

        int closingBrace = findPrimaryTypeClosingBrace(existingSource, describedType.simpleName());
        if (closingBrace < 0) {
            return existingSource;
        }

        String indent = memberIndentationBefore(existingSource, closingBrace);
        String insertion = JavaMethodRenderer.renderMissingMethods(missingMethods, describedType, indent);
        if (insertion.length() == 0) {
            return existingSource;
        }
        return insertBeforeClosingBrace(existingSource, closingBrace, insertion);
    }

    public static boolean hasMissingMethods(String existingSource, DescribedType describedType) {
        Objects.requireNonNull(existingSource, "existingSource must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");
        if (JavaTypeKind.SEALED_INTERFACE.equals(describedType.kind())) {
            return !SealedInterfaceMethodSynchronizer.updateSource(existingSource, describedType).equals(existingSource);
        }
        return !JavaMethodInventory.missingMethods(existingSource, describedType).isEmpty();
    }

    public static String updateFile(File classFile, DescribedType describedType) throws IOException {
        Objects.requireNonNull(classFile, "classFile must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");

        String existingSource = new String(Files.readAllBytes(classFile.toPath()), StandardCharsets.UTF_8);
        String updatedSource = updateSource(existingSource, describedType);
        if (!existingSource.equals(updatedSource)) {
            AtomicFileWriter.writeUtf8(classFile, updatedSource);
        }
        return updatedSource;
    }
}
