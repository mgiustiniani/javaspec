package io.github.jvmspec.compilation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Lightweight incremental compilation cache for {@link SourceCompiler}.
 *
 * <p>The cache stores the last-modified timestamps of all source files together
 * with a fingerprint of the compiler options and classpath entries in a
 * {@code .properties} file inside the compilation output directory.  A
 * subsequent compile call that finds all timestamps and the fingerprint
 * unchanged can skip the actual {@code javac} invocation.</p>
 *
 * <p>The cache is intentionally conservative: any I/O error reading the cache
 * is treated as a cache miss, triggering a full recompile.</p>
 */
final class CompilationCache {

    static final String CACHE_FILE_NAME = ".javaspec-compile-cache.properties";

    private static final String KEY_OPTIONS = "options";
    private static final String KEY_CLASSPATH = "classpath";
    private static final String KEY_SOURCE_PREFIX = "src.";

    private final File cacheFile;

    CompilationCache(File outputDirectory) {
        this.cacheFile = new File(outputDirectory, CACHE_FILE_NAME);
    }

    /**
     * Returns {@code true} when the cache fingerprint matches the current
     * source files, classpath, and compiler options — meaning no recompile is
     * needed.
     *
     * @param sourceFiles       the source files to check
     * @param classpathEntries  the classpath entries used for compilation
     * @param compilerOptions   the compiler options list
     * @return {@code true} if up to date
     */
    boolean isUpToDate(
            List<File> sourceFiles,
            List<File> classpathEntries,
            List<String> compilerOptions
    ) {
        if (!cacheFile.isFile()) {
            return false;
        }
        Properties stored = loadProperties();
        if (stored == null) {
            return false;
        }
        String storedOptions = stored.getProperty(KEY_OPTIONS, "");
        if (!optionsFingerprint(compilerOptions).equals(storedOptions)) {
            return false;
        }
        String storedClasspath = stored.getProperty(KEY_CLASSPATH, "");
        if (!classpathFingerprint(classpathEntries).equals(storedClasspath)) {
            return false;
        }
        List<File> sorted = sortedUnique(sourceFiles);
        if (sorted.size() != countSourceKeys(stored)) {
            return false;
        }
        for (int i = 0; i < sorted.size(); i++) {
            File file = sorted.get(i);
            String key = KEY_SOURCE_PREFIX + normalizedPath(file);
            String storedTs = stored.getProperty(key);
            if (storedTs == null) {
                return false;
            }
            long lastModified = file.lastModified();
            if (lastModified == 0L || !String.valueOf(lastModified).equals(storedTs)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Saves a new cache fingerprint after a successful compilation.
     *
     * @param sourceFiles       the source files that were compiled
     * @param classpathEntries  the classpath entries used
     * @param compilerOptions   the compiler options list
     */
    void save(
            List<File> sourceFiles,
            List<File> classpathEntries,
            List<String> compilerOptions
    ) {
        Properties props = new Properties();
        props.setProperty(KEY_OPTIONS, optionsFingerprint(compilerOptions));
        props.setProperty(KEY_CLASSPATH, classpathFingerprint(classpathEntries));
        List<File> sorted = sortedUnique(sourceFiles);
        for (int i = 0; i < sorted.size(); i++) {
            File file = sorted.get(i);
            props.setProperty(KEY_SOURCE_PREFIX + normalizedPath(file),
                    String.valueOf(file.lastModified()));
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(cacheFile);
            props.store(out, "javaspec compile cache — do not edit");
        } catch (IOException ignored) {
            // Cache write failures are non-fatal; next compile will miss and recompile.
        } finally {
            closeQuietly(out);
        }
    }

    /** Invalidates (deletes) the cache file so the next compile is forced. */
    void invalidate() {
        if (cacheFile.isFile()) {
            cacheFile.delete();
        }
    }

    // -------------------------------------------------------------------------
    // Internals

    private Properties loadProperties() {
        Properties props = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(cacheFile);
            props.load(in);
            return props;
        } catch (IOException ex) {
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    private static int countSourceKeys(Properties props) {
        int count = 0;
        for (Object key : props.keySet()) {
            if (key instanceof String && ((String) key).startsWith(KEY_SOURCE_PREFIX)) {
                count++;
            }
        }
        return count;
    }

    private static String optionsFingerprint(List<String> options) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) {
                builder.append('\u0000');
            }
            builder.append(options.get(i));
        }
        return builder.toString();
    }

    private static String classpathFingerprint(List<File> entries) {
        List<File> sorted = sortedUnique(entries);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                builder.append('\u0000');
            }
            builder.append(normalizedPath(sorted.get(i)));
        }
        return builder.toString();
    }

    private static List<File> sortedUnique(List<File> files) {
        List<String> seen = new ArrayList<String>();
        List<File> result = new ArrayList<File>();
        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            if (f == null) {
                continue;
            }
            String key = normalizedPath(f);
            if (!seen.contains(key)) {
                seen.add(key);
                result.add(f);
            }
        }
        Collections.sort(result, new java.util.Comparator<File>() {
            public int compare(File a, File b) {
                return normalizedPath(a).compareTo(normalizedPath(b));
            }
        });
        return result;
    }

    private static String normalizedPath(File file) {
        return file.toPath().toAbsolutePath().normalize().toString();
    }

    private static void closeQuietly(Object closeable) {
        if (closeable instanceof InputStream) {
            try {
                ((InputStream) closeable).close();
            } catch (IOException ignored) { /* ignore */ }
        } else if (closeable instanceof OutputStream) {
            try {
                ((OutputStream) closeable).close();
            } catch (IOException ignored) { /* ignore */ }
        }
    }
}
