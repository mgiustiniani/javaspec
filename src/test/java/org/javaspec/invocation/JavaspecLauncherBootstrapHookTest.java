package org.javaspec.invocation;

import org.javaspec.bootstrap.BootstrapContext;
import org.javaspec.bootstrap.BootstrapException;
import org.javaspec.bootstrap.BootstrapHook;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecExample;
import org.javaspec.model.DescribedClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JavaspecLauncherBootstrapHookTest {
    private static final List<String> events = new ArrayList<String>();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runsBootstrapHooksBeforeExamplesAndPassesSpecs() {
        ClassLoader classLoader = currentClassLoader();
        List<DiscoveredSpec> specs = Arrays.asList(spec(SequencedSpec.class, "it_runs_after_bootstrap"));
        RecordingBootstrapHook.reset();
        events.clear();

        JavaspecInvocationResult result = JavaspecLauncher.run(
                JavaspecInvocation.forSpecs(specs, classLoader)
                        .withBootstrapHook(RecordingBootstrapHook.class.getName())
        );

        assertEquals(0, result.exitCode());
        assertEquals(1, result.runResult().totalCount());
        assertEquals(1, result.runResult().passedCount());
        assertEquals(Arrays.asList("hook", "example"), events);
        assertEquals(specs, RecordingBootstrapHook.seenSpecs);
        assertSame(classLoader, RecordingBootstrapHook.seenClassLoader);
        assertEquals(specs, result.discoveredSpecs());
    }

    @Test
    public void withBootstrapDiscoveryExecutesServiceLoaderProviderBeforeExamples() throws Exception {
        URLClassLoader classLoader = serviceClassLoader(LauncherDiscoveredHook.class);
        try {
            List<DiscoveredSpec> specs = Arrays.asList(spec(DiscoverySequencedSpec.class, "it_runs_after_discovered_bootstrap"));
            events.clear();

            JavaspecInvocationResult result = JavaspecLauncher.run(
                    JavaspecInvocation.forSpecs(specs, classLoader).withBootstrapDiscovery(true)
            );

            assertEquals(0, result.exitCode());
            assertEquals(1, result.runResult().passedCount());
            assertEquals(Arrays.asList("discovered", "example"), events);
        } finally {
            classLoader.close();
            events.clear();
        }
    }

    @Test
    public void defaultInvocationDoesNotExecuteServiceLoaderProviders() throws Exception {
        URLClassLoader classLoader = serviceClassLoader(LauncherDiscoveredHook.class);
        try {
            List<DiscoveredSpec> specs = Arrays.asList(spec(DefaultDiscoverySpec.class, "it_runs_without_discovered_bootstrap_by_default"));
            events.clear();

            JavaspecInvocationResult result = JavaspecLauncher.run(JavaspecInvocation.forSpecs(specs, classLoader));

            assertEquals(0, result.exitCode());
            assertEquals(1, result.runResult().passedCount());
            assertEquals(Arrays.asList("example"), events);
        } finally {
            classLoader.close();
            events.clear();
        }
    }

    @Test
    public void discoveredBootstrapFailurePreventsExamplesFromRunning() throws Exception {
        URLClassLoader classLoader = serviceClassLoader(FailingLauncherDiscoveredHook.class);
        try {
            List<DiscoveredSpec> specs = Arrays.asList(spec(FailureGuardSpec.class, "it_must_not_run_after_bootstrap_failure"));
            events.clear();

            BootstrapException exception = assertThrows(BootstrapException.class, new org.junit.function.ThrowingRunnable() {
                @Override
                public void run() {
                    JavaspecLauncher.run(JavaspecInvocation.forSpecs(specs, classLoader).withBootstrapDiscovery(true));
                }
            });

            assertTrue(exception.getMessage(), exception.getMessage().contains(FailingLauncherDiscoveredHook.class.getName()));
            assertTrue(exception.getMessage(), exception.getMessage().contains("launcher discovered failure"));
            assertEquals(Arrays.asList("discovered-failing"), events);
        } finally {
            classLoader.close();
            events.clear();
        }
    }

    public static final class RecordingBootstrapHook implements BootstrapHook {
        private static List<DiscoveredSpec> seenSpecs;
        private static ClassLoader seenClassLoader;

        static void reset() {
            seenSpecs = null;
            seenClassLoader = null;
        }

        @Override
        public void bootstrap(BootstrapContext context) {
            events.add("hook");
            seenSpecs = context.specs();
            seenClassLoader = context.classLoader();
        }
    }

    public static final class LauncherDiscoveredHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            events.add("discovered");
        }
    }

    public static final class FailingLauncherDiscoveredHook implements BootstrapHook {
        public void bootstrap(BootstrapContext context) {
            events.add("discovered-failing");
            throw new IllegalStateException("launcher discovered failure");
        }
    }

    public static final class SequencedSpec {
        public void it_runs_after_bootstrap() {
            if (RecordingBootstrapHook.seenSpecs == null) {
                throw new AssertionError("bootstrap hook did not run before example");
            }
            events.add("example");
        }
    }

    public static final class DiscoverySequencedSpec {
        public void it_runs_after_discovered_bootstrap() {
            if (!events.contains("discovered")) {
                throw new AssertionError("discovered bootstrap hook did not run before example");
            }
            events.add("example");
        }
    }

    public static final class DefaultDiscoverySpec {
        public void it_runs_without_discovered_bootstrap_by_default() {
            if (events.contains("discovered")) {
                throw new AssertionError("ServiceLoader bootstrap hook should be disabled by default");
            }
            events.add("example");
        }
    }

    public static final class FailureGuardSpec {
        public void it_must_not_run_after_bootstrap_failure() {
            events.add("example");
            throw new AssertionError("example ran after bootstrap discovery failure");
        }
    }

    private URLClassLoader serviceClassLoader(Class<?>... providerTypes) throws Exception {
        File serviceRoot = temporaryFolder.newFolder("launcher-bootstrap-services-" + System.nanoTime());
        File serviceFile = new File(new File(serviceRoot, "META-INF" + File.separator + "services"),
                BootstrapHook.class.getName());
        File parent = serviceFile.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < providerTypes.length; i++) {
            content.append(providerTypes[i].getName()).append('\n');
        }
        Files.write(serviceFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
        return new URLClassLoader(new URL[] {serviceRoot.toURI().toURL()}, currentClassLoader());
    }

    private static DiscoveredSpec spec(Class<?> specClass, String exampleMethodName) {
        return DiscoveredSpec.of(
                new File(specClass.getName().replace('.', File.separatorChar) + ".java"),
                specClass.getName(),
                DescribedClass.of(specClass.getName()),
                Arrays.asList(SpecExample.of(exampleMethodName, 0))
        );
    }

    private static ClassLoader currentClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return JavaspecLauncherBootstrapHookTest.class.getClassLoader();
    }
}
