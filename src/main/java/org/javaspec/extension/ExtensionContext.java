package org.javaspec.extension;

import org.javaspec.formatter.RunFormatterRegistry;

import java.util.Objects;

/**
 * Registration context exposed to extensions.
 */
public final class ExtensionContext {
    private final RunFormatterRegistry runFormatterRegistry;

    public ExtensionContext(RunFormatterRegistry runFormatterRegistry) {
        this.runFormatterRegistry = Objects.requireNonNull(runFormatterRegistry, "runFormatterRegistry must not be null");
    }

    public static ExtensionContext of(RunFormatterRegistry runFormatterRegistry) {
        return new ExtensionContext(runFormatterRegistry);
    }

    public RunFormatterRegistry runFormatterRegistry() {
        return runFormatterRegistry;
    }

    public RunFormatterRegistry runFormatters() {
        return runFormatterRegistry;
    }
}
