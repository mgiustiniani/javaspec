package org.javaspec.cli.run;

/**
 * Outcome of the generation/update orchestration loop over discovered specs.
 * <p>Encapsulates three possible outcomes:
 * <ul>
 *   <li>exit code (0 = proceed, 1 = missing-not-generated, 70 = I/O error)</li>
 *   <li>whether execution should proceed ({@code shouldProceed})</li>
 *   <li>the final spec list (may have been extended with related specs)</li>
 * </ul>
 */
public final class GenerationOrchestratorResult {
    private final int exitCode;
    private final boolean shouldProceed;

    private GenerationOrchestratorResult(int exitCode, boolean shouldProceed) {
        this.exitCode = exitCode;
        this.shouldProceed = shouldProceed;
    }

    /** Factory for a result that allows execution to proceed. */
    public static GenerationOrchestratorResult proceed() {
        return new GenerationOrchestratorResult(0, true);
    }

    /** Factory for a result that blocks execution (missing-not-generated). */
    public static GenerationOrchestratorResult missingNotGenerated() {
        return new GenerationOrchestratorResult(1, false);
    }

    /** Factory for an I/O error that blocks execution. */
    public static GenerationOrchestratorResult ioError(int exitCode) {
        return new GenerationOrchestratorResult(exitCode, false);
    }

    public int exitCode() {
        return exitCode;
    }

    public boolean shouldProceed() {
        return shouldProceed;
    }
}
