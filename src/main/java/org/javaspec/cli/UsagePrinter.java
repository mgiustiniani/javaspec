package org.javaspec.cli;

import java.io.File;
import java.io.PrintStream;

/**
 * Prints usage information and usage errors for javaspec CLI commands.
 */
final class UsagePrinter {

    static void printUsageError(PrintStream err, String message) {
        err.println("Error: " + message);
        printUsage(err);
    }

    static void printUsage(PrintStream stream) {
        stream.println("Usage:");
        stream.println("  javaspec describe <ClassName> [--config <file>] [--suite <name>] [--spec-dir <dir>]");
        stream.println("  javaspec desc <ClassName> [--config <file>] [--suite <name>] [--spec-root <dir>]");
        stream.println("  javaspec run [--config <file>] [--suite <name>] [--spec-dir <dir>] [--source-dir <dir>] [--classpath <path-list>] [--classpath-file <file>] [--compile] [--compile-output <dir>] [--generate] [--dry-run] [--stop-on-failure] [--formatter <progress|pretty>] [--profile <java8|java11|java17|java21|java25>] [--verbose] [--report <file>] [--junit-xml <file>] [--constructor-policy <delete|preserve|comment>] [--class <name>] [--example <name>]");
        stream.println();
        stream.println("Commands:");
        stream.println("  describe <ClassName>  Create a PHPSpec-style specification skeleton; never creates production code.");
        stream.println("  desc <ClassName>      Alias for describe.");
        stream.println("  run                   Discover specs and check whether their described production types exist.");
        stream.println("  prophesize <FQCN>     Generate a typed Prophecy wrapper for an interface.");
        stream.println();
        stream.println("Options:");
        stream.println("  --config <file>       Load javaspec configuration from file.");
        stream.println("  --suite <name>        Select a configured suite (default: configuration default suite).");
        stream.println("  --spec-dir <dir>      Spec root to inspect and write to (default: " + Main.DEFAULT_SPEC_ROOT + ").");
        stream.println("  --spec-root <dir>     Alias for --spec-dir.");
        stream.println("  --source-dir <dir>    Source root used by run (default: " + Main.DEFAULT_SOURCE_ROOT + ").");
        stream.println("  --source-root <dir>   Alias for --source-dir.");
        stream.println("  --classpath <paths>   With run, add explicit classpath entries separated by '" + File.pathSeparator + "'.");
        stream.println("  --classpath-file <file> With run, read UTF-8 classpath entries, one per non-comment line.");
        stream.println("  --compile             With run, compile source and spec trees before executable examples.");
        stream.println("  --compile-output <dir> With run, write compiled classes to <dir> (default: " + Main.DEFAULT_COMPILE_OUTPUT + ").");
        stream.println("  --generate            With run, answer yes to missing production type generation prompts.");
        stream.println("  --dry-run             With run, report pending generation/update work without writing files or prompting.");
        stream.println("  --stop-on-failure     With run, stop after the first failed or broken executable example.");
        stream.println("  --formatter <value>   With run, choose example output formatter. Valid values: progress, pretty.");
        stream.println("  --profile <value>     With run, override target profile. Valid values: java8, java11, java17, java21, java25.");
        stream.println("  --verbose             With run, print effective run configuration before discovery.");
        stream.println("  --report <file>       With run, write a UTF-8 JSON runner report.");
        stream.println("  --report-file <file>  Alias for --report.");
        stream.println("  --junit-xml <file>    With run, write a UTF-8 JUnit XML-compatible runner report.");
        stream.println("  --junit-xml-file <file> Alias for --junit-xml.");
        stream.println("  --constructor-policy  Constructor handling policy. Valid values: delete, preserve, comment (default: comment).");
        stream.println("  --class <name>        With run, filter specs by described class name (exact match, repeatable).");
        stream.println("  --example <name>      With run, filter examples by method name, display name, or order index (repeatable).");
        stream.println();
        stream.println("Prophesize options:");
        stream.println("  --output <dir>        Output directory for the generated wrapper (default: " + Main.DEFAULT_GENERATED_SOURCES + ").");
        stream.println("  --package <name>      Target package name for the generated wrapper.");
        stream.println("  --overwrite           Overwrite existing wrapper file.");
        stream.println("  --dry-run             Print generated source without writing files.");
        stream.println("  --help, -h            Show this help.");
    }
}
