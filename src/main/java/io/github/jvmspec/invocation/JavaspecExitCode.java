package io.github.jvmspec.invocation;

import io.github.jvmspec.runner.RunResult;

import java.util.Objects;

/**
 * Deterministic exit-code derivation for dependency-free javaspec invocations.
 */
public final class JavaspecExitCode {
    public static final int SUCCESS = 0;
    public static final int FAILED_OR_BROKEN = 1;

    private JavaspecExitCode() {
    }

    public static int from(RunResult runResult) {
        Objects.requireNonNull(runResult, "runResult must not be null");
        if (runResult.hasFailures()) {
            return FAILED_OR_BROKEN;
        }
        return SUCCESS;
    }

    public static int exitCodeFor(RunResult runResult) {
        return from(runResult);
    }
}
