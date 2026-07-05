package io.github.jvmspec.doubles.agent;

import java.lang.reflect.InvocationHandler;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registry consulted by instrumented bytecode advice. */
public final class AgentDoubleRegistry {
    private static final Map<Object, InvocationHandler> INSTANCE_HANDLERS =
            Collections.synchronizedMap(new IdentityHashMap<Object, InvocationHandler>());
    private static final Map<Class<?>, InvocationHandler> STATIC_HANDLERS =
            new ConcurrentHashMap<Class<?>, InvocationHandler>();
    private static final Map<Class<?>, InvocationHandler> CONSTRUCTION_HANDLERS =
            new ConcurrentHashMap<Class<?>, InvocationHandler>();

    private AgentDoubleRegistry() {
    }

    public static void registerInstance(Object instance, InvocationHandler handler) {
        if (instance != null && handler != null) {
            INSTANCE_HANDLERS.put(instance, handler);
        }
    }

    public static void unregisterInstance(Object instance) {
        if (instance != null) {
            INSTANCE_HANDLERS.remove(instance);
        }
    }

    public static InvocationHandler handlerForInstance(Object instance) {
        if (instance == null) {
            return null;
        }
        return INSTANCE_HANDLERS.get(instance);
    }

    public static void registerStatic(Class<?> type, InvocationHandler handler) {
        if (type != null && handler != null) {
            STATIC_HANDLERS.put(type, handler);
        }
    }

    public static void unregisterStatic(Class<?> type) {
        if (type != null) {
            STATIC_HANDLERS.remove(type);
        }
    }

    public static InvocationHandler handlerForStatic(Class<?> type) {
        if (type == null) {
            return null;
        }
        return STATIC_HANDLERS.get(type);
    }

    public static void registerConstruction(Class<?> type, InvocationHandler handler) {
        if (type != null && handler != null) {
            CONSTRUCTION_HANDLERS.put(type, handler);
        }
    }

    public static void unregisterConstruction(Class<?> type) {
        if (type != null) {
            CONSTRUCTION_HANDLERS.remove(type);
        }
    }

    public static InvocationHandler handlerForConstruction(Class<?> type) {
        if (type == null) {
            return null;
        }
        return CONSTRUCTION_HANDLERS.get(type);
    }
}
