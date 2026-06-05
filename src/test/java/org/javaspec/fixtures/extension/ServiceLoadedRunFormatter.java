package org.javaspec.fixtures.extension;

import org.javaspec.formatter.RunFormatter;
import org.javaspec.runner.RunResult;

import java.io.PrintStream;

public final class ServiceLoadedRunFormatter implements RunFormatter {
    public String name() {
        return "external";
    }

    public void format(RunResult runResult, PrintStream out) {
        out.println("external formatter total=" + runResult.totalExamples()
                + " passed=" + runResult.passedCount()
                + " failed=" + runResult.failedCount());
    }
}
