package org.javaspec.invocation;

import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.discovery.SpecExample;
import org.javaspec.model.DescribedClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JavaspecInvocationTest {
    @Test
    public void bootstrapHooksAreTrimmedDefensivelyCopiedUnmodifiableAndPreserveDuplicates() {
        ClassLoader classLoader = currentClassLoader();
        DiscoveredSpec spec = spec("spec.example.InvocationSpec", "example.Invocation", "it_runs");
        List<DiscoveredSpec> specs = Arrays.asList(spec);
        List<String> hooks = new ArrayList<String>();
        hooks.add("  example.FirstHook  ");
        hooks.add("example.SecondHook");
        hooks.add("\texample.FirstHook\n");
        JavaspecInvocation base = JavaspecInvocation.forSpecs(specs, classLoader).stoppingOnFailure();

        JavaspecInvocation invocation = base.withBootstrapHooks(hooks);
        hooks.set(0, "example.ChangedHook");
        hooks.add("example.AddedHook");

        assertEquals(Collections.<String>emptyList(), base.bootstrapHooks());
        assertEquals(Arrays.asList("example.FirstHook", "example.SecondHook", "example.FirstHook"),
                invocation.bootstrapHooks());
        assertSame(invocation.bootstrapHooks(), invocation.bootstrap());
        assertSame(invocation.bootstrapHooks(), invocation.getBootstrapHooks());
        assertSame(invocation.bootstrapHooks(), invocation.getBootstrap());
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.bootstrapHooks().add("example.OtherHook");
            }
        });
        assertTrue(invocation.stopOnFailure());
        assertEquals(specs, invocation.discoveredSpecs());
        assertSame(classLoader, invocation.classLoader());
    }

    @Test
    public void withBootstrapHookAppendsWithoutMutatingOriginalAndPreservesDiscoveryRequest() {
        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(new File("src/test/java"));
        JavaspecInvocation base = JavaspecInvocation.discovering(request, currentClassLoader());

        JavaspecInvocation oneHook = base.withBootstrapHook(" example.FirstHook ");
        JavaspecInvocation twoHooks = oneHook.withBootstrapHook("example.SecondHook");

        assertEquals(Collections.<String>emptyList(), base.bootstrapHooks());
        assertEquals(Arrays.asList("example.FirstHook"), oneHook.bootstrapHooks());
        assertEquals(Arrays.asList("example.FirstHook", "example.SecondHook"), twoHooks.bootstrapHooks());
        assertSame(request, twoHooks.discoveryRequest());
        assertSame(request, twoHooks.getDiscoveryRequest());
        assertTrue(twoHooks.hasDiscoveryRequest());
        assertFalse(twoHooks.hasDiscoveredSpecs());
    }

    @Test
    public void rejectsNullAndBlankBootstrapHooksWithIndexedMessages() {
        final JavaspecInvocation invocation = JavaspecInvocation.forSpecs(
                Collections.<DiscoveredSpec>emptyList(),
                currentClassLoader()
        );

        NullPointerException missingHooks = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withBootstrapHooks(null);
            }
        });
        assertEquals("bootstrapHooks must not be null", missingHooks.getMessage());

        NullPointerException missingHookName = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withBootstrapHooks(Arrays.asList("example.ValidHook", null));
            }
        });
        assertEquals("bootstrapHooks[1] must not be null", missingHookName.getMessage());

        IllegalArgumentException blankHookName = assertThrows(IllegalArgumentException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withBootstrapHooks(Arrays.asList("example.ValidHook", "  \t  "));
            }
        });
        assertEquals("bootstrapHooks[1] must not be blank.", blankHookName.getMessage());
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
        return JavaspecInvocationTest.class.getClassLoader();
    }

    private abstract static class ThrowingRunnableAdapter implements org.junit.function.ThrowingRunnable {
    }
}
