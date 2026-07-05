package io.github.jvmspec.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable descriptor of a constructor's parameter types and body content.
 */
public final class ConstructorDescriptor {
    private final List<String> parameterTypes;
    private final List<String> parameterNames;
    private final String bodyContent;

    private ConstructorDescriptor(
            List<String> parameterTypes,
            List<String> parameterNames,
            String bodyContent
    ) {
        this.parameterTypes = Collections.unmodifiableList(
                Objects.requireNonNull(parameterTypes, "parameterTypes must not be null"));
        this.parameterNames = Collections.unmodifiableList(
                Objects.requireNonNull(parameterNames, "parameterNames must not be null"));
        this.bodyContent = Objects.requireNonNull(bodyContent, "bodyContent must not be null");
    }

    public static ConstructorDescriptor of(List<String> parameterTypes, List<String> parameterNames, String bodyContent) {
        List<String> types = new ArrayList<String>(Objects.requireNonNull(parameterTypes, "parameterTypes must not be null"));
        List<String> names = new ArrayList<String>(Objects.requireNonNull(parameterNames, "parameterNames must not be null"));
        if (types.size() != names.size()) {
            throw new IllegalArgumentException("parameterTypes size (" + types.size()
                    + ") must equal parameterNames size (" + names.size() + ")");
        }
        for (int i = 0; i < types.size(); i++) {
            Objects.requireNonNull(types.get(i), "parameterTypes[" + i + "] must not be null");
            Objects.requireNonNull(names.get(i), "parameterNames[" + i + "] must not be null");
        }
        return new ConstructorDescriptor(types, names, bodyContent);
    }

    public static ConstructorDescriptor empty() {
        return new ConstructorDescriptor(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                ""
        );
    }

    public static ConstructorDescriptor noArg(String bodyContent) {
        return new ConstructorDescriptor(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                bodyContent
        );
    }

    public List<String> parameterTypes() {
        return parameterTypes;
    }

    public List<String> parameterNames() {
        return parameterNames;
    }

    public String bodyContent() {
        return bodyContent;
    }

    public boolean hasBody() {
        return bodyContent.length() > 0;
    }

    public boolean hasParameters() {
        return !parameterTypes.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConstructorDescriptor)) {
            return false;
        }
        ConstructorDescriptor that = (ConstructorDescriptor) other;
        return parameterTypes.equals(that.parameterTypes)
                && parameterNames.equals(that.parameterNames)
                && bodyContent.equals(that.bodyContent);
    }

    @Override
    public int hashCode() {
        int result = parameterTypes.hashCode();
        result = 31 * result + parameterNames.hashCode();
        result = 31 * result + bodyContent.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ConstructorDescriptor{");
        sb.append("parameters=");
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameterTypes.get(i)).append(" ").append(parameterNames.get(i));
        }
        sb.append(", hasBody=").append(hasBody());
        sb.append("}");
        return sb.toString();
    }
}
