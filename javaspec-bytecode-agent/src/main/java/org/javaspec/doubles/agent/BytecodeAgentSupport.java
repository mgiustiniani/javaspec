package org.javaspec.doubles.agent;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.Instrumentation;

/** Internal holder/installer for the optional instrumentation instance. */
public final class BytecodeAgentSupport {
    private static volatile Instrumentation instrumentation;

    private BytecodeAgentSupport() {
    }

    static void setInstrumentation(Instrumentation installed) {
        instrumentation = installed;
    }

    public static Instrumentation instrumentation() {
        Instrumentation current = instrumentation;
        if (current != null) {
            return current;
        }
        synchronized (BytecodeAgentSupport.class) {
            current = instrumentation;
            if (current == null) {
                instrumentation = ByteBuddyAgent.install();
                current = instrumentation;
            }
            return current;
        }
    }

    public static boolean isAvailable() {
        try {
            return instrumentation() != null;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
