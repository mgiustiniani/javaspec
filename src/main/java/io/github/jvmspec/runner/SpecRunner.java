package io.github.jvmspec.runner;

import io.github.jvmspec.api.ExampleDataRowRecorder;
import io.github.jvmspec.api.ObjectBehavior;
import io.github.jvmspec.api.Pending;
import io.github.jvmspec.api.PendingExampleException;
import io.github.jvmspec.api.Skip;
import io.github.jvmspec.api.SkipExampleException;
import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecExample;
import io.github.jvmspec.doubles.Doubles;
import io.github.jvmspec.doubles.InterfaceDouble;
import io.github.jvmspec.doubles.prophecy.BaseObjectProphecy;
import io.github.jvmspec.doubles.prophecy.PredictionRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        return run(specs, classLoader, false, true);
    }

    public static RunResult run(List<DiscoveredSpec> specs, ClassLoader classLoader, boolean stopOnFailure) {
        return run(specs, classLoader, stopOnFailure, true);
    }

    /**
     * Runs all specs with auto-check predictions.
     *
     * @param specs                the specs to run
     * @param classLoader          the class loader for spec class loading
     * @param stopOnFailure        whether to stop after the first failure
     * @param autoCheckPredictions whether to automatically verify prophecy predictions
     * @return the run result
     */
    public static RunResult run(List<DiscoveredSpec> specs, ClassLoader classLoader, boolean stopOnFailure,
                                boolean autoCheckPredictions) {
        Objects.requireNonNull(specs, "specs must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        List<SpecResult> results = new ArrayList<SpecResult>();
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = Objects.requireNonNull(specs.get(i), "specs[" + i + "] must not be null");
            SpecResult result = runSpec(spec, classLoader, stopOnFailure, autoCheckPredictions);
            results.add(result);
            if (stopOnFailure && result.hasFailures()) {
                break;
            }
        }
        return RunResult.of(results);
    }

    public static RunResult run(DiscoveredSpec spec, ClassLoader classLoader) {
        return run(spec, classLoader, false, true);
    }

    public static RunResult run(DiscoveredSpec spec, ClassLoader classLoader, boolean stopOnFailure) {
        return run(spec, classLoader, stopOnFailure, true);
    }

    public static RunResult run(DiscoveredSpec spec, ClassLoader classLoader, boolean stopOnFailure,
                                boolean autoCheckPredictions) {
        Objects.requireNonNull(spec, "spec must not be null");
        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        specs.add(spec);
        return run(specs, classLoader, stopOnFailure, autoCheckPredictions);
    }

    private static SpecResult runSpec(DiscoveredSpec spec, ClassLoader classLoader, boolean stopOnFailure,
                                       boolean autoCheckPredictions) {
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
            ExampleResult result = runExample(spec, specClass, examples.get(i), autoCheckPredictions);
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

    private static ExampleResult runExample(DiscoveredSpec spec, Class<?> specClass, SpecExample example,
                                             boolean autoCheckPredictions) {
        Method exampleMethod;
        try {
            exampleMethod = publicVoidMethod(specClass, example.methodName());
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
            letMethod = optionalPublicVoidMethod(specClass, LET_METHOD);
            letGoMethod = optionalPublicVoidMethod(specClass, LET_GO_METHOD);
        } catch (Throwable ex) {
            return ExampleResult.broken(spec, example, "Could not inspect lifecycle methods", unwrap(ex));
        }

        Object instance;
        try {
            instance = newSpecInstance(specClass);
            if (autoCheckPredictions && instance instanceof ObjectBehavior) {
                ((ObjectBehavior<?>) instance).setAutoCheckPredictions(true);
            }
        } catch (Throwable ex) {
            return ExampleResult.broken(spec, example, "Could not instantiate specification", unwrap(ex));
        }

        Throwable letFailure = null;
        Throwable exampleFailure = null;
        Throwable letGoFailure = null;
        ExampleSignal runtimeSignal = null;

        InvocationContext invocationContext = new InvocationContext(instance);
        ExampleDataRowRecorder.start();
        try {
            if (letMethod != null) {
                letFailure = invoke(letMethod, instance, invocationContext);
                runtimeSignal = signalFor(letFailure);
            }
            if (letFailure == null && runtimeSignal == null) {
                exampleFailure = invoke(exampleMethod, instance, invocationContext);
                runtimeSignal = signalFor(exampleFailure);
            }
        } finally {
            if (letGoMethod != null) {
                letGoFailure = invoke(letGoMethod, instance, invocationContext);
            }
        }

        if (letGoFailure != null) {
            Throwable primary = letGoFailure;
            if (runtimeSignal != null) {
                primary = suppress(primary, runtimeSignal.throwable());
                return withExampleDataRows(ExampleResult.broken(spec, example,
                        "letGo() failed after " + runtimeSignal.label() + " example", primary));
            }
            if (letFailure != null) {
                primary = suppress(primary, letFailure);
                return withExampleDataRows(ExampleResult.broken(spec, example, "letGo() failed after let() failure", primary));
            }
            if (exampleFailure != null) {
                primary = suppress(primary, exampleFailure);
                return withExampleDataRows(ExampleResult.broken(spec, example, "letGo() failed after example execution", primary));
            }
            return withExampleDataRows(ExampleResult.broken(spec, example, "letGo() failed", primary));
        }

        if (runtimeSignal != null) {
            return withExampleDataRows(resultForSignal(spec, example, runtimeSignal));
        }
        if (letFailure != null) {
            return withExampleDataRows(ExampleResult.broken(spec, example, "let() failed", letFailure));
        }
        if (exampleFailure != null) {
            if (exampleFailure instanceof AssertionError) {
                return withExampleDataRows(ExampleResult.failed(spec, example, "Assertion failed", exampleFailure));
            }
            return withExampleDataRows(ExampleResult.broken(spec, example, "Example method threw an unexpected throwable", exampleFailure));
        }
        // Auto-check predictions if enabled on the spec instance.
        if (instance instanceof ObjectBehavior) {
            try {
                ((ObjectBehavior<?>) instance).checkPredictionsIfEnabled();
            } catch (AssertionError ex) {
                return withExampleDataRows(ExampleResult.failed(spec, example, "Assertion failed", ex));
            } catch (Throwable ex) {
                return withExampleDataRows(ExampleResult.broken(spec, example,
                        "Automatic prediction checking threw an unexpected throwable", ex));
            }
        }
        return withExampleDataRows(ExampleResult.passed(spec, example));
    }

    private static ExampleResult withExampleDataRows(ExampleResult result) {
        return result.withExampleDataRows(ExampleDataRowRecorder.finish());
    }

    private static String exampleMethodNotFoundReason(SpecExample example) {
        return "Example method not found or not public void: " + example.methodName()
                + ". The discovered specification source may not match the compiled specification class available "
                + "to the runner. Recompile test/spec sources so the compiled class contains a public "
                + "void example method.";
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

    private static Method optionalPublicVoidMethod(Class<?> specClass, String methodName) throws NoSuchMethodException {
        try {
            return publicVoidMethod(specClass, methodName);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static Method publicVoidMethod(Class<?> specClass, String methodName) throws NoSuchMethodException {
        Method selected = null;
        Method[] methods = specClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!methodName.equals(method.getName())) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers()) || !Void.TYPE.equals(method.getReturnType())) {
                continue;
            }
            if (selected != null) {
                throw new IllegalArgumentException("Ambiguous public void method overloads for " + methodName
                        + ". javaspec supports a single example/lifecycle method with that name.");
            }
            selected = method;
        }
        if (selected == null) {
            throw new NoSuchMethodException(methodName);
        }
        if (!selected.isAccessible()) {
            selected.setAccessible(true);
        }
        return selected;
    }

    private static Throwable invoke(Method method, Object instance, InvocationContext invocationContext) {
        try {
            method.invoke(instance, invocationContext.argumentsFor(method));
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

    private static final class InvocationContext {
        private final Object specInstance;
        private final Map<Class<?>, Object> resolvedParameters = new LinkedHashMap<Class<?>, Object>();

        private InvocationContext(Object specInstance) {
            this.specInstance = specInstance;
        }

        Object[] argumentsFor(Method method) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                arguments[i] = argumentFor(parameterTypes[i]);
            }
            return arguments;
        }

        private Object argumentFor(Class<?> parameterType) {
            Object existing = resolvedParameters.get(parameterType);
            if (existing != null) {
                return existing;
            }
            Object created = createArgument(parameterType);
            resolvedParameters.put(parameterType, created);
            return created;
        }

        private Object createArgument(Class<?> parameterType) {
            if (BaseObjectProphecy.class.isAssignableFrom(parameterType)) {
                return createProphecyWrapper(parameterType);
            }
            if (parameterType.isInterface()) {
                return Doubles.interfaceDouble(parameterType).instance();
            }
            throw new IllegalArgumentException("Unsupported parameter type for javaspec injection: "
                    + parameterType.getName()
                    + ". Supported parameters are generated *Prophecy wrappers and ordinary interfaces.");
        }

        private Object createProphecyWrapper(Class<?> wrapperType) {
            if (!(specInstance instanceof ObjectBehavior)) {
                throw new IllegalArgumentException("Cannot inject prophecy wrapper " + wrapperType.getName()
                        + " because the spec does not extend ObjectBehavior.");
            }
            Class<?> prophesizedType = prophesizedTypeOf(wrapperType);
            if (prophesizedType == null) {
                throw new IllegalArgumentException("Cannot determine the prophesized type for " + wrapperType.getName()
                        + ". Generated *Prophecy wrappers must extend ObjectProphecy<T> with a concrete type.");
            }
            InterfaceDouble<?> handle = prophesizedType.isInterface()
                    ? Doubles.interfaceDouble(prophesizedType)
                    : Doubles.concreteDouble(prophesizedType);
            PredictionRegistry registry = prophecyRegistryOf((ObjectBehavior<?>) specInstance);
            try {
                Constructor<?> constructor = wrapperType.getConstructor(InterfaceDouble.class, PredictionRegistry.class);
                return constructor.newInstance(handle, registry);
            } catch (InvocationTargetException ex) {
                throw new IllegalArgumentException("Could not instantiate prophecy wrapper " + wrapperType.getName()
                        + ": " + throwableSummary(invocationCause(ex)), invocationCause(ex));
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate prophecy wrapper " + wrapperType.getName()
                        + ". Expected a public constructor (InterfaceDouble, PredictionRegistry).", ex);
            }
        }

        private static Class<?> prophesizedTypeOf(Class<?> wrapperType) {
            Class<?> current = wrapperType;
            while (current != null && !Object.class.equals(current)) {
                Type genericSuperclass = current.getGenericSuperclass();
                Class<?> extracted = prophesizedTypeFrom(genericSuperclass);
                if (extracted != null) {
                    return extracted;
                }
                current = current.getSuperclass();
            }
            return null;
        }

        private static Class<?> prophesizedTypeFrom(Type type) {
            if (!(type instanceof ParameterizedType)) {
                return null;
            }
            ParameterizedType parameterized = (ParameterizedType) type;
            Type rawType = parameterized.getRawType();
            if (!(rawType instanceof Class)) {
                return null;
            }
            Class<?> rawClass = (Class<?>) rawType;
            if (!BaseObjectProphecy.class.isAssignableFrom(rawClass)) {
                return null;
            }
            Type argument = parameterized.getActualTypeArguments()[0];
            if (argument instanceof Class) {
                return (Class<?>) argument;
            }
            if (argument instanceof ParameterizedType) {
                Type rawArgument = ((ParameterizedType) argument).getRawType();
                if (rawArgument instanceof Class) {
                    return (Class<?>) rawArgument;
                }
            }
            return null;
        }

        private static PredictionRegistry prophecyRegistryOf(ObjectBehavior<?> behavior) {
            try {
                Method registryMethod = ObjectBehavior.class.getDeclaredMethod("prophecyRegistry");
                if (!registryMethod.isAccessible()) {
                    registryMethod.setAccessible(true);
                }
                return (PredictionRegistry) registryMethod.invoke(behavior);
            } catch (InvocationTargetException ex) {
                Throwable cause = invocationCause(ex);
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new IllegalArgumentException("Could not access the spec prophecy registry: "
                        + throwableSummary(cause), cause);
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not access the spec prophecy registry.", ex);
            }
        }
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
