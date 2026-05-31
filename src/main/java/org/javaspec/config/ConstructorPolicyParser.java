package org.javaspec.config;

import org.javaspec.generation.ConstructorPolicy;

import java.util.Objects;

/**
 * Parser for the configuration-facing constructor policy values.
 */
public final class ConstructorPolicyParser {
    public static final String DELETE = "delete";
    public static final String PRESERVE = "preserve";
    public static final String COMMENT = "comment";
    public static final String VALID_VALUES = DELETE + ", " + PRESERVE + ", " + COMMENT;

    private ConstructorPolicyParser() {
    }

    public static ConstructorPolicy parse(String value) {
        Objects.requireNonNull(value, "value must not be null");
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new ConfigurationException("Constructor policy must not be blank. Valid values: " + VALID_VALUES + ".");
        }
        if (DELETE.equals(trimmed)) {
            return ConstructorPolicy.DELETE;
        }
        if (PRESERVE.equals(trimmed)) {
            return ConstructorPolicy.PRESERVE;
        }
        if (COMMENT.equals(trimmed)) {
            return ConstructorPolicy.COMMENT;
        }
        throw new ConfigurationException("Invalid constructor policy: " + trimmed + ". Valid values: " + VALID_VALUES + ".");
    }

    public static String key(ConstructorPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        if (policy == ConstructorPolicy.DELETE) {
            return DELETE;
        }
        if (policy == ConstructorPolicy.PRESERVE) {
            return PRESERVE;
        }
        if (policy == ConstructorPolicy.COMMENT) {
            return COMMENT;
        }
        throw new ConfigurationException("Unsupported constructor policy: " + policy + ". Valid values: " + VALID_VALUES + ".");
    }
}
