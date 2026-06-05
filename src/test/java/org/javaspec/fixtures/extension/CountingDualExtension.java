package org.javaspec.fixtures.extension;

import org.javaspec.extension.Extension;
import org.javaspec.extension.ExtensionContext;
import org.javaspec.formatter.RunFormatter;
import org.javaspec.runner.RunResult;

import java.io.PrintStream;

public final class CountingDualExtension implements Extension {
    private static int configureCount;

    public static void reset() {
        configureCount = 0;
    }

    public static int configureCount() {
        return configureCount;
    }

    public void configure(ExtensionContext context) {
        configureCount++;
        context.runFormatters().register("dual", new RunFormatter() {
            public void format(RunResult runResult, PrintStream out) {
                out.println("dual extension configured " + configureCount + " time(s)");
            }
        });
    }
}
