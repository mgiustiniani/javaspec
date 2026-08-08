package io.github.jvmspec.cli.run;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.generation.AtomicFileWriter;
import io.github.jvmspec.generation.ProphecyExistenceChecker;
import io.github.jvmspec.generation.ProphecyFileGenerator;
import io.github.jvmspec.generation.ProphecyGenerationPlan;
import io.github.jvmspec.generation.ProphecySkeletonGenerator;
import io.github.jvmspec.generation.SpecGenerationPlan;
import io.github.jvmspec.generation.SpecSkeletonGenerator;
import io.github.jvmspec.generation.SpecSupportFileGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** Generates prophecy wrappers and support helpers without touching authored specification sources. */
final class ProphecyGenerationOrchestrator {
    private ProphecyGenerationOrchestrator() {
    }

    static boolean ensureWrappers(
            List<DiscoveredSpec> specs,
            File specRoot,
            File sourceRoot,
            BufferedReader input,
            PrintStream out,
            PrintStream err,
            boolean generate,
            boolean dryRun,
            ClassLoader classLoader,
            File generatedSourcesRoot,
            SpecNamingConvention namingConvention,
            GenerationActivity activity
    ) throws IOException {
        // Collect spec files
        List<File> specFiles = new ArrayList<File>();
        for (int i = 0; i < specs.size(); i++) {
            File specFile = specs.get(i).specFile();
            if (specFile != null && specFile.exists()) {
                specFiles.add(specFile);
            }
        }

        if (specFiles.isEmpty()) {
            return true;
        }

        // Find prophesize/prophecy calls in spec files
        List<String> prophesizedTypes;
        try {
            prophesizedTypes = ProphecyExistenceChecker.findProphesizedInterfaces(specFiles);
        } catch (IOException ex) {
            err.println("I/O error while scanning for prophecy calls: " + GenerationErrorMessages.messageOf(ex));
            return false;
        }

        if (prophesizedTypes.isEmpty()) {
            return true;
        }

        boolean allGenerated = true;

        for (int i = 0; i < prophesizedTypes.size(); i++) {
            String typeFqcn = prophesizedTypes.get(i);

            // Check if wrapper already exists
            if (ProphecyExistenceChecker.wrapperExists(typeFqcn, classLoader)) {
                continue;
            }

            // Resolve the prophesized type
            Class<?> prophesizedType = ProphecyExistenceChecker.resolveProphesizedType(typeFqcn, classLoader);
            if (prophesizedType == null) {
                err.println("Warning: cannot resolve prophesized type " + typeFqcn
                        + " referenced by prophesize() call; skipping wrapper generation.");
                continue;
            }

            String packageName = ProphecyExistenceChecker.defaultPackage(typeFqcn);
            String wrapperSimpleName = prophesizedType.getSimpleName() + "Prophecy";
            String packagePath = packageName.replace('.', '/');
            File targetFile;
            if (packagePath.length() > 0) {
                targetFile = new File(generatedSourcesRoot, packagePath + "/" + wrapperSimpleName + ".java");
            } else {
                targetFile = new File(generatedSourcesRoot, wrapperSimpleName + ".java");
            }

            String sourceCode = ProphecySkeletonGenerator.render(prophesizedType, packageName);

            out.println("Missing prophecy wrapper " + wrapperSimpleName
                    + " for " + typeFqcn + ".");
            out.println("Target path: " + targetFile.getPath());

            if (dryRun) {
                activity.proposed("PROPHECY_WRAPPER", targetFile);
                out.println("Would generate prophecy wrapper: " + targetFile.getPath());
                continue;
            }

            boolean accepted = generate || askToGenerateProphecy(input, out, typeFqcn);
            if (!accepted) {
                activity.proposed("PROPHECY_WRAPPER", targetFile);
                allGenerated = false;
                continue;
            }

            ProphecyGenerationPlan plan = ProphecyGenerationPlan.of(
                    prophesizedType, packageName, targetFile, sourceCode
            );
            try {
                File writtenFile = ProphecyFileGenerator.write(plan);
                activity.applied("PROPHECY_WRAPPER", writtenFile);
                out.println("Generated prophecy wrapper: " + writtenFile.getPath());
            } catch (IOException ex) {
                err.println("I/O error while generating prophecy wrapper: " + GenerationErrorMessages.messageOf(ex));
                err.println("Target path: " + targetFile.getPath());
                return false;
            } catch (SecurityException ex) {
                err.println("I/O error while generating prophecy wrapper: " + GenerationErrorMessages.messageOf(ex));
                err.println("Target path: " + targetFile.getPath());
                return false;
            }
        }

        // --- Update support class helpers in generatedSourcesRoot, never in src ---
        updateSupportProphecyHelpers(
                specs, specRoot, generatedSourcesRoot, namingConvention, prophesizedTypes,
                dryRun, generate, out, err, activity);

        return allGenerated;
    }

    private static boolean askToGenerateProphecy(
            BufferedReader input,
            PrintStream out,
            String interfaceFqcn
    ) throws IOException {
        while (true) {
            out.println("Do you want me to create prophecy wrapper " + interfaceFqcn + "Prophecy? [Y/n]");
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

    private static void updateSupportProphecyHelpers(
            List<DiscoveredSpec> specs,
            File specRoot,
            File generatedSourcesRoot,
            SpecNamingConvention namingConvention,
            List<String> prophesizedTypeNames,
            boolean dryRun,
            boolean generate,
            PrintStream out,
            PrintStream err,
            GenerationActivity activity
    ) {
        if (prophesizedTypeNames.isEmpty()) {
            return;
        }
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            // Collect only the types prophesized by this spec's source file
            File specFile = spec.specFile();
            if (specFile == null || !specFile.exists()) {
                continue;
            }
            List<String> typesForThisSpec = new ArrayList<String>();
            try {
                List<String> found = ProphecyExistenceChecker.findProphesizedInterfaces(specFile);
                for (int j = 0; j < prophesizedTypeNames.size(); j++) {
                    if (found.contains(prophesizedTypeNames.get(j))) {
                        typesForThisSpec.add(prophesizedTypeNames.get(j));
                    }
                }
            } catch (IOException ex) {
                err.println("I/O error scanning spec for prophecy helpers: " + GenerationErrorMessages.messageOf(ex));
                continue;
            }
            if (typesForThisSpec.isEmpty()) {
                continue;
            }
            // The support file lives only in generatedSourcesRoot, never in src
            io.github.jvmspec.model.DescribedClass describedClass = spec.describedClass();
            SpecGenerationPlan supportPlan = SpecSkeletonGenerator.supportPlan(
                    spec.describedType(), specRoot, generatedSourcesRoot, namingConvention);
            File supportFile = supportPlan.targetFile();
            if (!supportFile.isFile()) {
                continue;
            }
            try {
                String source = new String(
                        java.nio.file.Files.readAllBytes(supportFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                String updated = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                        source, spec.describedType(), typesForThisSpec);
                if (!source.equals(updated)) {
                    if (dryRun || !generate) {
                        activity.proposed("PROPHECY_SUPPORT_HELPERS", supportFile);
                        out.println("Would add prophecy helpers to support: " + supportFile.getPath());
                    } else {
                        AtomicFileWriter.writeUtf8(supportFile, updated);
                        activity.applied("PROPHECY_SUPPORT_HELPERS", supportFile);
                        out.println("Updated prophecy helpers in support: " + supportFile.getPath());
                    }
                }
            } catch (IOException ex) {
                err.println("I/O error updating prophecy helpers in support: " + GenerationErrorMessages.messageOf(ex));
            }
        }
    }
}
