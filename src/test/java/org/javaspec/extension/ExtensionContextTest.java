package org.javaspec.extension;

import org.javaspec.formatter.RunFormatter;
import org.javaspec.formatter.RunFormatterRegistry;
import org.javaspec.runner.RunResult;
import org.junit.Test;

import java.io.PrintStream;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ExtensionContextTest {
    @Test
    public void extensionReceivesContextAndRegistersCustomFormatter() {
        final RunFormatterRegistry registry = RunFormatterRegistry.builtIn();
        final RunFormatter formatter = formatterPrinting("tap-output");
        final ExtensionContext[] received = new ExtensionContext[1];
        JavaspecExtension extension = new JavaspecExtension() {
            public void configure(ExtensionContext context) {
                received[0] = context;
                context.runFormatterRegistry().register("tap", formatter);
            }
        };

        ExtensionContext context = ExtensionContext.of(registry);
        extension.configure(context);

        assertSame(context, received[0]);
        assertSame(registry, context.runFormatterRegistry());
        assertSame(formatter, registry.lookup("tap"));
        assertTrue(registry.formatterNames().contains("tap"));
    }

    @Test
    public void defaultRegisterMethodDelegatesToConfigure() {
        final RunFormatterRegistry registry = RunFormatterRegistry.builtIn();
        final RunFormatter formatter = formatterPrinting("compact-output");
        JavaspecExtension extension = new JavaspecExtension() {
            public void configure(ExtensionContext context) {
                context.runFormatters().register("compact", formatter);
            }
        };

        extension.register(new ExtensionContext(registry));

        assertSame(formatter, registry.lookup("compact"));
    }

    @Test
    public void shortNameAliasInterfaceIsUsableForLifecycleRegistration() {
        final RunFormatterRegistry registry = RunFormatterRegistry.builtIn();
        final RunFormatter formatter = formatterPrinting("alias-output");
        Extension extension = new Extension() {
            public void configure(ExtensionContext context) {
                context.runFormatters().register("alias", formatter);
            }
        };

        extension.register(ExtensionContext.of(registry));

        assertSame(formatter, registry.lookup("alias"));
    }

    private static RunFormatter formatterPrinting(final String text) {
        return new RunFormatter() {
            public void format(RunResult runResult, PrintStream out) {
                out.println(text);
            }
        };
    }
}
