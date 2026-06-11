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
import static org.junit.Assert.assertNull;
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
    public void withBootstrapDiscoveryTogglesFlagWithoutMutatingOriginal() {
        ClassLoader classLoader = currentClassLoader();
        List<DiscoveredSpec> specs = Arrays.asList(spec("spec.example.DiscoveryInvocationSpec", "example.DiscoveryInvocation", "it_runs"));
        JavaspecInvocation base = JavaspecInvocation.forSpecs(specs, classLoader)
                .withBootstrapHook("example.Hook")
                .withExtension("example.Extension")
                .withStopOnFailure(true);

        JavaspecInvocation enabled = base.withBootstrapDiscovery(true);
        JavaspecInvocation disabledAgain = enabled.withBootstrapDiscovery(false);

        assertFalse(base.bootstrapDiscovery());
        assertFalse(base.isBootstrapDiscoveryEnabled());
        assertFalse(base.getBootstrapDiscovery());
        assertTrue(enabled.bootstrapDiscovery());
        assertTrue(enabled.isBootstrapDiscoveryEnabled());
        assertTrue(enabled.getBootstrapDiscovery());
        assertFalse(disabledAgain.bootstrapDiscovery());
        assertEquals(Arrays.asList("example.Hook"), enabled.bootstrapHooks());
        assertEquals(Arrays.asList("example.Extension"), enabled.extensions());
        assertTrue(enabled.stopOnFailure());
        assertEquals(specs, enabled.discoveredSpecs());
        assertSame(classLoader, enabled.classLoader());
    }

    @Test
    public void extensionsAreTrimmedDefensivelyCopiedUnmodifiableAndPreserveDuplicates() {
        ClassLoader classLoader = currentClassLoader();
        DiscoveredSpec spec = spec("spec.example.ExtensionInvocationSpec", "example.ExtensionInvocation", "it_runs");
        List<DiscoveredSpec> specs = Arrays.asList(spec);
        List<String> extensions = new ArrayList<String>();
        extensions.add("  example.FirstExtension  ");
        extensions.add("example.SecondExtension");
        extensions.add("\texample.FirstExtension\n");
        JavaspecInvocation base = JavaspecInvocation.forSpecs(specs, classLoader).withBootstrapHook("example.Hook");

        JavaspecInvocation invocation = base.withExtensions(extensions);
        extensions.set(0, "example.ChangedExtension");
        extensions.add("example.AddedExtension");
        JavaspecInvocation appended = invocation.withExtension(" example.ThirdExtension ");

        assertEquals(Collections.<String>emptyList(), base.extensions());
        assertEquals(Arrays.asList("example.FirstExtension", "example.SecondExtension", "example.FirstExtension"),
                invocation.extensions());
        assertSame(invocation.extensions(), invocation.getExtensions());
        assertEquals(Arrays.asList("example.FirstExtension", "example.SecondExtension", "example.FirstExtension",
                "example.ThirdExtension"), appended.extensions());
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.extensions().add("example.OtherExtension");
            }
        });
        assertEquals(Arrays.asList("example.Hook"), invocation.bootstrapHooks());
        assertEquals(specs, invocation.discoveredSpecs());
        assertSame(classLoader, invocation.classLoader());
    }

    @Test
    public void rejectsNullAndBlankExtensionsWithIndexedMessages() {
        final JavaspecInvocation invocation = JavaspecInvocation.forSpecs(
                Collections.<DiscoveredSpec>emptyList(),
                currentClassLoader()
        );

        NullPointerException missingExtensions = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withExtensions(null);
            }
        });
        assertEquals("extensions must not be null", missingExtensions.getMessage());

        NullPointerException missingExtensionName = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withExtensions(Arrays.asList("example.ValidExtension", null));
            }
        });
        assertEquals("extensions[1] must not be null", missingExtensionName.getMessage());

        IllegalArgumentException blankExtensionName = assertThrows(IllegalArgumentException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withExtensions(Arrays.asList("example.ValidExtension", "  \t  "));
            }
        });
        assertEquals("extensions[1] must not be blank.", blankExtensionName.getMessage());
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

    @Test
    public void compilationDefaultsDisabledAndWithersPreserveCompilationSettings() {
        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(new File("specs"));
        ClassLoader classLoader = currentClassLoader();
        JavaspecInvocation base = JavaspecInvocation.discovering(request, classLoader);

        assertFalse(base.compilationEnabled());
        assertFalse(base.isCompilationEnabled());
        assertEquals(Collections.<File>emptyList(), base.compilationSourceRoots());
        assertSame(base.compilationSourceRoots(), base.getCompilationSourceRoots());
        assertNull(base.compilationOutputDirectory());
        assertNull(base.getCompilationOutputDirectory());
        assertEquals(Collections.<File>emptyList(), base.compilationClasspathEntries());
        assertSame(base.compilationClasspathEntries(), base.getCompilationClasspathEntries());

        File sourceRootWithWhitespace = new File("  src/main/java  ");
        File specRoot = new File("specs");
        File outputDirectory = new File("target/phase34-classes");
        File classpathEntry = new File("target/classes");
        List<File> sourceRoots = new ArrayList<File>();
        sourceRoots.add(sourceRootWithWhitespace);
        sourceRoots.add(specRoot);
        List<File> classpathEntries = new ArrayList<File>();
        classpathEntries.add(classpathEntry);

        JavaspecInvocation compiled = base.withCompilation(sourceRoots, outputDirectory, classpathEntries);
        sourceRoots.add(new File("changed-source-root"));
        classpathEntries.add(new File("changed-classpath-entry"));

        assertTrue(compiled.compilationEnabled());
        assertTrue(compiled.isCompilationEnabled());
        assertEquals(Arrays.asList(sourceRootWithWhitespace, specRoot), compiled.compilationSourceRoots());
        assertEquals(outputDirectory, compiled.compilationOutputDirectory());
        assertSame(compiled.compilationOutputDirectory(), compiled.getCompilationOutputDirectory());
        assertEquals(Arrays.asList(classpathEntry), compiled.compilationClasspathEntries());
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                compiled.compilationSourceRoots().add(new File("other"));
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                compiled.compilationClasspathEntries().add(new File("other"));
            }
        });

        JavaspecInvocation preserved = compiled
                .withBootstrapHook("example.Hook")
                .withBootstrapDiscovery(true)
                .withExtension("example.Extension")
                .withStopOnFailure(true);

        assertTrue(preserved.compilationEnabled());
        assertEquals(Arrays.asList(sourceRootWithWhitespace, specRoot), preserved.compilationSourceRoots());
        assertEquals(outputDirectory, preserved.compilationOutputDirectory());
        assertEquals(Arrays.asList(classpathEntry), preserved.compilationClasspathEntries());
        assertEquals(Arrays.asList("example.Hook"), preserved.bootstrapHooks());
        assertTrue(preserved.bootstrapDiscovery());
        assertEquals(Arrays.asList("example.Extension"), preserved.extensions());
        assertTrue(preserved.stopOnFailure());
        assertSame(request, preserved.discoveryRequest());
        assertSame(classLoader, preserved.classLoader());

        JavaspecInvocation disabled = preserved.withoutCompilation();

        assertFalse(disabled.compilationEnabled());
        assertEquals(Collections.<File>emptyList(), disabled.compilationSourceRoots());
        assertNull(disabled.compilationOutputDirectory());
        assertEquals(Collections.<File>emptyList(), disabled.compilationClasspathEntries());
        assertEquals(Arrays.asList("example.Hook"), disabled.bootstrapHooks());
        assertTrue(disabled.bootstrapDiscovery());
        assertEquals(Arrays.asList("example.Extension"), disabled.extensions());
        assertTrue(disabled.stopOnFailure());
    }

    @Test
    public void withCompilationConvenienceOverloadOrdersSourceAndSpecRootsAndPreservesClasspath() {
        JavaspecInvocation base = JavaspecInvocation.forSpecs(Collections.<DiscoveredSpec>emptyList(), currentClassLoader());
        File sourceRoot = new File("source-root");
        File specRoot = new File("spec-root");
        File outputDirectory = new File("output-root");
        File classpathEntry = new File("dependency.jar");

        JavaspecInvocation invocation = base.withCompilation(
                sourceRoot,
                specRoot,
                outputDirectory,
                Arrays.asList(classpathEntry)
        );

        assertTrue(invocation.compilationEnabled());
        assertEquals(Arrays.asList(sourceRoot, specRoot), invocation.compilationSourceRoots());
        assertEquals(outputDirectory, invocation.compilationOutputDirectory());
        assertEquals(Arrays.asList(classpathEntry), invocation.compilationClasspathEntries());
    }

    @Test
    public void rejectsInvalidCompilationInputsWithIndexedMessages() {
        final JavaspecInvocation invocation = JavaspecInvocation.forSpecs(
                Collections.<DiscoveredSpec>emptyList(),
                currentClassLoader()
        );
        final File outputDirectory = new File("output-root");
        final List<File> validSourceRoots = Arrays.asList(new File("source-root"));
        final List<File> validClasspathEntries = Arrays.asList(new File("dependency.jar"));

        NullPointerException missingSourceRoots = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withCompilation(null, outputDirectory, validClasspathEntries);
            }
        });
        assertEquals("compilationSourceRoots must not be null", missingSourceRoots.getMessage());

        NullPointerException missingSourceRootItem = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withCompilation(Arrays.asList(new File("source-root"), null), outputDirectory, validClasspathEntries);
            }
        });
        assertEquals("compilationSourceRoots[1] must not be null", missingSourceRootItem.getMessage());

        NullPointerException missingOutput = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withCompilation(validSourceRoots, null, validClasspathEntries);
            }
        });
        assertEquals("compilationOutputDirectory must not be null", missingOutput.getMessage());

        NullPointerException missingClasspathEntries = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withCompilation(validSourceRoots, outputDirectory, null);
            }
        });
        assertEquals("compilationClasspathEntries must not be null", missingClasspathEntries.getMessage());

        NullPointerException missingClasspathItem = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withCompilation(validSourceRoots, outputDirectory, Arrays.asList(new File("dependency.jar"), null));
            }
        });
        assertEquals("compilationClasspathEntries[1] must not be null", missingClasspathItem.getMessage());

        NullPointerException missingConvenienceSourceRoot = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withCompilation(null, new File("spec-root"), outputDirectory, validClasspathEntries);
            }
        });
        assertEquals("sourceRoot must not be null", missingConvenienceSourceRoot.getMessage());

        NullPointerException missingConvenienceSpecRoot = assertThrows(NullPointerException.class, new ThrowingRunnableAdapter() {
            @Override
            public void run() {
                invocation.withCompilation(new File("source-root"), null, outputDirectory, validClasspathEntries);
            }
        });
        assertEquals("specRoot must not be null", missingConvenienceSpecRoot.getMessage());
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
