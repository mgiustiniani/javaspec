package org.javaspec.api;

import java.util.Objects;

/**
 * Subject-type marker methods used by javaspec discovery and generation.
 * <p>
 * These methods allow specs to declare the expected nature of the subject type
 * (class, interface, enum, sealed, etc.) and type relationships (extends, implements, permits).
 * Discovery scans source code for these markers to infer subject metadata.
 * </p>
 *
 * @param <T> the subject type this spec describes
 */
public class SubjectTypeMarkers<T> {
    private final SubjectLifecycle<T> lifecycle;

    /**
     * Creates a new SubjectTypeMarkers with the given lifecycle.
     *
     * @param lifecycle the subject lifecycle for runtime type checking
     */
    public SubjectTypeMarkers(SubjectLifecycle<T> lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
    }

    /**
     * Asserts that the subject is an instance of (or assignable to) the given type.
     */
    public void shouldHaveType(Class<?> expectedType) {
        if (expectedType == null) {
            throw new AssertionError("Expected type must not be null");
        }
        if (!lifecycle.isInstantiated() && lifecycle.getSubjectType() == null) {
            return;
        }
        T current = lifecycle.subject();
        if (current == null) {
            throw new AssertionError("Expected an instance of " + expectedType.getName() + " but got null");
        }
        Class<?> actualType = current.getClass();
        if (!expectedType.isAssignableFrom(actualType)) {
            throw new AssertionError(
                    "Expected an instance of " + expectedType.getName()
                    + " but got " + actualType.getName()
            );
        }
    }

    public void shouldBeAClass() {
        // Marker used by discovery and generation.
    }

    public void shouldBeAFinalClass() {
        // Marker used by discovery and generation.
    }

    public void shouldBeAnInterface() {
        // Marker used by discovery and generation.
    }

    public void shouldBeAnEnum() {
        // Marker used by discovery and generation.
    }

    public void shouldBeAnAnnotation() {
        // Marker used by discovery and generation.
    }

    public void shouldBeARecord() {
        // Marker used by discovery and generation.
    }

    public void shouldBeASealedClass() {
        // Marker used by discovery and generation.
    }

    public void shouldBeASealedInterface() {
        // Marker used by discovery and generation.
    }

    public void shouldExtend(Class<?>... extendedTypes) {
        // Marker used by discovery and generation.
    }

    public void shouldImplement(Class<?>... implementedTypes) {
        // Marker used by discovery and generation.
    }

    public void shouldPermit(Class<?>... permittedTypes) {
        // Marker used by discovery and generation.
    }
}
