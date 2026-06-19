package org.javaspec.extension.fixtures;

import org.javaspec.extension.ExtensionContext;
import org.javaspec.extension.JavaspecExtension;

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
