package io.github.jvmspec.discovery;

import io.github.jvmspec.model.DescribedClass;
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
    public void discoversExpandedChainedMatcherNamesForMissingMethodInference() throws Exception {
        File specRoot = temporaryFolder.newFolder("expanded-matcher-discovery-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CatalogSpec.java",
                "package spec.com.example;\n\n" +
                        "public class CatalogSpec extends CatalogSpecSupport {\n" +
                        "    public void it_infers_expanded_chained_matchers() {\n" +
                        "        getAlias().shouldNotBeLike(\"ruby\");\n" +
                        "        getCanonicalName().shouldBeEqualTo(\"emerald\");\n" +
                        "        getDifferentName().shouldNotBeEqualTo(\"emerald\");\n" +
                        "        getInstance().shouldBeAnInstanceOf(Object.class);\n" +
                        "        getReturnedInstance().shouldReturnAnInstanceOf(Object.class);\n" +
                        "        getImplementation().shouldImplement(java.util.List.class);\n" +
                        "        getPrefix().shouldNotStartWith(\"ruby\");\n" +
                        "        getSuffix().shouldNotEndWith(\"ruby\");\n" +
                        "        getPattern().shouldNotMatchPattern(\"ruby.*\");\n" +
                        "        getItems().shouldHaveCount(2);\n" +
                        "        getEmptyItems().shouldBeEmpty();\n" +
                        "        getNonEmptyItems().shouldNotBeEmpty();\n" +
                        "        getLookup().shouldHaveKey(\"ruby\");\n" +
                        "        getMissingLookup().shouldNotHaveKey(\"sapphire\");\n" +
                        "        getValuedLookup().shouldHaveValue(\"emerald\");\n" +
                        "        getOtherValuedLookup().shouldNotHaveValue(\"sapphire\");\n" +
                        "    }\n" +
                        "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertEquals(Arrays.asList(
                MethodDescriptor.of("getAlias", "String"),
                MethodDescriptor.of("getCanonicalName", "String"),
                MethodDescriptor.of("getDifferentName", "String"),
                MethodDescriptor.of("getInstance", "Object"),
                MethodDescriptor.of("getReturnedInstance", "Object"),
                MethodDescriptor.of("getImplementation", "Object"),
                MethodDescriptor.of("getPrefix", "String"),
                MethodDescriptor.of("getSuffix", "String"),
                MethodDescriptor.of("getPattern", "String"),
                MethodDescriptor.of("getItems", "Object"),
                MethodDescriptor.of("getEmptyItems", "Object"),
                MethodDescriptor.of("getNonEmptyItems", "Object"),
                MethodDescriptor.of("getLookup", "Object"),
                MethodDescriptor.of("getMissingLookup", "Object"),
                MethodDescriptor.of("getValuedLookup", "Object"),
                MethodDescriptor.of("getOtherValuedLookup", "Object")
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

    @Test
    public void discoversMatchSubjectProxyCallsAsProductionMethods() throws Exception {
        File specRoot = temporaryFolder.newFolder("match-subject-proxy-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CalculatorSpec.java",
                "package spec.com.example;\n" +
                "public class CalculatorSpec extends CalculatorSpecSupport {\n" +
                "    public void it_calculates_total() {\n" +
                "        match(subject().total(10.0, 2.5)).shouldReturn(12.5);\n" +
                "    }\n" +
                "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        // MATCH_SUBJECT_PROXY_PATTERN infers total(double,double) returning double.
        // SUBJECT_VOID_CALL_PATTERN no longer matches here: the subject() call is nested inside
        // match(...), so it does not start a statement and no phantom void descriptor is produced.
        assertEquals(Arrays.asList(
                MethodDescriptor.of("total", "double", Arrays.asList("double", "double"), Arrays.asList("arg0", "arg1"))
        ), specs.get(0).describedType().methods());
    }

    @Test
    public void discoversMatchSubjectProxyCallsHandlesNestedInnerArgs() throws Exception {
        File specRoot = temporaryFolder.newFolder("match-subject-nested-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "DiscountSpec.java",
                "package spec.com.example;\n" +
                "public class DiscountSpec extends DiscountSpecSupport {\n" +
                "    public void it_computes_discounted() {\n" +
                "        match(subject().compute(getRate(), 100.0)).shouldReturn(15.0);\n" +
                "    }\n" +
                "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        // MATCH_SUBJECT_PROXY_PATTERN infers compute(Object,double) returning double.
        // SUBJECT_VOID_CALL_PATTERN no longer produces a phantom void compute(Object,Object):
        // the nested subject() call does not start a statement.
        assertEquals(Arrays.asList(
                MethodDescriptor.of("compute", "double", Arrays.asList("Object", "double"), Arrays.asList("arg0", "arg1"))
        ), specs.get(0).describedType().methods());
    }

    @Test
    public void matchSubjectProxyDistinctFromDirectProxyStyle() throws Exception {
        File specRoot = temporaryFolder.newFolder("hybrid-proxy-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "HybridSpec.java",
                "package spec.com.example;\n" +
                "public class HybridSpec extends HybridSpecSupport {\n" +
                "    public void it_uses_both_styles() {\n" +
                "        match(subject().total(10.0, 2.5)).shouldReturn(12.5);\n" +
                "        total(10.0, 2.5).shouldReturn(12.5);\n" +
                "    }\n" +
                "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        // Both styles infer the same method: total(double,double) returning double.
        // SUBJECT_VOID_CALL_PATTERN no longer fabricates a phantom void total(double,Object).
        assertEquals(Arrays.asList(
                MethodDescriptor.of("total", "double", Arrays.asList("double", "double"), Arrays.asList("arg0", "arg1"))
        ), specs.get(0).describedType().methods());
    }

    @Test
    public void matchSubjectProxyWithStringArg() throws Exception {
        File specRoot = temporaryFolder.newFolder("match-subject-string-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "FinderSpec.java",
                "package spec.com.example;\n" +
                "public class FinderSpec extends FinderSpecSupport {\n" +
                "    public void it_finds_by_name() {\n" +
                "        match(subject().find(\"item\")).shouldReturn(\"found\");\n" +
                "    }\n" +
                "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        // MATCH_SUBJECT_PROXY_PATTERN infers find(String) returning String.
        // SUBJECT_VOID_CALL_PATTERN does not match: the nested subject() call is not a statement.
        assertEquals(Arrays.asList(
                MethodDescriptor.of("find", "String", Arrays.asList("String"), Arrays.asList("arg0"))
        ), specs.get(0).describedType().methods());
    }

    @Test
    public void matchSubjectProxyWithIntegerArg() throws Exception {
        File specRoot = temporaryFolder.newFolder("match-subject-int-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "IndexerSpec.java",
                "package spec.com.example;\n" +
                "public class IndexerSpec extends IndexerSpecSupport {\n" +
                "    public void it_gets_by_index() {\n" +
                "        match(subject().get(42)).shouldReturn(\"value\");\n" +
                "    }\n" +
                "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        // MATCH_SUBJECT_PROXY_PATTERN infers get(int) returning String.
        // SUBJECT_VOID_CALL_PATTERN no longer fabricates a phantom void get(Object).
        assertEquals(Arrays.asList(
                MethodDescriptor.of("get", "String", Arrays.asList("int"), Arrays.asList("arg0"))
        ), specs.get(0).describedType().methods());
    }

    @Test
    public void repeatedProxyCallsWithLocalStringAndCastNullCollapseToOneMethod() throws Exception {
        File specRoot = temporaryFolder.newFolder("duplicate-signature-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CanonicalTextSpec.java",
                "package spec.com.example;\n" +
                "public class CanonicalTextSpec extends CanonicalTextSpecSupport {\n" +
                "    public void it_classifies_canonical_text() {\n" +
                "        String validId = \"abc\";\n" +
                "        isCanonicalText(validId).shouldReturn(true);\n" +
                "        isCanonicalText((String) null).shouldReturn(false);\n" +
                "    }\n" +
                "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertEquals(Arrays.asList(
                MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("String"), Arrays.asList("arg0"))
        ), specs.get(0).describedType().methods());
    }

    @Test
    public void repeatedProxyCallsWithJavaLangStringAndStringCollapseToOneMethod() throws Exception {
        File specRoot = temporaryFolder.newFolder("normalized-signature-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CanonicalTextSpec.java",
                "package spec.com.example;\n" +
                "public class CanonicalTextSpec extends CanonicalTextSpecSupport {\n" +
                "    public void it_classifies_canonical_text(java.lang.String value) {\n" +
                "        isCanonicalText(value).shouldReturn(true);\n" +
                "        isCanonicalText(\"abc\").shouldReturn(true);\n" +
                "    }\n" +
                "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertEquals(Arrays.asList(
                MethodDescriptor.of("isCanonicalText", "boolean", Arrays.asList("java.lang.String"), Arrays.asList("arg0"))
        ), specs.get(0).describedType().methods());
    }

    @Test
    public void subjectCallNestedInShouldReturnArgumentDoesNotFabricatePhantomMethod() throws Exception {
        File specRoot = temporaryFolder.newFolder("nested-subject-arg-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "KeySpec.java",
                "package spec.com.example;\n" +
                "public class KeySpec extends KeySpecSupport {\n" +
                "    public void it_returns_matching_handle() {\n" +
                "        Handle handle = subject().toKeyHandle();\n" +
                "        match(handle.keyId()).shouldReturn(subject().id());\n" +
                "    }\n" +
                "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        // Regression: subject().id() nested inside shouldReturn(...) previously produced a phantom
        // void id(Object arg0); subject().toKeyHandle() consumed by an assignment previously
        // produced a phantom void toKeyHandle(). Neither is a standalone statement, so no method
        // descriptor must be synthesized for either.
        assertEquals(Arrays.asList(), specs.get(0).describedType().methods());
    }

    @Test
    public void standaloneSubjectVoidStatementIsStillDiscovered() throws Exception {
        File specRoot = temporaryFolder.newFolder("standalone-void-root");
        writeFile(
                specRoot,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "ResetterSpec.java",
                "package spec.com.example;\n" +
                "public class ResetterSpec extends ResetterSpecSupport {\n" +
                "    public void it_resets_state() {\n" +
                "        subject().reset();\n" +
                "        subject().configure(\"mode\");\n" +
                "    }\n" +
                "}\n"
        );

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);

        assertEquals(1, specs.size());
        assertEquals(Arrays.asList(
                MethodDescriptor.voidMethod("reset", Arrays.<String>asList(), Arrays.<String>asList()),
                MethodDescriptor.voidMethod("configure", Arrays.asList("String"), Arrays.asList("arg0"))
        ), specs.get(0).describedType().methods());
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
