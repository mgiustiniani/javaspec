package org.javaspec.fixtures.extension;

import org.javaspec.extension.Extension;
import org.javaspec.extension.ExtensionContext;
import org.javaspec.formatter.RunFormatter;
import org.javaspec.runner.RunResult;

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
