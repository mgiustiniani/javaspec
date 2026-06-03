package org.javaspec.invocation;

import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.runner.RunResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Structured result of a programmatic javaspec invocation.
 */
public final class JavaspecInvocationResult {
    private static final List<DiscoveredSpec> EMPTY_SPECS = Collections.unmodifiableList(new ArrayList<DiscoveredSpec>());

    private final List<DiscoveredSpec> discoveredSpecs;
    private final RunResult runResult;
    private final int exitCode;

    private JavaspecInvocationResult(List<DiscoveredSpec> discoveredSpecs, RunResult runResult, int exitCode) {
        this.discoveredSpecs = immutableSpecs(discoveredSpecs);
        this.runResult = Objects.requireNonNull(runResult, "runResult must not be null");
        this.exitCode = exitCode;
    }

    public static JavaspecInvocationResult of(List<DiscoveredSpec> discoveredSpecs, RunResult runResult) {
        return new JavaspecInvocationResult(discoveredSpecs, runResult, JavaspecExitCode.from(runResult));
    }

    public static JavaspecInvocationResult of(List<DiscoveredSpec> discoveredSpecs, RunResult runResult, int exitCode) {
        return new JavaspecInvocationResult(discoveredSpecs, runResult, exitCode);
    }

    public List<DiscoveredSpec> discoveredSpecs() {
        return discoveredSpecs;
    }

    public List<DiscoveredSpec> specs() {
        return discoveredSpecs;
    }

    public List<DiscoveredSpec> getDiscoveredSpecs() {
        return discoveredSpecs;
    }

    public RunResult runResult() {
        return runResult;
    }

    public RunResult result() {
        return runResult;
    }

    public RunResult getRunResult() {
        return runResult;
    }

    public int exitCode() {
        return exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean successful() {
        return exitCode == JavaspecExitCode.SUCCESS;
    }

    public boolean isSuccessful() {
        return successful();
    }

    public boolean hasFailures() {
        return runResult.hasFailures();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JavaspecInvocationResult)) {
            return false;
        }
        JavaspecInvocationResult that = (JavaspecInvocationResult) other;
        return exitCode == that.exitCode
                && discoveredSpecs.equals(that.discoveredSpecs)
                && runResult.equals(that.runResult);
    }

    @Override
    public int hashCode() {
        int result = discoveredSpecs.hashCode();
        result = 31 * result + runResult.hashCode();
        result = 31 * result + exitCode;
        return result;
    }

    @Override
    public String toString() {
        return "JavaspecInvocationResult{" +
                "discoveredSpecs=" + discoveredSpecs +
                ", runResult=" + runResult +
                ", exitCode=" + exitCode +
                '}';
    }

    private static List<DiscoveredSpec> immutableSpecs(List<DiscoveredSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return EMPTY_SPECS;
        }
        List<DiscoveredSpec> copy = new ArrayList<DiscoveredSpec>();
        for (int i = 0; i < specs.size(); i++) {
            copy.add(Objects.requireNonNull(specs.get(i), "discoveredSpecs[" + i + "] must not be null"));
        }
        return Collections.unmodifiableList(copy);
    }
}
