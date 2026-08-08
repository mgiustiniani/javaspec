package io.github.jvmspec.cli.run;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.generation.ClassConstructorUpdater;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.generation.TypeSkeletonGenerator;
import io.github.jvmspec.generation.parser.JavaSourceParserLoader;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Fail-closed validation performed before generation or production-source synchronization. */
final class GenerationPreflightValidator {
    private static final String UNRESOLVED_FUNCTIONAL_PARAMETER_PREFIX = "__javaspecFunctionalArg";
    private static final int EXIT_IO_ERROR = 70;

    private GenerationPreflightValidator() {
    }

    static GenerationOrchestratorResult validateRecordComponentNames(
            DescribedType describedType,
            File sourceRoot,
            DiscoveredSpec spec,
            PrintStream err
    ) {
        if (!JavaTypeKind.RECORD.equals(describedType.kind()) || !describedType.hasConstructors()) {
            return null;
        }
        try {
            File sourceFile = new File(sourceRoot,
                    describedType.qualifiedName().replace('.', File.separatorChar) + ".java");
            if (sourceFile.isFile()) {
                String source = new String(
                        Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
                ClassConstructorUpdater.updateSource(
                        source, describedType, ConstructorPolicy.PRESERVE);
            } else {
                TypeSkeletonGenerator.plan(describedType, sourceRoot);
            }
            return null;
        } catch (IllegalArgumentException ex) {
            err.println(GenerationErrorMessages.messageOf(ex));
            err.println("Spec file: " + spec.specFile().getPath());
            err.println("Production source was not written. Add one unambiguous zero-argument accessor "
                    + "expectation for each constructor value or use a reliably named local/formal parameter.");
            return GenerationOrchestratorResult.missingNotGenerated();
        } catch (IOException ex) {
            err.println("I/O error while validating record component names: "
                    + GenerationErrorMessages.messageOf(ex));
            err.println("Spec file: " + spec.specFile().getPath());
            return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
        }
    }

    static GenerationOrchestratorResult validateFunctionalTargets(
            DescribedType describedType,
            DiscoveredSpec spec,
            PrintStream err
    ) {
        for (int methodIndex = 0; methodIndex < describedType.methods().size(); methodIndex++) {
            MethodDescriptor method = describedType.methods().get(methodIndex);
            for (int parameterIndex = 0;
                 parameterIndex < method.parameterNames().size();
                 parameterIndex++) {
                String parameterName = method.parameterNames().get(parameterIndex);
                if (!method.isParameterTypeUnknown(parameterIndex)
                        || !parameterName.startsWith(UNRESOLVED_FUNCTIONAL_PARAMETER_PREFIX)) {
                    continue;
                }
                err.println("Cannot infer functional-interface target for "
                        + describedType.qualifiedName() + "#" + method.methodName()
                        + " parameter " + (parameterIndex + 1) + ".");
                err.println("Spec file: " + spec.specFile().getPath());
                err.println("Assign the lambda or method reference to an explicitly typed variable, "
                        + "add an explicit functional-interface cast, or provide one unambiguous "
                        + "production signature.");
                return GenerationOrchestratorResult.missingNotGenerated();
            }
        }
        return null;
    }

    static GenerationOrchestratorResult validateExistingSourceShape(
            DescribedType describedType,
            File sourceRoot,
            PrintStream err
    ) {
        if (!describedType.hasMethods() && !describedType.hasConstructors()) return null;
        File sourceFile = new File(
                sourceRoot,
                describedType.qualifiedName().replace('.', File.separatorChar) + ".java"
        );
        if (!sourceFile.isFile()) return null;
        try {
            String source = new String(
                    Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);
            int closingBrace = JavaSourceParserLoader.defaultParser()
                    .parse(source)
                    .typeClosingBraceOffset(describedType.simpleName());
            if (closingBrace >= 0) return null;
            err.println("Unsupported source update: no named class-like declaration for "
                    + describedType.qualifiedName() + " was found in " + sourceFile.getPath() + ".");
            err.println("Compact source files and implicit classes are not supported as javaspec "
                    + "subjects; use a named class, record, interface, enum, or annotation.");
            return GenerationOrchestratorResult.missingNotGenerated();
        } catch (IOException ex) {
            err.println("I/O error while validating source shape: "
                    + GenerationErrorMessages.messageOf(ex));
            err.println("Target path: " + sourceFile.getPath());
            return GenerationOrchestratorResult.ioError(EXIT_IO_ERROR);
        }
    }
}
