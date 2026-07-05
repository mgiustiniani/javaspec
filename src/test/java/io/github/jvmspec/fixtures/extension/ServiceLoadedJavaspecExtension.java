package io.github.jvmspec.fixtures.extension;

import io.github.jvmspec.extension.ExtensionContext;
import io.github.jvmspec.extension.JavaspecExtension;
import io.github.jvmspec.formatter.RunFormatter;
import io.github.jvmspec.runner.RunResult;

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
