package com.example;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class Subject {
    public int existing() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {42});
        try (input) {
            return input.read();
        }
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
