package com.example;

/**
 * Service that depends on a final collaborator and a static formatting utility.
 */
public class GreetingService {
    private final FinalGreeter greeter;

    public GreetingService(FinalGreeter greeter) {
        this.greeter = greeter;
    }

    public String greet(String name) {
        return greeter.greet(name);
    }

    public String format(String message) {
        return StaticFormatter.format(message);
    }
}
