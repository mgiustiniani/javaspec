package org.javaspec.invocation;

import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecDiscoveryRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable input for a programmatic javaspec runner invocation.
 */
public final class JavaspecInvocation {
    private static final List<DiscoveredSpec> EMPTY_SPECS = Collections.unmodifiableList(new ArrayList<DiscoveredSpec>());
    private static final List<String> EMPTY_BOOTSTRAP_HOOKS = Collections.unmodifiableList(new ArrayList<String>());

    private final SpecDiscoveryRequest discoveryRequest;
    private final List<DiscoveredSpec> discoveredSpecs;
    private final ClassLoader classLoader;
    private final List<String> bootstrapHooks;
    private final boolean stopOnFailure;

    private JavaspecInvocation(
            SpecDiscoveryRequest discoveryRequest,
            List<DiscoveredSpec> discoveredSpecs,
            ClassLoader classLoader,
            List<String> bootstrapHooks,
            boolean stopOnFailure
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
        this.stopOnFailure = stopOnFailure;
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
                false
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
        return new JavaspecInvocation(null, discoveredSpecs, classLoader, EMPTY_BOOTSTRAP_HOOKS, false);
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
                stopOnFailure
        );
    }

    public JavaspecInvocation withBootstrapHook(String bootstrapHook) {
        List<String> hooks = new ArrayList<String>(bootstrapHooks);
        hooks.add(bootstrapHook);
        return withBootstrapHooks(hooks);
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
                stopOnFailure
        );
    }

    public JavaspecInvocation stoppingOnFailure() {
        return withStopOnFailure(true);
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
}
