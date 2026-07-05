package io.github.jvmspec.generation;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecDiscovery;
import io.github.jvmspec.discovery.SpecNamingConvention;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpecSupportFileGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private SpecGenerationPlan discoverSupportPlan(File specRoot, File generatedRoot) throws Exception {
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.isDirectory() || specDir.mkdirs());
        File specFile = new File(specDir, "GreeterSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "public class GreeterSpec extends GreeterSpecSupport {\n" +
                "    public void it_greets() {\n" +
                "        greeting().shouldReturn(\"hello\");\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);
        assertEquals(1, specs.size());
        return SpecSkeletonGenerator.supportPlan(
                specs.get(0).describedType(), specRoot, generatedRoot, SpecNamingConvention.defaultConvention());
    }

    // --- Full regeneration: SpecSupport is a derived artifact, never merged ---

    @Test
    public void removesStaleProxyMethodsByRegeneratingFromPlan() throws Exception {
        File specRoot = temporaryFolder.newFolder("regen-spec-root");
        File generatedRoot = temporaryFolder.newFolder("regen-generated-root");
        SpecGenerationPlan plan = discoverSupportPlan(specRoot, generatedRoot);

        // Simulate a stale generated support from a previous run: it contains a proxy
        // method that the current spec no longer references.
        File targetFile = plan.targetFile();
        assertTrue(targetFile.getParentFile().isDirectory() || targetFile.getParentFile().mkdirs());
        String staleSource =
                "package spec.com.example;\n\n" +
                "import com.example.Greeter;\n\n" +
                "public class GreeterSpecSupport extends io.github.jvmspec.api.ObjectBehavior<Greeter> {\n" +
                "    public GreeterSpecSupport() {\n" +
                "        super(Greeter.class);\n" +
                "    }\n\n" +
                "    protected io.github.jvmspec.matcher.Matchable<Object> staleProxy(Object arg0) {\n" +
                "        return match(subject().staleProxy(arg0));\n" +
                "    }\n" +
                "}\n";
        Files.write(targetFile.toPath(), staleSource.getBytes(StandardCharsets.UTF_8));

        SpecSupportFileGenerator.writeOrUpdate(plan);

        String regenerated = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
        assertFalse("stale proxy method must be removed by full regeneration",
                regenerated.contains("staleProxy"));
        assertTrue("current proxy method must be present",
                regenerated.contains("greeting()"));
        assertEquals("regenerated support must be exactly the rendered plan content",
                plan.sourceContent(), regenerated);
    }

    // --- Change reporting: the caller must know whether anything was written ---

    @Test
    public void reportsChangeOnFirstWriteAndNoChangeWhenContentIsCurrent() throws Exception {
        File specRoot = temporaryFolder.newFolder("report-spec-root");
        File generatedRoot = temporaryFolder.newFolder("report-generated-root");
        SpecGenerationPlan plan = discoverSupportPlan(specRoot, generatedRoot);

        SpecSupportFileGenerator.SupportWriteResult first = SpecSupportFileGenerator.writeOrUpdateResult(plan);
        assertTrue("creating the file must report a change", first.changed());
        assertEquals(plan.targetFile(), first.file());

        SpecSupportFileGenerator.SupportWriteResult second = SpecSupportFileGenerator.writeOrUpdateResult(plan);
        assertFalse("re-running with identical content must report no change", second.changed());
        assertEquals(plan.targetFile(), second.file());
    }
}
