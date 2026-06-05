package org.javaspec.fixtures.extension;

import org.javaspec.extension.ExtensionContext;
import org.javaspec.extension.JavaspecExtension;

public final class ThrowingJavaspecExtension implements JavaspecExtension {
    public void configure(ExtensionContext context) {
        throw new IllegalStateException("boom from extension configure");
    }
}
