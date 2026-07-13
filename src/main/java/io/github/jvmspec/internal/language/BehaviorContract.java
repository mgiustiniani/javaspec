package io.github.jvmspec.internal.language;

import io.github.jvmspec.model.DescribedType;

import java.util.Objects;

/**
 * Internal language-neutral handoff between specification discovery and production planning.
 * The Java 1.0 adapter delegates to the existing public descriptor while the portable model is
 * evolved without changing the frozen public API.
 */
public final class BehaviorContract {
    private final DescribedType describedType;

    private BehaviorContract(DescribedType describedType) {
        this.describedType = Objects.requireNonNull(
                describedType, "describedType must not be null");
    }

    public static BehaviorContract from(DescribedType describedType) {
        return new BehaviorContract(describedType);
    }

    public DescribedType describedType() {
        return describedType;
    }
}
