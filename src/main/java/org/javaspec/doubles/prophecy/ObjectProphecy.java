package org.javaspec.doubles.prophecy;

import org.javaspec.doubles.DoubleControl;
import org.javaspec.doubles.InterfaceDouble;

import java.util.Objects;

/**
 * A prophecy about an object of type {@code T}.
 * <p>
 * Wraps an {@link InterfaceDouble} and provides a reflective API for setting up
 * method stubs ({@code willReturn}, {@code willThrow}) and predictions
 * ({@code shouldBeCalled}, {@code shouldNotBeCalled}, {@code shouldBeCalledTimes}).
 * </p>
 *
 * <pre>{@code
 * ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);
 * mailer.method("send", any(), eq("hello")).willReturn(true);
 * mailer.method("send", any(), eq("hello")).shouldBeCalled();
 * }</pre>
 *
 * @param <T> the type of the prophesized object
 */
public final class ObjectProphecy<T> {
    private final InterfaceDouble<T> interfaceDouble;
    private final DoubleControl control;
    private final T proxy;
    private PredictionRegistry registry;

    /**
     * Creates a new object prophecy wrapping the given interface double.
     *
     * @param interfaceDouble the typed double handle
     * @param registry        the prediction registry (may be null if predictions are not used)
     */
    public ObjectProphecy(InterfaceDouble<T> interfaceDouble, PredictionRegistry registry) {
        this.interfaceDouble = Objects.requireNonNull(interfaceDouble, "interfaceDouble must not be null");
        this.control = interfaceDouble.control();
        this.proxy = interfaceDouble.instance();
        this.registry = registry;
    }

    /**
     * Returns a method prophecy for the named method with the given arguments.
     * <p>
     * Use this to set up stubs and predictions:
     * <pre>{@code
     * mailer.method("send", any(), eq("hello")).willReturn(true);
     * mailer.method("send", any(), eq("hello")).shouldBeCalled();
     * }</pre>
     *
     * @param methodName the method name
     * @param args       the method arguments (may contain {@code ArgumentMatcher} instances)
     * @param <R>        the return type (inferred)
     * @return a new method prophecy
     */
    @SuppressWarnings("unchecked")
    public <R> MethodProphecy<R> method(String methodName, Object... args) {
        return new MethodProphecy<R>(control, registry, methodName, args);
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
