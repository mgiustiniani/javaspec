package org.javaspec.doubles;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DoublesTest {
    @Test
    public void createsInterfaceDoublesThroughPublicFactoryMethods() {
        SampleService created = Doubles.create(SampleService.class);
        SampleService of = Doubles.of(SampleService.class);
        SampleService proxy = Doubles.proxy(SampleService.class);
        InterfaceDouble<SampleService> handle = Doubles.interfaceDouble(SampleService.class);

        assertTrue(Doubles.isDouble(created));
        assertTrue(Doubles.isDouble(of));
        assertTrue(Doubles.isDouble(proxy));
        assertTrue(Doubles.isDouble(handle.instance()));
        assertFalse(Doubles.isDouble(new Object()));
        assertFalse(Doubles.isDouble(null));
        assertSame(SampleService.class, handle.interfaceType());
        assertSame(handle.instance(), handle.proxy());
        assertNotNull(Doubles.control(created));
        assertNotNull(Doubles.inspect(handle.instance()));
    }

    @Test
    public void rejectsUnsupportedDoubleTypesWithUsefulMessages() {
        assertUnsupported(ConcreteType.class, "concrete class", ConcreteType.class.getCanonicalName(), "only interfaces");
        assertUnsupported(String.class, "final class", String.class.getName(), "only interfaces");
        assertUnsupported(int.class, "primitive type", "int", "only interfaces");
        assertUnsupported(String[].class, "array type", String[].class.getCanonicalName(), "only interfaces");
        assertUnsupported(SampleEnum.class, "enum type", SampleEnum.class.getCanonicalName(), "only interfaces");
        assertUnsupported(SampleAnnotation.class, "annotation type", SampleAnnotation.class.getCanonicalName(), "ordinary interfaces");
    }

    @Test
    public void stubsByMethodNameAndExactArgumentsIncludingNullAndArrays() {
        SampleService service = Doubles.create(SampleService.class);
        DoubleControl control = Doubles.control(service);

        control.when("greet").thenReturn("Hello anyone");
        control.when("greet", "Ada").thenReturn("Hello Ada");
        control.when("label", (Object) null).thenReturn("<null>");
        control.when("join", (Object) new String[] {"left", "right"}).thenReturn("left:right");

        assertEquals("Hello Ada", service.greet("Ada"));
        assertEquals("Hello anyone", service.greet("Grace"));
        assertEquals("<null>", service.label(null));
        assertNull(service.label("value"));
        assertEquals("left:right", service.join(new String[] {"left", "right"}));
        assertNull(service.join(new String[] {"left", "wrong"}));
    }

    @Test
    public void returnsDefaultValuesForUnstubbedMethods() {
        DefaultProbe probe = Doubles.create(DefaultProbe.class);

        assertFalse(probe.booleanValue());
        assertEquals(0, probe.byteValue());
        assertEquals(0, probe.shortValue());
        assertEquals(0, probe.intValue());
        assertEquals(0L, probe.longValue());
        assertEquals(0.0f, probe.floatValue(), 0.0f);
        assertEquals(0.0d, probe.doubleValue(), 0.0d);
        assertEquals('\0', probe.charValue());
        assertNull(probe.stringValue());
        assertNull(probe.objectValue());

        probe.voidValue("recorded");

        assertEquals(1, Doubles.control(probe).callCount("voidValue"));
    }

    @Test
    public void recordsCallsAndReturnsHistoryInCallOrder() {
        SampleService service = Doubles.create(SampleService.class);

        service.greet("first");
        service.join(new String[] {"a", "b"});
        service.greet("second");

        DoubleControl control = Doubles.control(service);
        List<Call> calls = control.calls();

        assertEquals(3, calls.size());
        assertEquals(Call.of("greet", "first"), calls.get(0));
        assertEquals("join", calls.get(1).methodName());
        assertTrue(calls.get(1).hasArguments((Object) new String[] {"a", "b"}));
        assertArrayEquals(new String[] {"a", "b"}, (String[]) calls.get(1).argument(0));
        assertEquals(Call.of("greet", "second"), calls.get(2));
        assertEquals(Arrays.asList(Call.of("greet", "first"), Call.of("greet", "second")), control.calls("greet"));
        assertEquals(1, control.calls("join", (Object) new String[] {"a", "b"}).size());
    }

    @Test
    public void verifiesCalledNotCalledAndExactCallCountsWithFailureMessages() {
        SampleService service = Doubles.create(SampleService.class);
        service.greet("Ada");
        service.greet("Ada");
        service.greet("Grace");

        DoubleControl control = Doubles.control(service);
        control.verifyCalled("greet");
        control.verifyCalled("greet", "Ada");
        control.verifyNotCalled("join");
        control.verifyNotCalled("greet", "missing");
        control.verifyCallCount("greet", 3);
        control.verifyCallCount("greet", 2, "Ada");
        control.verify("greet", "Ada").called();
        control.verify("join").notCalled();
        control.verify("greet", "Ada").times(2);
        assertEquals(2, control.verify("greet", "Ada").count());

        assertAssertionMessage(new ThrowingCall() {
            @Override
            public void run() {
                control.verifyCalled("missing");
            }
        }, "method 'missing'", "to have been called", "not called");

        assertAssertionMessage(new ThrowingCall() {
            @Override
            public void run() {
                control.verifyNotCalled("greet");
            }
        }, "method 'greet'", "not to have been called", "3 time(s)");

        assertAssertionMessage(new ThrowingCall() {
            @Override
            public void run() {
                control.verifyCallCount("greet", 1, "Ada");
            }
        }, "method 'greet' with arguments (Ada)", "1 time(s)", "2 time(s)");
    }

    @Test
    public void objectMethodsUseDeterministicIdentityBehaviorAndAreNotRecorded() {
        SampleService first = Doubles.create(SampleService.class);
        SampleService second = Doubles.create(SampleService.class);

        String firstDescription = first.toString();
        int firstHashCode = first.hashCode();

        assertEquals(firstDescription, first.toString());
        assertTrue(firstDescription.matches("Double<" + Pattern.quote(SampleService.class.getName()) + ">#\\d+"));
        assertEquals(firstHashCode, first.hashCode());
        assertTrue(first.equals(first));
        assertFalse(first.equals(second));
        assertFalse(first.equals(null));
        assertFalse(first.equals("not a double"));
        assertTrue(Doubles.control(first).calls().isEmpty());
    }

    private static void assertUnsupported(Class<?> type, String... fragments) {
        IllegalArgumentException error = (IllegalArgumentException) expect(IllegalArgumentException.class, new ThrowingCall() {
            @Override
            public void run() {
                Doubles.create(type);
            }
        });
        String message = String.valueOf(error.getMessage());
        for (int i = 0; i < fragments.length; i++) {
            assertTrue("Expected message to contain '" + fragments[i] + "' but was: " + message,
                    message.contains(fragments[i]));
        }
    }

    private static void assertAssertionMessage(ThrowingCall call, String... fragments) {
        AssertionError error = (AssertionError) expect(AssertionError.class, call);
        String message = String.valueOf(error.getMessage());
        for (int i = 0; i < fragments.length; i++) {
            assertTrue("Expected message to contain '" + fragments[i] + "' but was: " + message,
                    message.contains(fragments[i]));
        }
    }

    private static Throwable expect(Class<? extends Throwable> expectedType, ThrowingCall call) {
        try {
            call.run();
        } catch (Throwable thrown) {
            if (expectedType.isAssignableFrom(thrown.getClass())) {
                return thrown;
            }
            AssertionError error = new AssertionError("Expected " + expectedType.getName()
                    + " but got " + thrown.getClass().getName());
            error.initCause(thrown);
            throw error;
        }
        fail("Expected " + expectedType.getName() + " to be thrown");
        return null;
    }

    private interface ThrowingCall {
        void run() throws Throwable;
    }

    public interface SampleService {
        String greet(String name);

        String label(String value);

        String join(String[] values);
    }

    public interface DefaultProbe {
        boolean booleanValue();

        byte byteValue();

        short shortValue();

        int intValue();

        long longValue();

        float floatValue();

        double doubleValue();

        char charValue();

        String stringValue();

        Object objectValue();

        void voidValue(String value);
    }

    public static class ConcreteType {
    }

    public enum SampleEnum {
        VALUE
    }

    public @interface SampleAnnotation {
    }
}
