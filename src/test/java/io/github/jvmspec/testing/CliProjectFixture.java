package io.github.jvmspec.testing;

import io.github.jvmspec.cli.Main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Reusable black-box fixture for CLI projects with conventional production, spec, generated,
 * compilation, and report locations.
 */
public final class CliProjectFixture {
    private final File root;
    private final File sourceRoot;
    private final File specRoot;
    private final File generatedRoot;
    private final File classesRoot;

    private CliProjectFixture(File root) throws IOException {
        this.root = requireDirectory(root, "root");
        this.sourceRoot = createDirectory(new File(root, "src/main/java"));
        this.specRoot = createDirectory(new File(root, "src/test/java"));
        this.generatedRoot = createDirectory(new File(root, "target/generated-sources/javaspec"));
        this.classesRoot = createDirectory(new File(root, "target/javaspec-classes"));
    }

    public static CliProjectFixture create(File root) throws IOException {
        return new CliProjectFixture(root);
    }

    public File root() {
        return root;
    }

    public File sourceRoot() {
        return sourceRoot;
    }

    public File specRoot() {
        return specRoot;
    }

    public File generatedRoot() {
        return generatedRoot;
    }

    public File classesRoot() {
        return classesRoot;
    }

    public File report(String fileName) {
        return new File(root, fileName);
    }

    public File source(String qualifiedName, String body) throws IOException {
        return writeQualifiedSource(sourceRoot, qualifiedName, body);
    }

    public File spec(String qualifiedName, String body) throws IOException {
        return writeQualifiedSource(specRoot, qualifiedName, body);
    }

    public File generatedSource(String qualifiedName, String body) throws IOException {
        return writeQualifiedSource(generatedRoot, qualifiedName, body);
    }

    public RunResult run(String... arguments) {
        return runWithInput("", arguments);
    }

    public RunResult runAuthorized(String... arguments) {
        return runWithInput("y\n", arguments);
    }

    public RunResult runDenied(String... arguments) {
        return runWithInput("n\n", arguments);
    }

    public RunResult runWithInput(String input, String... arguments) {
        String[] effectiveArguments = Arrays.copyOf(arguments, arguments.length + 4);
        effectiveArguments[arguments.length] = "--spec-dir";
        effectiveArguments[arguments.length + 1] = specRoot.getAbsolutePath();
        effectiveArguments[arguments.length + 2] = "--source-dir";
        effectiveArguments[arguments.length + 3] = sourceRoot.getAbsolutePath();

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = Main.run(
                effectiveArguments,
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(stdout),
                new PrintStream(stderr)
        );
        return new RunResult(
                exitCode,
                new String(stdout.toByteArray(), StandardCharsets.UTF_8),
                new String(stderr.toByteArray(), StandardCharsets.UTF_8)
        );
    }

    public SourceSnapshot snapshot(File file) throws IOException {
        return SourceSnapshot.capture(file);
    }

    public String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    public static File writeQualifiedSource(File root, String qualifiedName, String body) throws IOException {
        int packageEnd = qualifiedName.lastIndexOf('.');
        String packageName = packageEnd < 0 ? "" : qualifiedName.substring(0, packageEnd);
        File file = new File(root, qualifiedName.replace('.', File.separatorChar) + ".java");
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create source directory " + parent);
        }
        String packageDeclaration = packageName.length() == 0 ? "" : "package " + packageName + ";\n";
        Files.write(file.toPath(), (packageDeclaration + body).getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static File requireDirectory(File directory, String fieldName) throws IOException {
        if (directory == null) {
            throw new NullPointerException(fieldName + " must not be null");
        }
        return createDirectory(directory);
    }

    private static File createDirectory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create directory " + directory);
        }
        return directory;
    }

    private static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (int i = 0; i < hash.length; i++) {
                result.append(String.format("%02x", Integer.valueOf(hash[i] & 0xff)));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    public static final class RunResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private RunResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int exitCode() {
            return exitCode;
        }

        public String stdout() {
            return stdout;
        }

        public String stderr() {
            return stderr;
        }
    }

    public static final class SourceSnapshot {
        private final File file;
        private final byte[] content;
        private final long modifiedTime;
        private final String sha256;

        private SourceSnapshot(File file, byte[] content, long modifiedTime) {
            this.file = file;
            this.content = content;
            this.modifiedTime = modifiedTime;
            this.sha256 = CliProjectFixture.sha256(content);
        }

        private static SourceSnapshot capture(File file) throws IOException {
            return new SourceSnapshot(file, Files.readAllBytes(file.toPath()), file.lastModified());
        }

        public byte[] content() {
            return content.clone();
        }

        public long modifiedTime() {
            return modifiedTime;
        }

        public String sha256() {
            return sha256;
        }

        public boolean isUnchanged() throws IOException {
            byte[] current = Files.readAllBytes(file.toPath());
            return modifiedTime == file.lastModified()
                    && sha256.equals(CliProjectFixture.sha256(current))
                    && Arrays.equals(content, current);
        }
    }
}
