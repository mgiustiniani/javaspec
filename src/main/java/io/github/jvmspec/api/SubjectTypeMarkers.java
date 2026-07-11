package io.github.jvmspec.api;

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
        // For enum types without an explicitly configured construction, skip instantiation.
        // Just verify the described class matches the expected type.
        if (!lifecycle.isInstantiated() && lifecycle.getSubjectType() != null && lifecycle.getSubjectType().isEnum()) {
            Class<?> declaredType = lifecycle.getSubjectType();
            if (!expectedType.isAssignableFrom(declaredType)) {
                throw new AssertionError(
                        "Expected an instance of " + expectedType.getName()
                        + " but the described type is " + declaredType.getName()
                );
            }
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

    /**
     * Declares that the subject enum type should have a constant with the given name.
     * Marker used by discovery and generation — no runtime assertion.
     */
    /**
     * Declares that the subject enum type should have a constant with the given name.
     * The second parameter is optional and represents constructor arguments for enum constants
     * with parameters (e.g. {@code EC_P256("secp256r1", 256)}).
     * Marker used by discovery and generation — no runtime assertion.
     */
    public void shouldHaveConstant(String name, Object... args) {
        Class<?> subjectType = lifecycle.getSubjectType();
        if (!subjectType.isEnum()) {
            throw new AssertionError("Expected " + subjectType.getName() + " to be an enum, but it is not");
        }
        Object[] constants = subjectType.getEnumConstants();
        for (int i = 0; i < constants.length; i++) {
            if (((Enum<?>) constants[i]).name().equals(name)) {
                return;
            }
        }
        throw new AssertionError("Expected enum " + subjectType.getSimpleName() + " to have constant '" + name + "', but it does not. "
                + "Existing constants: " + enumConstantNames(constants));
    }

    private static String enumConstantNames(Object[] constants) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < constants.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(((Enum<?>) constants[i]).name());
        }
        sb.append("]");
        return sb.toString();
    }
}
