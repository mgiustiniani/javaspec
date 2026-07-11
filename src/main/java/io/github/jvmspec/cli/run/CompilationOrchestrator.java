package io.github.jvmspec.cli.run;

import io.github.jvmspec.compilation.SourceCompilationResult;
import io.github.jvmspec.compilation.SourceCompiler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates source compilation for a spec run.
 * <p>Extracted from {@link io.github.jvmspec.cli.Main Main} to isolate compilation
 * logic and enable unit testing.</p>
 */
public final class CompilationOrchestrator {
    private static final int EXIT_OK = 0;
    private static final int EXIT_USAGE = 64;
    private static final int EXIT_COMPILATION_FAILED = 1;
    private static final int EXIT_IO_ERROR = 70;

    private CompilationOrchestrator() {
    }

    /**
     * Compiles source and spec trees and returns an updated {@link ClasspathSelection}
     * that includes the compile output directory.
     *
     * @param compileOutputPath   the output directory for compiled classes
     * @param sourceRoot          production source root
     * @param specRoot            spec source root
     * @param generatedSourcesRoot generated source root containing support/wrapper classes
     * @param classpathSelection  the base classpath selection
     * @param out                 output stream for diagnostic messages
     * @param err                 error stream for diagnostic messages
     * @return an updated classpath selection (with compile output), or an error
     *         selection with a non-zero exit code
     */
    /**
     * Compiles with no explicit Java release target.
     * Equivalent to calling {@link #compile(String, File, File, File, ClasspathSelection, String, PrintStream, PrintStream)}
     * with {@code releaseVersion = null}.
     */
    public static ClasspathSelection compile(
            String compileOutputPath,
            File sourceRoot,
            File specRoot,
            File generatedSourcesRoot,
            ClasspathSelection classpathSelection,
            PrintStream out,
            PrintStream err
    ) {
        return compile(compileOutputPath, sourceRoot, specRoot, generatedSourcesRoot,
                classpathSelection, null, out, err);
    }

    /**
     * Compiles source and spec trees and returns an updated {@link ClasspathSelection}
     * that includes the compile output directory.
     *
     * @param compileOutputPath    the output directory for compiled classes
     * @param sourceRoot           production source root
     * @param specRoot             spec source root
     * @param generatedSourcesRoot generated source root containing support/wrapper classes
     * @param classpathSelection   the base classpath selection
     * @param releaseVersion       optional Java release target (e.g. {@code "11"}); {@code null}
     *                             means no release option
     * @param out                  output stream for diagnostic messages
     * @param err                  error stream for diagnostic messages
     * @return an updated classpath selection (with compile output), or an error
     *         selection with a non-zero exit code
     */
    public static ClasspathSelection compile(
            String compileOutputPath,
            File sourceRoot,
            File specRoot,
            File generatedSourcesRoot,
            ClasspathSelection classpathSelection,
            String releaseVersion,
            PrintStream out,
            PrintStream err
    ) {
        File outputDirectory = new File(compileOutputPath);
        SourceCompilationResult result;
        try {
            result = SourceCompiler.compile(
                    compilationSourceRoots(sourceRoot, specRoot),
                    outputDirectory,
                    classpathSelection.entries(),
                    compilationSourcePathRoots(sourceRoot, specRoot, generatedSourcesRoot),
                    releaseVersion
            );
        } catch (NoClassDefFoundError ex) {
            printCompilerUnavailable(err);
            return classpathSelection.withExitCode(EXIT_USAGE);
        } catch (IOException ex) {
            err.println("I/O error while compiling sources: " + messageOf(ex));
            err.println("Target path: " + outputDirectory.getPath());
            return classpathSelection.withExitCode(EXIT_IO_ERROR);
        } catch (SecurityException ex) {
            err.println("I/O error while compiling sources: " + messageOf(ex));
            err.println("Target path: " + outputDirectory.getPath());
            return classpathSelection.withExitCode(EXIT_IO_ERROR);
        }

        if (!result.compilerAvailable()) {
            printCompilerUnavailable(err);
            return classpathSelection.withExitCode(EXIT_USAGE);
        }
        if (!result.successful()) {
            printCompilationFailure(result, err);
            return classpathSelection.withExitCode(EXIT_COMPILATION_FAILED);
        }
        if (result.sourceFileCount() == 0) {
            return classpathSelection;
        }
        if (result.skipped()) {
            out.println("Compilation up to date (" + result.sourceFileCount()
                    + " source file(s), " + result.outputDirectory().getPath() + ").");
            return ClasspathResolver.withCompileOutput(result.outputDirectory(), classpathSelection, err);
        }

        out.println("Compiled " + result.sourceFileCount() + " source file(s) to "
                + result.outputDirectory().getPath() + ".");
        return ClasspathResolver.withCompileOutput(result.outputDirectory(), classpathSelection, err);
    }

    private static List<File> compilationSourceRoots(File sourceRoot, File specRoot) {
        List<File> roots = new ArrayList<File>();
        roots.add(sourceRoot);
        roots.add(specRoot);
        return roots;
    }

    private static List<File> compilationSourcePathRoots(File sourceRoot, File specRoot, File generatedSourcesRoot) {
        List<File> roots = compilationSourceRoots(sourceRoot, specRoot);
        roots.add(generatedSourcesRoot);
        return roots;
    }

    private static void printCompilerUnavailable(PrintStream err) {
        err.println("Error: Java compiler is not available. Run javaspec with a JDK or omit --compile.");
    }

    private static void printCompilationFailure(SourceCompilationResult result, PrintStream err) {
        err.println("Compilation failed:");
        List<String> diagnostics = result.diagnostics();
        for (int i = 0; i < diagnostics.size(); i++) {
            err.println("  " + diagnostics.get(i));
        }
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
