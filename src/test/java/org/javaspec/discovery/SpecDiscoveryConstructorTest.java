package org.javaspec.discovery;

import org.javaspec.generation.TypeSkeletonGenerator;
import org.javaspec.model.DescribedType;
import org.javaspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpecDiscoveryConstructorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void discoversConstructorFromBeConstructedWith() throws Exception {
        File specRoot = temporaryFolder.newFolder("with-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "MarkdownSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import com.example.Writer;\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class MarkdownSpec extends ObjectBehavior<Markdown> {\n" +
                "    public void let(Writer writer) {\n" +
                "        beConstructedWith(writer);\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        DiscoveredSpec spec = specs.get(0);
        DescribedType type = spec.describedType();
        assertTrue(type.hasConstructors());
        assertEquals(1, type.constructors().size());
        assertEquals("com.example.Writer", type.constructors().get(0).parameterTypes().get(0));
        assertEquals("writer", type.constructors().get(0).parameterNames().get(0));
    }

    @Test
    public void discoversConstructorWithMultipleParameters() throws Exception {
        File specRoot = temporaryFolder.newFolder("multi-param-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "ServiceSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import com.example.Writer;\n" +
                "import com.example.Reader;\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class ServiceSpec extends ObjectBehavior<Service> {\n" +
                "    public void let(Writer writer, Reader reader) {\n" +
                "        beConstructedWith(writer, reader);\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        DiscoveredSpec spec = specs.get(0);
        DescribedType type = spec.describedType();
        assertTrue(type.hasConstructors());
        assertEquals(1, type.constructors().size());
        assertEquals(2, type.constructors().get(0).parameterTypes().size());
        assertEquals("com.example.Writer", type.constructors().get(0).parameterTypes().get(0));
        assertEquals("com.example.Reader", type.constructors().get(0).parameterTypes().get(1));
        assertEquals("writer", type.constructors().get(0).parameterNames().get(0));
        assertEquals("reader", type.constructors().get(0).parameterNames().get(1));
    }

    @Test
    public void discoversStaticFactoryMethodFromBeConstructedThroughWithoutConstructorMarker() throws Exception {
        File specRoot = temporaryFolder.newFolder("through-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "MarkdownSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class MarkdownSpec extends ObjectBehavior<Markdown> {\n" +
                "    public void let() {\n" +
                "        beConstructedThrough(\"createForWriting\");\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        DescribedType type = specs.get(0).describedType();
        assertFalse(type.hasConstructors());
        assertEquals(Arrays.asList(
                MethodDescriptor.staticMethod("createForWriting", "com.example.Markdown")
        ), type.methods());
    }

    @Test
    public void discoversFactoryParametersAndGeneratedStaticFactoryFromBeConstructedThroughLiterals() throws Exception {
        File specRoot = temporaryFolder.newFolder("through-literals-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "BookSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class BookSpec extends ObjectBehavior<Book> {\n" +
                "    public void let() {\n" +
                "        beConstructedThrough(\"create\", \"Wizard\", 5);\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        DescribedType type = specs.get(0).describedType();
        assertFalse(type.hasConstructors());
        assertEquals(Arrays.asList(
                MethodDescriptor.staticMethod(
                        "create",
                        "com.example.Book",
                        Arrays.asList("String", "int"),
                        Arrays.asList("arg0", "arg1")
                )
        ), type.methods());

        String generatedSource = TypeSkeletonGenerator.render(type);

        assertTrue(generatedSource.contains("public static Book create(String arg0, int arg1)"));
        assertTrue(generatedSource.contains("return new Book();"));
    }

    @Test
    public void discoversStaticFactoryMethodsFromNamedConstructionMarkers() throws Exception {
        File specRoot = temporaryFolder.newFolder("named-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "MarkdownSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class MarkdownSpec extends ObjectBehavior<Markdown> {\n" +
                "    public void let(String name, int edition) {\n" +
                "        beConstructedNamed(\"named\", name);\n" +
                "        beConstructedThroughNamed(\"createNamed\", edition);\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        DescribedType type = specs.get(0).describedType();
        assertFalse(type.hasConstructors());
        assertEquals(Arrays.asList(
                MethodDescriptor.staticMethod(
                        "named",
                        "com.example.Markdown",
                        Arrays.asList("String"),
                        Arrays.asList("name")
                ),
                MethodDescriptor.staticMethod(
                        "createNamed",
                        "com.example.Markdown",
                        Arrays.asList("int"),
                        Arrays.asList("edition")
                )
        ), type.methods());
    }

    @Test
    public void ignoresNonStringFactoryNamesWithoutAddingConstructorsOrMethods() throws Exception {
        File specRoot = temporaryFolder.newFolder("non-string-factory-name-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "MarkdownSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class MarkdownSpec extends ObjectBehavior<Markdown> {\n" +
                "    public void let(String factoryName) {\n" +
                "        beConstructedThrough(factoryName, \"Wizard\");\n" +
                "        beConstructedNamed(factoryName, 5);\n" +
                "        beConstructedThroughNamed(factoryName, \"Bob\");\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        DescribedType type = specs.get(0).describedType();
        assertFalse(type.hasConstructors());
        assertTrue(type.methods().isEmpty());
    }

    @Test
    public void specWithoutConstructorMarkersHasNoConstructors() throws Exception {
        File specRoot = temporaryFolder.newFolder("no-ctor-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "SimpleSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class SimpleSpec extends ObjectBehavior<Simple> {\n" +
                "    public void it_is_initializable() {\n" +
                "        shouldHaveType(Simple.class);\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        DiscoveredSpec spec = specs.get(0);
        DescribedType type = spec.describedType();
        assertFalse(type.hasConstructors());
    }
}
