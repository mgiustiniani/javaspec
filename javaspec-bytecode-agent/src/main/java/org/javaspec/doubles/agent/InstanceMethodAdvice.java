package org.javaspec.doubles.agent;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/** Advice for instance methods of instrumented classes. */
public final class InstanceMethodAdvice {
    private InstanceMethodAdvice() {
    }

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(
            @Advice.This Object self,
            @Advice.Origin Method method,
            @Advice.AllArguments Object[] arguments,
            @Advice.Local("javaspecResult") Object result,
            @Advice.Local("javaspecThrowable") Throwable throwable
    ) {
        InvocationHandler handler = AgentDoubleRegistry.handlerForInstance(self);
        if (handler == null) {
            return false;
        }
        try {
            result = handler.invoke(self, method, arguments);
        } catch (Throwable ex) {
            throwable = ex;
        }
        return true;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.Enter boolean intercepted,
            @Advice.Local("javaspecResult") Object result,
            @Advice.Local("javaspecThrowable") Throwable throwable,
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
            @Advice.Thrown(readOnly = false) Throwable thrown
    ) throws Throwable {
        if (!intercepted) {
            return;
        }
        if (throwable != null) {
            thrown = throwable;
            return;
        }
        returned = result;
        thrown = null;
    }
}
