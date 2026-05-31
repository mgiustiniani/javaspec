package org.javaspec.generation;

import org.javaspec.discovery.SpecNamingConvention;
import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class SpecSkeletonGeneratorNamingTest {

    // ── Default overload unchanged ──────────────────────────────────

    @Test
    public void defaultPlanProducesSpecOrgExamplePackageForOrgExampleBook() {
        File specRoot = new File("target/specs");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        SpecGenerationPlan plan = SpecSkeletonGenerator.plan(describedClass, specRoot);

        assertEquals("spec.org.example.BookSpec", plan.specQualifiedName());
        assertEquals("BookSpec", plan.specSimpleName());
        File expectedTarget = new File(specRoot, "spec" + File.separator + "org" + File.separator + "example" + File.separator + "BookSpec.java");
        assertEquals(expectedTarget, plan.targetFile());
    }

    @Test
    public void defaultPlanProducesSpecPackageForUnqualifiedCalculator() {
        File specRoot = new File("target/specs");
        DescribedClass describedClass = DescribedClass.of("Calculator");

        SpecGenerationPlan plan = SpecSkeletonGenerator.plan(describedClass, specRoot);

        assertEquals("spec.CalculatorSpec", plan.specQualifiedName());
        assertEquals("CalculatorSpec", plan.specSimpleName());
        File expectedTarget = new File(specRoot, "spec" + File.separator + "CalculatorSpec.java");
        assertEquals(expectedTarget, plan.targetFile());
    }

    @Test
    public void defaultPlanDefaultOverloadIsUnchanged() {
        File specRoot = new File("target/specs");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        SpecGenerationPlan planDefault = SpecSkeletonGenerator.plan(describedClass, specRoot);
        SpecGenerationPlan planExplicit = SpecSkeletonGenerator.plan(describedClass, specRoot, SpecNamingConvention.defaults());

        assertEquals(planDefault.specQualifiedName(), planExplicit.specQualifiedName());
        assertEquals(planDefault.targetFile(), planExplicit.targetFile());
    }

    @Test
    public void defaultPlanDescribedTypeOverloadIsUnchanged() {
        File specRoot = new File("target/specs");
        DescribedType describedType = DescribedType.of("org.example.Book", JavaTypeKind.CLASS);

        SpecGenerationPlan planDefault = SpecSkeletonGenerator.plan(describedType, specRoot);
        SpecGenerationPlan planExplicit = SpecSkeletonGenerator.plan(describedType, specRoot, SpecNamingConvention.defaults());

        assertEquals(planDefault.specQualifiedName(), planExplicit.specQualifiedName());
        assertEquals(planDefault.targetFile(), planExplicit.targetFile());
    }

    @Test
    public void defaultSupportPlanDefaultOverloadIsUnchanged() {
        File specRoot = new File("target/specs");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        SpecGenerationPlan planDefault = SpecSkeletonGenerator.supportPlan(describedClass, specRoot);
        SpecGenerationPlan planExplicit = SpecSkeletonGenerator.supportPlan(describedClass, specRoot, SpecNamingConvention.defaults());

        assertEquals(planDefault.specQualifiedName(), planExplicit.specQualifiedName());
        assertEquals(planDefault.targetFile(), planExplicit.targetFile());
    }

    // ── Custom naming convention ────────────────────────────────────

    @Test
    public void customConventionPlanUsesSpecPackageOnlyForOrgExampleBook() {
        File specRoot = new File("target/specs");
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        SpecGenerationPlan plan = SpecSkeletonGenerator.plan(describedClass, specRoot, custom);

        assertEquals("spec.BookSpec", plan.specQualifiedName());
        assertEquals("BookSpec", plan.specSimpleName());
        File expectedTarget = new File(specRoot, "spec" + File.separator + "BookSpec.java");
        assertEquals(expectedTarget, plan.targetFile());
    }

    @Test
    public void customConventionPlanUsesSpecDomainForSubPackage() {
        File specRoot = new File("target/specs");
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.domain.Book");

        SpecGenerationPlan plan = SpecSkeletonGenerator.plan(describedClass, specRoot, custom);

        assertEquals("spec.domain.BookSpec", plan.specQualifiedName());
        assertEquals("BookSpec", plan.specSimpleName());
        File expectedTarget = new File(specRoot, "spec" + File.separator + "domain" + File.separator + "BookSpec.java");
        assertEquals(expectedTarget, plan.targetFile());
    }

    @Test
    public void customConventionSupportPlanUsesSpecPackageOnlyForOrgExampleBook() {
        File specRoot = new File("target/specs");
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        SpecGenerationPlan plan = SpecSkeletonGenerator.supportPlan(describedClass, specRoot, custom);

        assertEquals("spec.BookSpecSupport", plan.specQualifiedName());
        assertEquals("BookSpecSupport", plan.specSimpleName());
        File expectedTarget = new File(specRoot, "spec" + File.separator + "BookSpecSupport.java");
        assertEquals(expectedTarget, plan.targetFile());
    }

    @Test
    public void customConventionSupportPlanUsesSpecDomainForSubPackage() {
        File specRoot = new File("target/specs");
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.domain.Book");

        SpecGenerationPlan plan = SpecSkeletonGenerator.supportPlan(describedClass, specRoot, custom);

        assertEquals("spec.domain.BookSpecSupport", plan.specQualifiedName());
        assertEquals("BookSpecSupport", plan.specSimpleName());
        File expectedTarget = new File(specRoot, "spec" + File.separator + "domain" + File.separator + "BookSpecSupport.java");
        assertEquals(expectedTarget, plan.targetFile());
    }

    // ── Custom convention with DescribedType overload ────────────────

    @Test
    public void customConventionPlanWithDescribedTypeUsesCorrectPackage() {
        File specRoot = new File("target/specs");
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedType describedType = DescribedType.of("org.example.Book", JavaTypeKind.CLASS);

        SpecGenerationPlan plan = SpecSkeletonGenerator.plan(describedType, specRoot, custom);

        assertEquals("spec.BookSpec", plan.specQualifiedName());
        assertEquals("BookSpec", plan.specSimpleName());
        File expectedTarget = new File(specRoot, "spec" + File.separator + "BookSpec.java");
        assertEquals(expectedTarget, plan.targetFile());
    }

    // ── Render overloads ────────────────────────────────────────────

    @Test
    public void renderDefaultOverloadIsUnchanged() {
        String defaultResult = SpecSkeletonGenerator.render(DescribedClass.of("org.example.Book"));
        String explicitResult = SpecSkeletonGenerator.render(DescribedClass.of("org.example.Book"),
                SpecNamingConvention.defaults());

        assertEquals(defaultResult, explicitResult);
    }

    @Test
    public void renderUsesCustomConventionPackage() {
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        String source = SpecSkeletonGenerator.render(DescribedClass.of("org.example.Book"), custom);

        assertEquals("package spec;\n\n" +
                "import org.example.Book;\n\n" +
                "public class BookSpec extends BookSpecSupport {\n" +
                "    public void it_is_initializable() {\n" +
                "        shouldHaveType(Book.class);\n" +
                "    }\n" +
                "}\n", source);
    }

    @Test
    public void renderDefaultDescribedTypeOverloadIsUnchanged() {
        DescribedType describedType = DescribedType.of("org.example.Book", JavaTypeKind.CLASS);
        String defaultResult = SpecSkeletonGenerator.render(describedType);
        String explicitResult = SpecSkeletonGenerator.render(describedType, SpecNamingConvention.defaults());

        assertEquals(defaultResult, explicitResult);
    }

    @Test
    public void renderSupportDefaultOverloadIsUnchanged() {
        DescribedType describedType = DescribedType.of("org.example.Book", JavaTypeKind.CLASS);
        String defaultResult = SpecSkeletonGenerator.renderSupport(describedType);
        String explicitResult = SpecSkeletonGenerator.renderSupport(describedType, SpecNamingConvention.defaults());

        assertEquals(defaultResult, explicitResult);
    }

    // ── Static helper overloads ─────────────────────────────────────

    @Test
    public void specSimpleNameHelperWithCustomConvention() {
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        assertEquals("BookSpec", SpecSkeletonGenerator.specSimpleName(describedClass, custom));
    }

    @Test
    public void supportSimpleNameHelperWithCustomConvention() {
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        assertEquals("BookSpecSupport", SpecSkeletonGenerator.supportSimpleName(describedClass, custom));
    }

    @Test
    public void specQualifiedNameHelperWithCustomConvention() {
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        assertEquals("spec.BookSpec", SpecSkeletonGenerator.specQualifiedName(describedClass, custom));
    }

    @Test
    public void supportQualifiedNameHelperWithCustomConvention() {
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        assertEquals("spec.BookSpecSupport", SpecSkeletonGenerator.supportQualifiedName(describedClass, custom));
    }

    @Test
    public void sourceRelativePathHelperWithCustomConvention() {
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");
        String separator = File.separator;

        assertEquals("spec" + separator + "BookSpec.java",
                SpecSkeletonGenerator.sourceRelativePath(describedClass, custom));
    }

    @Test
    public void supportSourceRelativePathHelperWithCustomConvention() {
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");
        String separator = File.separator;

        assertEquals("spec" + separator + "BookSpecSupport.java",
                SpecSkeletonGenerator.supportSourceRelativePath(describedClass, custom));
    }

    @Test
    public void specPackageNameHelperWithCustomConvention() {
        SpecNamingConvention custom = SpecNamingConvention.of("spec", "org.example");
        DescribedClass describedClass = DescribedClass.of("org.example.Book");

        assertEquals("spec", SpecSkeletonGenerator.specPackageName(describedClass, custom));
    }
}
