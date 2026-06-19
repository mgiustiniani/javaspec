package org.javaspec.compilation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of an opt-in source/spec compilation attempt.
 *
 * <p>Results describe compilation through the current JDK {@code javax.tools.JavaCompiler}.
 * A successful result with {@code sourceFileCount() == 0} means no source files were found and
 * no output classloader is required. Compiler-unavailable and compile-failure results are used
 * by callers to report failures without adding runtime dependencies.</p>
 */
public final class SourceCompilationResult {
    private static final List<String> EMPTY_DIAGNOSTICS = Collections.unmodifiableList(new ArrayList<String>());

    private final boolean compilerAvailable;
    private final boolean successful;
    private final boolean skipped;
    private final int sourceFileCount;
    private final File outputDirectory;
    private final List<String> diagnostics;

    private SourceCompilationResult(
            boolean compilerAvailable,
            boolean successful,
            boolean skipped,
            int sourceFileCount,
            File outputDirectory,
            List<String> diagnostics
    ) {
        this.compilerAvailable = compilerAvailable;
        this.successful = successful;
        this.skipped = skipped;
        this.sourceFileCount = sourceFileCount;
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        this.diagnostics = immutableDiagnostics(diagnostics);
    }

    public static SourceCompilationResult compilerUnavailable(File outputDirectory, int sourceFileCount) {
        return new SourceCompilationResult(false, false, false, sourceFileCount, outputDirectory, EMPTY_DIAGNOSTICS);
    }

    public static SourceCompilationResult success(File outputDirectory, int sourceFileCount) {
        return new SourceCompilationResult(true, true, false, sourceFileCount, outputDirectory, EMPTY_DIAGNOSTICS);
    }

    public static SourceCompilationResult failure(File outputDirectory, int sourceFileCount, List<String> diagnostics) {
        return new SourceCompilationResult(true, false, false, sourceFileCount, outputDirectory, diagnostics);
    }

    /**
     * Returns a result representing a cache hit: compilation was skipped because
     * nothing has changed since the last successful compile.
     *
     * @param outputDirectory the output directory that already contains up-to-date classes
     * @param sourceFileCount the number of source files that were fingerprinted
     * @return a skipped result
     */
    public static SourceCompilationResult skipped(File outputDirectory, int sourceFileCount) {
        return new SourceCompilationResult(true, true, true, sourceFileCount, outputDirectory, EMPTY_DIAGNOSTICS);
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

    /** Returns {@code true} when compilation was skipped due to an up-to-date cache hit. */
    public boolean skipped() {
        return skipped;
    }

    public boolean isSkipped() {
        return skipped;
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
