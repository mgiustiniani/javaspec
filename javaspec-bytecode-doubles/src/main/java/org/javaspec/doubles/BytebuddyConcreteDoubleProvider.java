package org.javaspec.doubles;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Modifier;

/**
 * A {@link ConcreteDoubleProvider} that generates concrete-class doubles via ByteBuddy subclass
 * generation.
 *
 * <p>This class intentionally resides in package {@code org.javaspec.doubles} (the same package
 * as the core) so that it can access the package-private constructors of {@link InterfaceDouble},
 * {@link DoubleControl}, and {@link DoubleInvocationHandler} to assemble the double handle.
 *
 * <p>For each requested type, ByteBuddy generates a subclass that overrides all non-final,
 * non-private, non-static methods and delegates every invocation to a
 * {@link DoubleInvocationHandler}, which records calls and applies configured stubs — the same
 * semantics used by {@link Doubles#interfaceDouble(Class)} for interface proxies.
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
     * intercepted methods delegate to a {@link DoubleInvocationHandler}.  The embedded
     * {@link DoubleControl} exposes the full stub/verify/inspect API.
     *
     * @throws IllegalArgumentException if the type is not supported or if ByteBuddy cannot
     *                                  generate or instantiate the subclass (e.g. no accessible
     *                                  no-arg constructor on the target class)
     */
    @Override
    public <T> InterfaceDouble<T> createDouble(Class<T> type) {
        if (!supports(type)) {
            String name = (type == null) ? "null" : type.getName();
            throw new IllegalArgumentException(
                    "Cannot create a concrete double for type: " + name
                    + ".  Type must be a non-final, non-enum, non-array, non-annotation"
                    + " concrete class.");
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
