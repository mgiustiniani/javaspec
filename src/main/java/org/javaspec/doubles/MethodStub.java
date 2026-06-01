package org.javaspec.doubles;

/**
 * Fluent return-value stub for a method on an interface double.
 */
public final class MethodStub {
    private final DoubleControl control;
    private final MethodPattern pattern;

    MethodStub(DoubleControl control, MethodPattern pattern) {
        this.control = control;
        this.pattern = pattern;
    }

    /**
     * Stubs matching calls to return the supplied value.
     *
     * @return the owning control object for optional chaining
     */
    public DoubleControl thenReturn(Object returnValue) {
        control.addStub(pattern, returnValue);
        return control;
    }

    /**
     * Alias for {@link #thenReturn(Object)}.
     */
    public DoubleControl returns(Object returnValue) {
        return thenReturn(returnValue);
    }
}
