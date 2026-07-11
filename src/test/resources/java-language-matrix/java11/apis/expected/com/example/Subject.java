package com.example;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Subject {
    public List<String> values() {
        return List.of("value");
    }

    public HttpClient client() {
        return HttpClient.newHttpClient();
    }

    public String read(Path path) throws IOException {
        return Files.readString(path);
    }

    public String addedBehavior() {
        // javaspec:stub
        return null;
    }
}
