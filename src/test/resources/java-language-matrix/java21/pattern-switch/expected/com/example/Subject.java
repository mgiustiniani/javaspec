package com.example;

record Label(String value) {
}

public class Subject {
    public String describe(Object value) {
        return switch (value) {
            case null -> "null";
            case Label(String text) -> text;
            case String text -> text;
            case Integer number -> Integer.toString(number);
            default -> "other";
        };
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
