package io.github.jvmspec.extension;

import io.github.jvmspec.formatter.RunFormatterRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import io.github.jvmspec.extension.ExtensionCatalog;

/**
 * Activates configuration-declared javaspec extensions against an existing run formatter registry.
 *
 * <p>Unlike {@link JavaspecExtensionLoader}, which discovers providers through the JDK
 * {@code ServiceLoader}, this activator receives an explicit ordered list of fully qualified
 * extension class names (typically the {@code extensions} key of a javaspec configuration file)
 * and configures them one by one.</p>
 *
 * <p>Semantics:</p>
 * <ul>
 *   <li><b>Ordering:</b> entries are activated strictly in list order; duplicate entries are
 *   preserved and configured once per occurrence, consistent with bootstrap hook semantics.</li>
 *   <li><b>Classloader:</b> every class is loaded from the supplied classloader, which must be
 *   the same classloader used to build the target registry (the effective run classloader).</li>
 *   <li><b>Failure:</b> a class that cannot be found, does not implement
 *   {@link JavaspecExtension} (or its {@link Extension} alias), lacks a public no-argument
 *   constructor, fails to instantiate, or throws from {@code configure(ExtensionContext)}
 *   raises an {@link ExtensionLoadingException} naming the offending class. Name-resolution
 *   failures additionally list the ServiceLoader-discovered extension provider names.</li>
 * </ul>
 */
public final class JavaspecExtensionActivator {
    private JavaspecExtensionActivator() {
    }

    /**
     * Activates the given extension class names, in order, against the supplied registry.
     *
     * @param extensionClassNames ordered, duplicate-preserving list of fully qualified class
     *                            names of {@link JavaspecExtension} (or {@link Extension})
     *                            implementations; an empty list is a no-op
     * @param classLoader         classloader used to resolve and instantiate every entry
     * @param registry            registry exposed to extensions through {@link ExtensionContext}
     * @throws ExtensionLoadingException when any entry cannot be loaded, instantiated, or configured
     */
    public static void activate(
            List<String> extensionClassNames,
            ClassLoader classLoader,
            RunFormatterRegistry registry
    ) {
        Objects.requireNonNull(registry, "registry must not be null");
        activate(extensionClassNames, classLoader, ExtensionContext.of(registry));
    }

    /**
     * Activates the given extension class names, in order, against the supplied extension context.
     *
     * @param extensionClassNames ordered, duplicate-preserving list of fully qualified class
     *                            names of {@link JavaspecExtension} (or {@link Extension})
     *                            implementations; an empty list is a no-op
     * @param classLoader         classloader used to resolve and instantiate every entry
     * @param context             context handed to each extension's {@code configure} call
     * @throws ExtensionLoadingException when any entry cannot be loaded, instantiated, or configured
     */
    public static void activate(
            List<String> extensionClassNames,
            ClassLoader classLoader,
            ExtensionContext context
    ) {
        Objects.requireNonNull(extensionClassNames, "extensionClassNames must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Objects.requireNonNull(context, "context must not be null");
        for (int i = 0; i < extensionClassNames.size(); i++) {
            String extensionClassName = extensionClassNameAt(extensionClassNames, i);
            activateExtension(extensionClassName, classLoader, context);
        }
    }

    private static String extensionClassNameAt(List<String> extensionClassNames, int index) {
        String extensionClassName = extensionClassNames.get(index);
        Objects.requireNonNull(extensionClassName, "extensionClassNames[" + index + "] must not be null");
        String trimmed = extensionClassName.trim();
        if (trimmed.length() == 0) {
            throw new ExtensionLoadingException("Configured extension class name at position "
                    + (index + 1) + " must not be blank.");
        }
        return trimmed;
    }

    private static void activateExtension(
            String extensionClassName,
            ClassLoader classLoader,
            ExtensionContext context
    ) {
        Class<?> extensionClass = loadExtensionClass(extensionClassName, classLoader);
        if (!JavaspecExtension.class.isAssignableFrom(extensionClass)) {
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' does not implement " + JavaspecExtension.class.getName()
                    + " (or the alias " + Extension.class.getName() + ").");
        }
        JavaspecExtension extension = instantiateExtension(extensionClassName, extensionClass);
        try {
            extension.register(context);
        } catch (RuntimeException ex) {
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' could not be configured: " + messageOf(ex), ex);
        }
    }

    private static Class<?> loadExtensionClass(String extensionClassName, ClassLoader classLoader) {
        try {
            return Class.forName(extensionClassName, true, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' was not found on the run classpath. Discovered extension providers: "
                    + discoveredProviderNames(classLoader) + "."
                    + System.lineSeparator()
                    + ExtensionCatalog.classpathRepairSuggestion(extensionClassName), ex);
        } catch (LinkageError ex) {
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' could not be loaded: " + messageOf(ex), ex);
        } catch (SecurityException ex) {
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' could not be loaded: " + messageOf(ex), ex);
        }
    }

    private static JavaspecExtension instantiateExtension(String extensionClassName, Class<?> extensionClass) {
        Constructor<?> constructor;
        try {
            constructor = extensionClass.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' must declare a public no-argument constructor.", ex);
        } catch (SecurityException ex) {
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' public no-argument constructor is not accessible: " + messageOf(ex), ex);
        }

        try {
            return (JavaspecExtension) constructor.newInstance();
        } catch (InstantiationException ex) {
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' could not be instantiated: " + messageOf(ex), ex);
        } catch (IllegalAccessException ex) {
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' could not be instantiated: " + messageOf(ex), ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new ExtensionLoadingException("Configured extension '" + extensionClassName
                    + "' constructor threw an exception: " + messageOf(cause), cause);
        }
    }

    /**
     * Best-effort, deterministic listing of ServiceLoader-discovered extension provider class
     * names, used to enrich name-resolution diagnostics. Providers that fail to load during
     * this diagnostic pass are silently skipped.
     */
    private static String discoveredProviderNames(ClassLoader classLoader) {
        Set<String> providerNames = new LinkedHashSet<String>();
        collectProviderNames(JavaspecExtension.class, classLoader, providerNames);
        collectProviderNames(Extension.class, classLoader, providerNames);
        if (providerNames.isEmpty()) {
            return "<none>";
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String providerName : providerNames) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(providerName);
            index++;
        }
        return builder.toString();
    }

    private static <T extends JavaspecExtension> void collectProviderNames(
            Class<T> serviceType,
            ClassLoader classLoader,
            Set<String> providerNames
    ) {
        try {
            Iterator<T> iterator = ServiceLoader.load(serviceType, classLoader).iterator();
            while (iterator.hasNext()) {
                T provider = iterator.next();
                if (provider != null) {
                    providerNames.add(provider.getClass().getName());
                }
            }
        } catch (ServiceConfigurationError ignored) {
            // Diagnostic collection is best-effort; broken providers are reported elsewhere.
        } catch (RuntimeException ignored) {
            // Diagnostic collection is best-effort; broken providers are reported elsewhere.
        }
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }
}
