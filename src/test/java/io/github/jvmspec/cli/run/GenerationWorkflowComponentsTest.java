package io.github.jvmspec.cli.run;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.generation.SpecGenerationPlan;
import io.github.jvmspec.model.DescribedClass;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GenerationWorkflowComponentsTest {
    @Test
    public void authorizationRemainsCentralAndFailsClosedAtEndOfInput() throws Exception {
        DescribedType type = DescribedType.of("com.example.Subject");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BufferedReader noInput = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8));

        boolean denied = GenerationAuthorization.authorizeSourceSynchronization(
                false, noInput, new PrintStream(output), new File("Subject.java"),
                type, true, true);
        boolean generated = GenerationAuthorization.authorizeSourceSynchronization(
                true, noInput, new PrintStream(output), new File("Subject.java"),
                type, true, true);

        assertFalse(denied);
        assertTrue(generated);
        assertTrue(output.toString("UTF-8").contains("constructors and methods"));
    }

    @Test
    public void functionalTargetValidationStopsBeforeGeneration() {
        MethodDescriptor unresolved = MethodDescriptor.of(
                "transform", "String", Arrays.asList("Object"),
                Arrays.asList("__javaspecFunctionalArg0"))
                .withUnknownParameterTypes(Arrays.asList(Boolean.TRUE));
        DescribedType type = describedType(
                "com.example.Subject", JavaTypeKind.CLASS,
                Collections.<String>emptyList(), Arrays.asList(unresolved));
        DiscoveredSpec spec = DiscoveredSpec.of(
                new File("SubjectSpec.java"), "spec.com.example.SubjectSpec", type);
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        GenerationOrchestratorResult result =
                GenerationPreflightValidator.validateFunctionalTargets(
                        type, spec, new PrintStream(errors));

        assertFalse(result.shouldProceed());
        assertEquals(1, result.exitCode());
        assertEquals(1, result.pendingGenerationWork());
        assertTrue(errors.toString().contains("Cannot infer functional-interface target"));
    }

    @Test
    public void relatedSpecDryRunProposesSupportAndSpecWithoutWriting() throws Exception {
        File root = Files.createTempDirectory("javaspec-related-dry-run").toFile();
        File specRoot = new File(root, "spec");
        File sourceRoot = new File(root, "source");
        File generatedRoot = new File(root, "generated");
        assertTrue(specRoot.mkdirs());
        assertTrue(sourceRoot.mkdirs());
        assertTrue(generatedRoot.mkdirs());

        DescribedType owner = DescribedType.of(
                DescribedClass.of("com.example.Subject"),
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Arrays.asList("com.example.Port"),
                Collections.<String>emptyList(),
                Collections.emptyList(),
                Collections.<MethodDescriptor>emptyList(),
                Collections.<DescribedType.EnumConstantInfo>emptyList()
        );
        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        specs.add(DiscoveredSpec.of(
                new File(specRoot, "SubjectSpec.java"),
                "spec.com.example.SubjectSpec", owner));
        GenerationActivity activity = new GenerationActivity();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        RelatedSpecGenerationOrchestrator.Result result =
                RelatedSpecGenerationOrchestrator.ensure(
                        owner, specs, specRoot, sourceRoot,
                        new BufferedReader(new InputStreamReader(
                                new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8)),
                        new PrintStream(output), false, true,
                        SpecNamingConvention.defaults(), getClass().getClassLoader(),
                        generatedRoot, activity);

        assertTrue(result.allAccepted());
        assertTrue(result.hasPendingChanges());
        assertEquals(2, specs.size());
        assertEquals(0, activity.appliedWriteCount());
        assertEquals(2, activity.actions().size());
        assertFalse(new File(specRoot, "spec/com/example/PortSpec.java").exists());
    }

    @Test
    public void supportDryRunDetectsMissingAndUnchangedTargets() throws Exception {
        File root = Files.createTempDirectory("javaspec-support-dry-run").toFile();
        DescribedType type = DescribedType.of("com.example.Subject");
        SpecGenerationPlan plan = io.github.jvmspec.generation.SpecSkeletonGenerator.supportPlan(
                type, root, root, SpecNamingConvention.defaults());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(GenerationDryRunReporter.reportSupport(
                plan, new PrintStream(output), "support"));
        assertTrue(plan.targetFile().getParentFile().mkdirs());
        Files.write(plan.targetFile().toPath(), plan.sourceContent().getBytes(StandardCharsets.UTF_8));
        assertFalse(GenerationDryRunReporter.reportSupport(
                plan, new PrintStream(output), "support"));
    }

    private static DescribedType describedType(
            String name,
            JavaTypeKind kind,
            List<String> implementedTypes,
            List<MethodDescriptor> methods
    ) {
        return DescribedType.of(
                DescribedClass.of(name), kind,
                Collections.<String>emptyList(), implementedTypes,
                Collections.<String>emptyList(), Collections.emptyList(), methods,
                Collections.<DescribedType.EnumConstantInfo>emptyList()
        );
    }
}
