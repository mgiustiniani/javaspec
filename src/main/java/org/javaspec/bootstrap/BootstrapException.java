package org.javaspec.bootstrap;

/**
 * Unchecked failure raised when a configured bootstrap hook cannot be executed.
 */
public final class BootstrapException extends RuntimeException {
    public BootstrapException(String message) {
        super(message);
    }

    public BootstrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
