package org.javaspec.extension;

import org.javaspec.formatter.RunFormatter;
import org.javaspec.formatter.RunFormatterRegistry;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Loads javaspec formatter and extension providers from a classloader.
 */
public final class JavaspecExtensionLoader {
    private JavaspecExtensionLoader() {
    }

    public static RunFormatterRegistry loadRunFormatterRegistry() {
        return loadRunFormatterRegistry(defaultClassLoader());
    }

    public static RunFormatterRegistry loadRunFormatterRegistry(ClassLoader classLoader) {
        ClassLoader effectiveClassLoader = effectiveClassLoader(classLoader);
        RunFormatterRegistry registry = RunFormatterRegistry.builtIn();
        loadRunFormatterProviders(registry, effectiveClassLoader);
        loadExtensionProviders(registry, effectiveClassLoader);
        return registry;
    }

    /**
     * Loads the run formatter registry and then activates the configured extensions.
     *
     * <p>Activation order is deterministic: built-in formatters first, ServiceLoader-discovered
     * providers next, then the configured extension class names in list order (duplicates
     * preserved). All configured classes are loaded from the effective classloader (the supplied
     * classloader, or the default classloader when {@code null} is supplied), which is the same
     * classloader used for ServiceLoader discovery, so the configured extensions act on the same
     * registry instance later used for formatter selection.</p>
     *
     * @param classLoader                 classloader for discovery and configured extension
     *                                    loading; {@code null} selects the default classloader
     * @param configuredExtensionClassNames ordered list of fully qualified
     *                                    {@link JavaspecExtension}/{@link Extension}
     *                                    implementation class names; {@code null} or empty
     *                                    leaves the registry unchanged
     * @return the registry with built-in, discovered, and configured contributions applied
     * @throws ExtensionLoadingException when discovery or configured extension activation fails
     */
    public static RunFormatterRegistry loadRunFormatterRegistry(
            ClassLoader classLoader,
            List<String> configuredExtensionClassNames
    ) {
        ClassLoader effectiveClassLoader = effectiveClassLoader(classLoader);
        RunFormatterRegistry registry = loadRunFormatterRegistry(effectiveClassLoader);
        if (configuredExtensionClassNames != null && !configuredExtensionClassNames.isEmpty()) {
            JavaspecExtensionActivator.activate(configuredExtensionClassNames, effectiveClassLoader, registry);
        }
        return registry;
    }

    public static RunFormatterRegistry loadRunFormatters() {
        return loadRunFormatterRegistry();
    }

    public static RunFormatterRegistry loadRunFormatters(ClassLoader classLoader) {
        return loadRunFormatterRegistry(classLoader);
    }

    public static RunFormatterRegistry loadRunFormatters(
            ClassLoader classLoader,
            List<String> configuredExtensionClassNames
    ) {
        return loadRunFormatterRegistry(classLoader, configuredExtensionClassNames);
    }

    private static void loadRunFormatterProviders(RunFormatterRegistry registry, ClassLoader classLoader) {
        ServiceLoader<RunFormatter> loader = ServiceLoader.load(RunFormatter.class, classLoader);
        Iterator<RunFormatter> iterator = loader.iterator();
        while (hasNext(iterator, RunFormatter.class)) {
            RunFormatter formatter = next(iterator, RunFormatter.class);
            registerRunFormatter(registry, formatter);
        }
    }

    private static void registerRunFormatter(RunFormatterRegistry registry, RunFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter provider must not be null");
        try {
            registry.register(formatter.name(), formatter);
        } catch (RuntimeException ex) {
            throw new ExtensionLoadingException("Invalid run formatter provider "
                    + formatter.getClass().getName() + ": " + messageOf(ex), ex);
        }
    }

    private static void loadExtensionProviders(RunFormatterRegistry registry, ClassLoader classLoader) {
        ExtensionContext context = ExtensionContext.of(registry);
        Set<String> configuredExtensionClasses = new HashSet<String>();
        configureExtensions(JavaspecExtension.class, classLoader, context, configuredExtensionClasses);
        configureExtensions(Extension.class, classLoader, context, configuredExtensionClasses);
    }

    private static <T extends JavaspecExtension> void configureExtensions(
            Class<T> serviceType,
            ClassLoader classLoader,
            ExtensionContext context,
            Set<String> configuredExtensionClasses
    ) {
        ServiceLoader<T> loader = ServiceLoader.load(serviceType, classLoader);
        Iterator<T> iterator = loader.iterator();
        while (hasNext(iterator, serviceType)) {
            T extension = next(iterator, serviceType);
            configureExtension(serviceType, context, configuredExtensionClasses, extension);
        }
    }

    private static void configureExtension(
            Class<?> serviceType,
            ExtensionContext context,
            Set<String> configuredExtensionClasses,
            JavaspecExtension extension
    ) {
        Objects.requireNonNull(extension, "extension provider must not be null");
        String implementationClassName = extension.getClass().getName();
        if (!configuredExtensionClasses.add(implementationClassName)) {
            return;
        }
        try {
            extension.register(context);
        } catch (RuntimeException ex) {
            throw new ExtensionLoadingException("Could not configure javaspec extension "
                    + implementationClassName + " from service " + serviceType.getName()
                    + ": " + messageOf(ex), ex);
        }
    }

    private static <T> boolean hasNext(Iterator<T> iterator, Class<?> serviceType) {
        try {
            return iterator.hasNext();
        } catch (ServiceConfigurationError ex) {
            throw serviceConfigurationException(serviceType, ex);
        }
    }

    private static <T> T next(Iterator<T> iterator, Class<?> serviceType) {
        try {
            return iterator.next();
        } catch (ServiceConfigurationError ex) {
            throw serviceConfigurationException(serviceType, ex);
        }
    }

    private static ExtensionLoadingException serviceConfigurationException(
            Class<?> serviceType,
            ServiceConfigurationError error
    ) {
        return new ExtensionLoadingException("Could not load service providers for "
                + serviceType.getName() + ": " + messageOf(error), error);
    }

    private static ClassLoader defaultClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }
        return JavaspecExtensionLoader.class.getClassLoader();
    }

    private static ClassLoader effectiveClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            return classLoader;
        }
        return defaultClassLoader();
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
