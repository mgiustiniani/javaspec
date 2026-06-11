package org.javaspec.cli;

import org.javaspec.bootstrap.BootstrapContext;
import org.javaspec.bootstrap.BootstrapHook;
import org.javaspec.fixtures.cli.BootstrapEventLog;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainPhase33BootstrapDiscoveryCliTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void resetBootstrapEvents() {
        BootstrapEventLog.reset();
    }

    @Test
    public void configEnabledBootstrapDiscoveryExecutesServiceLoaderHookBeforeExamples() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("discovery-cli-source-root");
        File serviceRoot = serviceRootWithProviders(DiscoveredCliBootstrapHook.class);
        File configFile = writeConfig("bootstrap-discovery.conf",
                suiteConfig("custom", testJavaRoot(), sourceRoot) +
                        "bootstrapDiscovery = true\n");

        CommandResult result = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--classpath", serviceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.BootstrapSubject",
                "--example", "it_runs_after_bootstrap"
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Found 1 specification(s) in " + testJavaRoot().getPath() + "."));
        assertTrue(result.out.contains("Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
        assertEquals("", result.err);
        assertEquals(Arrays.asList("discovered:1", "example"), BootstrapEventLog.events());
    }

    @Test
    public void discoveredBootstrapFailureExitsUsageAndDoesNotWriteReports() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("failing-discovery-cli-source-root");
        File serviceRoot = serviceRootWithProviders(FailingDiscoveredCliBootstrapHook.class);
        File jsonReport = new File(temporaryFolder.getRoot(), "reports/bootstrap-discovery-failure.json");
        File junitXmlReport = new File(temporaryFolder.getRoot(), "reports/bootstrap-discovery-failure.xml");
        File configFile = writeConfig("failing-bootstrap-discovery.conf",
                suiteConfig("custom", testJavaRoot(), sourceRoot) +
                        "bootstrapDiscovery = true\n");

        CommandResult result = run(
                "run",
                "--config", configFile.getAbsolutePath(),
                "--suite", "custom",
                "--classpath", serviceRoot.getAbsolutePath(),
                "--class", "org.javaspec.fixtures.cli.BootstrapSubject",
                "--example", "it_runs_after_bootstrap",
                "--report", jsonReport.getAbsolutePath(),
                "--junit-xml", junitXmlReport.getAbsolutePath()
        );

        assertEquals(64, result.exitCode);
        assertTrue(result.err.contains("Error: Bootstrap execution failed"));
        assertTrue(result.err.contains(FailingDiscoveredCliBootstrapHook.class.getName()));
        assertTrue(result.err.contains("phase 33 CLI discovered bootstrap failure"));
        assertFalse(result.out.contains("Examples:"));
        assertEquals(Arrays.asList("discovered-failing:1"), BootstrapEventLog.events());
        assertFalse(jsonReport.exists());
        assertFalse(junitXmlReport.exists());
    }

    @Test
    public void verboseOutputShowsBootstrapDiscoveryOnlyWhenEnabled() throws Exception {
        File enabledSpecRoot = temporaryFolder.newFolder("enabled-verbose-discovery-spec-root");
        File enabledSourceRoot = temporaryFolder.newFolder("enabled-verbose-discovery-source-root");
        File enabledConfig = writeConfig("enabled-verbose-discovery.conf",
                suiteConfig("custom", enabledSpecRoot, enabledSourceRoot) +
                        "bootstrapDiscovery = true\n");
        File disabledSpecRoot = temporaryFolder.newFolder("disabled-verbose-discovery-spec-root");
        File disabledSourceRoot = temporaryFolder.newFolder("disabled-verbose-discovery-source-root");
        File disabledConfig = writeConfig("disabled-verbose-discovery.conf",
                suiteConfig("custom", disabledSpecRoot, disabledSourceRoot));

        CommandResult enabled = run("run", "--config", enabledConfig.getAbsolutePath(), "--suite", "custom", "--verbose");
        CommandResult disabled = run("run", "--config", disabledConfig.getAbsolutePath(), "--suite", "custom", "--verbose");

        assertEquals(0, enabled.exitCode);
        assertTrue(enabled.out.contains("Run configuration:"));
        assertTrue(enabled.out.contains("  Bootstrap discovery: true"));
        assertEquals("", enabled.err);
        assertEquals(0, disabled.exitCode);
        assertTrue(disabled.out.contains("Run configuration:"));
        assertFalse(disabled.out.contains("  Bootstrap discovery: true"));
        assertEquals("", disabled.err);
    }

    public static final class DiscoveredCliBootstrapHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            BootstrapEventLog.record("discovered:" + context.specs().size());
        }
    }

    public static final class FailingDiscoveredCliBootstrapHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            BootstrapEventLog.record("discovered-failing:" + context.specs().size());
            throw new IllegalStateException("phase 33 CLI discovered bootstrap failure");
        }
    }

    private File serviceRootWithProviders(Class<?>... providerTypes) throws Exception {
        File serviceRoot = temporaryFolder.newFolder("cli-bootstrap-services-" + System.nanoTime());
        File serviceFile = new File(new File(serviceRoot, "META-INF" + File.separator + "services"),
                BootstrapHook.class.getName());
        File parent = serviceFile.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < providerTypes.length; i++) {
            content.append(providerTypes[i].getName()).append('\n');
        }
        Files.write(serviceFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
        return serviceRoot;
    }

    private File writeConfig(String fileName, String content) throws Exception {
        File configFile = temporaryFolder.newFile(fileName);
        writeFile(configFile, content);
        return configFile;
    }

    private static String suiteConfig(String suiteName, File specRoot, File sourceRoot) {
        return "defaultSuite=" + suiteName + "\n" +
                "suite." + suiteName + ".specDir=" + specRoot.getAbsolutePath() + "\n" +
                "suite." + suiteName + ".sourceDir=" + sourceRoot.getAbsolutePath() + "\n";
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
        File parent = file.getParentFile();
        assertTrue(parent == null || parent.isDirectory() || parent.mkdirs());
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
