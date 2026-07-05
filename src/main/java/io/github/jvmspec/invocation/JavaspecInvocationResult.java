package io.github.jvmspec.invocation;

import io.github.jvmspec.compilation.SourceCompilationResult;
import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.formatter.RunFormatterRegistry;
import io.github.jvmspec.runner.RunResult;

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
    private final RunFormatterRegistry runFormatterRegistry;
    private final SourceCompilationResult sourceCompilationResult;

    private JavaspecInvocationResult(
            List<DiscoveredSpec> discoveredSpecs,
            RunResult runResult,
            int exitCode,
            RunFormatterRegistry runFormatterRegistry,
            SourceCompilationResult sourceCompilationResult
    ) {
        this.discoveredSpecs = immutableSpecs(discoveredSpecs);
        this.runResult = Objects.requireNonNull(runResult, "runResult must not be null");
        this.exitCode = exitCode;
        this.runFormatterRegistry = runFormatterRegistry;
        this.sourceCompilationResult = sourceCompilationResult;
    }

    public static JavaspecInvocationResult of(List<DiscoveredSpec> discoveredSpecs, RunResult runResult) {
        return new JavaspecInvocationResult(discoveredSpecs, runResult, JavaspecExitCode.from(runResult), null, null);
    }

    public static JavaspecInvocationResult of(List<DiscoveredSpec> discoveredSpecs, RunResult runResult, int exitCode) {
        return new JavaspecInvocationResult(discoveredSpecs, runResult, exitCode, null, null);
    }

    public static JavaspecInvocationResult of(
            List<DiscoveredSpec> discoveredSpecs,
            RunResult runResult,
            int exitCode,
            RunFormatterRegistry runFormatterRegistry
    ) {
        return new JavaspecInvocationResult(discoveredSpecs, runResult, exitCode, runFormatterRegistry, null);
    }

    public static JavaspecInvocationResult of(
            List<DiscoveredSpec> discoveredSpecs,
            RunResult runResult,
            int exitCode,
            RunFormatterRegistry runFormatterRegistry,
            SourceCompilationResult sourceCompilationResult
    ) {
        return new JavaspecInvocationResult(
                discoveredSpecs,
                runResult,
                exitCode,
                runFormatterRegistry,
                sourceCompilationResult
        );
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

    /**
     * Run formatter registry the launcher built and activated configured extensions against,
     * or {@code null} when the invocation declared no configured extensions. When present,
     * callers should resolve formatters from this same instance so configured extension
     * contributions are visible. The registry has reference semantics and is intentionally
     * excluded from {@link #equals(Object)} and {@link #hashCode()}.
     */
    public RunFormatterRegistry runFormatterRegistry() {
        return runFormatterRegistry;
    }

    public RunFormatterRegistry getRunFormatterRegistry() {
        return runFormatterRegistry;
    }

    public boolean hasRunFormatterRegistry() {
        return runFormatterRegistry != null;
    }

    /**
     * Successful source/spec compilation result for this invocation, or {@code null} when
     * compilation was disabled, skipped because no specs were discovered, or failed before a
     * result could be returned. Failed compilation raises
     * {@code io.github.jvmspec.compilation.SourceCompilationException} instead of returning a runner
     * result. This diagnostic metadata is intentionally excluded from {@link #equals(Object)} and
     * {@link #hashCode()} to preserve existing invocation result value semantics.
     */
    public SourceCompilationResult sourceCompilationResult() {
        return sourceCompilationResult;
    }

    public SourceCompilationResult getSourceCompilationResult() {
        return sourceCompilationResult;
    }

    public boolean hasSourceCompilationResult() {
        return sourceCompilationResult != null;
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
