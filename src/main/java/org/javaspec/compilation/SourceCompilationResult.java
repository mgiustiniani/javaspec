package org.javaspec.compilation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of an opt-in source/spec compilation attempt.
 */
public final class SourceCompilationResult {
    private static final List<String> EMPTY_DIAGNOSTICS = Collections.unmodifiableList(new ArrayList<String>());

    private final boolean compilerAvailable;
    private final boolean successful;
    private final int sourceFileCount;
    private final File outputDirectory;
    private final List<String> diagnostics;

    private SourceCompilationResult(
            boolean compilerAvailable,
            boolean successful,
            int sourceFileCount,
            File outputDirectory,
            List<String> diagnostics
    ) {
        this.compilerAvailable = compilerAvailable;
        this.successful = successful;
        this.sourceFileCount = sourceFileCount;
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        this.diagnostics = immutableDiagnostics(diagnostics);
    }

    public static SourceCompilationResult compilerUnavailable(File outputDirectory, int sourceFileCount) {
        return new SourceCompilationResult(false, false, sourceFileCount, outputDirectory, EMPTY_DIAGNOSTICS);
    }

    public static SourceCompilationResult success(File outputDirectory, int sourceFileCount) {
        return new SourceCompilationResult(true, true, sourceFileCount, outputDirectory, EMPTY_DIAGNOSTICS);
    }

    public static SourceCompilationResult failure(File outputDirectory, int sourceFileCount, List<String> diagnostics) {
        return new SourceCompilationResult(true, false, sourceFileCount, outputDirectory, diagnostics);
    }

    public boolean compilerAvailable() {
        return compilerAvailable;
    }

    public boolean isCompilerAvailable() {
        return compilerAvailable;
    }

    public boolean successful() {
        return successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public int sourceFileCount() {
        return sourceFileCount;
    }

    public int getSourceFileCount() {
        return sourceFileCount;
    }

    public File outputDirectory() {
        return outputDirectory;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public List<String> diagnostics() {
        return diagnostics;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }

    private static List<String> immutableDiagnostics(List<String> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return EMPTY_DIAGNOSTICS;
        }
        return Collections.unmodifiableList(new ArrayList<String>(diagnostics));
    }
}
