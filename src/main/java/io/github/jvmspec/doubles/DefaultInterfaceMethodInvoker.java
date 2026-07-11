package io.github.jvmspec.doubles;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invokes Java 8 default interface methods from a JDK proxy invocation handler.
 */
final class DefaultInterfaceMethodInvoker {
    private DefaultInterfaceMethodInvoker() {
    }

    static Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        MethodHandles.Lookup lookup = lookupFor(declaringClass);
        MethodHandle handle = lookup.unreflectSpecial(method, declaringClass).bindTo(proxy);
        return handle.invokeWithArguments(arguments);
    }

    private static MethodHandles.Lookup lookupFor(Class<?> declaringClass) throws Throwable {
        try {
            Method privateLookupIn = MethodHandles.class.getMethod(
                    "privateLookupIn", Class.class, MethodHandles.Lookup.class);
            return (MethodHandles.Lookup) privateLookupIn.invoke(null, declaringClass, MethodHandles.lookup());
        } catch (NoSuchMethodException ignored) {
            return java8LookupFor(declaringClass);
        } catch (InvocationTargetException invocationFailure) {
            throw invocationFailure.getCause();
        }
    }

    private static MethodHandles.Lookup java8LookupFor(Class<?> declaringClass) throws Throwable {
        Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(
                Class.class, int.class);
        constructor.setAccessible(true);
        int allModes = MethodHandles.Lookup.PUBLIC
                | MethodHandles.Lookup.PRIVATE
                | MethodHandles.Lookup.PROTECTED
                | MethodHandles.Lookup.PACKAGE;
        return constructor.newInstance(declaringClass, Integer.valueOf(allModes));
    }
}
