package com.example;

public class Subject {
    public String describe(Object value) {
        if (value instanceof String text && !text.isEmpty()) {
            return text.trim();
        }
        return "unknown";
    }
}
