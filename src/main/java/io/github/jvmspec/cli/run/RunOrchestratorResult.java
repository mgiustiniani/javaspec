package io.github.jvmspec.cli.run;

import io.github.jvmspec.runner.RunResult;

/**
 * Encapsulates the outcome of a spec run orchestration: an exit code and an
 * optional {@link RunResult} produced only when execution completes.
 * <p>Replaces the pattern of returning raw exit-code constants ({@code EXIT_OK},
 * {@code EXIT_USAGE}, etc.) from the monolithic {@code runSpecifications}
 * method.</p>
 */
public final class RunOrchestratorResult {
    private final int exitCode;
    private final RunResult runResult;

    private RunOrchestratorResult(int exitCode, RunResult runResult) {
        this.exitCode = exitCode;
        this.runResult = runResult;
    }

    /** Factory for a result that did not reach spec execution (error, usage). */
    public static RunOrchestratorResult nonExecution(int exitCode) {
        return new RunOrchestratorResult(exitCode, null);
    }

    /** Factory for a successful execution with a {@link RunResult}. */
    public static RunOrchestratorResult success(RunResult runResult) {
        return new RunOrchestratorResult(0, runResult);
    }

    /** Exit code suitable for {@code System.exit()} or CLI error propagation. */
    public int exitCode() {
        return exitCode;
    }

    /** The run result, or {@code null} if execution did not complete. */
    public RunResult runResult() {
        return runResult;
    }
}
