package org.javaspec.api;

/**
 * Minimal PHPSpec-inspired base class for generated javaspec specifications.
 */
public class ObjectBehavior<T> {
    public void shouldHaveType(Class<? extends T> expectedType) {
        if (expectedType == null) {
            throw new AssertionError("Expected type must not be null");
        }
    }

    public void shouldBeAClass() {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldBeAFinalClass() {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldBeAnInterface() {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldBeAnEnum() {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldBeAnAnnotation() {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldBeARecord() {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldBeASealedClass() {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldBeASealedInterface() {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldExtend(Class<?>... extendedTypes) {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldImplement(Class<?>... implementedTypes) {
        // Marker used by discovery until the full matcher engine exists.
    }

    public void shouldPermit(Class<?>... permittedTypes) {
        // Marker used by discovery until the full matcher engine exists.
    }
}
