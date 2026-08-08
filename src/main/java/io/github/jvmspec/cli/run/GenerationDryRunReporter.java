package io.github.jvmspec.cli.run;

import io.github.jvmspec.generation.SpecGenerationPlan;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Detects and reports generation changes without writing files. */
final class GenerationDryRunReporter {
    private GenerationDryRunReporter() {
    }

    static boolean reportSupport(
            SpecGenerationPlan supportPlan,
            PrintStream out,
            String artifactName
    ) throws IOException {
        File targetFile = supportPlan.targetFile();
        if (!targetFile.exists()) {
            out.println("Would generate " + artifactName + ": " + targetFile.getPath());
            return true;
        }
        String existingSource = new String(
                Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
        if (!existingSource.equals(supportPlan.sourceContent())) {
            out.println("Would update " + artifactName + ": " + targetFile.getPath());
            return true;
        }
        return false;
    }
}
