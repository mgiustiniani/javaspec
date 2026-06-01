package org.javaspec.runner;

import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecExample;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Java 8-compatible, dependency-free reflection runner for discovered examples.
 */
public final class SpecRunner {
    private static final String LET_METHOD = "let";
    private static final String LET_GO_METHOD = "letGo";

    private SpecRunner() {
    }

    public static RunResult run(List<DiscoveredSpec> specs, ClassLoader classLoader) {
        Objects.requireNonNull(specs, "specs must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        List<SpecResult> results = new ArrayList<SpecResult>();
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = Objects.requireNonNull(specs.get(i), "specs[" + i + "] must not be null");
            results.add(runSpec(spec, classLoader));
        }
        return RunResult.of(results);
    }

    public static RunResult run(DiscoveredSpec spec, ClassLoader classLoader) {
        Objects.requireNonNull(spec, "spec must not be null");
        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        specs.add(spec);
        return run(specs, classLoader);
    }

    private static SpecResult runSpec(DiscoveredSpec spec, ClassLoader classLoader) {
        Class<?> specClass;
        try {
            specClass = Class.forName(spec.specQualifiedName(), false, classLoader);
        } catch (ClassNotFoundException ex) {
            return skippedSpec(spec, "Specification class not found: " + spec.specQualifiedName());
        } catch (NoClassDefFoundError ex) {
            return skippedSpec(spec, "Specification class could not be loaded: " + spec.specQualifiedName()
                    + " (" + throwableSummary(ex) + ")");
        } catch (LinkageError ex) {
            return skippedSpec(spec, "Specification class could not be loaded: " + spec.specQualifiedName()
                    + " (" + throwableSummary(ex) + ")");
        } catch (SecurityException ex) {
            return skippedSpec(spec, "Specification class could not be loaded: " + spec.specQualifiedName()
                    + " (" + throwableSummary(ex) + ")");
        }

        List<ExampleResult> results = new ArrayList<ExampleResult>();
        List<SpecExample> examples = spec.exampleMetadata();
        for (int i = 0; i < examples.size(); i++) {
            results.add(runExample(spec, specClass, examples.get(i)));
        }
        return SpecResult.executable(spec, results);
    }

    private static SpecResult skippedSpec(DiscoveredSpec spec, String reason) {
        List<ExampleResult> results = new ArrayList<ExampleResult>();
        List<SpecExample> examples = spec.exampleMetadata();
        for (int i = 0; i < examples.size(); i++) {
            results.add(ExampleResult.skipped(spec, examples.get(i), reason));
        }
        return SpecResult.notExecutable(spec, reason, results);
    }

    private static ExampleResult runExample(DiscoveredSpec spec, Class<?> specClass, SpecExample example) {
        Method exampleMethod;
        try {
            exampleMethod = publicNoArgMethod(specClass, example.methodName());
        } catch (NoSuchMethodException ex) {
            return ExampleResult.skipped(spec, example, "Example method not found or not public no-arg: " + example.methodName());
        } catch (Throwable ex) {
            return ExampleResult.broken(spec, example, "Could not inspect example method: " + example.methodName(), unwrap(ex));
        }

        Method letMethod;
        Method letGoMethod;
        try {
            letMethod = optionalPublicNoArgMethod(specClass, LET_METHOD);
            letGoMethod = optionalPublicNoArgMethod(specClass, LET_GO_METHOD);
        } catch (Throwable ex) {
            return ExampleResult.broken(spec, example, "Could not inspect lifecycle methods", unwrap(ex));
        }

        Object instance;
        try {
            instance = newSpecInstance(specClass);
        } catch (Throwable ex) {
            return ExampleResult.broken(spec, example, "Could not instantiate specification", unwrap(ex));
        }

        Throwable letFailure = null;
        Throwable exampleFailure = null;
        Throwable letGoFailure = null;

        try {
            if (letMethod != null) {
                letFailure = invoke(letMethod, instance);
            }
            if (letFailure == null) {
                exampleFailure = invoke(exampleMethod, instance);
            }
        } finally {
            if (letGoMethod != null) {
                letGoFailure = invoke(letGoMethod, instance);
            }
        }

        if (letGoFailure != null) {
            Throwable primary = letGoFailure;
            if (letFailure != null) {
                primary = suppress(primary, letFailure);
                return ExampleResult.broken(spec, example, "letGo() failed after let() failure", primary);
            }
            if (exampleFailure != null) {
                primary = suppress(primary, exampleFailure);
                return ExampleResult.broken(spec, example, "letGo() failed after example execution", primary);
            }
            return ExampleResult.broken(spec, example, "letGo() failed", primary);
        }

        if (letFailure != null) {
            return ExampleResult.broken(spec, example, "let() failed", letFailure);
        }
        if (exampleFailure != null) {
            if (exampleFailure instanceof AssertionError) {
                return ExampleResult.failed(spec, example, "Assertion failed", exampleFailure);
            }
            return ExampleResult.broken(spec, example, "Example method threw an unexpected throwable", exampleFailure);
        }
        return ExampleResult.passed(spec, example);
    }

    private static Object newSpecInstance(Class<?> specClass) throws Throwable {
        Constructor<?> constructor = specClass.getDeclaredConstructor();
        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        try {
            return constructor.newInstance();
        } catch (InvocationTargetException ex) {
            throw invocationCause(ex);
        }
    }

    private static Method optionalPublicNoArgMethod(Class<?> specClass, String methodName) throws NoSuchMethodException {
        try {
            return publicNoArgMethod(specClass, methodName);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static Method publicNoArgMethod(Class<?> specClass, String methodName) throws NoSuchMethodException {
        Method method = specClass.getMethod(methodName);
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new NoSuchMethodException(methodName);
        }
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        return method;
    }

    private static Throwable invoke(Method method, Object instance) {
        try {
            method.invoke(instance);
            return null;
        } catch (InvocationTargetException ex) {
            return invocationCause(ex);
        } catch (Throwable ex) {
            return ex;
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof InvocationTargetException) {
            return invocationCause((InvocationTargetException) throwable);
        }
        return throwable;
    }

    private static Throwable invocationCause(InvocationTargetException ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return ex;
        }
        return cause;
    }

    private static Throwable suppress(Throwable primary, Throwable secondary) {
        if (secondary != null && secondary != primary) {
            primary.addSuppressed(secondary);
        }
        return primary;
    }

    private static String throwableSummary(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.length() == 0) {
            return throwable.getClass().getName();
        }
        return throwable.getClass().getName() + ": " + message;
    }
}
