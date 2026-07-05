package io.github.jvmspec.bootstrap;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecExample;
import io.github.jvmspec.model.DescribedClass;
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
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BootstrapRunnerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void runsHooksInDeclarationOrderIncludingDuplicatesWithContextClassLoaderAndSpecs() {
        ClassLoader originalClassLoader = new ChildOnlyClassLoader(currentClassLoader());
        ClassLoader executionClassLoader = new ChildOnlyClassLoader(currentClassLoader());
        Thread thread = Thread.currentThread();
        ClassLoader previousClassLoader = thread.getContextClassLoader();
        List<DiscoveredSpec> specs = Arrays.asList(spec("spec.example.OrderSpec", "example.Order", "it_runs"));
        RecordingHooks.reset(executionClassLoader, specs);
        thread.setContextClassLoader(originalClassLoader);
        try {
            BootstrapRunner.run(Arrays.asList(
                    FirstHook.class.getName(),
                    SecondHook.class.getName(),
                    FirstHook.class.getName()
            ), executionClassLoader, specs);

            assertEquals(Arrays.asList(
                    "first:tccl=true:contextClassLoader=true:specs=true:count=1",
                    "second:tccl=true:contextClassLoader=true:specs=true:count=1",
                    "first:tccl=true:contextClassLoader=true:specs=true:count=1"
            ), RecordingHooks.events);
            assertSame(originalClassLoader, thread.getContextClassLoader());
        } finally {
            thread.setContextClassLoader(previousClassLoader);
            RecordingHooks.reset(null, Collections.<DiscoveredSpec>emptyList());
        }
    }

    @Test
    public void serviceLoaderDiscoveryRunsAfterExplicitHooksInClassNameOrderAndPreservesExplicitDuplicates() throws Exception {
        List<DiscoveredSpec> specs = Arrays.asList(spec("spec.example.DiscoveryOrderSpec", "example.DiscoveryOrder", "it_runs"));
        URLClassLoader executionClassLoader = serviceClassLoader(ZDiscoveredHook.class, ADiscoveredHook.class);
        try {
            RecordingHooks.reset(executionClassLoader, specs);

            BootstrapRunner.run(Arrays.asList(
                    FirstHook.class.getName(),
                    FirstHook.class.getName()
            ), executionClassLoader, specs, true);

            assertEquals(Arrays.asList(
                    "first:tccl=true:contextClassLoader=true:specs=true:count=1",
                    "first:tccl=true:contextClassLoader=true:specs=true:count=1",
                    "discovered-a:tccl=true:contextClassLoader=true:specs=true:count=1",
                    "discovered-z:tccl=true:contextClassLoader=true:specs=true:count=1"
            ), RecordingHooks.events);
        } finally {
            executionClassLoader.close();
            RecordingHooks.reset(null, Collections.<DiscoveredSpec>emptyList());
        }
    }

    @Test
    public void serviceLoaderDiscoveryDisabledLeavesProvidersUnexecuted() throws Exception {
        URLClassLoader executionClassLoader = serviceClassLoader(ADiscoveredHook.class);
        try {
            RecordingHooks.reset(executionClassLoader, Collections.<DiscoveredSpec>emptyList());

            BootstrapRunner.run(Collections.<String>emptyList(), executionClassLoader, Collections.<DiscoveredSpec>emptyList(), false);

            assertTrue(RecordingHooks.events.isEmpty());
        } finally {
            executionClassLoader.close();
            RecordingHooks.reset(null, Collections.<DiscoveredSpec>emptyList());
        }
    }

    @Test
    public void serviceLoaderDiscoveryEnabledWithNoProvidersIsNoOp() throws Exception {
        URLClassLoader executionClassLoader = serviceClassLoader(new String[0]);
        try {
            RecordingHooks.reset(executionClassLoader, Collections.<DiscoveredSpec>emptyList());

            BootstrapRunner.run(Collections.<String>emptyList(), executionClassLoader, Collections.<DiscoveredSpec>emptyList(), true);

            assertTrue(RecordingHooks.events.isEmpty());
        } finally {
            executionClassLoader.close();
            RecordingHooks.reset(null, Collections.<DiscoveredSpec>emptyList());
        }
    }

    @Test
    public void serviceLoaderProviderLoadingFailureIsWrappedInBootstrapException() throws Exception {
        URLClassLoader executionClassLoader = serviceClassLoader("example.MissingBootstrapHook");
        try {
            BootstrapException exception = assertThrows(BootstrapException.class, new ThrowingRunnableAdapter() {
                @Override
                public void run() {
                    BootstrapRunner.run(Collections.<String>emptyList(), executionClassLoader,
                            Collections.<DiscoveredSpec>emptyList(), true);
                }
            });

            assertTrue(exception.getMessage(), exception.getMessage().contains(
                    "Could not load ServiceLoader BootstrapHook provider 'example.MissingBootstrapHook'"));
            assertTrue(exception.getMessage(), exception.getMessage().contains(BootstrapHook.class.getName()));
        } finally {
            executionClassLoader.close();
        }
    }

    @Test
    public void discoveredProviderFailureIsWrappedInBootstrapException() throws Exception {
        URLClassLoader executionClassLoader = serviceClassLoader(FailingDiscoveredHook.class);
        try {
            RecordingHooks.reset(executionClassLoader, Collections.<DiscoveredSpec>emptyList());

            BootstrapException exception = assertThrows(BootstrapException.class, new ThrowingRunnableAdapter() {
                @Override
                public void run() {
                    BootstrapRunner.run(Collections.<String>emptyList(), executionClassLoader,
                            Collections.<DiscoveredSpec>emptyList(), true);
                }
            });

            assertTrue(exception.getMessage(), exception.getMessage().contains(
                    "ServiceLoader BootstrapHook provider '" + FailingDiscoveredHook.class.getName() + "' threw an exception"));
            assertTrue(exception.getMessage(), exception.getMessage().contains("discovered failure"));
            assertEquals(Arrays.asList("discovered-failing"), RecordingHooks.events);
        } finally {
            executionClassLoader.close();
            RecordingHooks.reset(null, Collections.<DiscoveredSpec>emptyList());
        }
    }

    @Test
    public void restoresContextClassLoaderWhenHookFails() {
        ClassLoader originalClassLoader = new ChildOnlyClassLoader(currentClassLoader());
        ClassLoader executionClassLoader = new ChildOnlyClassLoader(currentClassLoader());
        Thread thread = Thread.currentThread();
        ClassLoader previousClassLoader = thread.getContextClassLoader();
        RecordingHooks.reset(executionClassLoader, Collections.<DiscoveredSpec>emptyList());
        thread.setContextClassLoader(originalClassLoader);
        try {
            BootstrapException exception = assertThrows(BootstrapException.class, new ThrowingRunnableAdapter() {
                @Override
                public void run() {
                    BootstrapRunner.run(Arrays.asList(FailingHook.class.getName()),
                            executionClassLoader,
                            Collections.<DiscoveredSpec>emptyList());
                }
            });

            assertSame(originalClassLoader, thread.getContextClassLoader());
            assertTrue(exception.getMessage(), exception.getMessage().contains(FailingHook.class.getName()));
            assertTrue(exception.getMessage(), exception.getMessage().contains("root failure"));
            assertEquals(Arrays.asList("failing"), RecordingHooks.events);
        } finally {
            thread.setContextClassLoader(previousClassLoader);
            RecordingHooks.reset(null, Collections.<DiscoveredSpec>emptyList());
        }
    }

    @Test
    public void reportsDiagnosticsForInvalidHookConfiguration() {
        assertBootstrapException(
                Arrays.asList("  \t  "),
                "Bootstrap hook class name at index 0 must not be blank."
        );
        assertBootstrapException(
                Arrays.asList("example.DoesNotExist"),
                "Bootstrap hook 'example.DoesNotExist' was not found"
        );
        assertBootstrapException(
                Arrays.asList(NotABootstrapHook.class.getName()),
                "Bootstrap hook '" + NotABootstrapHook.class.getName() + "' does not implement " + BootstrapHook.class.getName()
        );
        assertBootstrapException(
                Arrays.asList(PrivateConstructorHook.class.getName()),
                "Bootstrap hook '" + PrivateConstructorHook.class.getName() + "' must declare a public no-argument constructor."
        );
        assertBootstrapException(
                Arrays.asList(ConstructorFailsHook.class.getName()),
                "Bootstrap hook '" + ConstructorFailsHook.class.getName() + "' constructor failed: constructor boom."
        );
    }

    private static void assertBootstrapException(final List<String> hookClassNames, final String expectedMessagePart) {
        BootstrapException exception = assertThrows(BootstrapException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                BootstrapRunner.run(hookClassNames, currentClassLoader(), Collections.<DiscoveredSpec>emptyList());
            }
        });
        assertTrue(exception.getMessage(), exception.getMessage().contains(expectedMessagePart));
    }

    private static DiscoveredSpec spec(String specQualifiedName, String describedClassName, String exampleMethodName) {
        return DiscoveredSpec.of(
                new File(specQualifiedName.replace('.', File.separatorChar) + ".java"),
                specQualifiedName,
                DescribedClass.of(describedClassName),
                Arrays.asList(SpecExample.of(exampleMethodName, 0))
        );
    }

    private URLClassLoader serviceClassLoader(Class<?>... providerTypes) throws Exception {
        String[] providerNames = new String[providerTypes.length];
        for (int i = 0; i < providerTypes.length; i++) {
            providerNames[i] = providerTypes[i].getName();
        }
        return serviceClassLoader(providerNames);
    }

    private URLClassLoader serviceClassLoader(String... providerNames) throws Exception {
        File serviceRoot = temporaryFolder.newFolder("bootstrap-service-root-" + System.nanoTime());
        if (providerNames.length > 0) {
            File serviceFile = new File(new File(serviceRoot, "META-INF" + File.separator + "services"),
                    BootstrapHook.class.getName());
            File parent = serviceFile.getParentFile();
            assertTrue(parent.isDirectory() || parent.mkdirs());
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < providerNames.length; i++) {
                content.append(providerNames[i]).append('\n');
            }
            Files.write(serviceFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
        }
        return new URLClassLoader(new URL[] {serviceRoot.toURI().toURL()}, currentClassLoader());
    }

    private static ClassLoader currentClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return BootstrapRunnerTest.class.getClassLoader();
    }

    public static final class FirstHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            RecordingHooks.record("first", context);
        }
    }

    public static final class SecondHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            RecordingHooks.record("second", context);
        }
    }

    public static final class FailingHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            RecordingHooks.events.add("failing");
            throw new IllegalStateException("outer failure", new IllegalArgumentException("root failure"));
        }
    }

    public static final class ADiscoveredHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            RecordingHooks.record("discovered-a", context);
        }
    }

    public static final class ZDiscoveredHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            RecordingHooks.record("discovered-z", context);
        }
    }

    public static final class FailingDiscoveredHook implements BootstrapHook {
        @Override
        public void bootstrap(BootstrapContext context) {
            RecordingHooks.events.add("discovered-failing");
            throw new IllegalStateException("discovered failure");
        }
    }

    public static final class PrivateConstructorHook implements BootstrapHook {
        private PrivateConstructorHook() {
        }

        @Override
        public void bootstrap(BootstrapContext context) {
        }
    }

    public static final class ConstructorFailsHook implements BootstrapHook {
        public ConstructorFailsHook() {
            throw new IllegalStateException("constructor boom");
        }

        @Override
        public void bootstrap(BootstrapContext context) {
        }
    }

    public static final class NotABootstrapHook {
    }

    private static final class RecordingHooks {
        private static final List<String> events = new ArrayList<String>();
        private static ClassLoader expectedClassLoader;
        private static List<DiscoveredSpec> expectedSpecs = Collections.emptyList();

        static void reset(ClassLoader classLoader, List<DiscoveredSpec> specs) {
            events.clear();
            expectedClassLoader = classLoader;
            expectedSpecs = specs;
        }

        static void record(String name, BootstrapContext context) {
            events.add(name
                    + ":tccl=" + (Thread.currentThread().getContextClassLoader() == expectedClassLoader)
                    + ":contextClassLoader=" + (context.classLoader() == expectedClassLoader)
                    + ":specs=" + context.specs().equals(expectedSpecs)
                    + ":count=" + context.specs().size());
        }
    }

    private static final class ChildOnlyClassLoader extends ClassLoader {
        ChildOnlyClassLoader(ClassLoader parent) {
            super(parent);
        }
    }

    private abstract static class ThrowingRunnableAdapter implements org.junit.function.ThrowingRunnable {
    }
}
