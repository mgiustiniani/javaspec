package io.github.jvmspec.extension.fixtures;

import io.github.jvmspec.extension.ExtensionContext;
import io.github.jvmspec.extension.JavaspecExtension;

/** ServiceLoader-visible no-op extension used by ExtensionCatalogTest. */
public final class CatalogTestExtension implements JavaspecExtension {
    public static int registrationCount;

    public CatalogTestExtension() {
    }

    @Override
    public void configure(ExtensionContext context) {
        registrationCount++;
    }
}
