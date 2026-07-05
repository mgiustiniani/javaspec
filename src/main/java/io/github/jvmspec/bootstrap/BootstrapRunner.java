package io.github.jvmspec.bootstrap;

import io.github.jvmspec.discovery.DiscoveredSpec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Executes explicit bootstrap hook classes and optional ServiceLoader-discovered providers.
 */
public final class BootstrapRunner {
    private BootstrapRunner() {
    }

    /**
     * Executes only the explicitly configured bootstrap hooks in declaration order.
     *
     * <p>This preserves the original default behavior: ServiceLoader discovery is disabled and
     * an empty hook list is a no-op.</p>
     */
    public static void run(List<String> hookClassNames, ClassLoader classLoader, List<DiscoveredSpec> discoveredSpecs) {
        run(hookClassNames, classLoader, discoveredSpecs, false);
    }

    /**
     * Executes bootstrap hooks for one run.
     *
     * <p>Explicit configured hook class names always run first, in the supplied order, with
     * duplicates preserved. ServiceLoader discovery of {@link BootstrapHook} providers is opt-in
     * and disabled by default in callers. When {@code bootstrapDiscovery} is {@code true},
     * providers are discovered from the supplied run classloader with the thread context
     * classloader temporarily set to that same classloader. Discovered providers execute after
     * explicit hooks in deterministic provider implementation class name order, with discovery
     * index used only as a stable tie-break for equal names.</p>
     *
     * @param hookClassNames     explicit hook class names; entries run first in list order
     * @param classLoader        run classloader used for explicit hook loading, ServiceLoader
     *                           discovery, and the hook execution context
     * @param discoveredSpecs    specs selected for this run
     * @param bootstrapDiscovery whether to discover {@link BootstrapHook} providers through
     *                           {@link ServiceLoader}
     * @throws BootstrapException when explicit hook loading/execution or ServiceLoader provider
     *                            discovery/execution fails
     */
    public static void run(
            List<String> hookClassNames,
            ClassLoader classLoader,
            List<DiscoveredSpec> discoveredSpecs,
            boolean bootstrapDiscovery
    ) {
        Objects.requireNonNull(hookClassNames, "hookClassNames must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Objects.requireNonNull(discoveredSpecs, "discoveredSpecs must not be null");
        if (hookClassNames.isEmpty() && !bootstrapDiscovery) {
            return;
        }

        BootstrapContext context = BootstrapContext.of(classLoader, discoveredSpecs);
        Thread currentThread = Thread.currentThread();
        ClassLoader originalContextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(classLoader);
        try {
            for (int i = 0; i < hookClassNames.size(); i++) {
                executeHook(hookClassNameAt(hookClassNames, i), classLoader, context);
            }
            if (bootstrapDiscovery) {
                executeDiscoveredHooks(classLoader, context);
            }
        } finally {
            currentThread.setContextClassLoader(originalContextClassLoader);
        }
    }

    private static void executeHook(String hookClassName, ClassLoader classLoader, BootstrapContext context) {
        Class<?> hookClass = loadHookClass(hookClassName, classLoader);
        if (!BootstrapHook.class.isAssignableFrom(hookClass)) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' does not implement " + BootstrapHook.class.getName() + ".");
        }
        BootstrapHook hook = instantiateHook(hookClassName, hookClass);
        try {
            hook.bootstrap(context);
        } catch (Throwable ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' threw an exception: " + rootCauseMessage(ex) + ".", ex);
        }
    }

    private static void executeDiscoveredHooks(ClassLoader classLoader, BootstrapContext context) {
        List<DiscoveredBootstrapHook> discoveredHooks = discoverBootstrapHooks(classLoader);
        if (discoveredHooks.isEmpty()) {
            return;
        }
        Collections.sort(discoveredHooks, discoveredHookOrder());
        for (int i = 0; i < discoveredHooks.size(); i++) {
            executeDiscoveredHook(discoveredHooks.get(i), context);
        }
    }

    private static List<DiscoveredBootstrapHook> discoverBootstrapHooks(ClassLoader classLoader) {
        List<DiscoveredBootstrapHook> discoveredHooks = new ArrayList<DiscoveredBootstrapHook>();
        ServiceLoader<BootstrapHook> loader = ServiceLoader.load(BootstrapHook.class, classLoader);
        Iterator<BootstrapHook> iterator = loader.iterator();
        int index = 0;
        while (hasNext(iterator)) {
            BootstrapHook hook = next(iterator);
            if (hook == null) {
                throw new BootstrapException("ServiceLoader provider for "
                        + BootstrapHook.class.getName() + " returned null.");
            }
            discoveredHooks.add(DiscoveredBootstrapHook.of(hook, index));
            index++;
        }
        return discoveredHooks;
    }

    private static Comparator<DiscoveredBootstrapHook> discoveredHookOrder() {
        return new Comparator<DiscoveredBootstrapHook>() {
            @Override
            public int compare(DiscoveredBootstrapHook left, DiscoveredBootstrapHook right) {
                int byClassName = left.className().compareTo(right.className());
                if (byClassName != 0) {
                    return byClassName;
                }
                if (left.discoveryIndex() < right.discoveryIndex()) {
                    return -1;
                }
                if (left.discoveryIndex() > right.discoveryIndex()) {
                    return 1;
                }
                return 0;
            }
        };
    }

    private static void executeDiscoveredHook(DiscoveredBootstrapHook discoveredHook, BootstrapContext context) {
        try {
            discoveredHook.hook().bootstrap(context);
        } catch (Throwable ex) {
            throw new BootstrapException("ServiceLoader BootstrapHook provider '"
                    + discoveredHook.className() + "' threw an exception: "
                    + rootCauseMessage(ex) + ".", ex);
        }
    }

    private static boolean hasNext(Iterator<BootstrapHook> iterator) {
        try {
            return iterator.hasNext();
        } catch (ServiceConfigurationError ex) {
            throw serviceConfigurationException(ex);
        }
    }

    private static BootstrapHook next(Iterator<BootstrapHook> iterator) {
        try {
            return iterator.next();
        } catch (ServiceConfigurationError ex) {
            throw serviceConfigurationException(ex);
        }
    }

    private static BootstrapException serviceConfigurationException(ServiceConfigurationError error) {
        String detail = messageOf(error);
        String providerName = providerNameFrom(detail);
        if (providerName == null) {
            return new BootstrapException("Could not load ServiceLoader providers for "
                    + BootstrapHook.class.getName() + ": " + detail + ".", error);
        }
        return new BootstrapException("Could not load ServiceLoader BootstrapHook provider '"
                + providerName + "' for " + BootstrapHook.class.getName() + ": " + detail + ".", error);
    }

    private static Class<?> loadHookClass(String hookClassName, ClassLoader classLoader) {
        try {
            return Class.forName(hookClassName, true, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' was not found: " + rootCauseMessage(ex) + ".", ex);
        } catch (LinkageError ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' could not be loaded: " + rootCauseMessage(ex) + ".", ex);
        } catch (SecurityException ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' could not be loaded: " + rootCauseMessage(ex) + ".", ex);
        }
    }

    private static BootstrapHook instantiateHook(String hookClassName, Class<?> hookClass) {
        Constructor<?> constructor;
        try {
            constructor = hookClass.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' must declare a public no-argument constructor.", ex);
        } catch (SecurityException ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' public no-argument constructor is not accessible: " + rootCauseMessage(ex) + ".", ex);
        }

        try {
            return (BootstrapHook) constructor.newInstance();
        } catch (InstantiationException ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' could not be instantiated: " + rootCauseMessage(ex) + ".", ex);
        } catch (IllegalAccessException ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' public no-argument constructor is not accessible: " + rootCauseMessage(ex) + ".", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' constructor failed: " + rootCauseMessage(cause) + ".", cause);
        } catch (ExceptionInInitializerError ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' initialization failed: " + rootCauseMessage(ex) + ".", ex);
        } catch (SecurityException ex) {
            throw new BootstrapException("Bootstrap hook '" + hookClassName
                    + "' could not be instantiated: " + rootCauseMessage(ex) + ".", ex);
        }
    }

    private static String hookClassNameAt(List<String> hookClassNames, int index) {
        String hookClassName = Objects.requireNonNull(hookClassNames.get(index),
                "hookClassNames[" + index + "] must not be null");
        String trimmed = hookClassName.trim();
        if (trimmed.length() == 0) {
            throw new BootstrapException("Bootstrap hook class name at index " + index + " must not be blank.");
        }
        return trimmed;
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return messageOf(root);
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return message;
    }

    private static String providerNameFrom(String message) {
        if (message == null) {
            return null;
        }
        int markerIndex = message.indexOf("Provider ");
        if (markerIndex < 0) {
            return null;
        }
        int start = markerIndex + "Provider ".length();
        while (start < message.length() && Character.isWhitespace(message.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < message.length()) {
            char character = message.charAt(end);
            if (character == '.' || Character.isJavaIdentifierPart(character)) {
                end++;
                continue;
            }
            break;
        }
        if (end == start) {
            return null;
        }
        return message.substring(start, end);
    }

    private static final class DiscoveredBootstrapHook {
        private final BootstrapHook hook;
        private final String className;
        private final int discoveryIndex;

        private DiscoveredBootstrapHook(BootstrapHook hook, String className, int discoveryIndex) {
            this.hook = hook;
            this.className = className;
            this.discoveryIndex = discoveryIndex;
        }

        static DiscoveredBootstrapHook of(BootstrapHook hook, int discoveryIndex) {
            Objects.requireNonNull(hook, "hook must not be null");
            return new DiscoveredBootstrapHook(hook, hook.getClass().getName(), discoveryIndex);
        }

        BootstrapHook hook() {
            return hook;
        }

        String className() {
            return className;
        }

        int discoveryIndex() {
            return discoveryIndex;
        }
    }
}
