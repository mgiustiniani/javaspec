package io.github.jvmspec.internal.language;

import java.util.Objects;

/** One language-neutral callable/construction parameter. */
public final class BehaviorParameter {
    private final String name;
    private final BehaviorTypeRef type;
    private final boolean inferredTypeUnknown;

    private BehaviorParameter(String name, BehaviorTypeRef type, boolean inferredTypeUnknown) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.inferredTypeUnknown = inferredTypeUnknown;
    }

    public static BehaviorParameter of(
            String name,
            BehaviorTypeRef type,
            boolean inferredTypeUnknown
    ) {
        return new BehaviorParameter(name, type, inferredTypeUnknown);
    }

    public String name() {
        return name;
    }

    public BehaviorTypeRef type() {
        return type;
    }

    public boolean inferredTypeUnknown() {
        return inferredTypeUnknown;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BehaviorParameter)) return false;
        BehaviorParameter that = (BehaviorParameter) other;
        return inferredTypeUnknown == that.inferredTypeUnknown
                && name.equals(that.name)
                && type.equals(that.type);
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (inferredTypeUnknown ? 1 : 0);
        return result;
    }
}
