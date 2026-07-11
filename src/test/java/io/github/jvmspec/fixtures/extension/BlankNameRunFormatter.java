package io.github.jvmspec.fixtures.extension;

import io.github.jvmspec.formatter.RunFormatter;
import io.github.jvmspec.runner.RunResult;

import java.io.PrintStream;

public final class BlankNameRunFormatter implements RunFormatter {
    public String name() {
        return "   ";
    }

    public void format(RunResult runResult, PrintStream out) {
        out.println("blank formatter should not be registered");
    }
}
