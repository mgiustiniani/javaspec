package io.github.jvmspec.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable configuration for one javaspec suite.
 */
public final class JavaspecSuiteConfiguration {
    public static final String DEFAULT_SUITE_NAME = "default";
    public static final String DEFAULT_SPEC_DIRECTORY = "src/test/java";
    public static final String DEFAULT_SOURCE_DIRECTORY = "src/main/java";
    public static final String DEFAULT_SPEC_PACKAGE_PREFIX = "spec";
    public static final String DEFAULT_PACKAGE_PREFIX = "";

    private static final List<String> EMPTY_BOOTSTRAP_HOOKS = Collections.unmodifiableList(new ArrayList<String>());
    private static final List<String> EMPTY_EXTENSIONS = Collections.unmodifiableList(new ArrayList<String>());
    private static final JavaspecSuiteConfiguration DEFAULT_CONFIGURATION = new JavaspecSuiteConfiguration(
            DEFAULT_SUITE_NAME,
            DEFAULT_SPEC_DIRECTORY,
            DEFAULT_SOURCE_DIRECTORY,
            DEFAULT_SPEC_PACKAGE_PREFIX,
            DEFAULT_PACKAGE_PREFIX,
            EMPTY_BOOTSTRAP_HOOKS,
            EMPTY_EXTENSIONS,
            false
    );

    private final String name;
    private final String specDirectory;
    private final String sourceDirectory;
    private final String specPackagePrefix;
    private final String packagePrefix;
    private final List<String> bootstrapHooks;
    private final List<String> extensions;
    private final boolean bootstrapDiscovery;

    private JavaspecSuiteConfiguration(
            String name,
            String specDirectory,
            String sourceDirectory,
            String specPackagePrefix,
            String packagePrefix,
            List<String> bootstrapHooks,
            List<String> extensions,
            boolean bootstrapDiscovery
    ) {
        this.name = validateRequiredValue("suite name", name);
        this.specDirectory = validateRequiredValue("specDir", specDirectory);
        this.sourceDirectory = validateRequiredValue("sourceDir", sourceDirectory);
        this.specPackagePrefix = validateRequiredValue("specPackagePrefix", specPackagePrefix);
        this.packagePrefix = validateOptionalValue("packagePrefix", packagePrefix);
        this.bootstrapHooks = immutableBootstrapHooks(bootstrapHooks);
        this.extensions = immutableExtensions(extensions);
        this.bootstrapDiscovery = bootstrapDiscovery;
    }

    public static JavaspecSuiteConfiguration defaults() {
        return DEFAULT_CONFIGURATION;
    }

    public static JavaspecSuiteConfiguration of(
            String name,
            String specDirectory,
            String sourceDirectory,
            String specPackagePrefix,
            String packagePrefix,
            List<String> bootstrapHooks
    ) {
        return new JavaspecSuiteConfiguration(
                name,
                specDirectory,
                sourceDirectory,
                specPackagePrefix,
                packagePrefix,
                bootstrapHooks,
                EMPTY_EXTENSIONS,
                false
        );
    }

    public static JavaspecSuiteConfiguration of(
            String name,
            String specDirectory,
            String sourceDirectory,
            String specPackagePrefix,
            String packagePrefix,
            List<String> bootstrapHooks,
            boolean bootstrapDiscovery
    ) {
        return new JavaspecSuiteConfiguration(
                name,
                specDirectory,
                sourceDirectory,
                specPackagePrefix,
                packagePrefix,
                bootstrapHooks,
                EMPTY_EXTENSIONS,
                bootstrapDiscovery
        );
    }

    public static JavaspecSuiteConfiguration of(
            String name,
            String specDirectory,
            String sourceDirectory,
            String specPackagePrefix,
            String packagePrefix,
            List<String> bootstrapHooks,
            List<String> extensions
    ) {
        return new JavaspecSuiteConfiguration(
                name,
                specDirectory,
                sourceDirectory,
                specPackagePrefix,
                packagePrefix,
                bootstrapHooks,
                extensions,
                false
        );
    }

    public static JavaspecSuiteConfiguration of(
            String name,
            String specDirectory,
            String sourceDirectory,
            String specPackagePrefix,
            String packagePrefix,
            List<String> bootstrapHooks,
            List<String> extensions,
            boolean bootstrapDiscovery
    ) {
        return new JavaspecSuiteConfiguration(
                name,
                specDirectory,
                sourceDirectory,
                specPackagePrefix,
                packagePrefix,
                bootstrapHooks,
                extensions,
                bootstrapDiscovery
        );
    }

    public String name() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String specDirectory() {
        return specDirectory;
    }

    public String specDir() {
        return specDirectory;
    }

    public String specRoot() {
        return specDirectory;
    }

    public String getSpecDirectory() {
        return specDirectory;
    }

    public String getSpecDir() {
        return specDirectory;
    }

    public String getSpecRoot() {
        return specDirectory;
    }

    public String sourceDirectory() {
        return sourceDirectory;
    }

    public String sourceDir() {
        return sourceDirectory;
    }

    public String sourceRoot() {
        return sourceDirectory;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getSourceDir() {
        return sourceDirectory;
    }

    public String getSourceRoot() {
        return sourceDirectory;
    }

    public String specPackagePrefix() {
        return specPackagePrefix;
    }

    public String getSpecPackagePrefix() {
        return specPackagePrefix;
    }

    public String packagePrefix() {
        return packagePrefix;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public List<String> bootstrapHooks() {
        return bootstrapHooks;
    }

    public List<String> bootstrap() {
        return bootstrapHooks;
    }

    public List<String> getBootstrapHooks() {
        return bootstrapHooks;
    }

    public List<String> getBootstrap() {
        return bootstrapHooks;
    }

    /**
     * Returns whether this suite opts into ServiceLoader discovery of
     * {@code io.github.jvmspec.bootstrap.BootstrapHook} providers.
     *
     * <p>The default is {@code false}. For a selected suite, discovery is enabled when either
     * the top-level configuration or this suite is enabled. Explicit configured hooks still run
     * first in their configured order; discovered providers are loaded from the run classloader
     * and executed afterward in provider implementation class name order.</p>
     */
    public boolean bootstrapDiscovery() {
        return bootstrapDiscovery;
    }

    public boolean isBootstrapDiscoveryEnabled() {
        return bootstrapDiscovery;
    }

    public boolean getBootstrapDiscovery() {
        return bootstrapDiscovery;
    }

    /**
     * Ordered, duplicate-preserving list of configured extension implementation class names
     * scoped to this suite. Defaults to an empty immutable list.
     */
    public List<String> extensions() {
        return extensions;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JavaspecSuiteConfiguration)) {
            return false;
        }
        JavaspecSuiteConfiguration that = (JavaspecSuiteConfiguration) other;
        return name.equals(that.name)
                && specDirectory.equals(that.specDirectory)
                && sourceDirectory.equals(that.sourceDirectory)
                && specPackagePrefix.equals(that.specPackagePrefix)
                && packagePrefix.equals(that.packagePrefix)
                && bootstrapHooks.equals(that.bootstrapHooks)
                && extensions.equals(that.extensions)
                && bootstrapDiscovery == that.bootstrapDiscovery;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + specDirectory.hashCode();
        result = 31 * result + sourceDirectory.hashCode();
        result = 31 * result + specPackagePrefix.hashCode();
        result = 31 * result + packagePrefix.hashCode();
        result = 31 * result + bootstrapHooks.hashCode();
        result = 31 * result + extensions.hashCode();
        result = 31 * result + (bootstrapDiscovery ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JavaspecSuiteConfiguration{"
                + "name='" + name + '\''
                + ", specDirectory='" + specDirectory + '\''
                + ", sourceDirectory='" + sourceDirectory + '\''
                + ", specPackagePrefix='" + specPackagePrefix + '\''
                + ", packagePrefix='" + packagePrefix + '\''
                + ", bootstrapHooks=" + bootstrapHooks
                + ", extensions=" + extensions
                + ", bootstrapDiscovery=" + bootstrapDiscovery
                + '}';
    }

    private static String validateRequiredValue(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new ConfigurationException(fieldName + " must not be blank.");
        }
        return trimmed;
    }

    private static String validateOptionalValue(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        return value.trim();
    }

    private static List<String> immutableBootstrapHooks(List<String> bootstrapHooks) {
        Objects.requireNonNull(bootstrapHooks, "bootstrapHooks must not be null");
        if (bootstrapHooks.size() == 0) {
            return EMPTY_BOOTSTRAP_HOOKS;
        }
        List<String> hooks = new ArrayList<String>();
        for (int i = 0; i < bootstrapHooks.size(); i++) {
            String hook = bootstrapHooks.get(i);
            Objects.requireNonNull(hook, "bootstrap hook must not be null");
            String trimmed = hook.trim();
            if (trimmed.length() == 0) {
                throw new ConfigurationException("bootstrap hook must not be blank.");
            }
            hooks.add(trimmed);
        }
        return Collections.unmodifiableList(hooks);
    }

    private static List<String> immutableExtensions(List<String> extensions) {
        Objects.requireNonNull(extensions, "extensions must not be null");
        if (extensions.size() == 0) {
            return EMPTY_EXTENSIONS;
        }
        List<String> copy = new ArrayList<String>();
        for (int i = 0; i < extensions.size(); i++) {
            String extension = extensions.get(i);
            Objects.requireNonNull(extension, "extension must not be null");
            String trimmed = extension.trim();
            if (trimmed.length() == 0) {
                throw new ConfigurationException("extension must not be blank.");
            }
            copy.add(trimmed);
        }
        return Collections.unmodifiableList(copy);
    }
}
