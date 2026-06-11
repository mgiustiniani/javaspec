package org.javaspec.doubles;

import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link BytebuddyConcreteDoubleProvider} — supports() predicate, createDouble()
 * semantics, and end-to-end integration via {@link Doubles#concreteDouble(Class)}.
 */
public class BytebuddyConcreteDoubleProviderTest {

    // -----------------------------------------------------------------------
    // Helper types
    // -----------------------------------------------------------------------

    /** A concrete, non-final class with an overridable method. */
    public static class SomeService {
        public String greet() {
            return "real";
        }
    }

    /** An abstract class with an abstract method and a default no-arg constructor. */
    public static abstract class AbstractBase {
        public AbstractBase() {}

        public abstract int value();
    }

    /** An enum — must NOT be supported. */
    public enum Color { RED }

    /** An annotation type — must NOT be supported. */
    public @interface Tag {}

    // -----------------------------------------------------------------------
    // Fixture
    // -----------------------------------------------------------------------

    private BytebuddyConcreteDoubleProvider provider;

    @Before
    public void setUp() {
        provider = new BytebuddyConcreteDoubleProvider();
    }

    // -----------------------------------------------------------------------
    // supports() — accepted types
    // -----------------------------------------------------------------------

    @Test
    public void supportsNonFinalConcreteClass() {
        assertTrue(provider.supports(SomeService.class));
    }

    @Test
    public void supportsAbstractClass() {
        assertTrue(provider.supports(AbstractBase.class));
    }

    // -----------------------------------------------------------------------
    // supports() — rejected types
    // -----------------------------------------------------------------------

    @Test
    public void doesNotSupportInterface() {
        assertFalse(provider.supports(Runnable.class));
    }

    @Test
    public void doesNotSupportFinalClass() {
        // java.lang.String is final
        assertFalse(provider.supports(String.class));
    }

    @Test
    public void doesNotSupportEnum() {
        assertFalse(provider.supports(Color.class));
    }

    @Test
    public void doesNotSupportNull() {
        assertFalse(provider.supports(null));
    }

    @Test
    public void doesNotSupportPrimitive() {
        assertFalse(provider.supports(int.class));
    }

    @Test
    public void doesNotSupportArray() {
        assertFalse(provider.supports(String[].class));
    }

    @Test
    public void doesNotSupportAnnotation() {
        // java.lang.annotation.Retention is an annotation type
        assertFalse(provider.supports(java.lang.annotation.Retention.class));
    }

    // -----------------------------------------------------------------------
    // createDouble() — structural assertions
    // -----------------------------------------------------------------------

    @Test
    public void createsDoubleForConcreteClass() {
        InterfaceDouble<SomeService> result = provider.createDouble(SomeService.class);

        assertNotNull("result must not be null", result);
        assertNotNull("instance() must not be null", result.instance());
        assertTrue("instance() must be instanceof SomeService",
                result.instance() instanceof SomeService);
        assertNotNull("control() must not be null", result.control());
        assertSame("interfaceType() must equal SomeService.class",
                SomeService.class, result.interfaceType());
    }

    @Test
    public void doubleInterceptsMethodCallsWithDefaultReturn() {
        // With no stub configured, greet() should NOT return the real "real" value.
        // The DoubleInvocationHandler returns the default for the return type (null for Object/String).
        InterfaceDouble<SomeService> result = provider.createDouble(SomeService.class);
        String actual = result.instance().greet();
        assertNull("unstubbed call should return null (default), not the real implementation",
                actual);
    }

    @Test
    public void doubleAllowsStubReturn() {
        InterfaceDouble<SomeService> result = provider.createDouble(SomeService.class);
        result.when("greet").thenReturn("stubbed");
        String actual = result.instance().greet();
        assertTrue("stub should be returned; got: " + actual, "stubbed".equals(actual));
    }

    @Test
    public void doubleTracksCallHistory() {
        InterfaceDouble<SomeService> result = provider.createDouble(SomeService.class);
        result.instance().greet();
        // Must not throw — call was recorded
        result.control().verify("greet").called();
    }

    @Test
    public void createDoubleForInterfaceThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                new ThrowingRunnable() {
                    public void run() {
                        provider.createDouble(Runnable.class);
                    }
                });
    }

    @Test
    public void createDoubleForFinalClassThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                new ThrowingRunnable() {
                    public void run() {
                        provider.createDouble(String.class);
                    }
                });
    }

    // -----------------------------------------------------------------------
    // Integration via Doubles.concreteDouble() — provider on adapter classpath
    // -----------------------------------------------------------------------

    @Test
    public void doublesConcreteDoubleUsesProviderWhenAvailable() {
        InterfaceDouble<SomeService> result = Doubles.concreteDouble(SomeService.class);
        assertNotNull("result must not be null", result);
        assertTrue("instance() must be instanceof SomeService",
                result.instance() instanceof SomeService);
    }

    @Test
    public void doublesClassDoubleAliasWorks() {
        InterfaceDouble<SomeService> result = Doubles.classDouble(SomeService.class);
        assertNotNull("result must not be null", result);
        assertTrue("instance() must be instanceof SomeService",
                result.instance() instanceof SomeService);
    }
}
