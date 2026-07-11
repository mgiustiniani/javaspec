package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;

public class Subject {
    public SequencedCollection<String> values() {
        return new ArrayList<>(List.of("first", "last"));
    }

    public String first() {
        return values().getFirst();
    }

    public String reversedFirst() {
        return values().reversed().getFirst();
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
