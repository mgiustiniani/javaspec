package org.javaspec.discovery;

import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
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
import static org.junit.Assert.assertTrue;

public class SpecDiscoveryTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void discoversSpecsAndInfersDescribedClassesFromSpecNamespaceDeterministically() throws Exception {
        File specRoot = temporaryFolder.newFolder("spec-root");
        File betaSpec = writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BetaSpec.java");
        File alphaSpec = writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "AlphaSpec.java");
        writeFile(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "Helper.java");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(2, specs.size());
        assertEquals(alphaSpec, specs.get(0).specFile());
        assertEquals("spec.com.example.AlphaSpec", specs.get(0).specQualifiedName());
        assertEquals(DescribedClass.of("com.example.Alpha"), specs.get(0).describedClass());
        assertEquals(DescribedType.of("com.example.Alpha", JavaTypeKind.CLASS), specs.get(0).describedType());
        assertEquals(betaSpec, specs.get(1).specFile());
        assertEquals("spec.com.example.BetaSpec", specs.get(1).specQualifiedName());
        assertEquals(DescribedClass.of("com.example.Beta"), specs.get(1).describedClass());
        assertEquals(DescribedType.of("com.example.Beta", JavaTypeKind.CLASS), specs.get(1).describedType());
    }

    @Test
    public void discoversClassLikeTypeMarkersFromSpecSource() throws Exception {
        File specRoot = temporaryFolder.newFolder("class-type-spec-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "PaymentGatewaySpec.java",
                "shouldBeAnInterface();\n"
        );
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "OrderStatusSpec.java",
                "shouldBeAnEnum();\n"
        );
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "ExperimentalSpec.java",
                "shouldBeAnAnnotation();\n"
        );
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "UserSpec.java",
                "shouldBeARecord();\n"
        );
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "ShapeSpec.java",
                "shouldBeASealedClass();\n"
        );
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "MessageSpec.java",
                "shouldBeASealedInterface();\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(6, specs.size());
        assertEquals(DescribedType.of("com.example.Experimental", JavaTypeKind.ANNOTATION), specs.get(0).describedType());
        assertEquals(DescribedType.of("com.example.Message", JavaTypeKind.SEALED_INTERFACE), specs.get(1).describedType());
        assertEquals(DescribedType.of("com.example.OrderStatus", JavaTypeKind.ENUM), specs.get(2).describedType());
        assertEquals(DescribedType.of("com.example.PaymentGateway", JavaTypeKind.INTERFACE), specs.get(3).describedType());
        assertEquals(DescribedType.of("com.example.Shape", JavaTypeKind.SEALED_CLASS), specs.get(4).describedType());
        assertEquals(DescribedType.of("com.example.User", JavaTypeKind.RECORD), specs.get(5).describedType());
    }

    @Test
    public void discoversExtendsAndImplementsTypesFromClassLiterals() throws Exception {
        File specRoot = temporaryFolder.newFolder("relationships-spec-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "ServiceSpec.java",
                "import com.example.BaseService;\n" +
                        "import com.example.PaymentGateway;\n" +
                        "shouldExtend(BaseService.class);\n" +
                        "shouldImplement(PaymentGateway.class);\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertEquals(DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Arrays.asList("com.example.BaseService"),
                Arrays.asList("com.example.PaymentGateway"),
                Arrays.<String>asList()
        ), specs.get(0).describedType());
    }

    @Test
    public void discoversPermittedTypesFromShouldPermitClassLiterals() throws Exception {
        File specRoot = temporaryFolder.newFolder("permits-spec-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "ShapeSpec.java",
                "import com.example.Circle;\n" +
                        "import com.example.Rectangle;\n" +
                        "shouldBeASealedClass();\n" +
                        "shouldPermit(Circle.class, Rectangle.class);\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertEquals(DescribedType.of(
                "com.example.Shape",
                JavaTypeKind.SEALED_CLASS,
                Arrays.<String>asList(),
                Arrays.<String>asList(),
                Arrays.asList("com.example.Circle", "com.example.Rectangle")
        ), specs.get(0).describedType());
    }

    @Test
    public void discoversTypedProxyCallsAsProductionMethodsButIgnoresDuringInstantiation() throws Exception {
        File specRoot = temporaryFolder.newFolder("typed-proxy-discovery-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpec.java",
                "package spec.com.example;\n\n" +
                        "public class BookSpec extends BookSpecSupport {\n" +
                        "    public void it_has_rating() {\n" +
                        "        getRating().shouldReturn(5);\n" +
                        "    }\n" +
                        "\n" +
                        "    public void it_has_title() {\n" +
                        "        getTitle().shouldContain(\"Wizard\");\n" +
                        "    }\n" +
                        "\n" +
                        "    public void it_is_enabled() {\n" +
                        "        isEnabled().shouldReturn(true);\n" +
                        "    }\n" +
                        "\n" +
                        "    public void it_rejects_negative_rating() {\n" +
                        "        shouldThrow(IllegalArgumentException.class).duringSetRating(-3);\n" +
                        "    }\n" +
                        "\n" +
                        "    public void it_rejects_invalid_construction() {\n" +
                        "        shouldThrow(IllegalArgumentException.class).duringInstantiation();\n" +
                        "    }\n" +
                        "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertEquals(Arrays.asList(
                MethodDescriptor.of("getRating", "int"),
                MethodDescriptor.of("getTitle", "String"),
                MethodDescriptor.of("isEnabled", "boolean"),
                MethodDescriptor.voidMethod("setRating", Arrays.asList("int"), Arrays.asList("rating"))
        ), specs.get(0).describedType().methods());
    }

    @Test
    public void discoversLegacySamePackageSpecsByConvention() throws Exception {
        File specRoot = temporaryFolder.newFolder("legacy-spec-root");
        File specFile = writeFile(specRoot, "com" + File.separator + "example" + File.separator + "LegacySpec.java");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertEquals(specFile, specs.get(0).specFile());
        assertEquals("com.example.LegacySpec", specs.get(0).specQualifiedName());
        assertEquals(DescribedClass.of("com.example.Legacy"), specs.get(0).describedClass());
        assertEquals(DescribedType.of("com.example.Legacy", JavaTypeKind.CLASS), specs.get(0).describedType());
    }

    @Test
    public void returnsEmptyWhenSpecRootDoesNotExist() throws Exception {
        File specRoot = new File(temporaryFolder.getRoot(), "missing-spec-root");

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertTrue(specs.isEmpty());
    }

    private static File writeFile(File root, String relativePath) throws Exception {
        return writeFile(root, relativePath, "public class Placeholder { }\n");
    }

    private static File writeFile(File root, String relativePath, String content) throws Exception {
        File file = new File(root, relativePath);
        File parent = file.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
