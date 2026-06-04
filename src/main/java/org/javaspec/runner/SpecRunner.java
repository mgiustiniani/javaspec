package org.javaspec.runner;

import org.javaspec.api.Pending;
import org.javaspec.api.PendingExampleException;
import org.javaspec.api.Skip;
import org.javaspec.api.SkipExampleException;
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
        return run(specs, classLoader, false);
    }

    public static RunResult run(List<DiscoveredSpec> specs, ClassLoader classLoader, boolean stopOnFailure) {
        Objects.requireNonNull(specs, "specs must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        List<SpecResult> results = new ArrayList<SpecResult>();
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = Objects.requireNonNull(specs.get(i), "specs[" + i + "] must not be null");
            SpecResult result = runSpec(spec, classLoader, stopOnFailure);
            results.add(result);
            if (stopOnFailure && result.hasFailures()) {
                break;
            }
        }
        return RunResult.of(results);
    }

    public static RunResult run(DiscoveredSpec spec, ClassLoader classLoader) {
        return run(spec, classLoader, false);
    }

    public static RunResult run(DiscoveredSpec spec, ClassLoader classLoader, boolean stopOnFailure) {
        Objects.requireNonNull(spec, "spec must not be null");
        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        specs.add(spec);
        return run(specs, classLoader, stopOnFailure);
    }

    private static SpecResult runSpec(DiscoveredSpec spec, ClassLoader classLoader, boolean stopOnFailure) {
        Class<?> specClass;
        try {
            specClass = Class.forName(spec.specQualifiedName(), false, classLoader);
        } catch (ClassNotFoundException ex) {
            return skippedSpec(spec, specClassNotFoundReason(spec));
        } catch (NoClassDefFoundError ex) {
            return skippedSpec(spec, specClassCouldNotLoadReason(spec, ex));
        } catch (LinkageError ex) {
            return skippedSpec(spec, specClassCouldNotLoadReason(spec, ex));
        } catch (SecurityException ex) {
            return skippedSpec(spec, specClassCouldNotLoadReason(spec, ex));
        }

        List<ExampleResult> results = new ArrayList<ExampleResult>();
        List<SpecExample> examples = spec.exampleMetadata();
        for (int i = 0; i < examples.size(); i++) {
            ExampleResult result = runExample(spec, specClass, examples.get(i));
            results.add(result);
            if (stopOnFailure && result.isFailure()) {
                break;
            }
        }
        return SpecResult.executable(spec, results);
    }

    private static String specClassNotFoundReason(DiscoveredSpec spec) {
        return "Specification class not found: " + spec.specQualifiedName()
                + ". The specification source was discovered, but the compiled specification class is not available "
                + "to the runner classloader. Compile the spec/test sources and add the compiled output and "
                + "required dependencies to the javaspec classpath.";
    }

    private static String specClassCouldNotLoadReason(DiscoveredSpec spec, Throwable throwable) {
        return "Specification class could not be loaded: " + spec.specQualifiedName()
                + " (" + throwableSummary(throwable) + "). The specification source was discovered, but the "
                + "compiled specification class or one of its dependencies is not available to the runner "
                + "classloader. Compile the spec/test sources and add the compiled output and required "
                + "dependencies to the javaspec classpath.";
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
            return ExampleResult.skipped(spec, example, exampleMethodNotFoundReason(example));
        } catch (Throwable ex) {
            return ExampleResult.broken(spec, example, "Could not inspect example method: " + example.methodName(), unwrap(ex));
        }

        ExampleSignal annotationSignal;
        try {
            annotationSignal = annotationSignalFor(exampleMethod);
        } catch (Throwable ex) {
            return ExampleResult.broken(spec, example, "Could not inspect example method annotations: " + example.methodName(), unwrap(ex));
        }
        if (annotationSignal != null) {
            return resultForSignal(spec, example, annotationSignal);
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
        ExampleSignal runtimeSignal = null;

        try {
            if (letMethod != null) {
                letFailure = invoke(letMethod, instance);
                runtimeSignal = signalFor(letFailure);
            }
            if (letFailure == null && runtimeSignal == null) {
                exampleFailure = invoke(exampleMethod, instance);
                runtimeSignal = signalFor(exampleFailure);
            }
        } finally {
            if (letGoMethod != null) {
                letGoFailure = invoke(letGoMethod, instance);
            }
        }

        if (letGoFailure != null) {
            Throwable primary = letGoFailure;
            if (runtimeSignal != null) {
                primary = suppress(primary, runtimeSignal.throwable());
                return ExampleResult.broken(spec, example,
                        "letGo() failed after " + runtimeSignal.label() + " example", primary);
            }
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

        if (runtimeSignal != null) {
            return resultForSignal(spec, example, runtimeSignal);
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

    private static String exampleMethodNotFoundReason(SpecExample example) {
        return "Example method not found or not public no-arg: " + example.methodName()
                + ". The discovered specification source may not match the compiled specification class available "
                + "to the runner. Recompile test/spec sources so the compiled class contains a public "
                + "no-argument example method.";
    }

    private static ExampleSignal annotationSignalFor(Method exampleMethod) {
        Skip skip = exampleMethod.getAnnotation(Skip.class);
        if (skip != null) {
            // Deterministic precedence: @Skip wins over @Pending when both markers are present.
            return ExampleSignal.skipped(annotationReason(skip.value(), skip.reason(), "Skipped by javaspec."), null);
        }
        Pending pending = exampleMethod.getAnnotation(Pending.class);
        if (pending != null) {
            return ExampleSignal.pending(annotationReason(pending.value(), pending.reason(), "Pending by javaspec."), null);
        }
        return null;
    }

    private static ExampleSignal signalFor(Throwable throwable) {
        if (throwable instanceof SkipExampleException) {
            return ExampleSignal.skipped(exceptionReason((SkipExampleException) throwable, "Skipped by javaspec."), throwable);
        }
        if (throwable instanceof PendingExampleException) {
            return ExampleSignal.pending(exceptionReason((PendingExampleException) throwable, "Pending by javaspec."), throwable);
        }
        return null;
    }

    private static ExampleResult resultForSignal(DiscoveredSpec spec, SpecExample example, ExampleSignal signal) {
        if (signal.isSkipped()) {
            return ExampleResult.skipped(spec, example, signal.detail());
        }
        return ExampleResult.pending(spec, example, signal.detail());
    }

    private static String annotationReason(String value, String reason, String defaultReason) {
        if (!isBlank(reason)) {
            return reason;
        }
        if (!isBlank(value)) {
            return value;
        }
        return defaultReason;
    }

    private static String exceptionReason(SkipExampleException exception, String defaultReason) {
        if (!isBlank(exception.reason())) {
            return exception.reason();
        }
        if (!isBlank(exception.getMessage())) {
            return exception.getMessage();
        }
        return defaultReason;
    }

    private static String exceptionReason(PendingExampleException exception, String defaultReason) {
        if (!isBlank(exception.reason())) {
            return exception.reason();
        }
        if (!isBlank(exception.getMessage())) {
            return exception.getMessage();
        }
        return defaultReason;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
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

    private static final class ExampleSignal {
        private final ExampleStatus status;
        private final String detail;
        private final Throwable throwable;

        private ExampleSignal(ExampleStatus status, String detail, Throwable throwable) {
            this.status = status;
            this.detail = detail;
            this.throwable = throwable;
        }

        static ExampleSignal skipped(String detail, Throwable throwable) {
            return new ExampleSignal(ExampleStatus.SKIPPED, detail, throwable);
        }

        static ExampleSignal pending(String detail, Throwable throwable) {
            return new ExampleSignal(ExampleStatus.PENDING, detail, throwable);
        }

        boolean isSkipped() {
            return ExampleStatus.SKIPPED.equals(status);
        }

        String label() {
            if (isSkipped()) {
                return "skipped";
            }
            return "pending";
        }

        String detail() {
            return detail;
        }

        Throwable throwable() {
            return throwable;
        }
    }
}
