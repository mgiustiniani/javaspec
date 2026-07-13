package io.github.jvmspec.cli.run;

/** Stable operational error-message fallback used by generation workflow components. */
final class GenerationErrorMessages {
    private GenerationErrorMessages() {
    }

    static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.length() == 0
                ? throwable.getClass().getName()
                : message;
    }
}
