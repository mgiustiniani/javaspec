package com.example;

public class Subject extends Base {
    public Subject(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value");
        }
        String normalized = value.trim();
        super(normalized);
    }
}
