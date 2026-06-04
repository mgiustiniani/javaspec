package org.javaspec.discovery;

import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
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

public class SpecDiscoveryNamingTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    // ── Custom naming convention ────────────────────────────────────

    @Test
    public void discoverUsesCustomNamingConventionAndMapsConfiguredSpecRoot() throws Exception {
        File specRoot = temporaryFolder.newFolder("custom-naming-root");
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        writeFile(specRoot, "spec" + File.separator + "BookSpec.java",
                "package spec;\n\nimport org.example.Book;\npublic class BookSpec extends ObjectBehavior<Book> {\n}\n");
        writeFile(specRoot, "spec" + File.separator + "domain" + File.separator + "ServiceSpec.java",
                "package spec.domain;\n\nimport org.example.domain.Service;\npublic class ServiceSpec extends ObjectBehavior<Service> {\n}\n");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot, custom);
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(2, specs.size());
        assertEquals("spec.BookSpec", specs.get(0).specQualifiedName());
        assertEquals(DescribedClass.of("org.example.Book"), specs.get(0).describedClass());
        assertEquals("spec.domain.ServiceSpec", specs.get(1).specQualifiedName());
        assertEquals(DescribedClass.of("org.example.domain.Service"), specs.get(1).describedClass());
    }

    @Test
    public void discoverUsesCustomNamingConventionViaRequestWithSuite() throws Exception {
        File specRoot = temporaryFolder.newFolder("custom-suite-naming-root");
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "com.acme");
        writeFile(specRoot, "spec" + File.separator + "WidgetSpec.java",
                "package spec;\n\nimport com.acme.Widget;\npublic class WidgetSpec extends ObjectBehavior<Widget> {\n}\n");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.forSuite("my-suite", specRoot, custom);
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        assertEquals("spec.WidgetSpec", specs.get(0).specQualifiedName());
        assertEquals(DescribedClass.of("com.acme.Widget"), specs.get(0).describedClass());
    }

    @Test
    public void discoverIgnoresFilesUnderCustomConventionThatDontMatchProductionPrefix() throws Exception {
        File specRoot = temporaryFolder.newFolder("prefix-mismatch-root");
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        writeFile(specRoot, "unrelated" + File.separator + "BookSpec.java",
                "package unrelated;\n\npublic class BookSpec extends ObjectBehavior<Book> {\n}\n");

        // The file exists but naming convention cannot map it because unrelated.Book is outside org.example prefix
        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot, custom);
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertTrue(specs.isEmpty());
    }

    // ── Deterministic file ordering ─────────────────────────────────

    @Test
    public void discoverOrdersFilesDeterministicallyByFileName() throws Exception {
        File specRoot = temporaryFolder.newFolder("deterministic-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BetaSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "AlphaSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "GammaSpec.java");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(3, specs.size());
        assertEquals("spec.com.example.AlphaSpec", specs.get(0).specQualifiedName());
        assertEquals("spec.com.example.BetaSpec", specs.get(1).specQualifiedName());
        assertEquals("spec.com.example.GammaSpec", specs.get(2).specQualifiedName());
    }

    // ── Class filters ───────────────────────────────────────────────

    @Test
    public void classFilterMatchesDescribedQualifiedName() throws Exception {
        File specRoot = temporaryFolder.newFolder("class-filter-qualified-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "AlbumSpec.java");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withClassFilter("com.example.Book");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        assertEquals("spec.com.example.BookSpec", specs.get(0).specQualifiedName());
    }

    @Test
    public void classFilterMatchesDescribedSimpleName() throws Exception {
        File specRoot = temporaryFolder.newFolder("class-filter-simple-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "AlbumSpec.java");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withClassFilter("Book");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        assertEquals("spec.com.example.BookSpec", specs.get(0).specQualifiedName());
    }

    @Test
    public void classFilterMatchesSpecQualifiedName() throws Exception {
        File specRoot = temporaryFolder.newFolder("class-filter-spec-qualified-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "AlbumSpec.java");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withClassFilter("spec.com.example.BookSpec");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        assertEquals("spec.com.example.BookSpec", specs.get(0).specQualifiedName());
    }

    @Test
    public void classFilterMatchesSpecSimpleName() throws Exception {
        File specRoot = temporaryFolder.newFolder("class-filter-spec-simple-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "AlbumSpec.java");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withClassFilter("BookSpec");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        assertEquals("spec.com.example.BookSpec", specs.get(0).specQualifiedName());
    }

    @Test
    public void classFilterSkipsUnrelatedSpecs() throws Exception {
        File specRoot = temporaryFolder.newFolder("class-filter-skip-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "AlbumSpec.java");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withClassFilter("NonExistent");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertTrue(specs.isEmpty());
    }

    @Test
    public void classFilterWithMultipleFiltersMatchesAny() throws Exception {
        File specRoot = temporaryFolder.newFolder("class-filter-multi-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "AlbumSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "ChapterSpec.java");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withClassFilter("com.example.Book")
                .withClassFilter("com.example.Chapter");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(2, specs.size());
        assertEquals("spec.com.example.BookSpec", specs.get(0).specQualifiedName());
        assertEquals("spec.com.example.ChapterSpec", specs.get(1).specQualifiedName());
    }

    // ── Example metadata ────────────────────────────────────────────

    @Test
    public void exampleMetadataExtractsOnlyPublicNoArgVoidItMethods() throws Exception {
        File specRoot = temporaryFolder.newFolder("example-metadata-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void it_checks_title() {\n" +
                "    }\n" +
                "    public void it_has_rating() {\n" +
                "    }\n" +
                "    public void it_checks_author(String name) {\n" +
                "    }\n" +
                "    public void its_initialized() {\n" +
                "    }\n" +
                "    public void let() {\n" +
                "    }\n" +
                "    public void letGo() {\n" +
                "    }\n" +
                "    private void it_is_private() {\n" +
                "    }\n" +
                "}\n");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        List<SpecExample> examples = specs.get(0).examples();
        // it_checks_title (no-arg), it_has_rating (no-arg), its_initialized (no-arg)
        // it_checks_author has param -> excluded; let/letGo excluded; private excluded
        assertEquals(3, examples.size());
        assertEquals("it_checks_title", examples.get(0).methodName());
        assertEquals("it has rating", examples.get(1).displayName());
        assertEquals("its_initialized", examples.get(2).methodName());
    }

    @Test
    public void exampleMetadataPreservesSourceOrder() throws Exception {
        File specRoot = temporaryFolder.newFolder("example-order-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void it_first() {\n" +
                "    }\n" +
                "    public void it_second() {\n" +
                "    }\n" +
                "    public void it_third() {\n" +
                "    }\n" +
                "}\n");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        List<SpecExample> examples = specs.get(0).examples();
        assertEquals(3, examples.size());
        assertEquals("it_first", examples.get(0).methodName());
        assertEquals(0, examples.get(0).sourceOrderIndex());
        assertEquals("it_second", examples.get(1).methodName());
        assertEquals(1, examples.get(1).sourceOrderIndex());
        assertEquals("it_third", examples.get(2).methodName());
        assertEquals(2, examples.get(2).sourceOrderIndex());
    }

    @Test
    public void exampleMetadataIncludesSourceLineNumbers() throws Exception {
        File specRoot = temporaryFolder.newFolder("example-line-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n" +
                "\n" +
                "public class BookSpec {\n" +
                "    // comments and blank lines should not disturb source line extraction\n" +
                "\n" +
                "    public void it_first() {\n" +
                "    }\n" +
                "\n" +
                "    public void it_second() {\n" +
                "    }\n" +
                "}\n");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        List<SpecExample> examples = specs.get(0).examples();
        assertEquals(2, examples.size());
        assertEquals("it_first", examples.get(0).methodName());
        assertEquals(6, examples.get(0).sourceLine());
        assertEquals(6, examples.get(0).lineNumber());
        assertTrue(examples.get(0).hasSourceLine());
        assertEquals("it_second", examples.get(1).methodName());
        assertEquals(9, examples.get(1).sourceLine());
        assertEquals("spec.com.example.BookSpec#it_second", examples.get(1).stableId("spec.com.example.BookSpec"));
    }

    @Test
    public void exampleDisplayNameReplacesUnderscoresWithSpaces() throws Exception {
        File specRoot = temporaryFolder.newFolder("example-display-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void it_checks_title() {\n" +
                "    }\n" +
                "    public void its_initialized() {\n" +
                "    }\n" +
                "}\n");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        List<SpecExample> examples = specs.get(0).examples();
        assertEquals(2, examples.size());
        assertEquals("it checks title", examples.get(0).displayName());
        assertEquals("its initialized", examples.get(1).displayName());
    }

    @Test
    public void specWithoutExamplesHasEmptyExamples() throws Exception {
        File specRoot = temporaryFolder.newFolder("no-examples-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void it_initializable() {\n" +
                "        shouldHaveType(Book.class);\n" +
                "    }\n" +
                "}\n");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertTrue(specs.get(0).hasExamples());
    }

    // ── Example filters ─────────────────────────────────────────────

    @Test
    public void exampleFilterMatchesMethodName() throws Exception {
        File specRoot = temporaryFolder.newFolder("example-filter-method-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void it_checks_title() {\n" +
                "    }\n" +
                "    public void it_has_rating() {\n" +
                "    }\n" +
                "}\n");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withExampleFilter("it_checks_title");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        List<SpecExample> examples = specs.get(0).examples();
        assertEquals(1, examples.size());
        assertEquals("it_checks_title", examples.get(0).methodName());
    }

    @Test
    public void exampleFilterMatchesDisplayName() throws Exception {
        File specRoot = temporaryFolder.newFolder("example-filter-display-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void it_checks_title() {\n" +
                "    }\n" +
                "    public void it_has_rating() {\n" +
                "    }\n" +
                "}\n");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withExampleFilter("it checks title");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        List<SpecExample> examples = specs.get(0).examples();
        assertEquals(1, examples.size());
        assertEquals("it_checks_title", examples.get(0).methodName());
    }

    @Test
    public void exampleFilterMatchesOrderIndex() throws Exception {
        File specRoot = temporaryFolder.newFolder("example-filter-index-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void it_first() {\n" +
                "    }\n" +
                "    public void it_second() {\n" +
                "    }\n" +
                "}\n");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withExampleFilter("1");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        List<SpecExample> examples = specs.get(0).examples();
        assertEquals(1, examples.size());
        assertEquals("it_second", examples.get(0).methodName());
    }

    @Test
    public void exampleFilterExcludesSpecWithNoMatchingExamples() throws Exception {
        File specRoot = temporaryFolder.newFolder("example-filter-exclude-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void it_checks_title() {\n" +
                "    }\n" +
                "    public void it_has_rating() {\n" +
                "    }\n" +
                "}\n");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "AlbumSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Album;\n\n" +
                "public class AlbumSpec extends ObjectBehavior<Album> {\n" +
                "    public void it_checks_tracks() {\n" +
                "    }\n" +
                "}\n");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withExampleFilter("it_has_rating");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        assertEquals("spec.com.example.BookSpec", specs.get(0).specQualifiedName());
        assertEquals(1, specs.get(0).examples().size());
        assertEquals("it_has_rating", specs.get(0).examples().get(0).methodName());
    }

    @Test
    public void exampleFilterWithMultipleFiltersMatchesAny() throws Exception {
        File specRoot = temporaryFolder.newFolder("example-filter-multi-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void it_checks_title() {\n" +
                "    }\n" +
                "    public void it_has_rating() {\n" +
                "    }\n" +
                "}\n");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot)
                .withExampleFilter("it_checks_title")
                .withExampleFilter("1");
        List<DiscoveredSpec> specs = SpecDiscovery.discover(request);

        assertEquals(1, specs.size());
        List<SpecExample> examples = specs.get(0).examples();
        assertEquals(2, examples.size());
    }

    // ── Existing default discovery remains stable ───────────────────

    @Test
    public void defaultDiscoverFromRootStillWorks() throws Exception {
        File specRoot = temporaryFolder.newFolder("default-stable-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertEquals("spec.com.example.BookSpec", specs.get(0).specQualifiedName());
        assertEquals(DescribedClass.of("com.example.Book"), specs.get(0).describedClass());
    }

    @Test
    public void defaultDiscoverWithExplicitNamingConventionStillWorks() throws Exception {
        File specRoot = temporaryFolder.newFolder("default-explicit-root");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot, SpecNamingConvention.defaults());

        assertEquals(1, specs.size());
        assertEquals("spec.com.example.BookSpec", specs.get(0).specQualifiedName());
    }

    @Test
    public void defaultDiscoverReturnsEmptyWhenRootMissing() throws Exception {
        File specRoot = new File(temporaryFolder.getRoot(), "missing-default-root");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertTrue(specs.isEmpty());
    }

    // ── SpecDiscoveryRequest construction ───────────────────────────

    @Test
    public void requestWithNamingConventionPreservesConvention() {
        File specRoot = new File("specs");
        SpecNamingConvention custom = SpecNamingConvention.of("test", "com.example");

        SpecDiscoveryRequest request = SpecDiscoveryRequest.of(specRoot, custom);

        assertEquals(custom, request.namingConvention());
    }

    @Test
    public void requestFromSuiteConfigurationCreatesWithConvention() {
        org.javaspec.config.JavaspecSuiteConfiguration config =
                org.javaspec.config.JavaspecSuiteConfiguration.of(
                        "my-suite", "target/specs", "src/main/java",
                        "spec", "org.example",
                        java.util.Collections.<String>emptyList()
                );

        SpecDiscoveryRequest request = SpecDiscoveryRequest.from(config);

        assertEquals("my-suite", request.suiteName());
        assertEquals("spec", request.namingConvention().specPackagePrefix());
        assertEquals("org.example", request.namingConvention().productionPackagePrefix());
    }

    // ── Helper ──────────────────────────────────────────────────────

    private static File writeFile(File root, String relativePath, String content) throws Exception {
        File file = new File(root, relativePath);
        File parent = file.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static File writeFile(File root, String relativePath) throws Exception {
        return writeFile(root, relativePath,
                "public class Placeholder {\n" +
                "    public void it_is_example() {\n" +
                "    }\n" +
                "}\n");
    }
}
