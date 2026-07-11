package io.github.jvmspec.generation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Finds {@code // javaspec:stub} markers left in generated production method bodies.
 *
 * <p>Every stub the generator inserts carries the marker as its first body line. Hand
 * implementation of the method (the GREEN step of a slice) removes the marker together with the
 * default body, so the remaining markers are exactly the stubs still pending implementation.</p>
 */
public final class StubMarkerScanner {
    /** Marker comment emitted as the first line of every generated stub body. */
    public static final String STUB_MARKER = "// javaspec:stub";

    private StubMarkerScanner() {
    }

    /**
     * Scans all {@code .java} files under {@code sourceRoot} for stub markers.
     *
     * @return one location per marker occurrence, ordered by file path and line number
     */
    public static List<StubLocation> scan(File sourceRoot) throws IOException {
        Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");
        List<StubLocation> locations = new ArrayList<StubLocation>();
        if (!sourceRoot.isDirectory()) {
            return locations;
        }
        List<File> javaFiles = new ArrayList<File>();
        collectJavaFiles(sourceRoot, javaFiles);
        Collections.sort(javaFiles);
        for (int i = 0; i < javaFiles.size(); i++) {
            File javaFile = javaFiles.get(i);
            String source = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
            String[] lines = source.split("\n", -1);
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                if (lines[lineIndex].contains(STUB_MARKER)) {
                    locations.add(new StubLocation(javaFile, lineIndex + 1));
                }
            }
        }
        return locations;
    }

    private static void collectJavaFiles(File directory, List<File> javaFiles) {
        File[] entries = directory.listFiles();
        if (entries == null) {
            return;
        }
        for (int i = 0; i < entries.length; i++) {
            File entry = entries[i];
            if (entry.isDirectory()) {
                collectJavaFiles(entry, javaFiles);
            } else if (entry.getName().endsWith(".java")) {
                javaFiles.add(entry);
            }
        }
    }

    /** A single pending stub occurrence: source file and 1-based line number of the marker. */
    public static final class StubLocation {
        private final File file;
        private final int line;

        StubLocation(File file, int line) {
            this.file = file;
            this.line = line;
        }

        public File file() {
            return file;
        }

        public int line() {
            return line;
        }

        public String toString() {
            return file.getPath() + ":" + line;
        }
    }
}
