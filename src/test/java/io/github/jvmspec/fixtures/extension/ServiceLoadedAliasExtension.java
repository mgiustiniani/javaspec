package io.github.jvmspec.fixtures.extension;

import io.github.jvmspec.extension.Extension;
import io.github.jvmspec.extension.ExtensionContext;
import io.github.jvmspec.formatter.RunFormatter;
import io.github.jvmspec.runner.RunResult;

import java.io.PrintStream;

public final class ServiceLoadedAliasExtension implements Extension {
    public void configure(ExtensionContext context) {
        context.runFormatters().register("alias", new RunFormatter() {
            public void format(RunResult runResult, PrintStream out) {
                out.println("alias formatter total=" + runResult.totalExamples());
            }
        });
    }
}
