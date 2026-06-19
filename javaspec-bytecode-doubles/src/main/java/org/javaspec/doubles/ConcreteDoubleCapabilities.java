package org.javaspec.doubles;

import java.lang.reflect.Modifier;

/**
 * Describes the capabilities and limitations of concrete-class double generation in
 * the current JVM environment.
 *
 * <h2>Supported</h2>
 * <ul>
 *   <li><b>Non-final concrete classes</b> with an accessible no-arg constructor: supported via
 *       ByteBuddy subclass generation.  All non-final, non-private, non-static methods are
 *       intercepted and routed through the same call-recording and stubbing engine used by
 *       core interface doubles.</li>
 * </ul>
 *
 * <h2>Not supported without a Java instrumentation agent</h2>
 * <ul>
 *   <li><b>Final classes</b>: subclassing is not possible; bytecode redefinition (via
 *       {@code java.lang.instrument.Instrumentation}) is required.  This is available when
 *       the JVM is started with {@code -javaagent:byte-buddy-agent.jar} or when self-attach
 *       is enabled ({@code -Djdk.attach.allowAttachSelf=true} on Java 9+).</li>
 *   <li><b>Static methods</b>: intercepting calls to static methods on an existing class
 *       requires bytecode transformation/redefinition at class-load time — an agent is
 *       required.  Without an agent, only instance methods on generated subclasses can be
 *       intercepted.</li>
 *   <li><b>Constructor interception</b>: intercepting {@code new} calls to an existing
 *       constructor requires bytecode rewriting of call sites.  Constructor interception is
 *       not available without an agent.</li>
 * </ul>
 *
 * <h2>Permanently unsupported</h2>
 * <ul>
 *   <li>Primitive types, arrays, enums, and annotations.</li>
 * </ul>
 *
 * <h2>Adding agent support (future optional artifact)</h2>
 * <p>A future {@code javaspec-bytecode-agent} artifact would wrap the ByteBuddy Agent
 * ({@code byte-buddy-agent}) to enable final-class and static-method doubles.  Until that
 * artifact is available, users needing these capabilities should consider design alternatives
 * (e.g. wrapping the final/static collaborator behind an interface).</p>
 */
public final class ConcreteDoubleCapabilities {

    private ConcreteDoubleCapabilities() {
    }

    /**
     * Returns a human-readable summary of what {@link BytebuddyConcreteDoubleProvider} can
     * do for the given type in the current JVM environment.
     *
     * @param type the candidate type; may be {@code null}
     * @return a non-null summary string
     */
    public static String describe(Class<?> type) {
        if (type == null) {
            return "null is not a valid type.";
        }
        if (type.isPrimitive()) {
            return "Primitive type '" + type.getName() + "' cannot be doubled.";
        }
        if (type.isArray()) {
            return "Array type '" + type.getCanonicalName() + "' cannot be doubled.";
        }
        if (type.isAnnotation()) {
            return "Annotation type '" + type.getName() + "' cannot be doubled.";
        }
        if (type.isEnum()) {
            return "Enum type '" + type.getName() + "' cannot be doubled.";
        }
        if (type.isInterface()) {
            return "Interface '" + type.getName() + "' should be doubled using the core "
                    + "Doubles.create() API (JDK dynamic proxy — no bytecode dependency).";
        }
        if (Modifier.isFinal(type.getModifiers())) {
            return "Final class '" + type.getName() + "' cannot be doubled without a "
                    + "Java instrumentation agent.\n"
                    + finalClassSuggestion(type);
        }
        return "Non-final class '" + type.getName() + "' is supported via ByteBuddy "
                + "subclass generation (requires an accessible no-arg constructor).";
    }

    /**
     * Returns {@code true} when the given type can be doubled by
     * {@link BytebuddyConcreteDoubleProvider} in the current JVM environment.
     *
     * @param type the candidate type; may be {@code null}
     * @return {@code true} if supported
     */
    public static boolean isSupported(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (type.isPrimitive() || type.isArray() || type.isAnnotation()
                || type.isEnum() || type.isInterface()) {
            return false;
        }
        return !Modifier.isFinal(type.getModifiers());
    }

    /**
     * Returns a multi-line string explaining why static method interception is not
     * available without an instrumentation agent, together with recommended workarounds.
     */
    public static String staticMethodLimitationNote() {
        return "Static method interception is not supported without a Java instrumentation\n"
                + "agent.  Recommended workarounds:\n"
                + "  1. Wrap the static collaborator behind an interface and double the interface.\n"
                + "  2. Use a 'seam' (e.g. a non-static delegate method) that can be subclassed.\n"
                + "  3. Add 'javaspec-bytecode-agent' to the test classpath once it is available.";
    }

    /**
     * Returns a multi-line string explaining why constructor interception is not
     * available without an instrumentation agent.
     */
    public static String constructorInterceptionLimitationNote() {
        return "Constructor interception is not supported without a Java instrumentation agent.\n"
                + "Recommended workarounds:\n"
                + "  1. Inject collaborators via constructor/setter injection rather than creating\n"
                + "     them inside the class under test.\n"
                + "  2. Use a factory/provider interface that can be doubled normally.";
    }

    // -------------------------------------------------------------------------
    // Internal

    private static String finalClassSuggestion(Class<?> type) {
        return "Workarounds for final class '" + type.getSimpleName() + "':\n"
                + "  1. Wrap the final class behind an interface and double the interface.\n"
                + "  2. Start the JVM with '-javaagent:byte-buddy-agent.jar' and add\n"
                + "     'javaspec-bytecode-agent' to the test classpath once it is available.\n"
                + "  3. On Java 9+: add '-Djdk.attach.allowAttachSelf=true' together with\n"
                + "     the agent artifact for dynamic self-attach.";
    }
}
