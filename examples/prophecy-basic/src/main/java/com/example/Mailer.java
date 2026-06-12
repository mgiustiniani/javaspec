package com.example;

/**
 * A simple mailer interface to demonstrate Prophecy-style doubles.
 *
 * @see <a href="https://phpspec.net/">phpspec/prophecy</a>
 */
public interface Mailer {
    /**
     * Sends an email to the given recipient with the given subject and body.
     *
     * @param recipient the email recipient address
     * @param subject   the email subject
     * @param body      the email body
     * @return true if the message was sent successfully
     */
    boolean send(String recipient, String subject, String body);

    /**
     * Returns the display name of this mailer.
     *
     * @return the mailer name
     */
    String name();
}
