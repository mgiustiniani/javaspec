package org.javaspec.doubles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Captures arguments passed to a stubbed or observed method call.
 *
 * <p>An {@code ArgumentCaptor} implements {@link ArgumentMatcher} so it can be
 * passed as an argument constraint to {@link DoubleControl#when(String, Object...)}
 * or {@link DoubleControl#verify(String, Object...)}.  Every time a call is
 * matched, the corresponding argument is stored and can be retrieved via
 * {@link #value()} (most recent) or {@link #allValues()} (entire history).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ArgumentCaptor<String> captor = ArgumentCaptor.create();
 * doubleControl.when("send", captor).thenReturn(null);
 * // ... invoke the double ...
 * assertEquals("hello", captor.value());
 * }</pre>
 *
 * @param <T> the expected type of the captured argument
 */
public final class ArgumentCaptor<T> implements ArgumentMatcher {

    private final List<Object> capturedValues = new ArrayList<Object>();

    private ArgumentCaptor() {
    }

    /**
     * Creates a new {@code ArgumentCaptor}.
     *
     * @param <T> the expected argument type
     * @return a fresh captor instance
     */
    public static <T> ArgumentCaptor<T> create() {
        return new ArgumentCaptor<T>();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always returns {@code true}; the argument is stored for later retrieval.</p>
     */
    @Override
    public boolean matches(Object argument) {
        capturedValues.add(argument);
        return true;
    }

    /**
     * Returns the most recently captured argument value.
     *
     * @return the last captured value, or {@code null} if no call has been captured yet
     * @throws IllegalStateException if no argument has been captured yet
     */
    @SuppressWarnings("unchecked")
    public T value() {
        if (capturedValues.isEmpty()) {
            throw new IllegalStateException("No argument has been captured yet.");
        }
        return (T) capturedValues.get(capturedValues.size() - 1);
    }

    /**
     * Returns all captured argument values in the order they were captured.
     *
     * @return immutable list of captured values (may be empty)
     */
    @SuppressWarnings("unchecked")
    public List<T> allValues() {
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < capturedValues.size(); i++) {
            result.add((T) capturedValues.get(i));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns {@code true} if at least one argument has been captured.
     */
    public boolean hasCaptured() {
        return !capturedValues.isEmpty();
    }

    /**
     * Returns the number of captured values.
     */
    public int captureCount() {
        return capturedValues.size();
    }
}
