package org.javaspec.doubles;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

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
