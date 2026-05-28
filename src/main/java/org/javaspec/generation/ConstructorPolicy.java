package org.javaspec.generation;

/**
 * Policy for handling existing constructors in a production class
 * when generating from spec markers. Applies only to constructors
 * that are NOT present in the spec.
 * <p>
 * Empty constructors not in spec are always deleted regardless of policy.
 * </p>
 */
public enum ConstructorPolicy {
    /**
     * Preserve existing constructors that are not present in the spec.
     * Non-empty constructors are kept as-is; empty ones are still deleted.
     */
    PRESERVE,
    /**
     * Delete existing constructors that are not present in the spec.
     * Both empty and non-empty constructors are removed entirely.
     */
    DELETE,
    /**
     * Comment out existing constructors that are not present in the spec.
     * Non-empty constructors are preserved as comments; empty ones are deleted.
     */
    COMMENT;

    public static ConstructorPolicy defaultPolicy() {
        return COMMENT;
    }
}
