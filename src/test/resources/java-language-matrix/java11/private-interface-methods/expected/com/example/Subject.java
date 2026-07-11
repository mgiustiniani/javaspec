package com.example;

public interface Subject {
    default String existing() {
        return decorate("value");
    }

    private String decorate(String value) {
        return "[" + value + "]";
    }

    String addedBehavior();
}
