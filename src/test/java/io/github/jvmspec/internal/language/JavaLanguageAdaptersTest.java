package io.github.jvmspec.internal.language;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecDiscovery;
import io.github.jvmspec.discovery.SpecDiscoveryRequest;
import io.github.jvmspec.generation.ClassConstructorUpdater;
import io.github.jvmspec.generation.ClassMethodUpdater;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JavaLanguageAdaptersTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void javaSpecFrontendPreservesCanonicalDiscovery() throws Exception {
        File specRoot = temporaryFolder.newFolder("spec-root");
        File packageDirectory = new File(specRoot, "spec/com/example");
        assertTrue(packageDirectory.mkdirs());
        Files.write(new File(packageDirectory, "SubjectSpec.java").toPath(), (
                "package spec.com.example;\n" +
                "import io.github.jvmspec.api.ObjectBehavior;\n" +
                "public class SubjectSpec extends ObjectBehavior<Subject> {\n" +
                "  public void it_has_a_name() { beConstructedWith(\"name\"); }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot);

        List<DiscoveredSpec> direct = SpecDiscovery.discover(request);
        List<DiscoveredSpec> adapted = LanguageRuntime.javaSpecFrontend().discover(request);

        assertEquals(SourceLanguage.JAVA, LanguageRuntime.javaSpecFrontend().language());
        assertEquals(direct, adapted);
    }

    @Test
    public void javaProductionBackendPreservesConstructorThenMethodPlanning() {
        String source = "package com.example;\n\npublic class Subject { }\n";
        DescribedType type = DescribedType.of(
                "com.example.Subject",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String"), Arrays.asList("name"), "")),
                Arrays.asList(MethodDescriptor.of("name", "String"))
        );
        String expected = ClassConstructorUpdater.updateSource(
                source, type, ConstructorPolicy.PRESERVE);
        expected = ClassMethodUpdater.updateSource(expected, type);

        SourceSynchronizationPlan plan = LanguageRuntime.javaProductionBackend()
                .planSynchronization(
                        source, BehaviorContract.from(type), ConstructorPolicy.PRESERVE);

        assertEquals(SourceLanguage.JAVA, LanguageRuntime.javaProductionBackend().language());
        assertEquals(expected, plan.proposedSource());
        assertTrue(plan.constructorChange());
        assertTrue(plan.methodChange());
        assertEquals(2, plan.proposedChangeCount());
    }

    @Test
    public void behaviorContractRetainsTheFrozenPublicDescriptor() {
        DescribedType type = DescribedType.classNamed("com.example.Subject");

        BehaviorContract contract = BehaviorContract.from(type);

        assertSame(type, contract.describedType());
        assertFalse(LanguageRuntime.javaProductionBackend().language().id().isEmpty());
    }
}
