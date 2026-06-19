package org.javaspec.resolver;

/**
 * Thrown when dependency resolution cannot produce a complete classpath.
 */
public final class DependencyResolutionException extends Exception {

    private static final long serialVersionUID = 1L;

    public DependencyResolutionException(String message) {
        super(message);
    }

    public DependencyResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
