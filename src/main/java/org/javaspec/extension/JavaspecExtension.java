package org.javaspec.extension;

/**
 * Extension lifecycle hook for registering javaspec capabilities.
 */
public interface JavaspecExtension {
    void configure(ExtensionContext context);

    default void register(ExtensionContext context) {
        configure(context);
    }
}
