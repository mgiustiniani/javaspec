package org.javaspec.cli;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Handles the {@code describe} command.
 * <p>Delegates to {@link Main#describeClass}.</p>
 */
final class DescribeCommandHandler implements CommandHandler {
    @Override
    public int execute(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err) {
        return Main.describeClass(parsed, out, err);
    }
}
