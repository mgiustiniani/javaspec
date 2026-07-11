package com.example;

import java.util.List;

record Pair(String left, String right) {
}

record Envelope(Pair pair) {
}

public class Subject {
    public String describe(Object value) {
        try {
            if (value instanceof Envelope(Pair(String left, _))) {
                return left;
            }
            for (String _ : List.<String>of()) {
                throw new AssertionError("unreachable");
            }
        } catch (RuntimeException _) {
            return "failed";
        }
        return "other";
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
