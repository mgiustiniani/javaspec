package io.github.jvmspec.cli.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolves the classpath for a spec run from explicit {@code --classpath} /
 * {@code --classpath-file} arguments and optional compile output.
 * <p>Extracted from {@link io.github.jvmspec.cli.Main Main} to isolate classpath
 * logic and enable unit testing.</p>
 */
public final class ClasspathResolver {
    private static final int EXIT_OK = 0;
    private static final int EXIT_USAGE = 64;
    private static final int EXIT_IO_ERROR = 70;

    private ClasspathResolver() {
    }

    /**
     * Builds a {@link ClasspathSelection} from explicit classpath arguments.
     * If no explicit classpath is configured, returns a selection using the
     * current context class loader with no extra entries.
     *
     * @param classpathArguments the parsed classpath arguments (may be null or empty)
     * @param err                error stream for diagnostic messages
     * @return a resolved {@link ClasspathSelection}
     */
    public static ClasspathSelection select(
            List<ClasspathArgument> classpathArguments,
            PrintStream err
    ) {
        ClassLoader parent = effectiveClassLoader();
        if (classpathArguments == null || classpathArguments.isEmpty()) {
            return ClasspathSelection.of(parent, Collections.<File>emptyList(), EXIT_OK, parent, false);
        }

        List<File> entries = new ArrayList<File>();
        for (int i = 0; i < classpathArguments.size(); i++) {
            ClasspathArgument argument = classpathArguments.get(i);
            if (argument.isFile()) {
                int exitCode = addClasspathFileEntries(argument.value(), entries, err);
                if (exitCode != EXIT_OK) {
                    return ClasspathSelection.of(parent, entries, exitCode, parent, false);
                }
            } else {
                addPathListEntries(argument.value(), entries);
            }
        }

        URL[] urls = new URL[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            try {
                urls[i] = entries.get(i).toURI().toURL();
            } catch (MalformedURLException ex) {
                printUsageError(err, "Invalid classpath entry: " + entries.get(i).getPath()
                        + " (" + messageOf(ex) + ").");
                return ClasspathSelection.of(parent, entries, EXIT_USAGE, parent, false);
            } catch (SecurityException ex) {
                err.println("I/O error while preparing explicit classpath: " + messageOf(ex));
                err.println("Classpath entry: " + entries.get(i).getPath());
                return ClasspathSelection.of(parent, entries, EXIT_IO_ERROR, parent, false);
            }
        }
        return ClasspathSelection.of(new URLClassLoader(urls, parent), entries, EXIT_OK, parent, false);
    }

    /**
     * Returns a new {@link ClasspathSelection} that prepends the compile-output
     * directory to the existing entries.
     *
     * @param compileOutputDirectory the directory containing compiled classes
     * @param baseSelection          the base classpath selection
     * @param err                    error stream for diagnostic messages
     * @return a new selection with compile output prepended, or an error selection
     */
    public static ClasspathSelection withCompileOutput(
            File compileOutputDirectory,
            ClasspathSelection baseSelection,
            PrintStream err
    ) {
        List<File> entries = new ArrayList<File>();
        entries.add(compileOutputDirectory);
        entries.addAll(baseSelection.entries());

        URL[] urls = new URL[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            try {
                urls[i] = entries.get(i).toURI().toURL();
            } catch (MalformedURLException ex) {
                printUsageError(err, "Invalid classpath entry: " + entries.get(i).getPath()
                        + " (" + messageOf(ex) + ").");
                return baseSelection.withExitCode(EXIT_USAGE);
            } catch (SecurityException ex) {
                err.println("I/O error while preparing compilation classpath: " + messageOf(ex));
                err.println("Target path: " + compileOutputDirectory.getPath());
                return baseSelection.withExitCode(EXIT_IO_ERROR);
            }
        }
        return ClasspathSelection.of(
                new URLClassLoader(urls, baseSelection.parentClassLoader()),
                entries,
                EXIT_OK,
                baseSelection.parentClassLoader(),
                true
        );
    }

    /**
     * Returns a new {@link ClasspathSelection} that prepends the given resolved
     * dependency JARs to the existing classpath entries.
     *
     * @param resolvedJars   JARs resolved by a {@code DependencyResolver}
     * @param baseSelection  the base classpath selection
     * @param err            error stream for diagnostic messages
     * @return a new selection with resolved JARs prepended, or an error selection
     */
    public static ClasspathSelection withResolvedDependencies(
            List<File> resolvedJars,
            ClasspathSelection baseSelection,
            PrintStream err
    ) {
        if (resolvedJars == null || resolvedJars.isEmpty()) {
            return baseSelection;
        }
        List<File> entries = new ArrayList<File>(resolvedJars);
        entries.addAll(baseSelection.entries());

        URL[] baseUrls;
        try {
            baseUrls = buildUrls(entries, err);
        } catch (ClasspathBuildException ex) {
            return baseSelection.withExitCode(ex.exitCode);
        }
        return ClasspathSelection.of(
                new URLClassLoader(baseUrls, baseSelection.parentClassLoader()),
                entries,
                EXIT_OK,
                baseSelection.parentClassLoader(),
                baseSelection.includesCompileOutput()
        );
    }

    private static URL[] buildUrls(List<File> entries, PrintStream err) throws ClasspathBuildException {
        URL[] urls = new URL[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            try {
                urls[i] = entries.get(i).toURI().toURL();
            } catch (MalformedURLException ex) {
                printUsageError(err, "Invalid classpath entry: " + entries.get(i).getPath()
                        + " (" + messageOf(ex) + ").");
                throw new ClasspathBuildException(EXIT_USAGE);
            } catch (SecurityException ex) {
                err.println("I/O error while preparing classpath: " + messageOf(ex));
                throw new ClasspathBuildException(EXIT_IO_ERROR);
            }
        }
        return urls;
    }

    private static final class ClasspathBuildException extends Exception {
        final int exitCode;
        ClasspathBuildException(int exitCode) {
            this.exitCode = exitCode;
        }
    }

    private static ClassLoader effectiveClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }
        return ClasspathResolver.class.getClassLoader();
    }

    private static int addClasspathFileEntries(String classpathFilePath, List<File> entries, PrintStream err) {
        File classpathFile = new File(classpathFilePath);
        BufferedReader reader = null;
        try {
            reader = Files.newBufferedReader(classpathFile.toPath(), StandardCharsets.UTF_8);
            String line;
            while ((line = reader.readLine()) != null) {
                String entry = line.trim();
                if (entry.length() == 0 || entry.startsWith("#")) {
                    continue;
                }
                entries.add(new File(entry));
            }
            return EXIT_OK;
        } catch (IOException ex) {
            err.println("I/O error while reading classpath file: " + messageOf(ex));
            err.println("Classpath file: " + classpathFile.getPath());
            return EXIT_IO_ERROR;
        } catch (SecurityException ex) {
            err.println("I/O error while reading classpath file: " + messageOf(ex));
            err.println("Classpath file: " + classpathFile.getPath());
            return EXIT_IO_ERROR;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // Ignore close failures after the classpath file has already been consumed.
                }
            }
        }
    }

    private static void addPathListEntries(String pathList, List<File> entries) {
        int start = 0;
        while (start <= pathList.length()) {
            int separatorIndex = pathList.indexOf(File.pathSeparator, start);
            String rawEntry;
            if (separatorIndex < 0) {
                rawEntry = pathList.substring(start);
                start = pathList.length() + 1;
            } else {
                rawEntry = pathList.substring(start, separatorIndex);
                start = separatorIndex + File.pathSeparator.length();
            }
            String entry = rawEntry.trim();
            if (entry.length() > 0) {
                entries.add(new File(entry));
            }
        }
    }

    private static void printUsageError(PrintStream err, String message) {
        err.println("Error: " + message);
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
