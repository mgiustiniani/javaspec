package com.example;

import java.time.Instant;
import java.time.InstantSource;
import java.util.HexFormat;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

public class Subject {
    public List<String> values() {
        return Stream.of("value").toList();
    }

    public String hex() {
        return HexFormat.of().formatHex(new byte[] {0x0a, 0x0b});
    }

    public Instant instant() {
        return InstantSource.fixed(Instant.EPOCH).instant();
    }

    public int randomValue() {
        return RandomGenerator.getDefault().nextInt();
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
