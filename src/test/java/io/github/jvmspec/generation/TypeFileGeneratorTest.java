package io.github.jvmspec.generation;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TypeFileGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writeCreatesParentDirectoriesAndFileContent() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("source-root");
        TypeGenerationPlan plan = TypeSkeletonGenerator.plan(
                DescribedType.of("com.example.PaymentGateway", JavaTypeKind.INTERFACE),
                sourceRoot
        );

        File written = TypeFileGenerator.write(plan);

        assertEquals(plan.targetFile(), written);
        assertTrue(new File(sourceRoot, "com" + File.separator + "example").isDirectory());
        assertTrue(written.isFile());
        assertEquals(plan.sourceContent(), readFile(written));
    }

    @Test
    public void writeRefusesToOverwriteExistingFiles() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("overwrite-source-root");
        final TypeGenerationPlan plan = TypeSkeletonGenerator.plan(
                DescribedType.of("com.example.PaymentGateway", JavaTypeKind.INTERFACE),
                sourceRoot
        );
        assertTrue(plan.targetFile().getParentFile().mkdirs());
        Files.write(plan.targetFile().toPath(), "existing\n".getBytes(StandardCharsets.UTF_8));

        assertThrows(FileAlreadyExistsException.class, new ThrowingRunnable() {
            public void run() throws Throwable {
                TypeFileGenerator.write(plan);
            }
        });
        assertEquals("existing\n", readFile(plan.targetFile()));
    }

    @Test
    public void writeOrUpdateUpdatesRecordHeaderAndAddsAccessorStub() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("record-source-root");
        DescribedType type = DescribedType.of(
                "com.example.CertificateProfileId",
                JavaTypeKind.RECORD,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String"),
                        Arrays.asList("arg0"),
                        "")),
                Arrays.asList(MethodDescriptor.of("value", "String"))
        );
        TypeGenerationPlan plan = TypeSkeletonGenerator.plan(type, sourceRoot);
        assertTrue(plan.targetFile().getParentFile().mkdirs());
        Files.write(plan.targetFile().toPath(),
                "package com.example;\n\npublic record CertificateProfileId() { }\n".getBytes(StandardCharsets.UTF_8));

        TypeFileGenerator.writeOrUpdate(plan, ConstructorPolicy.COMMENT);

        String updated = readFile(plan.targetFile());
        assertTrue(updated.contains("public record CertificateProfileId(String value)"));
        assertTrue(updated.contains("public String value() {"));
        assertTrue(updated.contains("return null;"));
        assertFalse(updated.contains("public record CertificateProfileId()"));
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
