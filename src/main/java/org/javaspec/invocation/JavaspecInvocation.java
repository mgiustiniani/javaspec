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

    private final SpecDiscoveryRequest discoveryRequest;
    private final List<DiscoveredSpec> discoveredSpecs;
    private final ClassLoader classLoader;
    private final boolean stopOnFailure;

    private JavaspecInvocation(
            SpecDiscoveryRequest discoveryRequest,
            List<DiscoveredSpec> discoveredSpecs,
            ClassLoader classLoader,
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
        return new JavaspecInvocation(null, discoveredSpecs, classLoader, false);
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

    public boolean stopOnFailure() {
        return stopOnFailure;
    }

    public boolean isStopOnFailure() {
        return stopOnFailure;
    }

    public JavaspecInvocation withStopOnFailure(boolean stopOnFailure) {
        return new JavaspecInvocation(discoveryRequest, hasDiscoveredSpecs() ? discoveredSpecs : null, classLoader, stopOnFailure);
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
}
