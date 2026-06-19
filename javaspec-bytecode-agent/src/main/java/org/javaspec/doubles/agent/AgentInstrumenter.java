package org.javaspec.doubles.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Installs ByteBuddy advice into classes selected for agent-backed doubles. */
public final class AgentInstrumenter {
    private static final Set<Class<?>> INSTRUMENTED = Collections.synchronizedSet(new HashSet<Class<?>>());

    private AgentInstrumenter() {
    }

    public static void instrument(Class<?> type) {
        validateInstrumentable(type);
        if (INSTRUMENTED.contains(type)) {
            return;
        }
        synchronized (INSTRUMENTED) {
            if (INSTRUMENTED.contains(type)) {
                return;
            }
            BytecodeAgentSupport.instrumentation();
            new ByteBuddy()
                    .redefine(type)
                    .visit(Advice.to(InstanceMethodAdvice.class).on(ElementMatchers.isMethod()
                            .and(ElementMatchers.not(ElementMatchers.isStatic()))
                            .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                            .and(ElementMatchers.not(ElementMatchers.isNative()))
                            .and(ElementMatchers.not(ElementMatchers.isPrivate()))
                            .and(ElementMatchers.not(ElementMatchers.isFinalizer()))))
                    .visit(Advice.to(StaticMethodAdvice.class).on(ElementMatchers.isMethod()
                            .and(ElementMatchers.isStatic())
                            .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                            .and(ElementMatchers.not(ElementMatchers.isNative()))
                            .and(ElementMatchers.not(ElementMatchers.isPrivate()))
                            .and(ElementMatchers.not(ElementMatchers.isTypeInitializer()))))
                    .visit(Advice.to(ConstructorAdvice.class).on(ElementMatchers.isConstructor()))
                    .make()
                    .load(type.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
            INSTRUMENTED.add(type);
        }
    }

    public static boolean canInstrument(Class<?> type) {
        try {
            validateInstrumentable(type);
            return BytecodeAgentSupport.isAvailable();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static void validateInstrumentable(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (type.isPrimitive() || type.isArray() || type.isAnnotation() || type.isEnum() || type.isInterface()) {
            throw new IllegalArgumentException("Type is not an instrumentable concrete class: " + type.getName());
        }
        if (Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalArgumentException("Abstract classes are not supported by javaspec-bytecode-agent: "
                    + type.getName());
        }
    }
}
