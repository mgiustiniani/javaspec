package org.javaspec.generation;

import org.javaspec.model.DescribedClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class SpecSkeletonGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void rendersPackagedSpecSkeleton() {
        String source = SpecSkeletonGenerator.render(DescribedClass.of("com.example.Calculator"));

        assertEquals("package spec.com.example;\n\n" +
                "import com.example.Calculator;\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class CalculatorSpec extends ObjectBehavior<Calculator> {\n" +
                "    public void it_is_initializable() {\n" +
                "        shouldHaveType(Calculator.class);\n" +
                "    }\n" +
                "}\n", source);
    }

    @Test
    public void rendersDefaultPackageSpecSkeleton() {
        String source = SpecSkeletonGenerator.render(DescribedClass.of("Calculator"));

        assertEquals("import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class CalculatorSpec extends ObjectBehavior<Calculator> {\n" +
                "    public void it_is_initializable() {\n" +
                "        shouldHaveType(Calculator.class);\n" +
                "    }\n" +
                "}\n", source);
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
                "import com.example.Calculator;\n" +
                "import org.javaspec.api.ObjectBehavior;\n\n" +
                "public class CalculatorSpec extends ObjectBehavior<Calculator> {\n" +
                "    public void it_is_initializable() {\n" +
                "        shouldHaveType(Calculator.class);\n" +
                "    }\n" +
                "}\n", plan.sourceContent());
    }
}
