package com.example;

import java.util.stream.Gatherers;
import java.util.stream.Stream;

public class Subject {
    public String folded() {
        return Stream.of("a", "b")
                .gather(Gatherers.fold(() -> "", (result, element) -> result + element))
                .findFirst()
                .orElse("");
    }
}
