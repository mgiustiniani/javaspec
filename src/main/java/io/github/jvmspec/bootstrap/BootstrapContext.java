package io.github.jvmspec.bootstrap;

import io.github.jvmspec.discovery.DiscoveredSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable context passed to bootstrap hooks for one javaspec run.
 */
public final class BootstrapContext {
    private static final List<DiscoveredSpec> EMPTY_SPECS = Collections.unmodifiableList(new ArrayList<DiscoveredSpec>());

    private final ClassLoader classLoader;
    private final List<DiscoveredSpec> discoveredSpecs;

    private BootstrapContext(ClassLoader classLoader, List<DiscoveredSpec> discoveredSpecs) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
        this.discoveredSpecs = immutableSpecs(discoveredSpecs);
    }

    public static BootstrapContext of(ClassLoader classLoader, List<DiscoveredSpec> discoveredSpecs) {
        return new BootstrapContext(classLoader, discoveredSpecs);
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
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

    public List<DiscoveredSpec> getSpecs() {
        return discoveredSpecs;
    }

    private static List<DiscoveredSpec> immutableSpecs(List<DiscoveredSpec> specs) {
        Objects.requireNonNull(specs, "discoveredSpecs must not be null");
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
