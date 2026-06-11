package org.javaspec.compilation;

import java.io.File;
import java.util.Objects;

/**
 * Unchecked programmatic failure raised for opt-in source/spec compilation.
 *
 * <p>Compilation uses the current JDK {@code javax.tools.JavaCompiler}. javaspec does not
 * resolve dependencies, fork {@code javac}, manage source/release levels, or maintain an
 * incremental cache. The attached {@link SourceCompilationResult}, when present, carries the
 * compiler availability flag, source-file count, output directory, and diagnostics. I/O and
 * security failures can occur before such a result exists.</p>
 */
public final class SourceCompilationException extends RuntimeException {
    private final Reason reason;
    private final SourceCompilationResult sourceCompilationResult;
    private final File outputDirectory;

    private SourceCompilationException(
            Reason reason,
            String message,
            SourceCompilationResult sourceCompilationResult,
            File outputDirectory,
            Throwable cause
    ) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.sourceCompilationResult = sourceCompilationResult;
        this.outputDirectory = sourceCompilationResult == null
                ? outputDirectory
                : sourceCompilationResult.outputDirectory();
    }

    /**
     * Creates a failure for a current runtime that does not expose {@code JavaCompiler}.
     */
    public static SourceCompilationException compilerUnavailable(SourceCompilationResult result) {
        return compilerUnavailable(result, null);
    }

    /**
     * Creates a failure for an unavailable or unloadable current-JDK compiler API.
     */
    public static SourceCompilationException compilerUnavailable(
            SourceCompilationResult result,
            Throwable cause
    ) {
        return new SourceCompilationException(
                Reason.COMPILER_UNAVAILABLE,
                "Java compiler is not available. Run javaspec with a JDK or disable compilation.",
                result,
                null,
                cause
        );
    }

    /**
     * Creates a failure for a completed compiler task that returned unsuccessful diagnostics.
     */
    public static SourceCompilationException compilationFailed(SourceCompilationResult result) {
        return new SourceCompilationException(
                Reason.COMPILATION_FAILED,
                "Compilation failed",
                Objects.requireNonNull(result, "result must not be null"),
                null,
                null
        );
    }

    /**
     * Creates a failure for an I/O or security error raised while compiling sources.
     */
    public static SourceCompilationException ioError(File outputDirectory, Throwable cause) {
        return new SourceCompilationException(
                Reason.IO_ERROR,
                ioErrorMessage(cause),
                null,
                outputDirectory,
                cause
        );
    }

    /**
     * Creates a failure for an I/O or security error raised after a compilation result exists.
     */
    public static SourceCompilationException ioError(SourceCompilationResult result, Throwable cause) {
        return new SourceCompilationException(
                Reason.IO_ERROR,
                ioErrorMessage(cause),
                result,
                null,
                cause
        );
    }

    public Reason reason() {
        return reason;
    }

    public Reason getReason() {
        return reason;
    }

    public SourceCompilationResult sourceCompilationResult() {
        return sourceCompilationResult;
    }

    public SourceCompilationResult getSourceCompilationResult() {
        return sourceCompilationResult;
    }

    public boolean hasSourceCompilationResult() {
        return sourceCompilationResult != null;
    }

    public File outputDirectory() {
        return outputDirectory;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    private static String ioErrorMessage(Throwable cause) {
        return "I/O error while compiling sources: " + messageOf(cause);
    }

    private static String messageOf(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }

    /**
     * Stable machine-readable reason for a programmatic compilation failure.
     */
    public enum Reason {
        /** The current runtime does not expose the JDK compiler API. */
        COMPILER_UNAVAILABLE,
        /** The compiler API ran and reported unsuccessful compilation. */
        COMPILATION_FAILED,
        /** An I/O or security failure occurred before bootstrap hooks or examples ran. */
        IO_ERROR
    }
}
