package org.javaspec.naming;

import org.javaspec.config.JavaspecSuiteConfiguration;

import java.util.Objects;

/**
 * Compatibility entry point for naming APIs; delegates to the discovery naming model.
 */
public final class SpecNamingConvention extends org.javaspec.discovery.SpecNamingConvention {
    private SpecNamingConvention(String specPackagePrefix, String productionPackagePrefix) {
        super(specPackagePrefix, productionPackagePrefix);
    }

    public static SpecNamingConvention defaults() {
        return new SpecNamingConvention(DEFAULT_SPEC_PACKAGE_PREFIX, DEFAULT_PRODUCTION_PACKAGE_PREFIX);
    }

    public static SpecNamingConvention defaultConvention() {
        return defaults();
    }

    public static SpecNamingConvention of(String specPackagePrefix, String productionPackagePrefix) {
        return new SpecNamingConvention(specPackagePrefix, productionPackagePrefix);
    }

    public static SpecNamingConvention from(JavaspecSuiteConfiguration suiteConfiguration) {
        Objects.requireNonNull(suiteConfiguration, "suiteConfiguration must not be null");
        return of(suiteConfiguration.specPackagePrefix(), suiteConfiguration.packagePrefix());
    }
}
