package io.github.jvmspec.extension;

/**
 * Unchecked diagnostic raised when javaspec extension or formatter providers cannot be loaded.
 */
public final class ExtensionLoadingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ExtensionLoadingException(String message) {
        super(message);
    }

    public ExtensionLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
