package com.example;

public class Subject {
    public String describe(Event event) {
        return switch (event) {
            case Created(String id) -> "created:" + id;
            case Deleted(String id) -> "deleted:" + id;
        };
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
