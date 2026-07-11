package io.github.jvmspec.invocation;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecDiscoveryRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable input for a programmatic javaspec runner invocation.
 *
 * <p>Source/spec compilation is disabled by default. Callers may opt in with
 * {@link #withCompilation(List, File, List)}. Programmatic compilation uses the current JDK
 * {@code javax.tools.JavaCompiler}; javaspec does not resolve dependencies, fork {@code javac},
 * manage source/release levels, or maintain an incremental cache. {@link JavaspecLauncher}
 * discovers specs first, skips compilation when no specs are discovered, and raises
 * {@code io.github.jvmspec.compilation.SourceCompilationException} for compiler-unavailable,
 * compile-failure, I/O, or security failures before bootstrap hooks or examples run.</p>
 */
public final class JavaspecInvocation {
    private static final List<DiscoveredSpec> EMPTY_SPECS = Collections.unmodifiableList(new ArrayList<DiscoveredSpec>());
    private static final List<String> EMPTY_BOOTSTRAP_HOOKS = Collections.unmodifiableList(new ArrayList<String>());
    private static final List<String> EMPTY_EXTENSIONS = Collections.unmodifiableList(new ArrayList<String>());
    private static final List<File> EMPTY_COMPILATION_FILES = Collections.unmodifiableList(new ArrayList<File>());

    private final SpecDiscoveryRequest discoveryRequest;
    private final List<DiscoveredSpec> discoveredSpecs;
    private final ClassLoader classLoader;
    private final List<String> bootstrapHooks;
    private final boolean bootstrapDiscovery;
    private final List<String> extensions;
    private final boolean stopOnFailure;
    private final boolean compilationEnabled;
    private final List<File> compilationSourceRoots;
    private final File compilationOutputDirectory;
    private final List<File> compilationClasspathEntries;

    private JavaspecInvocation(
            SpecDiscoveryRequest discoveryRequest,
            List<DiscoveredSpec> discoveredSpecs,
            ClassLoader classLoader,
            List<String> bootstrapHooks,
            boolean bootstrapDiscovery,
            List<String> extensions,
            boolean stopOnFailure,
            boolean compilationEnabled,
            List<File> compilationSourceRoots,
            File compilationOutputDirectory,
            List<File> compilationClasspathEntries
    ) {
        if (discoveryRequest == null && discoveredSpecs == null) {
            throw new IllegalArgumentException("Either discoveryRequest or discoveredSpecs is required.");
        }
        if (discoveryRequest != null && discoveredSpecs != null) {
            throw new IllegalArgumentException("Use either discoveryRequest or discoveredSpecs, not both.");
        }
        this.discoveryRequest = discoveryRequest;
        this.discoveredSpecs = immutableSpecs(discoveredSpecs);
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
        this.bootstrapHooks = immutableHookClassNames(bootstrapHooks);
        this.bootstrapDiscovery = bootstrapDiscovery;
        this.extensions = immutableExtensionClassNames(extensions);
        this.stopOnFailure = stopOnFailure;
        this.compilationEnabled = compilationEnabled;
        if (compilationEnabled) {
            this.compilationSourceRoots = immutableFiles(compilationSourceRoots, "compilationSourceRoots");
            this.compilationOutputDirectory = Objects.requireNonNull(
                    compilationOutputDirectory,
                    "compilationOutputDirectory must not be null"
            );
            this.compilationClasspathEntries = immutableFiles(
                    compilationClasspathEntries,
                    "compilationClasspathEntries"
            );
        } else {
            this.compilationSourceRoots = EMPTY_COMPILATION_FILES;
            this.compilationOutputDirectory = null;
            this.compilationClasspathEntries = EMPTY_COMPILATION_FILES;
        }
    }

    public static JavaspecInvocation of(SpecDiscoveryRequest discoveryRequest) {
        return of(discoveryRequest, defaultClassLoader());
    }

    public static JavaspecInvocation of(SpecDiscoveryRequest discoveryRequest, ClassLoader classLoader) {
        return new JavaspecInvocation(
                Objects.requireNonNull(discoveryRequest, "discoveryRequest must not be null"),
                null,
                classLoader,
                EMPTY_BOOTSTRAP_HOOKS,
                false,
                EMPTY_EXTENSIONS,
                false,
                false,
                EMPTY_COMPILATION_FILES,
                null,
                EMPTY_COMPILATION_FILES
        );
    }

    public static JavaspecInvocation discovering(SpecDiscoveryRequest discoveryRequest, ClassLoader classLoader) {
        return of(discoveryRequest, classLoader);
    }

    public static JavaspecInvocation discovering(SpecDiscoveryRequest discoveryRequest) {
        return of(discoveryRequest);
    }

    public static JavaspecInvocation of(List<DiscoveredSpec> discoveredSpecs) {
        return of(discoveredSpecs, defaultClassLoader());
    }

    public static JavaspecInvocation of(List<DiscoveredSpec> discoveredSpecs, ClassLoader classLoader) {
        return new JavaspecInvocation(
                null,
                discoveredSpecs,
                classLoader,
                EMPTY_BOOTSTRAP_HOOKS,
                false,
                EMPTY_EXTENSIONS,
                false,
                false,
                EMPTY_COMPILATION_FILES,
                null,
                EMPTY_COMPILATION_FILES
        );
    }

    public static JavaspecInvocation forSpecs(List<DiscoveredSpec> discoveredSpecs, ClassLoader classLoader) {
        return of(discoveredSpecs, classLoader);
    }

    public static JavaspecInvocation forSpecs(List<DiscoveredSpec> discoveredSpecs) {
        return of(discoveredSpecs);
    }

    public SpecDiscoveryRequest discoveryRequest() {
        return discoveryRequest;
    }

    public SpecDiscoveryRequest getDiscoveryRequest() {
        return discoveryRequest;
    }

    public boolean hasDiscoveryRequest() {
        return discoveryRequest != null;
    }

    public List<DiscoveredSpec> discoveredSpecs() {
        return discoveredSpecs;
    }

    public List<DiscoveredSpec> specs() {
        return discoveredSpecs;
    }

    public List<DiscoveredSpec> getDiscoveredSpecs() {
        return discoveredSpecs;
    }

    public boolean hasDiscoveredSpecs() {
        return discoveryRequest == null;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public List<String> bootstrapHooks() {
        return bootstrapHooks;
    }

    public List<String> bootstrap() {
        return bootstrapHooks;
    }

    public List<String> getBootstrapHooks() {
        return bootstrapHooks;
    }

    public List<String> getBootstrap() {
        return bootstrapHooks;
    }

    public JavaspecInvocation withBootstrapHooks(List<String> bootstrapHooks) {
        return new JavaspecInvocation(
                discoveryRequest,
                hasDiscoveredSpecs() ? discoveredSpecs : null,
                classLoader,
                bootstrapHooks,
                bootstrapDiscovery,
                extensions,
                stopOnFailure,
                compilationEnabled,
                compilationSourceRoots,
                compilationOutputDirectory,
                compilationClasspathEntries
        );
    }

    public JavaspecInvocation withBootstrapHook(String bootstrapHook) {
        List<String> hooks = new ArrayList<String>(bootstrapHooks);
        hooks.add(bootstrapHook);
        return withBootstrapHooks(hooks);
    }

    /**
     * Returns whether this invocation opts into ServiceLoader discovery of
     * {@code io.github.jvmspec.bootstrap.BootstrapHook} providers.
     *
     * <p>The default is {@code false}. When enabled, {@link JavaspecLauncher} uses this
     * invocation's classloader for discovery and temporarily sets the thread context classloader
     * to it while bootstrap hooks execute. Explicit configured hooks run first in configured
     * order with duplicates preserved; discovered providers run afterward in deterministic
     * provider implementation class name order.</p>
     */
    public boolean bootstrapDiscovery() {
        return bootstrapDiscovery;
    }

    public boolean isBootstrapDiscoveryEnabled() {
        return bootstrapDiscovery;
    }

    public boolean getBootstrapDiscovery() {
        return bootstrapDiscovery;
    }

    /**
     * Returns a copy of this invocation with ServiceLoader bootstrap hook discovery enabled or
     * disabled. The default is disabled, so existing programmatic invocations preserve their
     * behavior until they explicitly opt in.
     */
    public JavaspecInvocation withBootstrapDiscovery(boolean bootstrapDiscovery) {
        return new JavaspecInvocation(
                discoveryRequest,
                hasDiscoveredSpecs() ? discoveredSpecs : null,
                classLoader,
                bootstrapHooks,
                bootstrapDiscovery,
                extensions,
                stopOnFailure,
                compilationEnabled,
                compilationSourceRoots,
                compilationOutputDirectory,
                compilationClasspathEntries
        );
    }

    /**
     * Ordered, duplicate-preserving list of configured extension implementation class names
     * to activate before this invocation runs. Defaults to an empty immutable list.
     */
    public List<String> extensions() {
        return extensions;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * Returns a copy of this invocation with the given configured extension class names.
     * Each entry must be the fully qualified name of an
     * {@code io.github.jvmspec.extension.JavaspecExtension} (or {@code Extension} alias)
     * implementation loadable from this invocation's classloader; entries are activated in
     * list order with duplicates preserved.
     */
    public JavaspecInvocation withExtensions(List<String> extensions) {
        return new JavaspecInvocation(
                discoveryRequest,
                hasDiscoveredSpecs() ? discoveredSpecs : null,
                classLoader,
                bootstrapHooks,
                bootstrapDiscovery,
                extensions,
                stopOnFailure,
                compilationEnabled,
                compilationSourceRoots,
                compilationOutputDirectory,
                compilationClasspathEntries
        );
    }

    /**
     * Returns a copy of this invocation with the given extension class name appended.
     */
    public JavaspecInvocation withExtension(String extension) {
        List<String> extensionClassNames = new ArrayList<String>(extensions);
        extensionClassNames.add(extension);
        return withExtensions(extensionClassNames);
    }

    public boolean stopOnFailure() {
        return stopOnFailure;
    }

    public boolean isStopOnFailure() {
        return stopOnFailure;
    }

    public JavaspecInvocation withStopOnFailure(boolean stopOnFailure) {
        return new JavaspecInvocation(
                discoveryRequest,
                hasDiscoveredSpecs() ? discoveredSpecs : null,
                classLoader,
                bootstrapHooks,
                bootstrapDiscovery,
                extensions,
                stopOnFailure,
                compilationEnabled,
                compilationSourceRoots,
                compilationOutputDirectory,
                compilationClasspathEntries
        );
    }

    public JavaspecInvocation stoppingOnFailure() {
        return withStopOnFailure(true);
    }

    /**
     * Returns whether this invocation explicitly opts into programmatic source/spec compilation.
     *
     * <p>The default is {@code false}. When enabled, {@link JavaspecLauncher} discovers specs
     * first, skips compilation if the discovered spec list is empty, and otherwise compiles before
     * bootstrap hooks and examples. Compiler-unavailable, compile-failure, I/O, and security
     * failures raise {@code io.github.jvmspec.compilation.SourceCompilationException} and prevent
     * bootstrap hooks and examples from running.</p>
     */
    public boolean compilationEnabled() {
        return compilationEnabled;
    }

    public boolean isCompilationEnabled() {
        return compilationEnabled;
    }

    /**
     * Source roots used for opt-in programmatic compilation. The list is immutable and empty when
     * compilation is disabled. Non-directory roots are ignored by {@code SourceCompiler}.
     */
    public List<File> compilationSourceRoots() {
        return compilationSourceRoots;
    }

    public List<File> getCompilationSourceRoots() {
        return compilationSourceRoots;
    }

    /**
     * Output directory for opt-in programmatic compilation, or {@code null} when disabled.
     */
    public File compilationOutputDirectory() {
        return compilationOutputDirectory;
    }

    public File getCompilationOutputDirectory() {
        return compilationOutputDirectory;
    }

    /**
     * Explicit classpath entries supplied to the current-JDK compiler and to the temporary
     * output classloader after successful compilation. The list is immutable and empty by default.
     */
    public List<File> compilationClasspathEntries() {
        return compilationClasspathEntries;
    }

    public List<File> getCompilationClasspathEntries() {
        return compilationClasspathEntries;
    }

    /**
     * Returns a copy of this invocation with opt-in source/spec compilation enabled.
     *
     * <p>{@link JavaspecLauncher} compiles after discovery and only when specs exist. The compile
     * uses the current JDK {@code javax.tools.JavaCompiler}; no dependencies are resolved, no
     * external {@code javac} process is forked, no source/release level is managed, and no
     * incremental cache is maintained. The output directory is placed in a temporary run
     * classloader when at least one source file was compiled; if no {@code .java} files are found,
     * the original invocation classloader is kept. Compilation failures raise
     * {@code io.github.jvmspec.compilation.SourceCompilationException} before bootstrap hooks,
     * examples, or reports owned by adapters can run.</p>
     *
     * @param sourceRoots      roots whose {@code .java} files should be compiled
     * @param outputDirectory  destination directory for compiled classes
     * @param classpathEntries explicit compiler and run classloader dependency entries
     * @return a new invocation with compilation enabled
     */
    public JavaspecInvocation withCompilation(
            List<File> sourceRoots,
            File outputDirectory,
            List<File> classpathEntries
    ) {
        return new JavaspecInvocation(
                discoveryRequest,
                hasDiscoveredSpecs() ? discoveredSpecs : null,
                classLoader,
                bootstrapHooks,
                bootstrapDiscovery,
                extensions,
                stopOnFailure,
                true,
                sourceRoots,
                outputDirectory,
                classpathEntries
        );
    }

    /**
     * Convenience overload for the common production-source-root plus spec-root compile input.
     */
    public JavaspecInvocation withCompilation(
            File sourceRoot,
            File specRoot,
            File outputDirectory,
            List<File> classpathEntries
    ) {
        List<File> sourceRoots = new ArrayList<File>();
        sourceRoots.add(Objects.requireNonNull(sourceRoot, "sourceRoot must not be null"));
        sourceRoots.add(Objects.requireNonNull(specRoot, "specRoot must not be null"));
        return withCompilation(sourceRoots, outputDirectory, classpathEntries);
    }

    /**
     * Returns a copy of this invocation with programmatic compilation disabled.
     */
    public JavaspecInvocation withoutCompilation() {
        return new JavaspecInvocation(
                discoveryRequest,
                hasDiscoveredSpecs() ? discoveredSpecs : null,
                classLoader,
                bootstrapHooks,
                bootstrapDiscovery,
                extensions,
                stopOnFailure,
                false,
                EMPTY_COMPILATION_FILES,
                null,
                EMPTY_COMPILATION_FILES
        );
    }

    private static ClassLoader defaultClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }
        return JavaspecInvocation.class.getClassLoader();
    }

    private static List<DiscoveredSpec> immutableSpecs(List<DiscoveredSpec> specs) {
        if (specs == null) {
            return EMPTY_SPECS;
        }
        if (specs.isEmpty()) {
            return EMPTY_SPECS;
        }
        List<DiscoveredSpec> copy = new ArrayList<DiscoveredSpec>();
        for (int i = 0; i < specs.size(); i++) {
            copy.add(Objects.requireNonNull(specs.get(i), "discoveredSpecs[" + i + "] must not be null"));
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<String> immutableHookClassNames(List<String> hookClassNames) {
        Objects.requireNonNull(hookClassNames, "bootstrapHooks must not be null");
        if (hookClassNames.isEmpty()) {
            return EMPTY_BOOTSTRAP_HOOKS;
        }
        List<String> copy = new ArrayList<String>();
        for (int i = 0; i < hookClassNames.size(); i++) {
            String hookClassName = Objects.requireNonNull(hookClassNames.get(i),
                    "bootstrapHooks[" + i + "] must not be null");
            String trimmed = hookClassName.trim();
            if (trimmed.length() == 0) {
                throw new IllegalArgumentException("bootstrapHooks[" + i + "] must not be blank.");
            }
            copy.add(trimmed);
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<String> immutableExtensionClassNames(List<String> extensionClassNames) {
        Objects.requireNonNull(extensionClassNames, "extensions must not be null");
        if (extensionClassNames.isEmpty()) {
            return EMPTY_EXTENSIONS;
        }
        List<String> copy = new ArrayList<String>();
        for (int i = 0; i < extensionClassNames.size(); i++) {
            String extensionClassName = Objects.requireNonNull(extensionClassNames.get(i),
                    "extensions[" + i + "] must not be null");
            String trimmed = extensionClassName.trim();
            if (trimmed.length() == 0) {
                throw new IllegalArgumentException("extensions[" + i + "] must not be blank.");
            }
            copy.add(trimmed);
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<File> immutableFiles(List<File> files, String name) {
        Objects.requireNonNull(files, name + " must not be null");
        if (files.isEmpty()) {
            return EMPTY_COMPILATION_FILES;
        }
        List<File> copy = new ArrayList<File>();
        for (int i = 0; i < files.size(); i++) {
            copy.add(Objects.requireNonNull(files.get(i), name + "[" + i + "] must not be null"));
        }
        return Collections.unmodifiableList(copy);
    }
}
