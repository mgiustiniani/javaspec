package io.github.jvmspec.cli.run;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The resolved classpath configuration: a classloader, the list of entries,
 * an optional exit code (for error propagation), and metadata about whether
 * compile output is included.
 */
public final class ClasspathSelection {
    private final ClassLoader classLoader;
    private final List<File> entries;
    private final int exitCode;
    private final ClassLoader parentClassLoader;
    private final boolean includesCompileOutput;

    private ClasspathSelection(
            ClassLoader classLoader,
            List<File> entries,
            int exitCode,
            ClassLoader parentClassLoader,
            boolean includesCompileOutput
    ) {
        this.classLoader = classLoader;
        if (entries.isEmpty()) {
            this.entries = Collections.emptyList();
        } else {
            this.entries = Collections.unmodifiableList(new ArrayList<File>(entries));
        }
        this.exitCode = exitCode;
        this.parentClassLoader = parentClassLoader;
        this.includesCompileOutput = includesCompileOutput;
    }

    public static ClasspathSelection of(
            ClassLoader classLoader,
            List<File> entries,
            int exitCode,
            ClassLoader parentClassLoader,
            boolean includesCompileOutput
    ) {
        return new ClasspathSelection(classLoader, entries, exitCode, parentClassLoader, includesCompileOutput);
    }

    public ClasspathSelection withExitCode(int newExitCode) {
        return new ClasspathSelection(classLoader, entries, newExitCode, parentClassLoader, includesCompileOutput);
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public ClassLoader parentClassLoader() {
        return parentClassLoader;
    }

    public List<File> entries() {
        return entries;
    }

    public boolean hasExplicitEntries() {
        return !entries.isEmpty();
    }

    public boolean includesCompileOutput() {
        return includesCompileOutput;
    }

    public File compiledOutputDirectory() {
        if (!includesCompileOutput || entries.isEmpty()) {
            return null;
        }
        return entries.get(0);
    }

    public int exitCode() {
        return exitCode;
    }
}
