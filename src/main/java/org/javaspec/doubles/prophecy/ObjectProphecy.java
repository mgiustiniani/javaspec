package org.javaspec.doubles.prophecy;

import org.javaspec.doubles.InterfaceDouble;

/**
 * A prophecy about an object of type {@code T}.
 * <p>
 * Wraps a javaspec double handle and provides a reflective API for setting up
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
public class ObjectProphecy<T> extends BaseObjectProphecy<T> {

    /**
     * Creates a new object prophecy wrapping the given double handle.
     *
     * @param interfaceDouble the typed double handle
     * @param registry        the prediction registry (may be null if predictions are not used)
     */
    public ObjectProphecy(InterfaceDouble<T> interfaceDouble, PredictionRegistry registry) {
        super(interfaceDouble, registry);
    }
}
