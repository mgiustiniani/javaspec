package io.github.jvmspec.fixtures.extension;

import io.github.jvmspec.extension.ExtensionContext;
import io.github.jvmspec.extension.JavaspecExtension;

public final class ThrowingJavaspecExtension implements JavaspecExtension {
    public void configure(ExtensionContext context) {
        throw new IllegalStateException("boom from extension configure");
    }
}
