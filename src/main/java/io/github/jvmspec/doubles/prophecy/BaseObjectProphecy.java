package io.github.jvmspec.doubles.prophecy;

import io.github.jvmspec.doubles.DoubleControl;
import io.github.jvmspec.doubles.InterfaceDouble;

import java.util.Objects;

/**
 * Base class for prophecy wrappers (both reflective and generated).
 * <p>
 * Provides {@link #method(String, Object...)} for reflective access and
 * {@link #reveal()} to obtain the proxied instance. Generated {@code *Prophecy}
 * subclasses add typed delegation methods that call {@code method()} internally.
 * </p>
 *
 * @param <T> the type of the prophesized object
 */
public class BaseObjectProphecy<T> {
    private final InterfaceDouble<T> interfaceDouble;
    private final DoubleControl control;
    private final T proxy;
    private PredictionRegistry registry;

    /**
     * Creates a new base prophecy wrapping the given interface double.
     *
     * @param interfaceDouble the typed double handle
     * @param registry        the prediction registry (may be null if predictions are not used)
     */
    public BaseObjectProphecy(InterfaceDouble<T> interfaceDouble, PredictionRegistry registry) {
        this.interfaceDouble = Objects.requireNonNull(interfaceDouble, "interfaceDouble must not be null");
        this.control = interfaceDouble.control();
        this.proxy = interfaceDouble.instance();
        this.registry = registry;
    }

    /**
     * Returns a method prophecy for the named method with the given arguments.
     *
     * @param methodName the method name
     * @param args       the method arguments (may contain {@code ArgumentMatcher} instances)
     * @param <R>        the return type (inferred)
     * @return a new method prophecy
     */
    @SuppressWarnings("unchecked")
    public <R> MethodProphecy<R> method(String methodName, Object... args) {
        return new MethodProphecy<R>(control, registry, this, methodName, args);
    }

    /**
     * Returns the proxy instance (the actual double that should be used in the code under test).
     */
    public T reveal() {
        return proxy;
    }

    /**
     * Returns the double control for direct stubbing/verification.
     */
    public DoubleControl control() {
        return control;
    }

    /**
     * Returns the wrapped interface double.
     */
    public InterfaceDouble<T> interfaceDouble() {
        return interfaceDouble;
    }

    /**
     * Sets the prediction registry for this prophecy.
     *
     * @param registry the prediction registry (may be null to disable prediction tracking)
     */
    public void setPredictionRegistry(PredictionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the prediction registry, or {@code null} if none is set.
     */
    public PredictionRegistry predictionRegistry() {
        return registry;
    }
}
