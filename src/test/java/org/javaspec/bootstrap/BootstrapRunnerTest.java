package org.javaspec.bootstrap;

import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecExample;
import org.javaspec.model.DescribedClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BootstrapRunnerTest {
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
