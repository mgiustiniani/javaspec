package com.example;

/**
 * Final collaborator. Core javaspec cannot double this; javaspec-bytecode-agent can.
 */
public final class FinalGreeter {
    public final String greet(String name) {
        throw new UnsupportedOperationException("Real FinalGreeter calls an external greeting system.");
    }
}
