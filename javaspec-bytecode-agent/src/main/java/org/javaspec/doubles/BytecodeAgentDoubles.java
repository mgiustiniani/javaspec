package org.javaspec.doubles;

import org.javaspec.doubles.agent.AgentDoubleRegistry;
import org.javaspec.doubles.agent.AgentInstrumenter;

import java.lang.reflect.InvocationHandler;

/**
 * Explicit APIs for agent-backed static-method and construction-aware doubles.
 *
 * <p>These APIs require {@code javaspec-bytecode-agent} and ByteBuddy Agent instrumentation.
 * They are intentionally outside the zero-dependency core.</p>
 */
public final class BytecodeAgentDoubles {
    private BytecodeAgentDoubles() {
    }

    /**
     * Intercepts static methods of {@code type}. Calls to matching static methods are routed to
     * the returned control until {@link StaticDouble#close()} is called.
     */
    public static <T> StaticDouble<T> staticDouble(Class<T> type) {
        AgentInstrumenter.instrument(type);
        InvocationHandler handler = Doubles.newDoubleHandler(type);
        AgentDoubleRegistry.registerStatic(type, handler);
        return new StaticDouble<T>(type, Doubles.controlFromHandler(handler));
    }

    /**
     * Registers every subsequently constructed instance of {@code type} with the returned control.
     * Intercepted instance methods on those objects use normal javaspec stubbing/verification.
     */
    public static <T> ConstructionDouble<T> mockConstruction(Class<T> type) {
        AgentInstrumenter.instrument(type);
        InvocationHandler handler = Doubles.newDoubleHandler(type);
        AgentDoubleRegistry.registerConstruction(type, handler);
        return new ConstructionDouble<T>(type, Doubles.controlFromHandler(handler));
    }
}
