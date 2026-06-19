package org.javaspec.doubles;

import org.javaspec.doubles.agent.AgentDoubleRegistry;
import org.javaspec.doubles.agent.AgentInstrumenter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;

/**
 * Concrete double provider backed by java.lang.instrument/ByteBuddy Agent.
 *
 * <p>This provider supports final concrete classes for instance-method doubles. The class is
 * redefined in-place so final methods can be intercepted; only instances registered by this
 * provider are routed through javaspec's handler, while ordinary instances keep their original
 * behavior.</p>
 */
public final class AgentConcreteDoubleProvider implements ConcreteDoubleProvider {

    @Override
    public boolean supports(Class<?> type) {
        return AgentInstrumenter.canInstrument(type);
    }

    @Override
    public <T> InterfaceDouble<T> createDouble(Class<T> type) {
        if (!supports(type)) {
            throw new IllegalArgumentException("Cannot create agent-backed concrete double for type: "
                    + (type == null ? "null" : type.getName())
                    + ". Type must be a concrete class and ByteBuddy Agent instrumentation must be available.");
        }
        AgentInstrumenter.instrument(type);
        InvocationHandler handler = Doubles.newDoubleHandler(type);
        T instance = instantiate(type);
        AgentDoubleRegistry.registerInstance(instance, handler);
        return Doubles.assembleFromHandler(type, instance, handler);
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to instantiate agent-backed concrete double for type: "
                    + type.getName() + ". A no-argument constructor is required.", ex);
        }
    }
}
