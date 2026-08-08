package io.github.jvmspec.cli;

import io.github.jvmspec.cli.run.GenerationActivity;
import io.github.jvmspec.generation.AtomicFileWriter;
import io.github.jvmspec.generation.StubMarkerScanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Writes the deterministic machine-readable generation outcome report. */
final class GenerationReportWriter {
    private GenerationReportWriter() {
    }

    static int write(
            String reportPath,
            GenerationReportState state,
            ParsedArguments parsed,
            int exitCode,
            java.io.PrintStream err
    ) {
        if (reportPath == null) {
            return exitCode;
        }
        try {
            AtomicFileWriter.writeUtf8(new File(reportPath), render(state, parsed, exitCode));
            return exitCode;
        } catch (IOException ex) {
            err.println("I/O error while writing generation report: " + ex.getMessage());
            err.println("Generation report path: " + reportPath);
            return Main.EXIT_IO_ERROR;
        } catch (SecurityException ex) {
            err.println("I/O error while writing generation report: " + ex.getMessage());
            err.println("Generation report path: " + reportPath);
            return Main.EXIT_IO_ERROR;
        }
    }

    static String render(GenerationReportState state, ParsedArguments parsed, int exitCode) {
        List<GenerationActivity.Action> actions =
                new ArrayList<GenerationActivity.Action>(state.actions());
        Collections.sort(actions, new Comparator<GenerationActivity.Action>() {
            @Override
            public int compare(GenerationActivity.Action left, GenerationActivity.Action right) {
                int path = actionPath(left, parsed).compareTo(actionPath(right, parsed));
                if (path != 0) return path;
                int kind = left.kind().compareTo(right.kind());
                return kind != 0 ? kind : left.status().name().compareTo(right.status().name());
            }
        });
        List<StubMarkerScanner.StubLocation> stubs =
                new ArrayList<StubMarkerScanner.StubLocation>(state.pendingStubs());
        Collections.sort(stubs, new Comparator<StubMarkerScanner.StubLocation>() {
            @Override
            public int compare(StubMarkerScanner.StubLocation left, StubMarkerScanner.StubLocation right) {
                int path = relativePath(left.file(), new File(parsed.sourceRoot))
                        .compareTo(relativePath(right.file(), new File(parsed.sourceRoot)));
                return path != 0 ? path : Integer.compare(left.line(), right.line());
            }
        });
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"schemaVersion\": 1,\n");
        json.append("  \"outcome\": \"").append(outcome(state, parsed, exitCode)).append("\",\n");
        json.append("  \"exitCode\": ").append(exitCode).append(",\n");
        json.append("  \"proceed\": ").append(state.proceed()).append(",\n");
        json.append("  \"actions\": [");
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) json.append(',');
            GenerationActivity.Action action = actions.get(i);
            json.append("\n    {\"kind\": \"").append(escape(action.kind()))
                    .append("\", \"path\": \"").append(escape(actionPath(action, parsed)))
                    .append("\", \"status\": \"").append(action.status().name()).append("\"}");
        }
        if (!actions.isEmpty()) json.append("\n  ");
        json.append("],\n");
        json.append("  \"appliedWrites\": ").append(state.appliedWriteCount()).append(",\n");
        json.append("  \"pendingGenerationWork\": ")
                .append(state.pendingGenerationWork()).append(",\n");
        json.append("  \"pendingStubs\": [");
        for (int i = 0; i < stubs.size(); i++) {
            if (i > 0) json.append(',');
            json.append("\n    {\"path\": \"").append(escape(relativePath(
                    stubs.get(i).file(), new File(parsed.sourceRoot))))
                    .append("\", \"line\": ").append(stubs.get(i).line()).append('}');
        }
        if (!stubs.isEmpty()) json.append("\n  ");
        json.append("]\n");
        json.append("}\n");
        return json.toString();
    }

    private static String outcome(GenerationReportState state, ParsedArguments parsed, int exitCode) {
        if (state.generationObserved() && !state.proceed()) {
            return parsed.dryRun ? "PLANNED" : "STOPPED";
        }
        if (exitCode != Main.EXIT_OK) {
            return "FAILED";
        }
        if (parsed.dryRun) {
            return "NO_CHANGES";
        }
        return state.appliedWriteCount() > 0 ? "APPLIED" : "NO_CHANGES";
    }

    private static String actionPath(GenerationActivity.Action action, ParsedArguments parsed) {
        File file = new File(action.path());
        String sourcePath = relativePathIfWithin(file, new File(parsed.sourceRoot));
        if (sourcePath != null) return sourcePath;
        String specPath = relativePathIfWithin(file, new File(parsed.specRoot));
        if (specPath != null) return specPath;
        String generatedPath = relativePathIfWithin(file, new File(Main.DEFAULT_GENERATED_SOURCES));
        return generatedPath == null ? file.getName() : generatedPath;
    }

    private static String relativePath(File file, File sourceRoot) {
        String relative = relativePathIfWithin(file, sourceRoot);
        return relative == null ? file.getName() : relative;
    }

    private static String relativePathIfWithin(File file, File rootFile) {
        Path root = rootFile.toPath().toAbsolutePath().normalize();
        Path target = file.toPath().toAbsolutePath().normalize();
        if (!target.startsWith(root)) return null;
        return root.relativize(target).toString().replace(File.separatorChar, '/');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
