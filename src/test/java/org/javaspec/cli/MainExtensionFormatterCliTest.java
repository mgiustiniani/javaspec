package org.javaspec.cli;

import org.javaspec.fixtures.extension.ServiceLoadedRunFormatter;
import org.javaspec.formatter.RunFormatter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainExtensionFormatterCliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runLoadsExternalFormatterFromExplicitClasspathServiceDirectory() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("external-cli-source-root");
        File serviceRoot = serviceRootWithFormatter();

        CommandResult result = run(
                "run",
                "--classpath", serviceRoot.getAbsolutePath(),
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_passes",
                "--formatter", "external"
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Found 1 specification(s) in " + testJavaRoot().getAbsolutePath() + "."));
        assertTrue(result.out.contains("external formatter total=1 passed=1 failed=0"));
        assertFalse(result.out.contains("Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
        assertEquals("", result.err);
    }

    @Test
    public void runUsesExternalFormatterSelectedByConfigurationWithExplicitClasspathServiceDirectory() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("external-config-cli-source-root");
        File serviceRoot = serviceRootWithFormatter();
        File configFile = temporaryFolder.newFile("external-formatter.conf");
        writeFile(configFile, "formatter=external\n");

        CommandResult result = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--classpath", serviceRoot.getAbsolutePath(),
                "--spec-dir", testJavaRoot().getAbsolutePath(),
                "--source-dir", sourceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.FailingSubject",
                "--example", "it_passes"
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("external formatter total=1 passed=1 failed=0"));
        assertEquals("", result.err);
    }

    @Test
    public void invalidFormatterListsExternalFormatterNamesWhenServiceProviderIsLoaded() throws Exception {
        File serviceRoot = serviceRootWithFormatter();

        CommandResult result = run("run", "--classpath", serviceRoot.getAbsolutePath(), "--formatter", "dots");

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Invalid formatter: dots. Valid values: progress, pretty, external."));
        assertTrue(result.err.contains("Usage:"));
    }

    private File serviceRootWithFormatter() throws Exception {
        File serviceRoot = temporaryFolder.newFolder("cli-service-root-" + System.nanoTime());
        writeService(serviceRoot, RunFormatter.class, ServiceLoadedRunFormatter.class);
        return serviceRoot;
    }

    private static void writeService(File serviceRoot, Class<?> serviceType, Class<?> providerType) throws Exception {
        File serviceFile = new File(new File(serviceRoot, "META-INF" + File.separator + "services"), serviceType.getName());
        File parent = serviceFile.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        Files.write(serviceFile.toPath(), (providerType.getName() + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static File testJavaRoot() {
        return new File(System.getProperty("user.dir"), "src/test/java");
    }

    private static CommandResult run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = Main.run(
                args,
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(out),
                new PrintStream(err)
        );
        return new CommandResult(
                exitCode,
                new String(out.toByteArray(), StandardCharsets.UTF_8),
                new String(err.toByteArray(), StandardCharsets.UTF_8)
        );
    }

    private static void writeFile(File file, String content) throws Exception {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String out;
        private final String err;

        private CommandResult(int exitCode, String out, String err) {
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }
    }
}
