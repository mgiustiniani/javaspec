package io.github.jvmspec.cli;

import io.github.jvmspec.generation.ConstructorPolicy;

/**
 * Helper methods for configuration handling across javaspec CLI commands.
 */
final class ConfigurationHelper {

    /**
     * Resolves the effective constructor policy from parsed arguments.
     *
     * @param parsed the parsed arguments
     * @return the effective constructor policy
     */
    static ConstructorPolicy resolveConstructorPolicy(ParsedArguments parsed) {
        if (parsed.effectiveConstructorPolicy != null) {
            return parsed.effectiveConstructorPolicy;
        }
        return ConstructorPolicy.defaultPolicy();
    }

    /**
     * Returns a human-readable message from a throwable.
     *
     * @param throwable the throwable
     * @return the message or class name if message is empty
     */
    static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
