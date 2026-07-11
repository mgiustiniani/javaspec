package com.example;

public interface Subject {
    default String existing() {
        return "default";
    }

    static String utility() {
        return "static";
    }
}
