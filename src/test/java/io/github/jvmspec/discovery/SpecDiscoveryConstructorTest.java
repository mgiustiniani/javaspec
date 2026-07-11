package io.github.jvmspec.discovery;

import io.github.jvmspec.generation.TypeSkeletonGenerator;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.MethodDescriptor;
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
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
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
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
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
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
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
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
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
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
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
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
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
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
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

    @Test
    public void preservesLiteralConstructorArgumentsBeforeNamedLocalVariable() throws Exception {
        File specRoot = temporaryFolder.newFolder("local-final-arg-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "LifecycleEventSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import io.github.jvmspec.api.ObjectBehavior;\n" +
                "import java.time.Instant;\n\n" +
                "public class LifecycleEventSpec extends ObjectBehavior<LifecycleEvent> {\n" +
                "    public void let() {\n" +
                "        Instant occurredAt = Instant.parse(\"2026-07-10T12:00:00Z\");\n" +
                "        beConstructedWith(\"event-id\", \"profile-id\", 7L, \"created\", \"actor\", occurredAt);\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        List<String> parameterTypes = specs.get(0).describedType().constructors().get(0).parameterTypes();
        assertEquals(Arrays.asList(
                "String", "String", "long", "String", "String", "java.time.Instant"
        ), parameterTypes);
    }

    // --- Regression: enum-constant arguments must infer the enum type, not widen to Object ---

    @Test
    public void infersEnumConstantArgumentTypeFromQualifiedReferenceInsteadOfObject() throws Exception {
        File specRoot = temporaryFolder.newFolder("enum-arg-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "KeySpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import com.example.Algorithm;\n" +
                "import com.example.KeyUsage;\n" +
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
                "public class KeySpec extends ObjectBehavior<Key> {\n" +
                "    public void it_returns_the_configured_values() {\n" +
                "        beConstructedWith(Algorithm.EC_P256, KeyUsage.SIGN, \"code-signing-key\");\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        DiscoveredSpec spec = specs.get(0);
        DescribedType type = spec.describedType();
        assertTrue(type.hasConstructors());
        assertEquals(1, type.constructors().size());
        List<String> parameterTypes = type.constructors().get(0).parameterTypes();
        assertEquals(3, parameterTypes.size());
        assertEquals("enum constant argument must infer its imported qualifier as the type, not Object",
                "com.example.Algorithm", parameterTypes.get(0));
        assertEquals("enum constant argument must infer its imported qualifier as the type, not Object",
                "com.example.KeyUsage", parameterTypes.get(1));
        assertEquals("string literal argument must still infer String",
                "String", parameterTypes.get(2));
    }

    @Test
    public void derivesIdiomaticParameterNamesFromEnumConstantArguments() throws Exception {
        File specRoot = temporaryFolder.newFolder("enum-name-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "KeySpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import com.example.Algorithm;\n" +
                "import com.example.KeyUsage;\n" +
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
                "public class KeySpec extends ObjectBehavior<Key> {\n" +
                "    public void it_returns_the_configured_values() {\n" +
                "        beConstructedWith(Algorithm.EC_P256, KeyUsage.SIGN, \"code-signing-key\");\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        List<String> parameterNames = specs.get(0).describedType().constructors().get(0).parameterNames();
        assertEquals("enum constant argument must yield a decapitalized type-derived name",
                "algorithm", parameterNames.get(0));
        assertEquals("enum constant argument must yield a decapitalized type-derived name",
                "keyUsage", parameterNames.get(1));
        assertEquals("literal argument without a derivable name keeps the positional fallback",
                "arg2", parameterNames.get(2));
    }

    @Test
    public void fallsBackToPositionalNameWhenTypeDerivedNamesWouldCollide() throws Exception {
        File specRoot = temporaryFolder.newFolder("enum-collision-spec-root");
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, "RangeSpec.java");
        String specSource =
                "package spec.com.example;\n\n" +
                "import com.example.Bound;\n" +
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
                "public class RangeSpec extends ObjectBehavior<Range> {\n" +
                "    public void it_spans_bounds() {\n" +
                "        beConstructedWith(Bound.LOWER, Bound.UPPER);\n" +
                "    }\n" +
                "}\n";
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        List<String> parameterNames = specs.get(0).describedType().constructors().get(0).parameterNames();
        assertEquals("first occurrence takes the type-derived name", "bound", parameterNames.get(0));
        assertEquals("colliding name must fall back to the positional name", "arg1", parameterNames.get(1));
    }
}
