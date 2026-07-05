package io.github.jvmspec.doubles;

/**
 * SPI for optional concrete-class double providers.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} from the effective
 * run classloader. When no provider is installed, javaspec defaults to interface-only doubles.
 *
 * <p>An implementation may reject a type by throwing {@link IllegalArgumentException} with a
 * clear message (e.g. for final classes, arrays, or enums).
 *
 * @since Phase 37
 */
public interface ConcreteDoubleProvider {
    /**
     * Returns true when this provider supports creating a double for the given type.
     *
     * <p>Implementations must be conservative: if they cannot create a usable double for
     * {@code type} (e.g. it is final, an array, or an enum), they must return false here.
     * The method must not throw.
     */
    boolean supports(Class<?> type);

    /**
     * Creates a concrete double for the given non-interface type.
     *
     * <p>The returned handle must expose stub/verify semantics compatible with
     * {@link InterfaceDouble}: the {@link InterfaceDouble#instance()} proxy forwards invocations
     * to an embedded {@link DoubleControl}.
     *
     * @throws IllegalArgumentException if the type cannot be doubled (final, array, enum, etc.)
     */
    <T> InterfaceDouble<T> createDouble(Class<T> type);
}
