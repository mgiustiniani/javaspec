package org.javaspec.formatter;

import org.javaspec.runner.RunResult;

import java.io.PrintStream;

/**
 * Renders a runner result to an output stream.
 */
@FunctionalInterface
public interface RunFormatter {
    void format(RunResult runResult, PrintStream out);

    default String name() {
        return "";
    }
}
