package com.example;

import java.util.function.Function;

public class Subject {
    public String transform(String value, Function<String, String> normalizer) {
        return normalizer.apply(value);
    }
}
