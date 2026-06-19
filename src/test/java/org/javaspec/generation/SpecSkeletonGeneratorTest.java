package org.javaspec.generation;

import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.model.MethodDescriptor;
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
                "public class CalculatorSpecSupport extends org.javaspec.api.ObjectBehavior<Calculator> {\n" +
                "    public CalculatorSpecSupport() {\n" +
                "        super(Calculator.class);\n" +
                "    }\n" +
                "}\n", source);
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
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.staticMethod("create", "com.example.Book"),
                        MethodDescriptor.of("getTitle", "String")
                )
        );

        SpecGenerationPlan plan = SpecSkeletonGenerator.supportPlan(describedType, specRoot);
        String source = plan.sourceContent();

        assertTrue(source.contains("protected org.javaspec.matcher.Matchable<String> getTitle()"));
        assertTrue(source.contains("public void duringGetTitle()"));
        assertFalse(source.contains("protected org.javaspec.matcher.Matchable<Book> create()"));
        assertFalse(source.contains("duringCreate"));
        assertFalse(source.contains("subject().create("));
    }

    @Test
    public void supportFileUpdateSkipsStaticFactoryMethodsInTypedProxiesAndThrowProxies() {
        String source = "package spec.com.example;\n\n" +
                "import com.example.Book;\n\n" +
                "public class BookSpecSupport extends org.javaspec.api.ObjectBehavior<Book> {\n" +
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
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.staticMethod("create", "com.example.Book"),
                        MethodDescriptor.voidMethod("setRating", Arrays.asList("int"), Arrays.asList("rating"))
                )
        );

        String updated = SpecSupportFileGenerator.updateSource(source, describedType);

        assertTrue(updated.contains("protected void setRating(int rating)"));
        assertTrue(updated.contains("public void duringSetRating(final int rating)"));
        assertFalse(updated.contains("protected org.javaspec.matcher.Matchable<Book> create()"));
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
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
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
        assertTrue(source.contains("public class BookSpecSupport extends org.javaspec.api.ObjectBehavior<Book>"));
        assertTrue(source.contains("public BookSpecSupport()"));
        assertTrue(source.contains("super(Book.class);"));
        assertTrue(source.contains("protected org.javaspec.matcher.Matchable<Integer> getRating()"));
        assertTrue(source.contains("return match(subject().getRating());"));
        assertTrue(source.contains("protected org.javaspec.matcher.Matchable<String> getTitle()"));
        assertTrue(source.contains("return match(subject().getTitle());"));
        assertTrue(source.contains("protected void setRating(int rating)"));
        assertTrue(source.contains("subject().setRating(rating);"));
        assertTrue(source.contains("@Override\n    public BookThrowExpectation shouldThrow(Class<? extends Throwable> expectedType)"));
        assertTrue(source.contains("protected class BookThrowExpectation extends org.javaspec.api.ObjectBehavior.ThrowExpectation"));
        assertTrue(source.contains("public void duringSetRating(final int rating)"));
        assertTrue(source.contains("subject().setRating(rating);"));
    }
}
