package spec.com.example;

import com.example.Mailer;
import com.example.UserService;
import org.javaspec.api.ObjectBehavior;
import org.javaspec.doubles.prophecy.ObjectProphecy;

import static org.javaspec.doubles.prophecy.Argument.*;

/**
 * Specification demonstrating Prophecy-style doubles with a {@link UserService}
 * that depends on {@link Mailer}.
 *
 * Uses the reflective API ({@link ObjectProphecy#method(String, Object...)})
 * with argument matchers ({@code any()}, {@code eq()}, {@code containingString}),
 * promises ({@code willReturn}), and predictions ({@code shouldBeCalled},
 * {@code shouldNotBeCalled}, {@code shouldBeCalledTimes}).
 */
public class UserServiceSpec extends ObjectBehavior<UserService> {

    public UserServiceSpec() {
        super(UserService.class);
    }

    /**
     * Example 1: Stub the Mailer dependency with willReturn and verify the
     * method was called via shouldBeCalled.
     */
    public void it_stubs_mailer_and_verifies_it_was_called() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        // --- Promise: configure the stub ---
        mailer.method("send", any(String.class), any(String.class), any(String.class))
                .willReturn(true);

        // --- Inject the prophecy double into the subject ---
        subject().setMailer(mailer.reveal());

        // --- Execute the code under test ---
        subject().sendWelcomeEmail("user@example.com");

        // --- Prediction: verify the method was called ---
        mailer.method("send", any(String.class), any(String.class), any(String.class))
                .shouldBeCalled();

        checkPredictions();
    }

    /**
     * Example 2: Verify a method was NOT called on the Mailer dependency.
     */
    public void it_verifies_mailer_was_not_called() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        // --- Promise: configure the stub ---
        mailer.method("send", any(String.class), any(String.class), any(String.class))
                .willReturn(true);

        // --- Inject the prophecy double into the subject ---
        subject().setMailer(mailer.reveal());

        // --- Execute greet() which does NOT call mailer.send() ---
        String greeting = subject().greet("World");

        match(greeting).shouldReturn("Hello, World");

        // --- Prediction: send() should NOT have been called ---
        mailer.method("send", any(String.class), any(String.class), any(String.class))
                .shouldNotBeCalled();

        checkPredictions();
    }

    /**
     * Example 3: Verify exact call count with shouldBeCalledTimes.
     */
    public void it_verifies_exact_call_count() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        mailer.method("send", any(String.class), any(String.class), any(String.class))
                .willReturn(true);

        subject().setMailer(mailer.reveal());

        // --- Call sendWelcomeEmail twice ---
        subject().sendWelcomeEmail("a@example.com");
        subject().sendWelcomeEmail("b@example.com");

        mailer.method("send", any(String.class), any(String.class), any(String.class))
                .shouldBeCalledTimes(2);

        checkPredictions();
    }

    /**
     * Example 4: Use willThrow to simulate mailer failures.
     */
    public void it_stubs_mailer_to_throw() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        mailer.method("send", any(String.class), any(String.class), any(String.class))
                .willThrow(new RuntimeException("Connection refused"));

        subject().setMailer(mailer.reveal());

        try {
            subject().sendWelcomeEmail("x@y.z");
            throw new AssertionError("Expected exception was not thrown");
        } catch (RuntimeException e) {
            match(e.getMessage()).shouldContain("Connection refused");
        }
    }

    /**
     * Example 5: Use argument matchers (any, eq, containingString).
     */
    public void it_uses_argument_matchers() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        // Match send() where type is "email" and body contains "Welcome"
        mailer.method("send", eq("email"), any(String.class), containingString("Welcome"))
                .willReturn(true);

        subject().setMailer(mailer.reveal());

        // Execute — sendWelcomeEmail returns void, so verify via prediction
        subject().sendWelcomeEmail("user@example.com");

        mailer.method("send", eq("email"), any(String.class), containingString("Welcome"))
                .shouldBeCalled();

        checkPredictions();
    }
}
