package com.example;

import java.util.function.Function;
import java.util.function.Supplier;

public class Subject {
    public String existing() {
        final String prefix = "captured:";
        Supplier<String> expression = () -> prefix;
        Function<String, String> block = (String value) -> {
            String brace = "}";
            return expression.get() + value + brace;
        };
        return block.apply("value");
    }
}
