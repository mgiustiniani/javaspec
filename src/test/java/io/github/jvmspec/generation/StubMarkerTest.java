package io.github.jvmspec.generation;

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
import static org.junit.Assert.assertTrue;

/**
 * Generated production stubs carry a {@code // javaspec:stub} marker so the CLI can report
 * how many stubs are still pending hand implementation (the GREEN work of a slice).
 */
public class StubMarkerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static DescribedType typeWithMethod(MethodDescriptor method) {
        return DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(method)
        );
    }

    @Test
    public void methodUpdaterMarksInsertedStubBodies() {
        String existingSource =
                "package com.example;\n\npublic class Service {\n}\n";
        MethodDescriptor method = MethodDescriptor.of(
                "greeting", "String", Collections.<String>emptyList(), Collections.<String>emptyList());

        String updated = ClassMethodUpdater.updateSource(existingSource, typeWithMethod(method));

        assertTrue("inserted stub body must carry the javaspec:stub marker",
                updated.contains("// javaspec:stub"));
        assertTrue(updated.contains("return null;"));
    }

    @Test
    public void typeSkeletonGeneratorMarksSkeletonMethodBodies() {
        MethodDescriptor method = MethodDescriptor.of(
                "greeting", "String", Collections.<String>emptyList(), Collections.<String>emptyList());

        String rendered = TypeSkeletonGenerator.render(typeWithMethod(method));

        assertTrue("skeleton method body must carry the javaspec:stub marker",
                rendered.contains("// javaspec:stub"));
    }

    @Test
    public void scannerReportsPendingStubsWithFileAndLine() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("stub-scan-root");
        File packageDir = new File(sourceRoot, "com" + File.separator + "example");
        assertTrue(packageDir.mkdirs());
        File implemented = new File(packageDir, "Done.java");
        Files.write(implemented.toPath(), (
                "package com.example;\n" +
                "public class Done {\n" +
                "    public String value() {\n" +
                "        return \"real\";\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        File pending = new File(packageDir, "Pending.java");
        Files.write(pending.toPath(), (
                "package com.example;\n" +
                "public class Pending {\n" +
                "    public String value() {\n" +
                "        // javaspec:stub\n" +
                "        return null;\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        List<StubMarkerScanner.StubLocation> locations = StubMarkerScanner.scan(sourceRoot);

        assertEquals("only the pending stub must be reported", 1, locations.size());
        StubMarkerScanner.StubLocation location = locations.get(0);
        assertTrue("location must point at the pending file",
                location.file().getPath().endsWith("Pending.java"));
        assertEquals("marker is on line 4 of the pending file", 4, location.line());
    }

    @Test
    public void scannerReturnsEmptyListWhenNoMarkersExist() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("stub-clean-root");
        File packageDir = new File(sourceRoot, "com" + File.separator + "example");
        assertTrue(packageDir.mkdirs());
        File implemented = new File(packageDir, "Done.java");
        Files.write(implemented.toPath(),
                "package com.example;\npublic class Done { }\n".getBytes(StandardCharsets.UTF_8));

        assertTrue(StubMarkerScanner.scan(sourceRoot).isEmpty());
    }
}
