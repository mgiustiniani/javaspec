package io.github.jvmspec.doubles;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link Doubles#concreteDouble(Class)} and {@link Doubles#classDouble(Class)} SPI
 * behavior when <em>no</em> {@link ConcreteDoubleProvider} is registered on the classpath.
 *
 * <p>The core module test classpath intentionally omits the {@code javaspec-bytecode-doubles}
 * adapter, so all concrete-double requests for a non-interface, non-rejected type must fail with
 * {@link IllegalStateException}.
 */
public class ConcreteDoubleProviderSpiTest {

    /** A simple concrete (non-interface) class used as the target type under test. */
    static class ConcreteTarget {}

    // -----------------------------------------------------------------------
    // concreteDouble() validation — rejected types throw IllegalArgumentException
    // -----------------------------------------------------------------------

    @Test
    public void concreteDoubleForInterfaceTypeThrowsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                new ThrowingRunnable() {
                    public void run() {
                        Doubles.concreteDouble(Runnable.class);
                    }
                });
        assertTrue("message should mention 'interface'; was: " + ex.getMessage(),
                ex.getMessage().contains("interface"));
    }

    @Test
    public void concreteDoubleForNullThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                new ThrowingRunnable() {
                    public void run() {
                        Doubles.concreteDouble(null);
                    }
                });
    }

    // -----------------------------------------------------------------------
    // concreteDouble() / classDouble() — no provider → IllegalStateException
    // -----------------------------------------------------------------------

    @Test
    public void concreteDoubleWithNoProviderThrowsIllegalState() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                new ThrowingRunnable() {
                    public void run() {
                        Doubles.concreteDouble(ConcreteTarget.class);
                    }
                });
        assertTrue("message should mention 'No ConcreteDoubleProvider is registered'; was: "
                        + ex.getMessage(),
                ex.getMessage().contains("No ConcreteDoubleProvider is registered"));
    }

    @Test
    public void classDoubleAliasForConcreteDoubleWithNoProvider() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                new ThrowingRunnable() {
                    public void run() {
                        Doubles.classDouble(ConcreteTarget.class);
                    }
                });
        assertTrue("message should mention 'No ConcreteDoubleProvider is registered'; was: "
                        + ex.getMessage(),
                ex.getMessage().contains("No ConcreteDoubleProvider is registered"));
    }

    // -----------------------------------------------------------------------
    // interfaceDouble() — unaffected by concrete-double SPI
    // -----------------------------------------------------------------------

    @Test
    public void interfaceDoubleRemainsUnaffected() {
        InterfaceDouble<Runnable> d = Doubles.interfaceDouble(Runnable.class);
        assertNotNull("instance() must not be null", d.instance());
        assertNotNull("control() must not be null", d.control());
    }
}
