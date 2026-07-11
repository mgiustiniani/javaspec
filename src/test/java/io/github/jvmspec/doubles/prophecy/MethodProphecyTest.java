package io.github.jvmspec.doubles.prophecy;

import io.github.jvmspec.doubles.Call;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.jvmspec.doubles.prophecy.Argument.any;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MethodProphecyTest {
    @Test
    public void customArgumentTokenParticipatesInProphecyMatchingAndDiagnostics() {
        ObjectProphecy<Mailer> prophecy = Prophecies.prophesize(Mailer.class);
        Mailer mailer = prophecy.reveal();

        prophecy.<Boolean>method("send", Argument.token(new ArgumentToken() {
            @Override
            public boolean matches(Object actual) {
                return actual instanceof String && ((String) actual).endsWith("@example.com");
            }

            @Override
            public String describe() {
                return "exampleAddress()";
            }
        })).willReturn(Boolean.TRUE).shouldBeCalled();

        assertEquals(Boolean.TRUE, Boolean.valueOf(mailer.send("user@example.com")));
        prophecy.predictionRegistry().checkAll();
    }

    @Test
    public void customArgumentTokenAliasRejectsNull() {
        NullPointerException error = (NullPointerException) expect(NullPointerException.class, new ThrowingCheck() {
            @Override
            public void run() {
                Arg.custom(null);
            }
        });
        assertTrue(error.getMessage().contains("token must not be null"));
    }

    @Test
    public void customPredictionCallbackReceivesMatchingCallsAndAllCalls() {
        ObjectProphecy<Mailer> prophecy = Prophecies.prophesize(Mailer.class);
        Mailer mailer = prophecy.reveal();
        final List<Call> seen = new ArrayList<Call>();

        prophecy.<Boolean>method("send", any(String.class)).should(new PredictionCallback() {
            @Override
            public void check(PredictionContext context) {
                assertEquals("send", context.methodName());
                assertEquals(2, context.callCount());
                assertEquals(3, context.allCalls().size());
                assertSame(context.calls(), context.getCalls());
                assertSame(context.allCalls(), context.getAllCalls());
                seen.addAll(context.calls());
            }
        });

        mailer.send("first");
        mailer.flush();
        mailer.send("second");

        prophecy.predictionRegistry().checkAll();

        assertEquals(2, seen.size());
        assertEquals("first", seen.get(0).argument(0));
        assertEquals("second", seen.get(1).argument(0));
    }

    @Test
    public void customPredictionAssertionFailureIsReportedByRegistry() {
        ObjectProphecy<Mailer> prophecy = Prophecies.prophesize(Mailer.class);

        prophecy.<Boolean>method("send", "expected@example.com").should(new PredictionCallback() {
            @Override
            public void check(PredictionContext context) {
                throw new AssertionError("domain prediction failed for " + context.methodName());
            }
        });

        AssertionError failure = expectAssertion(new ThrowingCheck() {
            @Override
            public void run() {
                prophecy.predictionRegistry().checkAll();
            }
        });
        assertTrue(failure.getMessage().contains("Prophecy predictions failed (1)"));
        assertTrue(failure.getMessage().contains("domain prediction failed for send"));
    }

    @Test
    public void customPredictionExceptionIsWrappedWithMethodContext() {
        ObjectProphecy<Mailer> prophecy = Prophecies.prophesize(Mailer.class);

        prophecy.<Boolean>method("send", "expected@example.com").should(new PredictionCallback() {
            @Override
            public void check(PredictionContext context) {
                throw new IllegalStateException("boom");
            }
        });

        AssertionError failure = expectAssertion(new ThrowingCheck() {
            @Override
            public void run() {
                prophecy.predictionRegistry().checkAll();
            }
        });
        assertTrue(failure.getMessage().contains("Custom prediction for method 'send' failed: boom"));
        assertEquals(1, failure.getSuppressed().length);
        assertNotNull(failure.getSuppressed()[0].getCause());
    }

    private static AssertionError expectAssertion(ThrowingCheck check) {
        return (AssertionError) expect(AssertionError.class, check);
    }

    private static Throwable expect(Class<? extends Throwable> expectedType, ThrowingCheck check) {
        try {
            check.run();
        } catch (Throwable thrown) {
            if (expectedType.isAssignableFrom(thrown.getClass())) {
                return thrown;
            }
            AssertionError error = new AssertionError("Expected " + expectedType.getName()
                    + " but got " + thrown.getClass().getName());
            error.initCause(thrown);
            throw error;
        }
        fail("Expected " + expectedType.getName());
        return null;
    }

    private interface ThrowingCheck {
        void run();
    }

    public interface Mailer {
        boolean send(String address);

        void flush();
    }
}
