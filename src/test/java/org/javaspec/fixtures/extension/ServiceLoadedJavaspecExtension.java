package org.javaspec.fixtures.extension;

import org.javaspec.extension.ExtensionContext;
import org.javaspec.extension.JavaspecExtension;
import org.javaspec.formatter.RunFormatter;
import org.javaspec.runner.RunResult;

import java.io.PrintStream;

public final class ServiceLoadedJavaspecExtension implements JavaspecExtension {
    public void configure(ExtensionContext context) {
        context.runFormatters().register("extension", new RunFormatter() {
            public void format(RunResult runResult, PrintStream out) {
                out.println("extension formatter total=" + runResult.totalExamples());
            }
        });
    }
}
