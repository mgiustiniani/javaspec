package org.javaspec.doubles.prophecy;

import org.javaspec.doubles.Doubles;
import org.javaspec.doubles.InterfaceDouble;

/**
 * Standalone factory for Prophecy-style object prophecies.
 *
 * <p>This class is independent from {@code ObjectBehavior}: it can be used from any Java test or
 * adapter. {@code ObjectBehavior.prophesize(...)} is only a spec-author convenience that delegates
 * to this factory.</p>
 */
public final class Prophecies {
    private Prophecies() {
    }

    /**
     * Creates a prophecy for an interface or concrete class.
     *
     * <p>Interfaces use core JDK-proxy doubles. Concrete classes use the registered
     * {@link org.javaspec.doubles.ConcreteDoubleProvider}; add {@code javaspec-bytecode-doubles}
     * for non-final concrete classes or {@code javaspec-bytecode-agent} for final/static/agent-backed
     * scenarios.</p>
     *
     * @param type the type to prophesize
     * @param <T>  the type
     * @return a prophecy backed by the appropriate javaspec double
     */
    public static <T> ObjectProphecy<T> prophesize(Class<T> type) {
        return prophesize(type, new PredictionRegistry());
    }

    /**
     * Creates a prophecy using the supplied prediction registry.
     *
     * @param type     the type to prophesize
     * @param registry registry used for predictions; may be {@code null}
     * @param <T>      the type
     * @return a prophecy backed by the appropriate javaspec double
     */
    public static <T> ObjectProphecy<T> prophesize(Class<T> type, PredictionRegistry registry) {
        InterfaceDouble<T> doubleHandle;
        if (type != null && type.isInterface()) {
            doubleHandle = Doubles.interfaceDouble(type);
        } else {
            doubleHandle = Doubles.concreteDouble(type);
        }
        return new ObjectProphecy<T>(doubleHandle, registry);
    }

    /** Alias for {@link #prophesize(Class)}. */
    public static <T> ObjectProphecy<T> prophecy(Class<T> type) {
        return prophesize(type);
    }

    /** Alias for {@link #prophesize(Class, PredictionRegistry)}. */
    public static <T> ObjectProphecy<T> prophecy(Class<T> type, PredictionRegistry registry) {
        return prophesize(type, registry);
    }
}
