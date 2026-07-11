package io.github.jvmspec.api;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * Encapsulates the lazy construction lifecycle for a subject under test.
 * <p>Extracted from {@link ObjectBehavior} to reduce responsibility concentration.
 * {@link ObjectBehavior} owns a {@code SubjectLifecycle} instance and delegates
 * subject-related calls to it.</p>
 *
 * @param <T> the subject type this lifecycle manages
 */
public class SubjectLifecycle<T> {
    private final Class<? extends T> subjectType;
    private T subject;
    private boolean subjectInstantiated;
    private Construction construction;

    /**
     * Creates a new lifecycle with no subject type configured.
     */
    public SubjectLifecycle() {
        this(null);
    }

    /**
     * Creates a new lifecycle with a subject type for lazy construction.
     *
     * @param subjectType the concrete subject type to instantiate lazily
     */
    public SubjectLifecycle(Class<? extends T> subjectType) {
        this.subjectType = subjectType;
        this.construction = Construction.withDefaultConstructor(new Object[0]);
    }

    /**
     * Returns the subject instance, constructing it lazily when a subject type is configured.
     */
    public T subject() {
        if (!subjectInstantiated && subjectType != null) {
            try {
                subject = constructSubject();
                subjectInstantiated = true;
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Error err) {
                throw err;
            } catch (Throwable ex) {
                throw new IllegalStateException("Could not instantiate subject " + subjectType.getName(), ex);
            }
        }
        return subject;
    }

    /**
     * Returns the subject type, or {@code null} if none was configured.
     */
    public Class<? extends T> getSubjectType() {
        return subjectType;
    }

    /**
     * Returns whether the subject has been instantiated yet.
     */
    public boolean isInstantiated() {
        return subjectInstantiated;
    }

    /**
     * Sets the subject instance. Called by the runner during lifecycle setup.
     */
    public void setSubject(T subject) {
        this.subject = subject;
        this.subjectInstantiated = true;
    }

    /**
     * Configures lazy subject construction through a constructor with the given arguments.
     */
    public void beConstructedWith(Object... args) {
        ensureConstructionCanChange();
        construction = Construction.withExplicitConstructor(copyArgs(args));
    }

    /**
     * Configures lazy subject construction through a static factory method.
     */
    public void beConstructedThrough(String methodName, Object... args) {
        ensureConstructionCanChange();
        construction = Construction.withFactory(validConstructionName(methodName, "methodName"), copyArgs(args));
    }

    /**
     * Configures lazy subject construction through a named static factory method.
     */
    public void beConstructedNamed(String name, Object... args) {
        ensureConstructionCanChange();
        construction = Construction.withFactory(validConstructionName(name, "name"), copyArgs(args));
    }

    /**
     * Configures lazy subject construction through a named static factory method.
     */
    public void beConstructedNamed(String name) {
        beConstructedNamed(name, new Object[0]);
    }

    /**
     * Configures lazy subject construction through a named static factory method.
     */
    public void beConstructedThroughNamed(String name, Object... args) {
        ensureConstructionCanChange();
        construction = Construction.withFactory(validConstructionName(name, "name"), copyArgs(args));
    }

    /**
     * Configures lazy subject construction through a named static factory method.
     */
    public void beConstructedThroughNamed(String name) {
        beConstructedThroughNamed(name, new Object[0]);
    }

    /**
     * Creates a throw expectation for constructor/factory or method invocation checks.
     */
    public ThrowExpectation shouldThrow(Class<? extends Throwable> expectedType) {
        return new ThrowExpectation(this, expectedType);
    }

    // --- Private helpers ---

    private void ensureConstructionCanChange() {
        if (subjectInstantiated) {
            throw new IllegalStateException("Cannot change subject construction after the subject has been instantiated.");
        }
    }

    private static String validConstructionName(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return value;
    }

    private static Object[] copyArgs(Object[] args) {
        if (args == null) {
            return new Object[0];
        }
        Object[] copy = new Object[args.length];
        System.arraycopy(args, 0, copy, 0, args.length);
        return copy;
    }

    private T constructSubject() throws Throwable {
        if (subjectType == null) {
            throw new IllegalStateException("Subject type is not configured. Use SubjectLifecycle(Class) or a generated support class.");
        }
        if (construction.usesFactory()) {
            return constructThroughFactory();
        }
        return constructThroughConstructor();
    }

    @SuppressWarnings("unchecked")
    private T constructThroughConstructor() throws Throwable {
        ResolvedConstructor resolved = resolveConstructor();
        if (resolved == null) {
            throw new IllegalStateException("No matching constructor found for " + subjectType.getName());
        }
        try {
            if (!resolved.constructor.isAccessible()) {
                resolved.constructor.setAccessible(true);
            }
            return (T) resolved.constructor.newInstance(resolved.args);
        } catch (InvocationTargetException ex) {
            throw invocationCause(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private T constructThroughFactory() throws Throwable {
        Method method = findFactoryMethod();
        if (method == null) {
            throw new IllegalStateException("No matching static factory method '" + construction.methodName
                    + "' found for " + subjectType.getName());
        }
        try {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            Object value = method.invoke(null, construction.args);
            return (T) value;
        } catch (InvocationTargetException ex) {
            throw invocationCause(ex);
        }
    }

    private ResolvedConstructor resolveConstructor() {
        Constructor<?> exact = findExactConstructor(construction.args);
        if (exact != null) {
            return new ResolvedConstructor(exact, construction.args);
        }
        if (construction.explicitConstructor) {
            ResolvedConstructor paddedRecordConstructor = findCanonicalRecordConstructorWithTrailingDefaults();
            if (paddedRecordConstructor != null) {
                return paddedRecordConstructor;
            }
        }
        return null;
    }

    private Constructor<?> findExactConstructor(Object[] args) {
        Constructor<?>[] constructors = subjectType.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            if (parametersMatch(constructor.getParameterTypes(), args)) {
                return constructor;
            }
        }
        return null;
    }

    private ResolvedConstructor findCanonicalRecordConstructorWithTrailingDefaults() {
        Class<?>[] componentTypes = recordComponentTypes();
        if (componentTypes == null || construction.args.length >= componentTypes.length) {
            return null;
        }
        for (int i = 0; i < construction.args.length; i++) {
            if (!argumentMatches(componentTypes[i], construction.args[i])) {
                return null;
            }
        }
        try {
            Constructor<?> canonical = subjectType.getDeclaredConstructor(componentTypes);
            Object[] paddedArgs = new Object[componentTypes.length];
            System.arraycopy(construction.args, 0, paddedArgs, 0, construction.args.length);
            for (int i = construction.args.length; i < componentTypes.length; i++) {
                paddedArgs[i] = defaultValue(componentTypes[i]);
            }
            return new ResolvedConstructor(canonical, paddedArgs);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private Method findFactoryMethod() {
        Method method = findFactoryMethod(subjectType.getMethods());
        if (method != null) {
            return method;
        }
        return findFactoryMethod(subjectType.getDeclaredMethods());
    }

    private Method findFactoryMethod(Method[] methods) {
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!method.getName().equals(construction.methodName)) {
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!subjectType.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            if (parametersMatch(method.getParameterTypes(), construction.args)) {
                return method;
            }
        }
        return null;
    }

    private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!argumentMatches(parameterTypes[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean argumentMatches(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }
        Class<?> argumentType = arg.getClass();
        if (!parameterType.isPrimitive()) {
            return parameterType.isAssignableFrom(argumentType);
        }
        Class<?> wrapperType = wrapperType(parameterType);
        if (wrapperType.isAssignableFrom(argumentType)) {
            return true;
        }
        return isWideningPrimitiveMatch(parameterType, argumentType);
    }

    private static boolean isWideningPrimitiveMatch(Class<?> parameterType, Class<?> argumentType) {
        if (Boolean.class.equals(argumentType)) {
            return boolean.class.equals(parameterType);
        }
        if (Character.class.equals(argumentType)) {
            return char.class.equals(parameterType)
                    || int.class.equals(parameterType)
                    || long.class.equals(parameterType)
                    || float.class.equals(parameterType)
                    || double.class.equals(parameterType);
        }
        if (Byte.class.equals(argumentType)) {
            return short.class.equals(parameterType)
                    || int.class.equals(parameterType)
                    || long.class.equals(parameterType)
                    || float.class.equals(parameterType)
                    || double.class.equals(parameterType);
        }
        if (Short.class.equals(argumentType)) {
            return int.class.equals(parameterType)
                    || long.class.equals(parameterType)
                    || float.class.equals(parameterType)
                    || double.class.equals(parameterType);
        }
        if (Integer.class.equals(argumentType)) {
            return long.class.equals(parameterType)
                    || float.class.equals(parameterType)
                    || double.class.equals(parameterType);
        }
        if (Long.class.equals(argumentType)) {
            return float.class.equals(parameterType)
                    || double.class.equals(parameterType);
        }
        if (Float.class.equals(argumentType)) {
            return double.class.equals(parameterType);
        }
        return false;
    }

    private static Class<?> wrapperType(Class<?> primitiveType) {
        if (boolean.class.equals(primitiveType)) return Boolean.class;
        if (byte.class.equals(primitiveType)) return Byte.class;
        if (short.class.equals(primitiveType)) return Short.class;
        if (int.class.equals(primitiveType)) return Integer.class;
        if (long.class.equals(primitiveType)) return Long.class;
        if (float.class.equals(primitiveType)) return Float.class;
        if (double.class.equals(primitiveType)) return Double.class;
        if (char.class.equals(primitiveType)) return Character.class;
        if (void.class.equals(primitiveType)) return Void.class;
        return primitiveType;
    }

    private Class<?>[] recordComponentTypes() {
        if (!isRecordType()) {
            return null;
        }
        try {
            Method getRecordComponents = Class.class.getMethod("getRecordComponents");
            Object rawComponents = getRecordComponents.invoke(subjectType);
            if (!(rawComponents instanceof Object[])) {
                return null;
            }
            Object[] components = (Object[]) rawComponents;
            Class<?>[] componentTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                Method getType = components[i].getClass().getMethod("getType");
                Object rawType = getType.invoke(components[i]);
                if (!(rawType instanceof Class<?>)) {
                    return null;
                }
                componentTypes[i] = (Class<?>) rawType;
            }
            return componentTypes;
        } catch (ReflectiveOperationException ex) {
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean isRecordType() {
        try {
            Method isRecord = Class.class.getMethod("isRecord");
            Object result = isRecord.invoke(subjectType);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException ex) {
            return false;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) return Boolean.FALSE;
        if (byte.class.equals(type)) return Byte.valueOf((byte) 0);
        if (short.class.equals(type)) return Short.valueOf((short) 0);
        if (int.class.equals(type)) return Integer.valueOf(0);
        if (long.class.equals(type)) return Long.valueOf(0L);
        if (float.class.equals(type)) return Float.valueOf(0.0f);
        if (double.class.equals(type)) return Double.valueOf(0.0d);
        if (char.class.equals(type)) return Character.valueOf('\0');
        return null;
    }

    private static Throwable invocationCause(InvocationTargetException ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return ex;
        }
        return cause;
    }

    private void instantiateSubjectForExpectation() throws Throwable {
        if (!subjectInstantiated) {
            subject = constructSubject();
            subjectInstantiated = true;
        }
    }

    private static final class ResolvedConstructor {
        private final Constructor<?> constructor;
        private final Object[] args;

        ResolvedConstructor(Constructor<?> constructor, Object[] args) {
            this.constructor = constructor;
            this.args = copyArgs(args);
        }
    }

    private static final class Construction {
        private final String methodName;
        private final Object[] args;
        private final boolean explicitConstructor;

        private Construction(String methodName, Object[] args, boolean explicitConstructor) {
            this.methodName = methodName;
            this.args = args;
            this.explicitConstructor = explicitConstructor;
        }

        static Construction withDefaultConstructor(Object[] args) {
            return new Construction(null, args, false);
        }

        static Construction withExplicitConstructor(Object[] args) {
            return new Construction(null, args, true);
        }

        static Construction withFactory(String methodName, Object[] args) {
            return new Construction(methodName, args, false);
        }

        boolean usesFactory() {
            return methodName != null;
        }
    }

    /**
     * Runnable contract used by throw expectations without adding assertion dependencies.
     */
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    /**
     * Zero-dependency throw expectation for method and instantiation checks.
     */
    public static class ThrowExpectation {
        private final SubjectLifecycle<?> lifecycle;
        private final Class<? extends Throwable> expectedType;

        protected ThrowExpectation(SubjectLifecycle<?> lifecycle, Class<? extends Throwable> expectedType) {
            this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle must not be null");
            this.expectedType = Objects.requireNonNull(expectedType, "expectedType must not be null");
        }

        public void during(ThrowingRunnable runnable) {
            Objects.requireNonNull(runnable, "runnable must not be null");
            try {
                runnable.run();
            } catch (Throwable thrown) {
                if (expectedType.isAssignableFrom(thrown.getClass())) {
                    return;
                }
                AssertionError error = new AssertionError("Expected " + expectedType.getName()
                        + " to be thrown, but got " + thrown.getClass().getName());
                error.initCause(thrown);
                throw error;
            }
            throw new AssertionError("Expected " + expectedType.getName() + " to be thrown, but nothing was thrown");
        }

        public void duringInstantiation() {
            during(new ThrowingRunnable() {
                @Override
                public void run() throws Throwable {
                    lifecycle.instantiateSubjectForExpectation();
                }
            });
        }
    }
}
