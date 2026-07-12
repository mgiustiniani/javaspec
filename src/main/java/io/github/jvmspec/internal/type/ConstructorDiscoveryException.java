package io.github.jvmspec.internal.type;

/** Internal typed fail-closed diagnostic for incompatible constructor evidence. */
public final class ConstructorDiscoveryException extends RuntimeException {
    public ConstructorDiscoveryException(String message) {
        super(message);
    }
}
