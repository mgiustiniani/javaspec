package org.javaspec.invocation;

import org.javaspec.bootstrap.BootstrapContext;
import org.javaspec.bootstrap.BootstrapHook;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecExample;
import org.javaspec.model.DescribedClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class JavaspecLauncherBootstrapHookTest {
    private static final List<String> events = new ArrayList<String>();

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

    public static final class SequencedSpec {
        public void it_runs_after_bootstrap() {
            if (RecordingBootstrapHook.seenSpecs == null) {
                throw new AssertionError("bootstrap hook did not run before example");
            }
            events.add("example");
        }
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
