package com.example;

/**
 * Static utility to demonstrate scoped static interception.
 */
public final class StaticFormatter {
    private StaticFormatter() {
    }

    public static String format(String message) {
        return "real:" + message;
    }
}
