package com.example;

import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class Subject {
    public Supplier<Subject> factory() {
        return Subject::new;
    }

    public Function<Subject, String> reader() {
        return Subject::existing;
    }

    public Supplier<String> boundReader() {
        return this::existing;
    }

    public Supplier<String> staticReader() {
        return Subject::staticExisting;
    }

    public IntFunction<String[]> arrayFactory() {
        return String[]::new;
    }

    public String existing() {
        return "existing";
    }

    public static String staticExisting() {
        return "static";
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
