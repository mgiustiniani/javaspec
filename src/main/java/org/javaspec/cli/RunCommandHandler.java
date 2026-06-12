package org.javaspec.cli;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Handles the {@code run} command.
 * <p>Delegates to the orchestrator pipeline in {@link Main}.</p>
 */
final class RunCommandHandler implements CommandHandler {
    @Override
    public int execute(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err) {
        return Main.runSpecifications(parsed, in, out, err);
    }
}
