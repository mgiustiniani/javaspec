package io.github.jvmspec.internal.language;

import java.util.Objects;

/** Immutable in-memory production-source synchronization plan. */
public final class SourceSynchronizationPlan {
    private final String originalSource;
    private final String proposedSource;
    private final boolean constructorChange;
    private final boolean methodChange;

    private SourceSynchronizationPlan(
            String originalSource,
            String proposedSource,
            boolean constructorChange,
            boolean methodChange
    ) {
        this.originalSource = Objects.requireNonNull(
                originalSource, "originalSource must not be null");
        this.proposedSource = Objects.requireNonNull(
                proposedSource, "proposedSource must not be null");
        this.constructorChange = constructorChange;
        this.methodChange = methodChange;
    }

    public static SourceSynchronizationPlan of(
            String originalSource,
            String proposedSource,
            boolean constructorChange,
            boolean methodChange
    ) {
        return new SourceSynchronizationPlan(
                originalSource, proposedSource, constructorChange, methodChange);
    }

    public String originalSource() {
        return originalSource;
    }

    public String proposedSource() {
        return proposedSource;
    }

    public boolean constructorChange() {
        return constructorChange;
    }

    public boolean methodChange() {
        return methodChange;
    }

    public int proposedChangeCount() {
        return (constructorChange ? 1 : 0) + (methodChange ? 1 : 0);
    }

    public boolean hasChanges() {
        return !originalSource.equals(proposedSource);
    }
}
