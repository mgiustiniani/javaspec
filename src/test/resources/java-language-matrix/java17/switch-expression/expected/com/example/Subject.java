package com.example;

public class Subject {
    public int category(int value) {
        return switch (value) {
            case 0 -> 0;
            case 1, 2 -> 1;
            default -> {
                String fake = "public String addedBehavior() { }";
                yield fake.length();
            }
        };
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
