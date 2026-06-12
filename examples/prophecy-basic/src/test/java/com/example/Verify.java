package com.example;

import org.javaspec.doubles.Doubles;
import org.javaspec.doubles.InterfaceDouble;
import org.javaspec.doubles.prophecy.ObjectProphecy;
import org.javaspec.doubles.prophecy.PredictionRegistry;

import static org.javaspec.doubles.prophecy.Argument.*;

/**
 * Verification test that exercises the Prophecy-style doubles API programmatically.
 *
 * This test runs without the full javaspec runner to demonstrate that the
 * prophecy API works independently from spec infrastructure.
 */
public class Verify {

    public static void main(String[] args) {
        System.out.println("Running Verify for Prophecy-style doubles...");

        // --- Test 1: Basic stub + prediction ---
        System.out.print("  Test 1 (stub + prediction)... ");
        {
            InterfaceDouble<Mailer> id = Doubles.interfaceDouble(Mailer.class);
            PredictionRegistry registry = new PredictionRegistry();
            ObjectProphecy<Mailer> mailer = new ObjectProphecy<>(id, registry);

            mailer.method("send", any(String.class), eq("hello"), any(String.class))
                    .willReturn(true);
            mailer.method("send", any(String.class), eq("hello"), any(String.class))
                    .shouldBeCalled();

            Mailer proxy = mailer.reveal();
            boolean result = proxy.send("user@example.com", "hello", "Greetings!");

            if (!result) {
                throw new AssertionError("Expected true but got false");
            }

            registry.checkAll();
            System.out.println("PASS");
        }

        // --- Test 2: willThrow ---
        System.out.print("  Test 2 (willThrow)... ");
        {
            InterfaceDouble<Mailer> id = Doubles.interfaceDouble(Mailer.class);
            PredictionRegistry registry = new PredictionRegistry();
            ObjectProphecy<Mailer> mailer = new ObjectProphecy<>(id, registry);

            mailer.method("send", any(), any(), any())
                    .willThrow(new RuntimeException("Connection refused"));

            Mailer proxy = mailer.reveal();
            try {
                proxy.send("x@y.z", "fail", "body");
                throw new AssertionError("Expected exception was not thrown");
            } catch (RuntimeException e) {
                if (!"Connection refused".equals(e.getMessage())) {
                    throw new AssertionError("Expected 'Connection refused' but got: " + e.getMessage());
                }
            }
            System.out.println("PASS");
        }

        // --- Test 3: shouldNotBeCalled ---
        System.out.print("  Test 3 (shouldNotBeCalled)... ");
        {
            InterfaceDouble<Mailer> id = Doubles.interfaceDouble(Mailer.class);
            PredictionRegistry registry = new PredictionRegistry();
            ObjectProphecy<Mailer> mailer = new ObjectProphecy<>(id, registry);

            mailer.method("name").willReturn("TestMailer");

            Mailer proxy = mailer.reveal();
            String name = proxy.name();

            if (!"TestMailer".equals(name)) {
                throw new AssertionError("Expected 'TestMailer' but got: " + name);
            }

            mailer.method("send", any(), any(), any())
                    .shouldNotBeCalled();

            registry.checkAll();
            System.out.println("PASS");
        }

        // --- Test 4: shouldBeCalledTimes ---
        System.out.print("  Test 4 (shouldBeCalledTimes)... ");
        {
            InterfaceDouble<Mailer> id = Doubles.interfaceDouble(Mailer.class);
            PredictionRegistry registry = new PredictionRegistry();
            ObjectProphecy<Mailer> mailer = new ObjectProphecy<>(id, registry);

            mailer.method("send", any(), any(), any()).willReturn(true);

            Mailer proxy = mailer.reveal();
            proxy.send("a@b.com", "subj1", "body1");
            proxy.send("c@d.com", "subj2", "body2");

            mailer.method("send", any(), any(), any())
                    .shouldBeCalledTimes(2);

            registry.checkAll();
            System.out.println("PASS");
        }

        System.out.println("All Verify tests PASSED.");
    }
}
