package io.github.jvmspec.cli.run;

import io.github.jvmspec.generation.AtomicFileWriter;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.generation.SpecGenerationPlan;
import io.github.jvmspec.generation.TypeGenerationPlan;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/** Central CLI authorization and prompting for all generation source writes. */
final class GenerationAuthorization {
    private GenerationAuthorization() {
    }

    static String promptTarget(DescribedType describedType) {
        if (JavaTypeKind.CLASS.equals(describedType.kind())) {
            return describedType.qualifiedName();
        }
        return describedType.kind().displayName() + " " + describedType.qualifiedName();
    }

    static String policyOptionName(ConstructorPolicy policy) {
        return policy.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }

    static boolean askToGenerateSpec(
            BufferedReader input,
            PrintStream out,
            SpecGenerationPlan plan
    ) throws IOException {
        while (true) {
            out.println("Do you want me to create specification " + plan.specQualifiedName()
                    + " for " + promptTarget(plan.describedType()) + "? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }

    static boolean askToGenerate(
            BufferedReader input,
            PrintStream out,
            TypeGenerationPlan plan
    ) throws IOException {
        while (true) {
            out.println("Do you want me to create " + promptTarget(plan.describedType()) + " for you? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }

    static boolean authorizeSourceSynchronization(
            boolean generate,
            BufferedReader input,
            PrintStream out,
            File sourceFile,
            DescribedType describedType,
            boolean constructorChange,
            boolean methodChange
    ) throws IOException {
        if (generate) return true;
        if (methodChange && !constructorChange) {
            return askToUpdateMethods(input, out, sourceFile, describedType);
        }
        String scope = methodChange ? "constructors and methods" : "constructors";
        while (true) {
            out.println("Do you want me to update " + scope + " for "
                    + promptTarget(describedType) + " in " + sourceFile.getPath() + "? [Y/n]");
            String answer = input.readLine();
            if (answer == null) return false;
            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized)
                    || "yes".equalsIgnoreCase(normalized)) return true;
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) return false;
            out.println("Please answer y or n.");
        }
    }

    static void applyAuthorizedSourceUpdate(
            File sourceFile,
            String proposedSource,
            GenerationActivity activity
    ) throws IOException {
        AtomicFileWriter.writeUtf8(sourceFile, proposedSource);
        activity.applied("SOURCE_SYNCHRONIZATION", sourceFile);
    }

    static boolean askToUpdateMethods(
            BufferedReader input,
            PrintStream out,
            File sourceFile,
            DescribedType describedType
    ) throws IOException {
        while (true) {
            out.println("Do you want me to add missing method skeletons to "
                    + promptTarget(describedType) + " in " + sourceFile.getPath() + "? [Y/n]");
            String answer = input.readLine();
            if (answer == null) {
                return false;
            }

            String normalized = answer.trim();
            if (normalized.length() == 0 || "y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
                return true;
            }
            if ("n".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
                return false;
            }
            out.println("Please answer y or n.");
        }
    }
}
