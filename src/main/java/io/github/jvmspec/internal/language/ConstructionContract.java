package io.github.jvmspec.internal.language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Portable evidence that the subject must support one construction signature. */
public final class ConstructionContract {
    private final List<BehaviorParameter> parameters;

    private ConstructionContract(List<BehaviorParameter> parameters) {
        this.parameters = Collections.unmodifiableList(
                new ArrayList<BehaviorParameter>(Objects.requireNonNull(
                        parameters, "parameters must not be null")));
    }

    public static ConstructionContract of(List<BehaviorParameter> parameters) {
        return new ConstructionContract(parameters);
    }

    public List<BehaviorParameter> parameters() {
        return parameters;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof ConstructionContract
                && parameters.equals(((ConstructionContract) other).parameters);
    }

    public int hashCode() {
        return parameters.hashCode();
    }
}
