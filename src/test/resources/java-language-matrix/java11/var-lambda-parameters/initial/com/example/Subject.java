package com.example;

import java.util.function.BiFunction;

public class Subject {
    public String existing() {
        BiFunction<String, String, String> join = (@Deprecated var left, var right) -> left + right;
        return join.apply("left", "right");
    }
}
