package io.github.jvmspec.doubles;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Predicate;

/**
 * Factory and lookup methods for zero-dependency interface doubles.
 */
public final class Doubles {
    private Doubles() {
    }

    /**
     * Creates a proxy instance for the supplied interface type.
     */
    public static <T> T create(Class<T> interfaceType) {
        return interfaceDouble(interfaceType).instance();
    }

    /**
     * Alias for {@link #create(Class)}.
     */
    public static <T> T of(Class<T> interfaceType) {
        return create(interfaceType);
    }

    /**
     * Alias for {@link #create(Class)}.
     */
    public static <T> T proxy(Class<T> interfaceType) {
        return create(interfaceType);
    }

    /**
     * Creates a typed double handle for a concrete (non-interface) class.
     *
     * <p>Requires a {@link ConcreteDoubleProvider} on the classpath (e.g. javaspec-bytecode-doubles).
     *
     * @throws IllegalArgumentException if {@code type} is null, primitive, array, annotation,
     *                                  enum, or an interface
     * @throws IllegalStateException    if no {@link ConcreteDoubleProvider} is registered
     */
    public static <T> InterfaceDouble<T> concreteDouble(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Cannot create a concrete double for null type.");
        }
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot create a concrete double for primitive type "
                    + type.getName() + ".");
        }
        if (type.isArray()) {
            throw new IllegalArgumentException("Cannot create a concrete double for array type "
                    + type.getCanonicalName() + ".");
        }
        if (type.isAnnotation()) {
            throw new IllegalArgumentException("Cannot create a concrete double for annotation type "
                    + type.getCanonicalName() + ".");
        }
        if (type.isEnum()) {
            throw new IllegalArgumentException("Cannot create a concrete double for enum type "
                    + type.getCanonicalName() + ".");
        }
        if (type.isInterface()) {
            throw new IllegalArgumentException("Cannot create a concrete double for interface type "
                    + type.getName()
                    + "; use Doubles.interfaceDouble() for interfaces.");
        }
        ConcreteDoubleProvider provider = ConcreteDoubleRegistry.findProvider(type);
        if (provider == null) {
            throw new IllegalStateException("No ConcreteDoubleProvider is registered. "
                    + "Add javaspec-bytecode-doubles (or another provider) to the classpath "
                    + "to enable concrete-class doubles.");
        }
        return provider.createDouble(type);
    }

    /**
     * Alias for {@link #concreteDouble(Class)}.
     */
    public static <T> InterfaceDouble<T> classDouble(Class<T> type) {
        return concreteDouble(type);
    }

    /**
     * Creates a typed double handle containing both proxy and control API.
     */
    public static <T> InterfaceDouble<T> interfaceDouble(Class<T> interfaceType) {
        DoubleTypeValidator.requireSupportedInterface(interfaceType);
        DoubleInvocationHandler handler = new DoubleInvocationHandler(interfaceType, DoubleIdentity.nextId());
        Object proxy = Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[] {interfaceType},
                handler
        );
        return new InterfaceDouble<T>(interfaceType, interfaceType.cast(proxy), new DoubleControl(handler));
    }

    /**
     * Returns true when the object is a proxy created by this factory.
     */
    public static boolean isDouble(Object value) {
        if (value == null || !Proxy.isProxyClass(value.getClass())) {
            return false;
        }
        return Proxy.getInvocationHandler(value) instanceof DoubleInvocationHandler;
    }

    /**
     * Returns the control API for a proxy created by this factory.
     */
    public static DoubleControl control(Object doubleInstance) {
        return new DoubleControl(handlerFor(doubleInstance));
    }

    /**
     * Alias for {@link #control(Object)}.
     */
    public static DoubleControl inspect(Object doubleInstance) {
        return control(doubleInstance);
    }

    /**
     * Matches any argument value, including null.
     */
    public static ArgumentMatcher any() {
        return ArgumentMatchers.any();
    }

    /**
     * Alias for {@link #any()}.
     */
    public static ArgumentMatcher anyArgument() {
        return ArgumentMatchers.anyArgument();
    }

    /**
     * Matches null or an argument assignable to the supplied type.
     */
    public static ArgumentMatcher any(Class<?> type) {
        return ArgumentMatchers.any(type);
    }

    /**
     * Alias for {@link #any(Class)}.
     */
    public static ArgumentMatcher anyType(Class<?> type) {
        return ArgumentMatchers.anyType(type);
    }

    /**
     * Matches only null.
     */
    public static ArgumentMatcher isNull() {
        return ArgumentMatchers.isNull();
    }

    /**
     * Matches any non-null argument.
     */
    public static ArgumentMatcher notNull() {
        return ArgumentMatchers.notNull();
    }

    /**
     * Matches an argument equal to the expected value using javaspec's array-aware equality.
     */
    public static ArgumentMatcher eq(Object expected) {
        return ArgumentMatchers.eq(expected);
    }

    /**
     * Alias for {@link #eq(Object)}.
     */
    public static ArgumentMatcher equalTo(Object expected) {
        return ArgumentMatchers.equalTo(expected);
    }

    /**
     * Matches the exact same object reference using {@code ==} semantics.
     */
    public static ArgumentMatcher same(Object expected) {
        return ArgumentMatchers.same(expected);
    }

    /**
     * Alias for {@link #same(Object)}.
     */
    public static ArgumentMatcher identicalTo(Object expected) {
        return ArgumentMatchers.identicalTo(expected);
    }

    /**
     * Matches when the actual value equals one of the supplied candidates.
     */
    public static ArgumentMatcher in(Object... candidates) {
        return ArgumentMatchers.in(candidates);
    }

    /**
     * Matches when the actual value does not equal any supplied candidate.
     */
    public static ArgumentMatcher notIn(Object... candidates) {
        return ArgumentMatchers.notIn(candidates);
    }

    /**
     * Matches through a custom predicate.
     */
    public static ArgumentMatcher matching(Predicate<Object> predicate) {
        return ArgumentMatchers.matching(predicate);
    }

    /**
     * Matches through a custom predicate with a diagnostic description.
     */
    public static ArgumentMatcher matching(Predicate<Object> predicate, String description) {
        return ArgumentMatchers.matching(predicate, description);
    }

    /**
     * Creates a new double invocation handler for the given type.
     *
     * <p>This low-level factory is intended for use by {@link ConcreteDoubleProvider}
     * implementations. The returned handler may be passed to
     * {@link #assembleFromHandler(Class, Object, java.lang.reflect.InvocationHandler)}.
     *
     * @param forType the described type (used for identity and diagnostics)
     * @return a fresh {@link java.lang.reflect.InvocationHandler} backed by javaspec stub/verify semantics
     */
    public static InvocationHandler newDoubleHandler(Class<?> forType) {
        return new DoubleInvocationHandler(forType, DoubleIdentity.nextId());
    }

    /**
     * Creates a {@link DoubleControl} for a handler previously returned by
     * {@link #newDoubleHandler(Class)}.
     *
     * <p>This keeps optional adapters from depending directly on package-private core
     * implementation classes, which is important when adapters are loaded by isolated build-tool
     * classloaders.</p>
     *
     * @throws IllegalArgumentException if {@code handler} was not created by
     *         {@link #newDoubleHandler(Class)}
     */
    public static DoubleControl controlFromHandler(InvocationHandler handler) {
        if (!(handler instanceof DoubleInvocationHandler)) {
            throw new IllegalArgumentException(
                    "handler must be created by Doubles.newDoubleHandler(); got: "
                    + handler.getClass().getName());
        }
        return new DoubleControl((DoubleInvocationHandler) handler);
    }

    /**
     * Assembles an {@link InterfaceDouble} from an externally-supplied proxy and a handler
     * previously returned by {@link #newDoubleHandler(Class)}.
     *
     * <p>This low-level factory is intended for use by {@link ConcreteDoubleProvider}
     * implementations that generate subclass proxies (e.g. ByteBuddy) where the proxy is
     * not a JDK {@link java.lang.reflect.Proxy}.
     *
     * @throws IllegalArgumentException if {@code handler} was not created by
     *         {@link #newDoubleHandler(Class)}
     */
    public static <T> InterfaceDouble<T> assembleFromHandler(Class<T> type, T proxy,
            InvocationHandler handler) {
        return new InterfaceDouble<T>(type, proxy, controlFromHandler(handler));
    }

    private static DoubleInvocationHandler handlerFor(Object doubleInstance) {
        if (doubleInstance == null) {
            throw new IllegalArgumentException("Double instance must not be null");
        }
        if (!Proxy.isProxyClass(doubleInstance.getClass())) {
            throw new IllegalArgumentException("Object is not a javaspec interface double: "
                    + doubleInstance.getClass().getName());
        }
        InvocationHandler handler = Proxy.getInvocationHandler(doubleInstance);
        if (!(handler instanceof DoubleInvocationHandler)) {
            throw new IllegalArgumentException("Object is not a javaspec interface double: "
                    + doubleInstance.getClass().getName());
        }
        return (DoubleInvocationHandler) handler;
    }
}
