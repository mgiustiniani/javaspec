package io.github.jvmspec.cli;

import io.github.jvmspec.cli.run.GenerationOrchestratorResult;
import io.github.jvmspec.generation.StubMarkerScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mutable per-invocation state used only to render the final deterministic generation report. */
final class GenerationReportState {
    private boolean generationObserved;
    private boolean proceed = true;
    private int generationExitCode;
    private List<StubMarkerScanner.StubLocation> pendingStubs =
            Collections.<StubMarkerScanner.StubLocation>emptyList();

    void generationCompleted(GenerationOrchestratorResult result) {
        generationObserved = true;
        proceed = result.shouldProceed();
        generationExitCode = result.exitCode();
    }

    void pendingStubs(List<StubMarkerScanner.StubLocation> stubs) {
        pendingStubs = Collections.unmodifiableList(
                new ArrayList<StubMarkerScanner.StubLocation>(stubs));
    }

    boolean generationObserved() {
        return generationObserved;
    }

    boolean proceed() {
        return proceed;
    }

    int generationExitCode() {
        return generationExitCode;
    }

    List<StubMarkerScanner.StubLocation> pendingStubs() {
        return pendingStubs;
    }
}
