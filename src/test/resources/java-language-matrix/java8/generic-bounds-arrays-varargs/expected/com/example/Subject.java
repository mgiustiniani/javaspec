package com.example;

import java.util.Arrays;
import java.util.List;

public class Subject {
    @SafeVarargs
    public final <T extends Comparable<T>> List<? extends T> existing(T[] values, T... more) {
        return Arrays.asList(values);
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
