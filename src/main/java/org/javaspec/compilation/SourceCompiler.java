package org.javaspec.compilation;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Zero-dependency adapter around the current JDK compiler API for opt-in source/spec compilation.
 *
 * <p>This compiler uses {@code javax.tools.JavaCompiler} from the runtime JDK. It does not
 * resolve dependencies, fork {@code javac}, manage source/release levels, or maintain an
 * incremental cache. Callers provide source roots, an output directory, and any explicit
 * classpath entries they need for the compile.</p>
 */
public final class SourceCompiler {
    private static final String JAVA_SUFFIX = ".java";
    private static final String ENCODING = "UTF-8";

    private SourceCompiler() {
    }

    /**
     * Compiles sources from the given roots into {@code outputDirectory}.
     * Equivalent to calling the four-argument overload with {@code sourceRoots} as
     * the source-path roots and no release version.
     */
    public static SourceCompilationResult compile(
            List<File> sourceRoots,
            File outputDirectory,
            List<File> explicitClasspathEntries
    ) throws IOException {
        return compile(sourceRoots, outputDirectory, explicitClasspathEntries, sourceRoots, null);
    }

    /**
     * Compiles sources with an explicit source-path (for generated-source directories) and
     * no release version.
     */
    public static SourceCompilationResult compile(
            List<File> sourceRoots,
            File outputDirectory,
            List<File> explicitClasspathEntries,
            List<File> sourcePathRoots
    ) throws IOException {
        return compile(sourceRoots, outputDirectory, explicitClasspathEntries, sourcePathRoots, null);
    }

    /**
     * Full compilation entry point.
     *
     * @param sourceRoots           directories scanned for {@code .java} compilation units
     * @param outputDirectory       directory where compiled {@code .class} files are written
     * @param explicitClasspathEntries extra classpath entries (resolved dependencies, etc.)
     * @param sourcePathRoots       directories placed on the javac {@code -sourcepath}
     *                              (includes generated-source directories)
     * @param releaseVersion        optional Java release target ({@code "8"}, {@code "11"}, …);
     *                              {@code null} means no release option is passed to javac
     * @return compilation result
     * @throws IOException if the output directory cannot be created or sources cannot be listed
     */
    public static SourceCompilationResult compile(
            List<File> sourceRoots,
            File outputDirectory,
            List<File> explicitClasspathEntries,
            List<File> sourcePathRoots,
            String releaseVersion
    ) throws IOException {
        Objects.requireNonNull(sourceRoots, "sourceRoots must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Objects.requireNonNull(explicitClasspathEntries, "explicitClasspathEntries must not be null");
        Objects.requireNonNull(sourcePathRoots, "sourcePathRoots must not be null");

        List<File> sourceFiles = sourceFiles(sourceRoots);
        if (sourceFiles.isEmpty()) {
            return SourceCompilationResult.success(outputDirectory, 0);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return SourceCompilationResult.compilerUnavailable(outputDirectory, sourceFiles.size());
        }

        ensureOutputDirectory(outputDirectory);

        List<String> options = compilerOptions(
                outputDirectory, explicitClasspathEntries, sourcePathRoots, releaseVersion);

        // Incremental cache check: skip compilation if nothing has changed.
        CompilationCache cache = new CompilationCache(outputDirectory);
        if (cache.isUpToDate(sourceFiles, explicitClasspathEntries, options)) {
            return SourceCompilationResult.skipped(outputDirectory, sourceFiles.size());
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics,
                Locale.ROOT,
                StandardCharsets.UTF_8
        );
        StringWriter compilerOutput = new StringWriter();
        Boolean successful;
        try {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    compilerOutput,
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    compilationUnits
            );
            successful = task.call();
        } finally {
            fileManager.close();
        }

        if (Boolean.TRUE.equals(successful)) {
            cache.save(sourceFiles, explicitClasspathEntries, options);
            return SourceCompilationResult.success(outputDirectory, sourceFiles.size());
        }
        cache.invalidate();
        return SourceCompilationResult.failure(
                outputDirectory,
                sourceFiles.size(),
                diagnosticLines(diagnostics.getDiagnostics(), compilerOutput.toString())
        );
    }

    private static List<File> sourceFiles(List<File> roots) {
        Map<String, File> filesByPath = new TreeMap<String, File>();
        for (int i = 0; i < roots.size(); i++) {
            File root = roots.get(i);
            if (root == null || !root.isDirectory()) {
                continue;
            }
            collectSourceFiles(root, filesByPath);
        }
        return new ArrayList<File>(filesByPath.values());
    }

    private static void collectSourceFiles(File directory, Map<String, File> filesByPath) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, new Comparator<File>() {
            public int compare(File left, File right) {
                return left.getName().compareTo(right.getName());
            }
        });
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                collectSourceFiles(child, filesByPath);
            } else if (child.isFile() && child.getName().endsWith(JAVA_SUFFIX)) {
                String key = normalizedPath(child);
                if (!filesByPath.containsKey(key)) {
                    filesByPath.put(key, child);
                }
            }
        }
    }

    private static String normalizedPath(File file) {
        return file.toPath().toAbsolutePath().normalize().toString();
    }

    private static void ensureOutputDirectory(File outputDirectory) throws IOException {
        if (outputDirectory.isDirectory()) {
            return;
        }
        if (outputDirectory.exists()) {
            throw new IOException("Compilation output path exists but is not a directory.");
        }
        if (!outputDirectory.mkdirs() && !outputDirectory.isDirectory()) {
            throw new IOException("Could not create compilation output directory.");
        }
    }

    private static List<String> compilerOptions(
            File outputDirectory,
            List<File> explicitClasspathEntries,
            List<File> sourcePathRoots,
            String releaseVersion
    ) {
        List<String> options = new ArrayList<String>();
        options.add("-d");
        options.add(outputDirectory.getPath());
        options.add("-classpath");
        options.add(classpath(outputDirectory, explicitClasspathEntries));
        options.add("-sourcepath");
        options.add(sourcePath(sourcePathRoots));
        options.add("-encoding");
        options.add(ENCODING);
        if (releaseVersion != null && !releaseVersion.isEmpty()) {
            addReleaseOptions(options, releaseVersion);
        }
        return options;
    }

    /**
     * Adds Java release-target options to the compiler options list.
     *
     * <p>On JDK 9+, {@code --release N} is used (preferred because it also
     * restricts the API to the target release).  On JDK 8, the equivalent
     * {@code -source N -target N} pair is used instead because {@code --release}
     * is not available.</p>
     */
    private static void addReleaseOptions(List<String> options, String releaseVersion) {
        if (isJava9OrLater()) {
            options.add("--release");
            options.add(releaseVersion);
        } else {
            options.add("-source");
            options.add(releaseVersion);
            options.add("-target");
            options.add(releaseVersion);
        }
    }

    /**
     * Returns {@code true} if the current JVM is Java 9 or later.
     * Uses {@code java.specification.version} which is {@code "1.8"} on Java 8
     * and {@code "9"}, {@code "11"}, etc. on later versions.
     */
    static boolean isJava9OrLater() {
        String version = System.getProperty("java.specification.version", "1.8");
        if (version.startsWith("1.")) {
            // Java 8 and earlier: "1.8", "1.7", etc.
            return false;
        }
        try {
            return Integer.parseInt(version) >= 9;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String sourcePath(List<File> sourcePathRoots) {
        List<String> entries = new ArrayList<String>();
        for (int i = 0; i < sourcePathRoots.size(); i++) {
            File root = sourcePathRoots.get(i);
            if (root != null) {
                entries.add(root.getPath());
            }
        }
        return joinPathList(entries);
    }

    private static String classpath(File outputDirectory, List<File> explicitClasspathEntries) {
        List<String> entries = new ArrayList<String>();
        entries.add(outputDirectory.getPath());
        for (int i = 0; i < explicitClasspathEntries.size(); i++) {
            entries.add(explicitClasspathEntries.get(i).getPath());
        }
        addCurrentProcessClasspath(entries);
        return joinPathList(entries);
    }

    private static void addCurrentProcessClasspath(List<String> entries) {
        String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.length() == 0) {
            return;
        }
        int start = 0;
        while (start <= classpath.length()) {
            int separatorIndex = classpath.indexOf(File.pathSeparator, start);
            String entry;
            if (separatorIndex < 0) {
                entry = classpath.substring(start);
                start = classpath.length() + 1;
            } else {
                entry = classpath.substring(start, separatorIndex);
                start = separatorIndex + File.pathSeparator.length();
            }
            if (entry.length() > 0) {
                entries.add(entry);
            }
        }
    }

    private static String joinPathList(List<String> entries) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                builder.append(File.pathSeparator);
            }
            builder.append(entries.get(i));
        }
        return builder.toString();
    }

    private static List<String> diagnosticLines(
            List<Diagnostic<? extends JavaFileObject>> diagnostics,
            String compilerOutput
    ) {
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < diagnostics.size(); i++) {
            lines.add(diagnosticLine(diagnostics.get(i)));
        }
        addCompilerOutputLines(lines, compilerOutput);
        if (lines.isEmpty()) {
            lines.add("javac returned failure without diagnostics.");
        }
        return Collections.unmodifiableList(lines);
    }

    private static String diagnosticLine(Diagnostic<? extends JavaFileObject> diagnostic) {
        String kind = diagnostic.getKind().name().toLowerCase(Locale.ROOT);
        String message = singleLine(diagnostic.getMessage(Locale.ROOT));
        StringBuilder builder = new StringBuilder();
        JavaFileObject source = diagnostic.getSource();
        if (source == null) {
            builder.append(kind).append(": ").append(message);
            return builder.toString();
        }

        builder.append(source.getName());
        if (diagnostic.getLineNumber() != Diagnostic.NOPOS) {
            builder.append(':').append(diagnostic.getLineNumber());
            if (diagnostic.getColumnNumber() != Diagnostic.NOPOS) {
                builder.append(':').append(diagnostic.getColumnNumber());
            }
        }
        builder.append(": ").append(kind).append(": ").append(message);
        return builder.toString();
    }

    private static String singleLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    private static void addCompilerOutputLines(List<String> lines, String compilerOutput) {
        if (compilerOutput == null || compilerOutput.length() == 0) {
            return;
        }
        int start = 0;
        while (start <= compilerOutput.length()) {
            int separatorIndex = compilerOutput.indexOf('\n', start);
            String rawLine;
            if (separatorIndex < 0) {
                rawLine = compilerOutput.substring(start);
                start = compilerOutput.length() + 1;
            } else {
                rawLine = compilerOutput.substring(start, separatorIndex);
                start = separatorIndex + 1;
            }
            String line = stripTrailingCarriageReturn(rawLine);
            if (line.length() > 0) {
                lines.add("javac: " + line);
            }
        }
    }

    private static String stripTrailingCarriageReturn(String line) {
        if (line.endsWith("\r")) {
            return line.substring(0, line.length() - 1);
        }
        return line;
    }
}
