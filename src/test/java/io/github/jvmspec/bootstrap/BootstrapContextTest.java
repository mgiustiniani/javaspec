package io.github.jvmspec.bootstrap;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecExample;
import io.github.jvmspec.model.DescribedClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class BootstrapContextTest {
    @Test
    public void defensivelyCopiesSpecsAndExposesUnmodifiableAliases() {
        ClassLoader classLoader = currentClassLoader();
        DiscoveredSpec first = spec("spec.example.FirstSpec", "example.First", "it_runs");
        DiscoveredSpec second = spec("spec.example.SecondSpec", "example.Second", "it_runs");
        List<DiscoveredSpec> source = new ArrayList<DiscoveredSpec>();
        source.add(first);

        BootstrapContext context = BootstrapContext.of(classLoader, source);
        source.add(second);

        assertSame(classLoader, context.classLoader());
        assertSame(classLoader, context.getClassLoader());
        assertEquals(Arrays.asList(first), context.discoveredSpecs());
        assertSame(context.discoveredSpecs(), context.specs());
        assertSame(context.discoveredSpecs(), context.getDiscoveredSpecs());
        assertSame(context.discoveredSpecs(), context.getSpecs());
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                context.discoveredSpecs().add(second);
            }
        });
    }

    @Test
    public void emptySpecsAreUnmodifiableAcrossAliases() {
        BootstrapContext context = BootstrapContext.of(currentClassLoader(), Collections.<DiscoveredSpec>emptyList());

        assertEquals(Collections.<DiscoveredSpec>emptyList(), context.discoveredSpecs());
        assertSame(context.discoveredSpecs(), context.specs());
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                context.specs().add(spec("spec.example.OtherSpec", "example.Other", "it_runs"));
            }
        });
    }

    @Test
    public void rejectsNullInputsWithIndexedMessages() {
        NullPointerException missingClassLoader = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                BootstrapContext.of(null, Collections.<DiscoveredSpec>emptyList());
            }
        });
        assertEquals("classLoader must not be null", missingClassLoader.getMessage());

        NullPointerException missingSpecs = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                BootstrapContext.of(currentClassLoader(), null);
            }
        });
        assertEquals("discoveredSpecs must not be null", missingSpecs.getMessage());

        final List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        specs.add(spec("spec.example.ValidSpec", "example.Valid", "it_runs"));
        specs.add(null);
        NullPointerException missingSpec = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                BootstrapContext.of(currentClassLoader(), specs);
            }
        });
        assertEquals("discoveredSpecs[1] must not be null", missingSpec.getMessage());
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
        return BootstrapContextTest.class.getClassLoader();
    }

    private abstract static class ThrowingRunnableAdapter implements org.junit.function.ThrowingRunnable {
    }
}
