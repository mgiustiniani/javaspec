package org.javaspec.fixtures.extension;

import org.javaspec.formatter.RunFormatter;
import org.javaspec.runner.RunResult;

import java.io.PrintStream;

public final class BlankNameRunFormatter implements RunFormatter {
    public String name() {
        return "   ";
    }

    public void format(RunResult runResult, PrintStream out) {
        out.println("blank formatter should not be registered");
    }
}
