package org.javaspec.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Discovers and selects a {@link DependencyResolver} for a given descriptor.
 *
 * <p>The built-in {@link LocalMavenRepoResolver} is always tried first.
 * External providers registered via {@code ServiceLoader} are consulted if the
 * built-in resolver does not support the descriptor.</p>
 */
public final class DependencyResolverLoader {

    private DependencyResolverLoader() {
    }

    /**
     * Finds the first resolver that {@linkplain DependencyResolver#supports supports}
     * the given descriptor.  The built-in {@link LocalMavenRepoResolver} is
     * checked before any external providers.
     *
     * @param descriptor  the descriptor file (e.g. {@code pom.xml})
     * @param classLoader class loader used for {@code ServiceLoader} discovery
     * @return the first matching resolver, or {@code null} if none is available
     */
    public static DependencyResolver findFor(File descriptor, ClassLoader classLoader) {
        LocalMavenRepoResolver builtin = new LocalMavenRepoResolver();
        if (builtin.supports(descriptor)) {
            return builtin;
        }
        ServiceLoader<DependencyResolver> loader =
                ServiceLoader.load(DependencyResolver.class, classLoader);
        Iterator<DependencyResolver> it = loader.iterator();
        while (it.hasNext()) {
            DependencyResolver resolver = it.next();
            if (resolver.supports(descriptor)) {
                return resolver;
            }
        }
        return null;
    }

    /**
     * Returns all resolvers visible on the given class loader (for diagnostic
     * use in verbose output).
     *
     * @param classLoader class loader used for {@code ServiceLoader} discovery
     * @return ordered list of available resolvers
     */
    public static List<DependencyResolver> loadAll(ClassLoader classLoader) {
        List<DependencyResolver> resolvers = new ArrayList<DependencyResolver>();
        resolvers.add(new LocalMavenRepoResolver());
        ServiceLoader<DependencyResolver> loader =
                ServiceLoader.load(DependencyResolver.class, classLoader);
        Iterator<DependencyResolver> it = loader.iterator();
        while (it.hasNext()) {
            resolvers.add(it.next());
        }
        return resolvers;
    }
}
