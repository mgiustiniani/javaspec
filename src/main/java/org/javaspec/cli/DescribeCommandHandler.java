package org.javaspec.cli;

import org.javaspec.discovery.SpecNamingConvention;
import org.javaspec.generation.SpecFileGenerator;
import org.javaspec.generation.SpecGenerationPlan;
import org.javaspec.generation.SpecSkeletonGenerator;
import org.javaspec.generation.SpecSupportFileGenerator;
import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Handles the {@code describe} command.
 * <p>Creates PHPSpec-style specification skeletons for a given class.</p>
 */
final class DescribeCommandHandler implements CommandHandler {
    @Override
    public int execute(ParsedArguments parsed, InputStream in, PrintStream out, PrintStream err) {
        DescribedClass describedClass;
        try {
            describedClass = DescribedClass.of(parsed.className);
        } catch (IllegalArgumentException ex) {
            UsagePrinter.printUsageError(err, "Invalid class name: " + ex.getMessage());
            return Main.EXIT_USAGE;
        }

        SpecNamingConvention namingConvention = parsed.namingConvention;
        File specRoot = new File(parsed.specRoot);
        DescribedType describedType = DescribedType.of(describedClass);
        SpecGenerationPlan plan;
        SpecGenerationPlan supportPlan;
        try {
            plan = SpecSkeletonGenerator.plan(describedType, specRoot, namingConvention);
            supportPlan = SpecSkeletonGenerator.supportPlan(describedType, specRoot, namingConvention);
        } catch (IllegalArgumentException ex) {
            UsagePrinter.printUsageError(err, "Naming error: " + Main.messageOf(ex));
            return Main.EXIT_USAGE;
        }
        if (plan.targetFile().exists()) {
            out.println("Specification " + plan.specQualifiedName() + " exists; no generation needed.");
            out.println("Spec file: " + plan.targetFile().getPath());
            try {
                if (!supportPlan.targetFile().exists()) {
                    File generatedSupport = SpecSupportFileGenerator.writeOrUpdate(supportPlan);
                    out.println("Generated specification support: " + generatedSupport.getPath());
                }
            } catch (IOException ex) {
                err.println("I/O error while generating specification support: " + Main.messageOf(ex));
                err.println("Target path: " + supportPlan.targetFile().getPath());
                return Main.EXIT_IO_ERROR;
            } catch (SecurityException ex) {
                err.println("I/O error while generating specification support: " + Main.messageOf(ex));
                err.println("Target path: " + supportPlan.targetFile().getPath());
                return Main.EXIT_IO_ERROR;
            }
            out.println("No production class was generated.");
            return Main.EXIT_OK;
        }

        try {
            File generatedSupport = SpecSupportFileGenerator.writeOrUpdate(supportPlan);
            File generatedFile = SpecFileGenerator.write(plan);
            out.println("Generated specification support: " + generatedSupport.getPath());
            out.println("Generated specification: " + generatedFile.getPath());
            out.println("Specification class: " + plan.specQualifiedName());
            out.println("Described class: " + describedClass.qualifiedName());
            out.println("No production class was generated. Run `javaspec run` to continue the PHPSpec-style workflow.");
            return Main.EXIT_OK;
        } catch (IOException ex) {
            err.println("I/O error while generating specification: " + Main.messageOf(ex));
            err.println("Target path: " + plan.targetFile().getPath());
            return Main.EXIT_IO_ERROR;
        } catch (SecurityException ex) {
            err.println("I/O error while generating specification: " + Main.messageOf(ex));
            err.println("Target path: " + plan.targetFile().getPath());
            return Main.EXIT_IO_ERROR;
        }
    }
}
