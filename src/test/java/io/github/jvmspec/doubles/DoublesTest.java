package io.github.jvmspec.doubles;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
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
    public void exposesControlFactoryForAdapterHandlers() {
        InvocationHandler handler = Doubles.newDoubleHandler(SampleService.class);
        DoubleControl control = Doubles.controlFromHandler(handler);

        assertNotNull(control);
        control.when("greet", "Ada").thenReturn("Hello Ada");
    }

    @Test
    public void controlFactoryRejectsForeignHandlers() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return null;
            }
        };

        try {
            Doubles.controlFromHandler(handler);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Doubles.newDoubleHandler"));
        }
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
    public void argumentMatchersWorkAcrossVarargStubbingVerificationQueriesAndCalls() {
        MatcherService service = Doubles.create(MatcherService.class);
        DoubleControl control = Doubles.control(service);

        control.when("anyPair", Doubles.any(), Doubles.anyArgument()).thenReturn("any pair");
        control.when("typedReference", Doubles.any(String.class)).thenReturn("string value");
        control.when("typedAlias", Doubles.anyType(Number.class)).thenReturn("number value");
        control.when("typedPrimitiveToken", Doubles.any(int.class)).thenReturn("boxed int value");
        control.when("primitiveNumber", Doubles.anyType(int.class)).thenReturn("primitive int value");
        control.when("onlyNull", Doubles.isNull()).thenReturn("null value");
        control.when("onlyNotNull", Doubles.notNull()).thenReturn("not-null value");

        assertEquals("any pair", service.anyPair(null, "right"));
        assertEquals("any pair", service.anyPair("left", null));
        assertEquals("string value", service.typedReference(null));
        assertEquals("string value", service.typedReference("text"));
        assertNull(service.typedReference(Integer.valueOf(42)));
        assertEquals("number value", service.typedAlias(null));
        assertEquals("number value", service.typedAlias(Long.valueOf(42L)));
        assertNull(service.typedAlias("not a number"));
        assertEquals("boxed int value", service.typedPrimitiveToken(null));
        assertEquals("boxed int value", service.typedPrimitiveToken(Integer.valueOf(7)));
        assertNull(service.typedPrimitiveToken(Long.valueOf(7L)));
        assertEquals("primitive int value", service.primitiveNumber(9));
        assertEquals("null value", service.onlyNull(null));
        assertNull(service.onlyNull("value"));
        assertEquals("not-null value", service.onlyNotNull("value"));
        assertNull(service.onlyNotNull(null));

        control.verifyCalled("anyPair", Doubles.any(), Doubles.anyArgument());
        control.verifyCalled("typedReference", Doubles.any(String.class));
        control.verifyCalled("typedPrimitiveToken", Doubles.any(int.class));
        control.verifyNotCalled("missing", Doubles.any());
        control.verifyCallCount("anyPair", 2, Doubles.any(), Doubles.anyArgument());
        control.verifyCallCount("typedReference", 2, Doubles.any(String.class));
        assertEquals(2, control.callCount("anyPair", Doubles.any(), Doubles.anyArgument()));
        assertEquals(2, control.callCount("typedAlias", Doubles.anyType(Number.class)));
        assertEquals(2, control.callCount("typedPrimitiveToken", Doubles.any(int.class)));
        assertEquals(1, control.callCount("primitiveNumber", Doubles.anyType(int.class)));
        assertEquals(1, control.callCount("onlyNull", Doubles.isNull()));
        assertEquals(1, control.callCount("onlyNotNull", Doubles.notNull()));
        control.verify("anyPair", Doubles.any(), Doubles.anyArgument()).times(2);
        assertEquals(2, control.verify("anyPair", Doubles.any(), Doubles.anyArgument()).count());

        List<Call> matchingCalls = control.calls("anyPair", Doubles.any(), Doubles.anyArgument());
        assertEquals(2, matchingCalls.size());
        assertTrue(matchingCalls.get(0).hasArguments(Doubles.any(), Doubles.equalTo("right")));
        assertTrue(matchingCalls.get(1).hasArguments(Doubles.any(String.class), Doubles.anyArgument()));
        assertFalse(matchingCalls.get(1).hasArguments(Doubles.any(Integer.class), Doubles.notNull()));

        assertAssertionMessage(new ThrowingCall() {
            @Override
            public void run() {
                control.verifyCalled("missing", Doubles.any(), Doubles.any(String.class),
                        Doubles.eq(new String[] {"a", "b"}));
            }
        }, "any()", "any(java.lang.String)", "eq([a, b])");
    }

    @Test
    public void equalityMatchersUseArrayAwareEqualityAndDefensiveExpectedCopies() {
        MatcherService service = Doubles.create(MatcherService.class);
        DoubleControl control = Doubles.control(service);

        String[] expected = new String[] {"a", "b"};
        control.when("arrayValue", Doubles.eq(expected)).thenReturn("eq-array");
        expected[0] = "changed";
        control.when("arrayValue", Doubles.equalTo(new String[] {"x", "y"})).thenReturn("equal-to-array");

        assertEquals("eq-array", service.arrayValue(new String[] {"a", "b"}));
        assertEquals("equal-to-array", service.arrayValue(new String[] {"x", "y"}));
        assertNull(service.arrayValue(new String[] {"changed", "b"}));

        control.verifyCalled("arrayValue", Doubles.eq(new String[] {"a", "b"}));
        control.verifyCallCount("arrayValue", 1, Doubles.equalTo(new String[] {"x", "y"}));
        assertEquals(1, control.callCount("arrayValue", Doubles.eq(new String[] {"a", "b"})));

        String[] queryExpected = new String[] {"a", "b"};
        ArgumentMatcher queryMatcher = Doubles.equalTo(queryExpected);
        queryExpected[1] = "changed";
        List<Call> calls = control.calls("arrayValue", queryMatcher);
        assertEquals(1, calls.size());
        assertTrue(calls.get(0).hasArguments(Doubles.eq(new String[] {"a", "b"})));
        assertFalse(calls.get(0).hasArguments(Doubles.eq(new String[] {"changed", "b"})));
    }

    @Test
    public void argumentConstrainedStubsTakePriorityAndNewestMatchingConstrainedStubWins() {
        MatcherService service = Doubles.create(MatcherService.class);
        DoubleControl control = Doubles.control(service);

        control.when("choose").thenReturn("method-wide original");
        control.when("choose", Doubles.any()).thenReturn("constrained any");
        control.when("choose").thenReturn("method-wide newest");

        assertEquals("constrained any", service.choose("anything"));
        assertEquals("constrained any", service.choose(null));

        control.when("choose", Doubles.eq("specific")).thenReturn("specific constrained");
        assertEquals("specific constrained", service.choose("specific"));

        control.when("choose", Doubles.any()).thenReturn("constrained newest");
        assertEquals("constrained newest", service.choose("specific"));
        assertEquals("constrained newest", service.choose("other"));
    }

    @Test
    public void sequentialReturnStubsReturnValuesInOrderAndRepeatFinalValue() {
        SampleService service = Doubles.create(SampleService.class);
        DoubleControl control = Doubles.control(service);

        control.when("greet", "Ada").thenReturn("first", "second", "third");
        control.when("label").returns("one", "two");

        assertEquals("first", service.greet("Ada"));
        assertEquals("second", service.greet("Ada"));
        assertEquals("third", service.greet("Ada"));
        assertEquals("third", service.greet("Ada"));
        assertEquals("one", service.label("a"));
        assertEquals("two", service.label("b"));
        assertEquals("two", service.label("c"));
        assertEquals(4, control.callCount("greet", "Ada"));
        assertEquals(3, control.callCount("label"));
    }

    @Test
    public void throwingStubsThrowConfiguredThrowableAndRecordCallsBeforeThrowing() {
        SampleService service = Doubles.create(SampleService.class);
        DoubleControl control = Doubles.control(service);
        RuntimeException failure = new IllegalStateException("boom");

        control.when("greet", Doubles.eq("boom")).thenThrow(failure);

        Throwable thrown = expect(RuntimeException.class, new ThrowingCall() {
            @Override
            public void run() {
                service.greet("boom");
            }
        });
        assertSame(failure, thrown);
        assertEquals(1, control.callCount("greet", "boom"));
        assertEquals(1, control.callCount("greet", Doubles.eq("boom")));

        CheckedService checked = Doubles.create(CheckedService.class);
        DoubleControl checkedControl = Doubles.control(checked);
        IOException checkedFailure = new IOException("checked boom");
        checkedControl.when("load", Doubles.any()).throwsException(checkedFailure);

        Throwable checkedThrown = expect(IOException.class, new ThrowingCall() {
            @Override
            public void run() throws Throwable {
                checked.load("key");
            }
        });
        assertSame(checkedFailure, checkedThrown);
        assertEquals(1, checkedControl.callCount("load", Doubles.any()));
    }

    @Test
    public void throwingStubsRejectNullThrowableClearly() {
        SampleService service = Doubles.create(SampleService.class);
        final DoubleControl control = Doubles.control(service);

        NullPointerException thenThrowError = (NullPointerException) expect(NullPointerException.class,
                new ThrowingCall() {
                    @Override
                    public void run() {
                        control.when("greet").thenThrow(null);
                    }
                });
        assertThrowableMessage(thenThrowError, "throwable must not be null");

        NullPointerException aliasError = (NullPointerException) expect(NullPointerException.class,
                new ThrowingCall() {
                    @Override
                    public void run() {
                        control.when("greet").throwsException(null);
                    }
                });
        assertThrowableMessage(aliasError, "throwable must not be null");
    }

    @Test
    public void answerStubsReceiveImmutableInvocationAndReturnCallbackResult() {
        AnswerService service = Doubles.create(AnswerService.class);
        DoubleControl control = Doubles.control(service);
        final DoubleInvocation[] invocationSeen = new DoubleInvocation[1];
        final String[] submitted = new String[] {"a", "b"};

        control.when("describe", Doubles.eq("prefix"), Doubles.any(String[].class)).thenAnswer(new StubAnswer() {
            @Override
            public Object answer(DoubleInvocation invocation) throws Throwable {
                invocationSeen[0] = invocation;
                Method expectedMethod = AnswerService.class.getMethod("describe", String.class, String[].class);

                assertEquals("describe", invocation.methodName());
                assertEquals("describe", invocation.getMethodName());
                assertEquals(expectedMethod, invocation.method());
                assertEquals(expectedMethod, invocation.getMethod());
                assertEquals(2, invocation.argumentCount());
                assertEquals("prefix", invocation.argument(0));
                assertEquals(2, invocation.arguments().size());
                assertEquals(2, invocation.getArguments().size());
                assertEquals(2, invocation.argumentsArray().length);
                assertEquals("describe(prefix, [a, b])", invocation.toString());

                List<Object> arguments = invocation.arguments();
                try {
                    arguments.add("extra");
                    fail("Expected invocation arguments to be immutable");
                } catch (UnsupportedOperationException expected) {
                    // expected
                }
                String[] listArray = (String[]) arguments.get(1);
                listArray[0] = "list mutation";
                assertArrayEquals(new String[] {"a", "b"}, (String[]) invocation.argument(1));

                Object[] argumentArray = invocation.argumentsArray();
                argumentArray[0] = "array mutation";
                ((String[]) argumentArray[1])[0] = "nested array mutation";
                assertEquals("prefix", invocation.argument(0));
                assertArrayEquals(new String[] {"a", "b"}, (String[]) invocation.argument(1));

                String[] indexedArgument = (String[]) invocation.argument(1);
                indexedArgument[1] = "indexed mutation";
                assertArrayEquals(new String[] {"a", "b"}, (String[]) invocation.argument(1));

                String[] values = (String[]) invocation.argument(1);
                return invocation.methodName() + ":" + values[0] + ":" + values[1];
            }
        });

        assertEquals("describe:a:b", service.describe("prefix", submitted));
        submitted[0] = "caller mutation";
        assertArrayEquals(new String[] {"a", "b"}, (String[]) invocationSeen[0].argument(1));

        control.when("primitiveResult", Doubles.any()).answers(new StubAnswer() {
            @Override
            public Object answer(DoubleInvocation invocation) {
                return Integer.valueOf(invocation.argumentCount() + 10);
            }
        });
        assertEquals(11, service.primitiveResult("score"));
    }

    @Test
    public void answerStubsPropagateExceptionsRecordCallsAndValidateReturnValues() {
        AnswerService service = Doubles.create(AnswerService.class);
        DoubleControl control = Doubles.control(service);

        control.when("describe", "bad", Doubles.any()).thenAnswer(new StubAnswer() {
            @Override
            public Object answer(DoubleInvocation invocation) {
                return Integer.valueOf(5);
            }
        });
        IllegalStateException typeError = (IllegalStateException) expect(IllegalStateException.class,
                new ThrowingCall() {
                    @Override
                    public void run() {
                        service.describe("bad", new String[] {"x"});
                    }
                });
        assertThrowableMessage(typeError, "Stubbed value", "describe", Integer.class.getName(), String.class.getName());
        assertEquals(1, control.callCount("describe", "bad", Doubles.any()));

        control.when("primitiveResult", "null").answers(new StubAnswer() {
            @Override
            public Object answer(DoubleInvocation invocation) {
                return null;
            }
        });
        IllegalStateException nullError = (IllegalStateException) expect(IllegalStateException.class,
                new ThrowingCall() {
                    @Override
                    public void run() {
                        service.primitiveResult("null");
                    }
                });
        assertThrowableMessage(nullError, "primitiveResult", "null", "primitive int");
        assertEquals(1, control.callCount("primitiveResult", "null"));

        final RuntimeException failure = new RuntimeException("answer boom");
        control.when("describe", "throw", Doubles.any()).thenAnswer(new StubAnswer() {
            @Override
            public Object answer(DoubleInvocation invocation) {
                throw failure;
            }
        });
        Throwable thrown = expect(RuntimeException.class, new ThrowingCall() {
            @Override
            public void run() {
                service.describe("throw", new String[] {"x"});
            }
        });
        assertSame(failure, thrown);
        assertEquals(1, control.callCount("describe", "throw", Doubles.any()));
    }

    @Test
    public void answerStubsRejectNullAnswerClearly() {
        AnswerService service = Doubles.create(AnswerService.class);
        final DoubleControl control = Doubles.control(service);

        NullPointerException thenAnswerError = (NullPointerException) expect(NullPointerException.class,
                new ThrowingCall() {
                    @Override
                    public void run() {
                        control.when("describe").thenAnswer(null);
                    }
                });
        assertThrowableMessage(thenAnswerError, "answer must not be null");

        NullPointerException aliasError = (NullPointerException) expect(NullPointerException.class,
                new ThrowingCall() {
                    @Override
                    public void run() {
                        control.when("describe").answers(null);
                    }
                });
        assertThrowableMessage(aliasError, "answer must not be null");
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
    public void invokesUnstubbedDefaultInterfaceMethodsAndStillRecordsCalls() {
        DefaultMethodService service = Doubles.create(DefaultMethodService.class);
        DoubleControl control = Doubles.control(service);

        assertEquals("Hello Ada", service.greet("Ada"));
        assertEquals(5, service.add(2, 3));
        assertNull(service.plain("missing"));

        assertEquals(1, control.callCount("greet", "Ada"));
        assertEquals(1, control.callCount("add", 2, 3));
        assertEquals(1, control.callCount("plain", "missing"));
    }

    @Test
    public void stubsOverrideDefaultInterfaceMethods() {
        DefaultMethodService service = Doubles.create(DefaultMethodService.class);
        DoubleControl control = Doubles.control(service);

        control.when("greet", "Ada").thenReturn("Stubbed Ada");
        control.when("add").thenReturn(Integer.valueOf(99));

        assertEquals("Stubbed Ada", service.greet("Ada"));
        assertEquals("Hello Grace", service.greet("Grace"));
        assertEquals(99, service.add(2, 3));
        assertEquals(2, control.callCount("greet"));
        assertEquals(1, control.callCount("add", 2, 3));
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

    private static void assertThrowableMessage(Throwable error, String... fragments) {
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

    public interface MatcherService {
        String anyPair(Object first, Object second);

        String typedReference(Object value);

        String typedAlias(Object value);

        String typedPrimitiveToken(Object value);

        String primitiveNumber(int value);

        String onlyNull(Object value);

        String onlyNotNull(Object value);

        String arrayValue(String[] values);

        String choose(String value);
    }

    public interface CheckedService {
        String load(String key) throws IOException;
    }

    public interface AnswerService {
        String describe(String prefix, String[] values);

        int primitiveResult(String key);
    }

    public interface SampleService {
        String greet(String name);

        String label(String value);

        String join(String[] values);
    }

    public interface OrderedService {
        void first();
        void second();
        void third();
    }

    public interface CaptureService {
        String process(String input);
    }

    public interface ExhaustService {
        int next();
    }

    public interface AnswerSeqService {
        String transform(String input);
    }

    public interface DefaultMethodService {
        default String greet(String name) {
            return "Hello " + name;
        }

        default int add(int left, int right) {
            return left + right;
        }

        String plain(String value);
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

    // -------------------------------------------------------------------------
    // Phase 43: Ordered verification, ArgumentCaptor, answer sequences, exhaustion

    @Test
    public void verifyInOrderPassesWhenMethodsCalledInOrder() {
        OrderedService svc = Doubles.create(OrderedService.class);
        DoubleControl ctrl = Doubles.control(svc);
        svc.first();
        svc.second();
        svc.third();
        ctrl.verifyInOrder("first", "second", "third");
    }

    @Test
    public void verifyInOrderFailsWhenOrderIsWrong() {
        OrderedService svc = Doubles.create(OrderedService.class);
        DoubleControl ctrl = Doubles.control(svc);
        svc.second();
        svc.first();
        Throwable err = expect(AssertionError.class, new ThrowingCall() {
            public void run() throws Throwable {
                ctrl.verifyInOrder("first", "second");
            }
        });
        assertTrue(err.getMessage().contains("first"));
        assertTrue(err.getMessage().contains("second"));
    }

    @Test
    public void verifyInOrderFailsWhenMethodNotCalled() {
        OrderedService svc = Doubles.create(OrderedService.class);
        DoubleControl ctrl = Doubles.control(svc);
        svc.first();
        Throwable err = expect(AssertionError.class, new ThrowingCall() {
            public void run() throws Throwable {
                ctrl.verifyInOrder("first", "second");
            }
        });
        assertTrue(err.getMessage().contains("second"));
    }

    @Test
    public void verifyCalledBeforePassesWhenFirstBeforeSecond() {
        OrderedService svc = Doubles.create(OrderedService.class);
        DoubleControl ctrl = Doubles.control(svc);
        svc.first();
        svc.second();
        ctrl.verifyCalledBefore("first", "second");
    }

    @Test
    public void argumentCaptorCapturesPassedValue() {
        CaptureService svc = Doubles.create(CaptureService.class);
        DoubleControl ctrl = Doubles.control(svc);
        ArgumentCaptor<String> captor = ArgumentCaptor.create();
        ctrl.when("process", captor).thenReturn("ok");

        svc.process("hello");

        assertEquals("hello", captor.value());
        assertEquals(1, captor.captureCount());
        assertTrue(captor.hasCaptured());
    }

    @Test
    public void argumentCaptorCapturesAllValues() {
        CaptureService svc = Doubles.create(CaptureService.class);
        DoubleControl ctrl = Doubles.control(svc);
        ArgumentCaptor<String> captor = ArgumentCaptor.create();
        ctrl.when("process", captor).thenReturn("ok");

        svc.process("first");
        svc.process("second");
        svc.process("third");

        List<String> captured = captor.allValues();
        assertEquals(3, captured.size());
        assertEquals("first", captured.get(0));
        assertEquals("second", captured.get(1));
        assertEquals("third", captured.get(2));
    }

    @Test
    public void argumentCaptorThrowsWhenNoValueCaptured() {
        ArgumentCaptor<String> captor = ArgumentCaptor.create();
        assertFalse(captor.hasCaptured());
        assertEquals(0, captor.captureCount());
        expect(IllegalStateException.class, new ThrowingCall() {
            public void run() {
                captor.value();
            }
        });
    }

    @Test
    public void thenReturnThenThrowDeliversValuesAndThenThrows() throws Throwable {
        ExhaustService svc = Doubles.create(ExhaustService.class);
        DoubleControl ctrl = Doubles.control(svc);
        RuntimeException ex = new RuntimeException("exhausted");
        ctrl.when("next").thenReturnThenThrow(ex, 1, 2);

        assertEquals(1, svc.next());
        assertEquals(2, svc.next());
        Throwable thrown = expect(RuntimeException.class, new ThrowingCall() {
            public void run() {
                svc.next();
            }
        });
        assertEquals("exhausted", thrown.getMessage());
    }

    @Test
    public void thenAnswerSequenceDeliversAnswersInOrder() {
        AnswerSeqService svc = Doubles.create(AnswerSeqService.class);
        DoubleControl ctrl = Doubles.control(svc);
        ctrl.when("transform").thenAnswerSequence(
                new StubAnswer() {
                    public Object answer(DoubleInvocation inv) { return "A:" + inv.argument(0); }
                },
                new StubAnswer() {
                    public Object answer(DoubleInvocation inv) { return "B:" + inv.argument(0); }
                }
        );

        assertEquals("A:x", svc.transform("x"));
        assertEquals("B:y", svc.transform("y"));
        // After sequence exhausted, last answer is repeated.
        assertEquals("B:z", svc.transform("z"));
    }

    public static class ConcreteType {
    }

    public enum SampleEnum {
        VALUE
    }

    public @interface SampleAnnotation {
    }
}
