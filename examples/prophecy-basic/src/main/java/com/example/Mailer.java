package com.example;

public interface Mailer {
    boolean send(String type, String recipient, String body);
    String name();
}
