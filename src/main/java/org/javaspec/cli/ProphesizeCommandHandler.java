package org.javaspec.cli;

import org.javaspec.generation.ProphecyFileGenerator;
import org.javaspec.generation.ProphecyGenerationPlan;
import org.javaspec.generation.ProphecySkeletonGenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Handles the {@code javaspec prophesize <fqcn>} command.
 * <p>
 * Generates a typed Prophecy wrapper class for an interface or concrete class type.
 * </p>
 */
final class ProphesizeCommandHandler implements CommandHandler {
    private static final int EXIT_OK = 0;
    private static final int EXIT_USAGE = 64;
    private static final int EXIT_IO_ERROR = 70;

    @Override
    public int execute(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err) {
        String className = parsed.className;
        if (className == null) {
            UsagePrinter.printUsageError(err, "Missing class name.");
            return EXIT_USAGE;
        }

        Class<?> prophesizedType;
        try {
            prophesizedType = Class.forName(className);
        } catch (ClassNotFoundException e) {
            UsagePrinter.printUsageError(err, "Class not found: " + className);
            return EXIT_USAGE;
        }

        if (prophesizedType.isPrimitive() || prophesizedType.isArray() || prophesizedType.isAnnotation()
                || prophesizedType.isEnum()) {
            UsagePrinter.printUsageError(err, "Prophecy wrappers require an interface or concrete class type, got: "
                    + prophesizedType.getName());
            return EXIT_USAGE;
        }

        String packageName = parsed.prophesizePackageName;
        if (packageName == null) {
            packageName = "";
        }

        String sourceCode = ProphecySkeletonGenerator.render(prophesizedType, packageName);

        String outputDir = parsed.prophesizeOutputDir;
        if (outputDir == null) {
            outputDir = Main.DEFAULT_GENERATED_SOURCES;
        }

        String wrapperSimpleName = prophesizedType.getSimpleName() + "Prophecy";
        String packagePath = packageName.replace('.', '/');
        File targetFile;
        if (packagePath.length() > 0) {
            targetFile = new File(outputDir, packagePath + "/" + wrapperSimpleName + ".java");
        } else {
            targetFile = new File(outputDir, wrapperSimpleName + ".java");
        }

        if (parsed.dryRun) {
            out.println("Would generate prophecy wrapper: " + targetFile.getPath());
            out.println("---");
            out.println(sourceCode);
            return EXIT_OK;
        }

        if (targetFile.exists() && !parsed.prophesizeOverwrite) {
            out.println("Prophecy wrapper already exists: " + targetFile.getPath());
            out.println("Use --overwrite to replace.");
            return EXIT_OK;
        }

        ProphecyGenerationPlan plan = ProphecyGenerationPlan.of(
                prophesizedType, packageName, targetFile, sourceCode
        );

        try {
            File writtenFile = ProphecyFileGenerator.write(plan);
            out.println("Generated prophecy wrapper: " + writtenFile.getPath());
            return EXIT_OK;
        } catch (IOException e) {
            err.println("I/O error while generating prophecy wrapper: "
                    + ConfigurationHelper.messageOf(e));
            err.println("Target path: " + targetFile.getPath());
            return EXIT_IO_ERROR;
        } catch (SecurityException e) {
            err.println("I/O error while generating prophecy wrapper: "
                    + ConfigurationHelper.messageOf(e));
            err.println("Target path: " + targetFile.getPath());
            return EXIT_IO_ERROR;
        }
    }
}
