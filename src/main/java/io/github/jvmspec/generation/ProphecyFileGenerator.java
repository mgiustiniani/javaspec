package io.github.jvmspec.generation;

import java.io.File;
import java.io.IOException;

/**
 * Writes generated Prophecy wrapper source files.
 */
public final class ProphecyFileGenerator {

    private ProphecyFileGenerator() {
    }

    /**
     * Writes the generated source code to the target file.
     *
     * @param plan the prophecy generation plan
     * @return the written file
     * @throws IOException if writing fails
     */
    public static File write(ProphecyGenerationPlan plan) throws IOException {
        File targetFile = plan.targetFile();
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        AtomicFileWriter.writeUtf8(targetFile, plan.sourceCode());
        return targetFile;
    }
}
