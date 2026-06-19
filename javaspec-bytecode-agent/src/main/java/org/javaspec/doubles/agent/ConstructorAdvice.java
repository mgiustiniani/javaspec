package org.javaspec.doubles.agent;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.InvocationHandler;

/** Advice for constructors of instrumented classes. */
public final class ConstructorAdvice {
    private ConstructorAdvice() {
    }

    @Advice.OnMethodExit
    public static void exit(@Advice.This Object self, @Advice.Origin Class<?> type) {
        InvocationHandler handler = AgentDoubleRegistry.handlerForConstruction(type);
        if (handler != null) {
            AgentDoubleRegistry.registerInstance(self, handler);
        }
    }
}
