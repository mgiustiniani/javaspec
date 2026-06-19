package org.javaspec.resolver;

import java.io.File;
import java.util.List;

/**
 * SPI for optional dependency resolution at CLI run time.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} and
 * must not introduce runtime dependencies in the core artifact.  The built-in
 * {@link LocalMavenRepoResolver} is always tried first; external providers are
 * consulted if the built-in one cannot handle the descriptor.</p>
 *
 * <p>To register an external implementation, place a
 * {@code META-INF/services/org.javaspec.resolver.DependencyResolver} file on
 * the classpath with the fully-qualified class name of the provider.</p>
 */
public interface DependencyResolver {

    /**
     * Human-readable name of this resolver for diagnostic output.
     *
     * @return resolver name, never {@code null}
     */
    String name();

    /**
     * Returns {@code true} when this resolver can handle the given descriptor
     * file (e.g. a Maven {@code pom.xml}).
     *
     * @param descriptor the descriptor file
     * @return {@code true} if this resolver supports the descriptor
     */
    boolean supports(File descriptor);

    /**
     * Resolves all runtime-scope dependency JARs reachable from the given
     * descriptor, including transitive dependencies.
     *
     * <p>Only runtime/compile-scope dependencies are included; {@code test},
     * {@code provided}, and {@code system} scope dependencies are excluded from
     * transitive resolution.  Direct {@code test} and {@code provided} scope
     * dependencies of the root descriptor are also excluded.</p>
     *
     * @param descriptor the descriptor file (e.g. a {@code pom.xml})
     * @return ordered list of JAR {@link File}s (may be empty, never {@code null})
     * @throws DependencyResolutionException if resolution fails
     */
    List<File> resolve(File descriptor) throws DependencyResolutionException;
}
