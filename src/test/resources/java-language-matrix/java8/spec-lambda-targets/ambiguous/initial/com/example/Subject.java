package com.example;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class Subject {
    public String transform(String value, Function<String, String> normalizer) {
        return normalizer.apply(value);
    }

    public String transform(String value, UnaryOperator<String> normalizer) {
        return normalizer.apply(value);
    }
}
