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
        assertEquals("com.example.Subject", contract.subjectQualifiedName());
        assertEquals(BehaviorTypeShape.REFERENCE_CLASS, contract.subjectShape());
        assertTrue(contract.isPortable());
        assertFalse(LanguageRuntime.javaProductionBackend().language().id().isEmpty());
    }

    @Test
    public void behaviorContractCarriesStructuredPortableConstructionAndCallables() {
        DescribedType type = DescribedType.of(
                "com.example.Subject",
                JavaTypeKind.RECORD,
                Arrays.asList("com.example.Base"),
                Arrays.asList("java.lang.Comparable"),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("java.util.List<java.lang.String>[]"),
                        Arrays.asList("values"), "")),
                Arrays.asList(MethodDescriptor.staticMethod(
                        "create", "com.example.Subject",
                        Arrays.asList("java.util.List<? extends java.lang.Number>"),
                        Arrays.asList("numbers")))
        );

        BehaviorContract contract = BehaviorContract.from(type);

        assertEquals(BehaviorTypeShape.PRODUCT_TYPE, contract.subjectShape());
        assertEquals("com.example.Base", contract.extendedTypes().get(0).jvmName());
        BehaviorTypeRef constructorType = contract.constructions().get(0).parameters().get(0).type();
        assertEquals(BehaviorTypeRef.Kind.ARRAY, constructorType.kind());
        assertEquals("java.util.List", constructorType.component().jvmName());
        assertEquals("java.lang.String",
                constructorType.component().arguments().get(0).jvmName());
        CallableContract callable = contract.callables().get(0);
        assertEquals(CallableContract.InvocationKind.TYPE, callable.invocationKind());
        assertEquals(BehaviorTypeRef.Variance.EXTENDS,
                callable.parameters().get(0).type().arguments().get(0).variance());
        assertTrue(contract.isPortable());
    }

    @Test
    public void portableEquivalenceIgnoresJavaSpellingAndConstructorBodies() {
        DescribedType first = DescribedType.of(
                "com.example.Subject",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String"), Arrays.asList("name"), "first body"))
        );
        DescribedType second = DescribedType.of(
                "com.example.Subject",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("java.lang.String"), Arrays.asList("name"), "second body"))
        );

        assertTrue(BehaviorContract.from(first).portableEquivalent(BehaviorContract.from(second)));
    }

    @Test
    public void behaviorTypeRetainsOpaqueEvidenceWhenJavaSyntaxCannotBeResolved() {
        BehaviorTypeRef type = BehaviorTypeRef.fromJava("not a valid type");

        assertEquals(BehaviorTypeRef.Kind.UNKNOWN, type.kind());
        assertEquals("not a valid type", type.sourceEvidence());
        assertFalse(type.isPortable());
    }
}
