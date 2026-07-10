package io.github.jvmspec.generation;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedClass;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpecSkeletonGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void rendersPackagedSpecSkeleton() {
        String source = SpecSkeletonGenerator.render(DescribedClass.of("com.example.Calculator"));

        assertEquals("package spec.com.example;\n\n" +
                "import com.example.Calculator;\n\n" +
                "public class CalculatorSpec extends CalculatorSpecSupport {\n" +
                "    public void it_is_initializable() {\n" +
                "        shouldHaveType(Calculator.class);\n" +
                "    }\n" +
                "}\n", source);
    }

    @Test
    public void rendersSpecPackageSkeletonForDefaultPackageType() {
        String source = SpecSkeletonGenerator.render(DescribedClass.of("Calculator"));

        assertEquals("package spec;\n\n" +
                "public class CalculatorSpec extends CalculatorSpecSupport {\n" +
                "    public void it_is_initializable() {\n" +
                "        shouldHaveType(Calculator.class);\n" +
                "    }\n" +
                "}\n", source);
    }

    @Test
    public void rendersPackagedSupportSkeleton() {
        String source = SpecSkeletonGenerator.renderSupport(DescribedType.of(DescribedClass.of("com.example.Calculator")));

        assertEquals("package spec.com.example;\n\n" +
                "import com.example.Calculator;\n\n" +
                "public class CalculatorSpecSupport extends io.github.jvmspec.api.ObjectBehavior<Calculator> {\n" +
                "    public CalculatorSpecSupport() {\n" +
                "        super(Calculator.class);\n" +
                "    }\n" +
                "}\n", source);
    }

    @Test
    public void rendersRecordSupportWithDefaultConstructionForCanonicalConstructor() {
        DescribedType describedType = DescribedType.of(
                "com.example.CertificateProfileId",
                JavaTypeKind.RECORD,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(
                        ConstructorDescriptor.empty(),
                        ConstructorDescriptor.of(
                                Arrays.asList("String", "int", "long", "double", "boolean", "char"),
                                Arrays.asList("value", "version", "counter", "weight", "active", "marker"),
                                "")),
                Collections.<MethodDescriptor>emptyList()
        );

        String source = SpecSkeletonGenerator.renderSupport(describedType);

        assertTrue(source.contains("super(CertificateProfileId.class);\n" +
                "        beConstructedWith((String) null, 0, 0L, 0.0d, false, '\\0');"));
    }

    @Test
    public void rendersRecordSupportWithPrecisePrimitiveDefaults() {
        DescribedType describedType = DescribedType.of(
                "com.example.PrimitiveRecord",
                JavaTypeKind.RECORD,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("byte", "short", "float", "java.lang.String", "java.time.Instant"),
                        Arrays.asList("byteValue", "shortValue", "floatValue", "name", "createdAt"),
                        "")),
                Collections.<MethodDescriptor>emptyList()
        );

        String source = SpecSkeletonGenerator.renderSupport(describedType);

        assertTrue(source.contains("beConstructedWith((byte) 0, (short) 0, 0.0f, (String) null, (java.time.Instant) null);"));
    }

    @Test
    public void supportPlanSkipsStaticFactoryMethodsInTypedProxiesAndThrowProxies() throws Exception {
        File specRoot = temporaryFolder.newFolder("support-static-factory-root");
        DescribedType describedType = DescribedType.of(
                "com.example.Book",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.staticMethod("create", "com.example.Book"),
                        MethodDescriptor.of("getTitle", "String")
                )
        );

        SpecGenerationPlan plan = SpecSkeletonGenerator.supportPlan(describedType, specRoot);
        String source = plan.sourceContent();

        assertTrue(source.contains("protected io.github.jvmspec.matcher.Matchable<String> getTitle()"));
        assertTrue(source.contains("public void duringGetTitle()"));
        assertFalse(source.contains("protected io.github.jvmspec.matcher.Matchable<Book> create()"));
        assertFalse(source.contains("duringCreate"));
        assertFalse(source.contains("subject().create("));
    }

    @Test
    public void supportFileUpdateSkipsStaticFactoryMethodsInTypedProxiesAndThrowProxies() {
        String source = "package spec.com.example;\n\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpecSupport extends io.github.jvmspec.api.ObjectBehavior<Book> {\n" +
                "    public BookSpecSupport() {\n" +
                "        super(Book.class);\n" +
                "    }\n" +
                "}\n";
        DescribedType describedType = DescribedType.of(
                "com.example.Book",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.staticMethod("create", "com.example.Book"),
                        MethodDescriptor.voidMethod("setRating", Arrays.asList("int"), Arrays.asList("rating"))
                )
        );

        String updated = SpecSupportFileGenerator.updateSource(source, describedType);

        assertTrue(updated.contains("protected void setRating(int rating)"));
        assertTrue(updated.contains("public void duringSetRating(final int rating)"));
        assertFalse(updated.contains("protected io.github.jvmspec.matcher.Matchable<Book> create()"));
        assertFalse(updated.contains("duringCreate"));
        assertFalse(updated.contains("subject().create("));
    }

    @Test
    public void createsPlanWithTargetPathAndSpecContent() throws Exception {
        File specRoot = temporaryFolder.newFolder("spec-root");
        DescribedClass describedClass = DescribedClass.of("com.example.Calculator");

        SpecGenerationPlan plan = SpecSkeletonGenerator.plan(describedClass, specRoot);

        assertEquals(describedClass, plan.describedClass());
        assertEquals("spec.com.example.CalculatorSpec", plan.specQualifiedName());
        assertEquals("CalculatorSpec", plan.specSimpleName());
        assertEquals(specRoot, plan.specRoot());
        assertEquals(new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CalculatorSpec.java"), plan.targetFile());
        assertEquals("package spec.com.example;\n\n" +
                "import com.example.Calculator;\n\n" +
                "public class CalculatorSpec extends CalculatorSpecSupport {\n" +
                "    public void it_is_initializable() {\n" +
                "        shouldHaveType(Calculator.class);\n" +
                "    }\n" +
                "}\n", plan.sourceContent());
    }

    @Test
    public void supportPlanRendersTypedProxyMethodsAndThrowProxyMethods() throws Exception {
        File specRoot = temporaryFolder.newFolder("support-plan-root");
        DescribedType describedType = DescribedType.of(
                "com.example.Book",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("getRating", "int"),
                        MethodDescriptor.of("getTitle", "String"),
                        MethodDescriptor.voidMethod("setRating", Arrays.asList("int"), Arrays.asList("rating"))
                )
        );

        SpecGenerationPlan plan = SpecSkeletonGenerator.supportPlan(describedType, specRoot);

        assertEquals("spec.com.example.BookSpecSupport", plan.specQualifiedName());
        assertEquals(new File("target/generated-sources/javaspec", "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpecSupport.java"), plan.targetFile());
        String source = plan.sourceContent();
        assertTrue(source.contains("public class BookSpecSupport extends io.github.jvmspec.api.ObjectBehavior<Book>"));
        assertTrue(source.contains("public BookSpecSupport()"));
        assertTrue(source.contains("super(Book.class);"));
        assertTrue(source.contains("protected io.github.jvmspec.matcher.Matchable<Integer> getRating()"));
        assertTrue(source.contains("return match(subject().getRating());"));
        assertTrue(source.contains("protected io.github.jvmspec.matcher.Matchable<String> getTitle()"));
        assertTrue(source.contains("return match(subject().getTitle());"));
        assertTrue(source.contains("protected void setRating(int rating)"));
        assertTrue(source.contains("subject().setRating(rating);"));
        assertTrue(source.contains("@Override\n    public BookThrowExpectation shouldThrow(Class<? extends Throwable> expectedType)"));
        assertTrue(source.contains("protected class BookThrowExpectation extends io.github.jvmspec.api.ObjectBehavior.ThrowExpectation"));
        assertTrue(source.contains("public void duringSetRating(final int rating)"));
        assertTrue(source.contains("subject().setRating(rating);"));
    }

    @Test
    public void supportPlanSkipsObjectOverrideMethodsInProxiesAndThrowProxy() throws Exception {
        File specRoot = temporaryFolder.newFolder("support-object-method-root");
        DescribedType describedType = DescribedType.of(
                "com.example.NamedThing",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("name", "String"),
                        MethodDescriptor.of("equals", "boolean", Arrays.asList("Object"), Arrays.asList("other")),
                        MethodDescriptor.of("hashCode", "int"),
                        MethodDescriptor.of("toString", "String")
                )
        );

        String source = SpecSkeletonGenerator.supportPlan(describedType, specRoot).sourceContent();

        assertTrue(source.contains("protected io.github.jvmspec.matcher.Matchable<String> name()"));
        assertTrue(source.contains("public void duringName()"));
        assertFalse(source.contains("Matchable<Boolean> equals"));
        assertFalse(source.contains("Matchable<Integer> hashCode"));
        assertFalse(source.contains("Matchable<String> toString"));
        assertFalse(source.contains("duringEquals"));
        assertFalse(source.contains("duringHashCode"));
        assertFalse(source.contains("duringToString"));
    }

    @Test
    public void supportPlanDoesNotAddThrowProxyWhenOnlyObjectOverrideMethodsExist() throws Exception {
        File specRoot = temporaryFolder.newFolder("support-only-object-method-root");
        DescribedType describedType = DescribedType.of(
                "com.example.NamedThing",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("equals", "boolean", Arrays.asList("java.lang.Object"), Arrays.asList("other")),
                        MethodDescriptor.of("hashCode", "int"),
                        MethodDescriptor.of("toString", "String")
                )
        );

        String source = SpecSkeletonGenerator.supportPlan(describedType, specRoot).sourceContent();

        assertFalse(source.contains("NamedThingThrowExpectation"));
        assertFalse(source.contains("subject().equals"));
        assertFalse(source.contains("subject().hashCode"));
        assertFalse(source.contains("subject().toString"));
    }
}
