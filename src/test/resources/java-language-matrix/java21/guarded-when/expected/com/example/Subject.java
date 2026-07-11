package com.example;

public class Subject {
    public String describe(Object value) {
        return switch (value) {
            case String text when !text.isBlank() -> {
                String fake = "public String addedBehavior() { }";
                yield text + fake.substring(0, 0);
            }
            case String ignored -> "blank";
            default -> "other";
        };
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
