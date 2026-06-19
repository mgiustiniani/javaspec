package org.javaspec.doubles.agent;

import java.lang.instrument.Instrumentation;

/**
 * Optional javaagent entry point for javaspec bytecode-agent doubles.
 *
 * <p>The module also supports dynamic self-attach through ByteBuddyAgent.install(). Supplying this
 * class as {@code -javaagent:javaspec-bytecode-agent.jar} makes instrumentation available before
 * tests start and avoids self-attach restrictions on JVMs that disable dynamic attach.</p>
 */
public final class JavaspecBytecodeAgent {
    private JavaspecBytecodeAgent() {
    }

    public static void premain(String args, Instrumentation instrumentation) {
        BytecodeAgentSupport.setInstrumentation(instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        BytecodeAgentSupport.setInstrumentation(instrumentation);
    }
}
