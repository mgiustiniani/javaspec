package io.github.jvmspec.doubles;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Modifier;

/**
 * A {@link ConcreteDoubleProvider} that generates concrete-class doubles via ByteBuddy subclass
 * generation.
 *
 * <p>For each requested type, ByteBuddy generates a subclass that overrides all non-final,
 * non-private, non-static methods. Intercepted calls are routed to an invocation handler obtained
 * from {@link Doubles#newDoubleHandler(Class)}, preserving the same call-recording and stubbing
 * semantics used by {@link Doubles#interfaceDouble(Class)} for interface proxies.
 *
 * <p>The final {@link InterfaceDouble} is assembled through
 * {@link Doubles#assembleFromHandler(Class, Object, java.lang.reflect.InvocationHandler)}, so this
 * adapter does not need package-private access to core constructors. That keeps concrete doubles
 * working across plugin/run classloader boundaries where package-private access would fail.
 *
 * @since Phase 37
 */
public final class BytebuddyConcreteDoubleProvider implements ConcreteDoubleProvider {

    /**
     * Returns {@code true} when this provider can generate a double for {@code type}.
     *
     * <p>Returns {@code false} (never throws) when the type is null, primitive, an array,
     * an annotation, an enum, an interface, or a final class.
     */
    @Override
    public boolean supports(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (type.isPrimitive()) {
            return false;
        }
        if (type.isArray()) {
            return false;
        }
        if (type.isAnnotation()) {
            return false;
        }
        if (type.isEnum()) {
            return false;
        }
        if (type.isInterface()) {
            return false;
        }
        if (Modifier.isFinal(type.getModifiers())) {
            return false;
        }
        return true;
    }

    /**
     * Creates a concrete double for {@code type} using ByteBuddy subclass generation.
     *
     * <p>The returned {@link InterfaceDouble} wraps a generated subclass instance whose
     * intercepted methods delegate to an invocation handler created by
     * {@link Doubles#newDoubleHandler(Class)}. The double handle is assembled through
     * {@link Doubles#assembleFromHandler(Class, Object, java.lang.reflect.InvocationHandler)},
     * avoiding package-private constructor access across plugin/run classloader boundaries.
     *
     * @throws IllegalArgumentException if the type is not supported or if ByteBuddy cannot
     *                                  generate or instantiate the subclass (e.g. no accessible
     *                                  no-arg constructor on the target class)
     */
    @Override
    public <T> InterfaceDouble<T> createDouble(Class<T> type) {
        if (!supports(type)) {
            throw new IllegalArgumentException(
                    "Cannot create a concrete double.  "
                    + ConcreteDoubleCapabilities.describe(type));
        }

        java.lang.reflect.InvocationHandler handler = Doubles.newDoubleHandler(type);

        try {
            ClassLoader classLoader = type.getClassLoader();
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (classLoader == null) {
                classLoader = BytebuddyConcreteDoubleProvider.class.getClassLoader();
            }

            Class<? extends T> dynamicType = new ByteBuddy()
                    .subclass(type)
                    .method(ElementMatchers.isMethod()
                            .and(ElementMatchers.not(ElementMatchers.isFinal()))
                            .and(ElementMatchers.not(ElementMatchers.isPrivate()))
                            .and(ElementMatchers.not(ElementMatchers.isStatic())))
                    .intercept(InvocationHandlerAdapter.of(handler))
                    .make()
                    .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();

            T instance = dynamicType.getDeclaredConstructor().newInstance();
            return Doubles.assembleFromHandler(type, instance, handler);

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to create ByteBuddy concrete double for type: " + type.getName(), e);
        }
    }
}
