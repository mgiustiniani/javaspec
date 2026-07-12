package io.github.jvmspec.cli;

import io.github.jvmspec.cli.run.GenerationActivity;
import io.github.jvmspec.cli.run.GenerationOrchestratorResult;
import io.github.jvmspec.generation.StubMarkerScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mutable per-invocation state used only to render the final deterministic generation report. */
final class GenerationReportState {
    private boolean generationObserved;
    private boolean proceed = true;
    private int pendingGenerationWork;
    private List<GenerationActivity.Action> actions =
            Collections.<GenerationActivity.Action>emptyList();
    private List<StubMarkerScanner.StubLocation> pendingStubs =
            Collections.<StubMarkerScanner.StubLocation>emptyList();

    void generationCompleted(GenerationOrchestratorResult result, GenerationActivity activity) {
        generationObserved = true;
        proceed = result.shouldProceed();
        actions = activity.actions();
        int proposed = 0;
        for (int i = 0; i < actions.size(); i++) {
            if (GenerationActivity.Status.PROPOSED.equals(actions.get(i).status())) proposed++;
        }
        pendingGenerationWork = proposed > 0 ? proposed : result.pendingGenerationWork();
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

    int pendingGenerationWork() {
        return pendingGenerationWork;
    }

    List<GenerationActivity.Action> actions() {
        return actions;
    }

    int appliedWriteCount() {
        int count = 0;
        for (int i = 0; i < actions.size(); i++) {
            if (GenerationActivity.Status.APPLIED.equals(actions.get(i).status())) count++;
        }
        return count;
    }

    List<StubMarkerScanner.StubLocation> pendingStubs() {
        return pendingStubs;
    }
}
