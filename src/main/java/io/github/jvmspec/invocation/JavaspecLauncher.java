package io.github.jvmspec.invocation;

import io.github.jvmspec.bootstrap.BootstrapRunner;
import io.github.jvmspec.compilation.SourceCompilationException;
import io.github.jvmspec.compilation.SourceCompilationResult;
import io.github.jvmspec.compilation.SourceCompiler;
import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.extension.JavaspecExtensionLoader;
import io.github.jvmspec.formatter.RunFormatterRegistry;
import io.github.jvmspec.discovery.SpecDiscovery;
import io.github.jvmspec.discovery.SpecDiscoveryRequest;
import io.github.jvmspec.runner.RunResult;
import io.github.jvmspec.runner.SpecRunner;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Programmatic launcher over the canonical javaspec discovery and reflection runner.
 */
public final class JavaspecLauncher {
    public JavaspecLauncher() {
    }

    public static JavaspecInvocationResult launch(JavaspecInvocation invocation) {
        return run(invocation);
    }

    /**
     * Runs the invocation. When the invocation declares configured extensions, a run formatter
     * registry is built from the invocation classloader (built-in formatters first,
     * ServiceLoader-discovered providers next, configured extensions last in configured order,
     * duplicates preserved) before bootstrap hooks execute, and the activated registry is
     * exposed through {@link JavaspecInvocationResult#runFormatterRegistry()} so formatter
     * selection uses the same instance. When no extensions are configured, no registry is
     * built and behavior is unchanged. Bootstrap hook ServiceLoader discovery is opt-in through
     * {@link JavaspecInvocation#withBootstrapDiscovery(boolean)}; explicit hooks run before
     * discovered {@code io.github.jvmspec.bootstrap.BootstrapHook} providers, and discovered
     * providers come from the run classloader in deterministic implementation class name order.
     * Activation failures raise {@code io.github.jvmspec.extension.ExtensionLoadingException}
     * before any spec executes.
     *
     * <p>Programmatic source/spec compilation is disabled by default and must be enabled with
     * {@link JavaspecInvocation#withCompilation(List, File, List)}. When enabled, specs are
     * discovered first; an empty discovered spec list skips compilation. Otherwise the current
     * JDK {@code javax.tools.JavaCompiler} compiles the requested source roots after discovery
     * and before bootstrap hooks or examples. javaspec does not resolve dependencies, fork
     * {@code javac}, manage source/release levels, or maintain an incremental cache. Compiler
     * unavailability, compile failures, I/O errors, and security errors raise
     * {@link SourceCompilationException} before bootstrap hooks or examples run. After a
     * successful compile with at least one source file, bootstrap hooks and examples run with a
     * temporary {@link URLClassLoader} whose URLs contain the compilation output directory before
     * the invocation's compilation classpath entries and whose parent is the invocation
     * classloader. The caller-owned invocation classloader is never closed; the temporary loader
     * is closed after the run on a best-effort basis.</p>
     */
    public static JavaspecInvocationResult run(JavaspecInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation must not be null");
        RunFormatterRegistry runFormatterRegistry = activateConfiguredExtensions(invocation);
        List<DiscoveredSpec> specs = invocation.hasDiscoveredSpecs()
                ? invocation.discoveredSpecs()
                : SpecDiscovery.discover(invocation.discoveryRequest());
        SourceCompilationResult sourceCompilationResult = compileSourcesIfRequested(invocation, specs);
        URLClassLoader temporaryClassLoader = temporaryRunClassLoader(invocation, sourceCompilationResult);
        ClassLoader runClassLoader = temporaryClassLoader == null ? invocation.classLoader() : temporaryClassLoader;
        try {
            BootstrapRunner.run(
                    invocation.bootstrapHooks(),
                    runClassLoader,
                    specs,
                    invocation.bootstrapDiscovery()
            );
            RunResult runResult = SpecRunner.run(specs, runClassLoader, invocation.stopOnFailure());
            return JavaspecInvocationResult.of(
                    specs,
                    runResult,
                    exitCodeFor(runResult),
                    runFormatterRegistry,
                    sourceCompilationResult
            );
        } finally {
            closeQuietly(temporaryClassLoader);
        }
    }

    private static SourceCompilationResult compileSourcesIfRequested(
            JavaspecInvocation invocation,
            List<DiscoveredSpec> specs
    ) {
        if (!invocation.compilationEnabled() || specs.isEmpty()) {
            return null;
        }
        SourceCompilationResult result;
        try {
            result = SourceCompiler.compile(
                    invocation.compilationSourceRoots(),
                    invocation.compilationOutputDirectory(),
                    invocation.compilationClasspathEntries()
            );
        } catch (NoClassDefFoundError ex) {
            throw SourceCompilationException.compilerUnavailable(null, ex);
        } catch (IOException ex) {
            throw SourceCompilationException.ioError(invocation.compilationOutputDirectory(), ex);
        } catch (SecurityException ex) {
            throw SourceCompilationException.ioError(invocation.compilationOutputDirectory(), ex);
        }

        if (!result.compilerAvailable()) {
            throw SourceCompilationException.compilerUnavailable(result);
        }
        if (!result.successful()) {
            throw SourceCompilationException.compilationFailed(result);
        }
        return result;
    }

    private static URLClassLoader temporaryRunClassLoader(
            JavaspecInvocation invocation,
            SourceCompilationResult sourceCompilationResult
    ) {
        if (sourceCompilationResult == null || sourceCompilationResult.sourceFileCount() == 0) {
            return null;
        }
        List<File> entries = new ArrayList<File>();
        entries.add(sourceCompilationResult.outputDirectory());
        entries.addAll(invocation.compilationClasspathEntries());
        try {
            return new URLClassLoader(urlsFor(entries), invocation.classLoader());
        } catch (MalformedURLException ex) {
            throw SourceCompilationException.ioError(sourceCompilationResult, ex);
        } catch (SecurityException ex) {
            throw SourceCompilationException.ioError(sourceCompilationResult, ex);
        }
    }

    private static URL[] urlsFor(List<File> entries) throws MalformedURLException {
        URL[] urls = new URL[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            urls[i] = entries.get(i).toURI().toURL();
        }
        return urls;
    }

    private static void closeQuietly(URLClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException ignored) {
            // The temporary run classloader is best-effort cleanup only.
        }
    }

    private static RunFormatterRegistry activateConfiguredExtensions(JavaspecInvocation invocation) {
        if (invocation.extensions().isEmpty()) {
            return null;
        }
        return JavaspecExtensionLoader.loadRunFormatterRegistry(invocation.classLoader(), invocation.extensions());
    }

    public static JavaspecInvocationResult invoke(JavaspecInvocation invocation) {
        return run(invocation);
    }

    public static JavaspecInvocationResult run(SpecDiscoveryRequest discoveryRequest, ClassLoader classLoader) {
        return run(JavaspecInvocation.of(discoveryRequest, classLoader));
    }

    public static JavaspecInvocationResult run(
            SpecDiscoveryRequest discoveryRequest,
            ClassLoader classLoader,
            boolean stopOnFailure
    ) {
        return run(JavaspecInvocation.of(discoveryRequest, classLoader).withStopOnFailure(stopOnFailure));
    }

    public static JavaspecInvocationResult run(List<DiscoveredSpec> specs, ClassLoader classLoader) {
        return run(JavaspecInvocation.of(specs, classLoader));
    }

    public static JavaspecInvocationResult run(List<DiscoveredSpec> specs, ClassLoader classLoader, boolean stopOnFailure) {
        return run(JavaspecInvocation.of(specs, classLoader).withStopOnFailure(stopOnFailure));
    }

    public static int exitCodeFor(RunResult runResult) {
        return JavaspecExitCode.from(runResult);
    }
}
