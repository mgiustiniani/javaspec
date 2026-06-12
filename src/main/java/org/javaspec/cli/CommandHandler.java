package org.javaspec.cli;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Handles a single javaspec CLI command (e.g. "run", "describe").
 * <p>Implementations encapsulate the full execution logic for one command,
 * reducing responsibility concentration in {@link Main}.</p>
 */
public interface CommandHandler {
    /**
     * Executes the command with the given parsed arguments.
     *
     * @param parsed the parsed CLI arguments
     * @param in     the input stream (may be ignored by some handlers)
     * @param out    the output stream
     * @param err    the error stream
     * @return an exit code (0 for success, non-zero for error)
     */
    int execute(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err);
}
