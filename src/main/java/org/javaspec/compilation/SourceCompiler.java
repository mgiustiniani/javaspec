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
 * Zero-dependency adapter around the JDK compiler API for CLI opt-in compilation.
 */
public final class SourceCompiler {
    private static final String JAVA_SUFFIX = ".java";
    private static final String ENCODING = "UTF-8";

    private SourceCompiler() {
    }

    public static SourceCompilationResult compile(
            List<File> sourceRoots,
            File outputDirectory,
            List<File> explicitClasspathEntries
    ) throws IOException {
        Objects.requireNonNull(sourceRoots, "sourceRoots must not be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        Objects.requireNonNull(explicitClasspathEntries, "explicitClasspathEntries must not be null");

        List<File> sourceFiles = sourceFiles(sourceRoots);
        if (sourceFiles.isEmpty()) {
            return SourceCompilationResult.success(outputDirectory, 0);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return SourceCompilationResult.compilerUnavailable(outputDirectory, sourceFiles.size());
        }

        ensureOutputDirectory(outputDirectory);

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
                    compilerOptions(outputDirectory, explicitClasspathEntries),
                    null,
                    compilationUnits
            );
            successful = task.call();
        } finally {
            fileManager.close();
        }

        if (Boolean.TRUE.equals(successful)) {
            return SourceCompilationResult.success(outputDirectory, sourceFiles.size());
        }
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

    private static List<String> compilerOptions(File outputDirectory, List<File> explicitClasspathEntries) {
        List<String> options = new ArrayList<String>();
        options.add("-d");
        options.add(outputDirectory.getPath());
        options.add("-classpath");
        options.add(classpath(outputDirectory, explicitClasspathEntries));
        options.add("-encoding");
        options.add(ENCODING);
        return options;
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
