package io.github.jvmspec.config;

/**
 * Exception raised when javaspec configuration cannot be parsed or validated.
 */
public final class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    static ConfigurationException atLine(int lineNumber, String message) {
        return new ConfigurationException("Line " + lineNumber + ": " + message);
    }
}
