package org.javaspec.generation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
        Files.write(targetFile.toPath(), plan.sourceCode().getBytes(StandardCharsets.UTF_8));
        return targetFile;
    }
}
