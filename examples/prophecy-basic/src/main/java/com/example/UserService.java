package com.example;

/**
 * A service that depends on Mailer to send welcome emails.
 */
public class UserService {
    private Mailer mailer;

    public void setMailer(Mailer mailer) {
        this.mailer = mailer;
    }

    public void sendWelcomeEmail(String email) {
        mailer.send("email", email, "Welcome to javaspec!");
    }

    public String greet(String name) {
        return "Hello, " + name;
    }

    public void setMailer(Object mailer) {
    }
}
