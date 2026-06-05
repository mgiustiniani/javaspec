package org.javaspec.bootstrap;

import org.javaspec.discovery.DiscoveredSpec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

/**
 * Executes configured bootstrap hook classes in declaration order.
 */
public final class BootstrapRunner {
    private BootstrapRunner() {
    }

    public static void run(List<String> hookClassNames, ClassLoader classLoader, List<DiscoveredSpec> discoveredSpecs) {
        Objects.requireNonNull(hookClassNames, "hookClassNames must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Objects.requireNonNull(discoveredSpecs, "discoveredSpecs must not be null");
        if (hookClassNames.isEmpty()) {
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
        String message = root.getMessage();
        if (message == null || message.length() == 0) {
            return root.getClass().getName();
        }
        return message;
    }
}
