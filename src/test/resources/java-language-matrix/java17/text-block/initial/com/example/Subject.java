package com.example;

public class Subject {
    private final String template = """
            A single " quote does not close this text block.
            fake brace: }
            fake comment: // public String addedBehavior() {
            fake block: /* return "fake"; } */
            """;

    public String template() {
        return template;
    }
}
