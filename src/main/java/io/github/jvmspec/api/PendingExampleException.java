package io.github.jvmspec.api;

/**
 * Unchecked signal that marks the current example as pending.
 * <p>
 * Specs that do not extend {@link ObjectBehavior} can throw this exception directly. When thrown from
 * {@code let()} or an example method, the runner reports the example as pending after running {@code letGo()}
 * successfully.
 * </p>
 */
public class PendingExampleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String reason;

    /**
     * Creates a pending-example signal with the default javaspec reason.
     */
    public PendingExampleException() {
        this("");
    }

    /**
     * Creates a pending-example signal with an optional reason.
     *
     * @param reason human-readable pending reason; blank values use the default message
     */
    public PendingExampleException(String reason) {
        super(messageFor(reason));
        this.reason = safeReason(reason);
    }

    /**
     * Creates a pending-example signal with an optional reason and cause.
     *
     * @param reason human-readable pending reason; blank values use the default message
     * @param cause optional underlying cause
     */
    public PendingExampleException(String reason, Throwable cause) {
        super(messageFor(reason), cause);
        this.reason = safeReason(reason);
    }

    /**
     * Returns the explicit reason supplied by the spec, or an empty string when none was supplied.
     */
    public String reason() {
        return reason;
    }

    /**
     * JavaBeans-style alias for {@link #reason()}.
     */
    public String getReason() {
        return reason;
    }

    private static String messageFor(String reason) {
        String safeReason = safeReason(reason);
        if (safeReason.trim().length() == 0) {
            return "Pending by javaspec.";
        }
        return safeReason;
    }

    private static String safeReason(String reason) {
        if (reason == null) {
            return "";
        }
        return reason;
    }
}
