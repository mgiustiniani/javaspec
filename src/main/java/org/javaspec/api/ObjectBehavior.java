package org.javaspec.api;

import org.javaspec.matcher.Matchable;
import org.javaspec.matcher.MatcherRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * PHPSpec-inspired base class for generated javaspec specifications.
 * <p>
 * Provides lazy construction for the subject under test and matcher methods for specifying behaviour.
 * The pattern {@code match(subject().method()).shouldReturn(expected)} enables readable specifications,
 * while generated support classes can expose subject-specific typed proxy methods.
 * </p>
 *
 * @param <T> the subject type this spec describes
 */
public class ObjectBehavior<T> {
    private final Class<? extends T> subjectType;
    private T subject;
    private boolean subjectInstantiated;
    private MatcherRegistry matcherRegistry;
    private Construction construction;

    /**
     * Creates a new ObjectBehavior with default matchers and no configured subject type.
     */
    public ObjectBehavior() {
        this(null);
    }

    /**
     * Creates a new ObjectBehavior with default matchers and a subject type for lazy construction.
     *
     * @param subjectType the concrete subject type to instantiate lazily
     */
    public ObjectBehavior(Class<? extends T> subjectType) {
        this.subjectType = subjectType;
        this.matcherRegistry = MatcherRegistry.createWithDefaults();
        this.construction = Construction.withConstructor(new Object[0]);
    }

    // --- Subject access ---

    /**
     * Returns the subject instance under test, constructing it lazily when a subject type is configured.
     */
    protected T subject() {
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
     * Sets the subject instance. Called by the runner during lifecycle setup.
     */
    public void setSubject(T subject) {
        this.subject = subject;
        this.subjectInstantiated = true;
    }

    // --- Matcher registry ---

    /**
     * Returns the matcher registry. Custom matchers can be added via
     * {@code matcherRegistry().register("name", matcher)}.
     */
    protected MatcherRegistry matcherRegistry() {
        return matcherRegistry;
    }

    /**
     * Replaces the default matcher registry with a custom one.
     * Useful for JUnit integration: pass a registry backed by JUnit assertions.
     */
    public void setMatcherRegistry(MatcherRegistry registry) {
        this.matcherRegistry = Objects.requireNonNull(registry, "registry must not be null");
    }

    // --- Matchable wrapper ---

    /**
     * Wraps a value in a {@link Matchable} so that matcher methods can be chained.
     * <p>
     * Usage: {@code match(subject().getRating()).shouldReturn(5)}
     * </p>
     *
     * @param value the value to wrap
     * @param <R>   the type of the value
     * @return a Matchable that delegates to the registered matchers
     */
    protected <R> Matchable<R> match(R value) {
        return new Matchable<R>(value, matcherRegistry);
    }

    // --- Direct assertion methods (convenience) ---

    /**
     * Asserts that the subject value is identical to the expected value (by == semantics).
     */
    public void shouldBe(Object actual, Object expected) {
        match(actual).shouldBe(expected);
    }

    /**
     * Asserts that the subject value is equal to the expected value (by equals semantics).
     */
    public void shouldEqual(Object actual, Object expected) {
        match(actual).shouldEqual(expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object, Object)} using PHPSpec return terminology.
     */
    public void shouldReturn(Object actual, Object expected) {
        match(actual).shouldReturn(expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object, Object)}.
     */
    public void shouldBeLike(Object actual, Object expected) {
        match(actual).shouldBeLike(expected);
    }

    /**
     * Alias for {@link #shouldEqual(Object, Object)}.
     */
    public void shouldBeEqualTo(Object actual, Object expected) {
        match(actual).shouldBeEqualTo(expected);
    }

    /**
     * Asserts that the subject value is NOT identical to the unexpected value.
     */
    public void shouldNotBe(Object actual, Object unexpected) {
        match(actual).shouldNotBe(unexpected);
    }

    /**
     * Asserts that the subject value is NOT equal to the unexpected value.
     */
    public void shouldNotEqual(Object actual, Object unexpected) {
        match(actual).shouldNotEqual(unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object, Object)} using PHPSpec return terminology.
     */
    public void shouldNotReturn(Object actual, Object unexpected) {
        match(actual).shouldNotReturn(unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object, Object)}.
     */
    public void shouldNotBeLike(Object actual, Object unexpected) {
        match(actual).shouldNotBeLike(unexpected);
    }

    /**
     * Alias for {@link #shouldNotEqual(Object, Object)}.
     */
    public void shouldNotBeEqualTo(Object actual, Object unexpected) {
        match(actual).shouldNotBeEqualTo(unexpected);
    }

    /**
     * Asserts that the subject value has the given type.
     */
    public void shouldHaveType(Object actual, Class<?> expectedType) {
        match(actual).shouldHaveType(expectedType);
    }

    /**
     * Alias for {@link #shouldHaveType(Object, Class)}.
     */
    public void shouldBeAnInstanceOf(Object actual, Class<?> expectedType) {
        match(actual).shouldBeAnInstanceOf(expectedType);
    }

    /**
     * Alias for {@link #shouldHaveType(Object, Class)} using PHPSpec return terminology.
     */
    public void shouldReturnAnInstanceOf(Object actual, Class<?> expectedType) {
        match(actual).shouldReturnAnInstanceOf(expectedType);
    }

    /**
     * Asserts that the subject value implements or extends the expected type.
     */
    public void shouldImplement(Object actual, Class<?> expectedType) {
        match(actual).shouldImplement(expectedType);
    }

    /**
     * Asserts that strings, collections, maps, arrays, or iterables contain the expected value.
     */
    public void shouldContain(Object actual, Object expected) {
        match(actual).shouldContain(expected);
    }

    /**
     * Asserts that strings, collections, maps, arrays, or iterables do not contain the unexpected value.
     */
    public void shouldNotContain(Object actual, Object unexpected) {
        match(actual).shouldNotContain(unexpected);
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables have the expected count.
     */
    public void shouldHaveCount(Object actual, int expectedCount) {
        match(actual).shouldHaveCount(expectedCount);
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables are empty.
     */
    public void shouldBeEmpty(Object actual) {
        match(actual).shouldBeEmpty();
    }

    /**
     * Asserts that arrays, collections, maps, character sequences, or iterables are not empty.
     */
    public void shouldNotBeEmpty(Object actual) {
        match(actual).shouldNotBeEmpty();
    }

    /**
     * Asserts that the map contains the expected key.
     */
    public void shouldHaveKey(Object actual, Object key) {
        match(actual).shouldHaveKey(key);
    }

    /**
     * Asserts that the map does not contain the unexpected key.
     */
    public void shouldNotHaveKey(Object actual, Object key) {
        match(actual).shouldNotHaveKey(key);
    }

    /**
     * Asserts that the map contains the expected value.
     */
    public void shouldHaveValue(Object actual, Object value) {
        match(actual).shouldHaveValue(value);
    }

    /**
     * Asserts that the map does not contain the unexpected value.
     */
    public void shouldNotHaveValue(Object actual, Object value) {
        match(actual).shouldNotHaveValue(value);
    }

    /**
     * Asserts that the character sequence does not start with the unexpected prefix.
     */
    public void shouldNotStartWith(Object actual, String prefix) {
        match(actual).shouldNotStartWith(prefix);
    }

    /**
     * Asserts that the character sequence does not end with the unexpected suffix.
     */
    public void shouldNotEndWith(Object actual, String suffix) {
        match(actual).shouldNotEndWith(suffix);
    }

    /**
     * Asserts that the character sequence does not match the supplied regular expression.
     */
    public void shouldNotMatchPattern(Object actual, String pattern) {
        match(actual).shouldNotMatchPattern(pattern);
    }

    // --- Discovery marker and runtime construction methods ---

    public void shouldHaveType(Class<?> expectedType) {
        if (expectedType == null) {
            throw new AssertionError("Expected type must not be null");
        }
        if (!subjectInstantiated && subjectType == null) {
            return;
        }
        T current = subject();
        if (current == null) {
            throw new AssertionError("Expected an instance of " + expectedType.getName() + " but got null");
        }
        Class<?> actualType = current.getClass();
        if (!expectedType.isAssignableFrom(actualType)) {
            throw new AssertionError(
                    "Expected an instance of " + expectedType.getName()
                    + " but got " + actualType.getName()
            );
        }
    }

    public void shouldBeAClass() {
        // Marker used by discovery and generation.
    }

    public void shouldBeAFinalClass() {
        // Marker used by discovery and generation.
    }

    public void shouldBeAnInterface() {
        // Marker used by discovery and generation.
    }

    public void shouldBeAnEnum() {
        // Marker used by discovery and generation.
    }

    public void shouldBeAnAnnotation() {
        // Marker used by discovery and generation.
    }

    public void shouldBeARecord() {
        // Marker used by discovery and generation.
    }

    public void shouldBeASealedClass() {
        // Marker used by discovery and generation.
    }

    public void shouldBeASealedInterface() {
        // Marker used by discovery and generation.
    }

    public void shouldExtend(Class<?>... extendedTypes) {
        // Marker used by discovery and generation.
    }

    public void shouldImplement(Class<?>... implementedTypes) {
        // Marker used by discovery and generation.
    }

    public void shouldPermit(Class<?>... permittedTypes) {
        // Marker used by discovery and generation.
    }

    /**
     * Configures lazy subject construction through a constructor with the given arguments.
     */
    public void beConstructedWith(Object... args) {
        ensureConstructionCanChange();
        construction = Construction.withConstructor(copyArgs(args));
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
            throw new IllegalStateException("Subject type is not configured. Use ObjectBehavior(Class) or a generated support class.");
        }
        if (construction.usesFactory()) {
            return constructThroughFactory();
        }
        return constructThroughConstructor();
    }

    @SuppressWarnings("unchecked")
    private T constructThroughConstructor() throws Throwable {
        Constructor<?> constructor = findConstructor();
        if (constructor == null) {
            throw new IllegalStateException("No matching constructor found for " + subjectType.getName());
        }
        try {
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return (T) constructor.newInstance(construction.args);
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

    private Constructor<?> findConstructor() {
        Constructor<?>[] constructors = subjectType.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            if (parametersMatch(constructor.getParameterTypes(), construction.args)) {
                return constructor;
            }
        }
        return null;
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

    private static final class Construction {
        private final String methodName;
        private final Object[] args;

        private Construction(String methodName, Object[] args) {
            this.methodName = methodName;
            this.args = args;
        }

        static Construction withConstructor(Object[] args) {
            return new Construction(null, args);
        }

        static Construction withFactory(String methodName, Object[] args) {
            return new Construction(methodName, args);
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
        private final ObjectBehavior<?> behavior;
        private final Class<? extends Throwable> expectedType;

        protected ThrowExpectation(ObjectBehavior<?> behavior, Class<? extends Throwable> expectedType) {
            this.behavior = Objects.requireNonNull(behavior, "behavior must not be null");
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
                    behavior.instantiateSubjectForExpectation();
                }
            });
        }
    }
}
