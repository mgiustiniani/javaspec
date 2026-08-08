package io.github.jvmspec.cli.run;

/**
 * Outcome of the generation/update orchestration loop over discovered specs.
 * <p>Encapsulates three possible outcomes:
 * <ul>
 *   <li>exit code (0 = proceed, 1 = missing-not-generated, 70 = I/O error)</li>
 *   <li>whether execution should proceed ({@code shouldProceed})</li>
 *   <li>the exact number of pending generation/update actions when execution is blocked</li>
 * </ul>
 */
public final class GenerationOrchestratorResult {
    private final int exitCode;
    private final boolean shouldProceed;
    private final int pendingGenerationWork;

    private GenerationOrchestratorResult(int exitCode, boolean shouldProceed, int pendingGenerationWork) {
        this.exitCode = exitCode;
        this.shouldProceed = shouldProceed;
        this.pendingGenerationWork = pendingGenerationWork;
    }

    /** Factory for a result that allows execution to proceed. */
    public static GenerationOrchestratorResult proceed() {
        return new GenerationOrchestratorResult(0, true, 0);
    }

    /** Factory for a result that blocks execution (missing-not-generated). */
    public static GenerationOrchestratorResult missingNotGenerated() {
        return missingNotGenerated(1);
    }

    /** Factory for a blocking result with an exact deterministic pending-work count. */
    public static GenerationOrchestratorResult missingNotGenerated(int pendingGenerationWork) {
        return new GenerationOrchestratorResult(1, false, Math.max(1, pendingGenerationWork));
    }

    /** Factory for an I/O error that blocks execution. */
    public static GenerationOrchestratorResult ioError(int exitCode) {
        return new GenerationOrchestratorResult(exitCode, false, 0);
    }

    public int exitCode() {
        return exitCode;
    }

    public boolean shouldProceed() {
        return shouldProceed;
    }

    public int pendingGenerationWork() {
        return pendingGenerationWork;
    }
}
