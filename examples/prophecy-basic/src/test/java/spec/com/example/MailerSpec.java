package spec.com.example;

import com.example.Mailer;
import org.javaspec.api.ObjectBehavior;
import org.javaspec.doubles.prophecy.ObjectProphecy;

import static org.javaspec.doubles.prophecy.Argument.*;

/**
 * Specification demonstrating the Prophecy-style doubles API.
 *
 * This spec uses the reflective API ({@link ObjectProphecy#method(String, Object...)})
 * with argument matchers ({@code any()}, {@code eq()}), promises ({@code willReturn}),
 * and predictions ({@code shouldBeCalled}, {@code shouldNotBeCalled}).
 *
 * The generated {@code MailerProphecy} typed wrapper can be used instead for
 * concise {@code mailer.send(...).willReturn(true)} syntax.
 */
public class MailerSpec extends ObjectBehavior<Mailer> {

    public MailerSpec() {
        super(Mailer.class);
    }

    /**
     * Example 1: Using the reflective Prophecy API to stub a method and verify it was called.
     */
    public void it_stubs_a_prophecy_method_and_verifies_it_was_called() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        // --- Promise: configure the stub ---
        mailer.method("send", any(String.class), eq("hello"), any(String.class))
                .willReturn(true);

        // --- Use the prophesized object in the code under test ---
        Mailer proxy = mailer.reveal();
        boolean result = proxy.send("user@example.com", "hello", "Greetings!");

        // --- Verify the return value ---
        match(result).shouldReturn(true);

        // --- Prediction: verify the method was called ---
        mailer.method("send", any(String.class), eq("hello"), any(String.class))
                .shouldBeCalled();
    }

    /**
     * Example 2: Using the reflective API with {@code shouldNotBeCalled}.
     */
    public void it_verifies_a_method_was_not_called() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        mailer.method("name").willReturn("TestMailer");

        // Call name() but NOT send()
        Mailer proxy = mailer.reveal();
        match(proxy.name()).shouldReturn("TestMailer");

        // Predict that send() should NOT have been called
        mailer.method("send", any(), any(), any())
                .shouldNotBeCalled();
    }

    /**
     * Example 3: Using {@code shouldBeCalledTimes} to verify exact call count.
     */
    public void it_verifies_exact_call_count() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        mailer.method("send", any(), any(), any())
                .willReturn(true);

        Mailer proxy = mailer.reveal();
        proxy.send("a@b.com", "subj1", "body1");
        proxy.send("c@d.com", "subj2", "body2");

        mailer.method("send", any(), any(), any())
                .shouldBeCalledTimes(2);
    }

    /**
     * Example 4: Using {@code willThrow} to simulate failures.
     */
    public void it_stubs_a_method_to_throw() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        mailer.method("send", any(), any(), any())
                .willThrow(new RuntimeException("Connection refused"));

        Mailer proxy = mailer.reveal();

        try {
            proxy.send("x@y.z", "fail", "body");
            throw new AssertionError("Expected exception was not thrown");
        } catch (RuntimeException e) {
            match(e.getMessage()).shouldContain("Connection refused");
        }
    }

    /**
     * Example 5: Using argument matchers with {@code eq} and {@code containingString}.
     */
    public void it_uses_advanced_argument_matchers() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        // Match send() where the subject contains "order" and body is any string
        mailer.method("send", any(String.class), containingString("order"), any(String.class))
                .willReturn(true);

        Mailer proxy = mailer.reveal();
        match(proxy.send("shop@example.com", "order #123", "Details")).shouldReturn(true);
    }

    /**
     * Example 6: Using {@code isNull} and {@code notNull} matchers.
     */
    public void it_uses_isNull_and_notNull_matchers() {
        ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);

        mailer.method("send", isNull(), any(), any())
                .willReturn(false);

        Mailer proxy = mailer.reveal();
        match(proxy.send(null, "any", "body")).shouldReturn(false);

        mailer.method("send", notNull(), any(), any())
                .willReturn(true);
        match(proxy.send("someone@example.com", "any", "body")).shouldReturn(true);
    }
}
