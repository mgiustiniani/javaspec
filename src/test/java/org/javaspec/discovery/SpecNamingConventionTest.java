package org.javaspec.discovery;

import org.javaspec.model.DescribedClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SpecNamingConventionTest {

    // ── Default convention mapping ──────────────────────────────────

    @Test
    public void defaultConventionMapsOrgExampleBookToSpecOrgExampleBookSpec() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("spec.org.example.BookSpec", convention.specQualifiedName("org.example.Book"));
        assertEquals("spec.org.example.BookSpecSupport", convention.supportQualifiedName("org.example.Book"));
    }

    @Test
    public void defaultConventionMapsUnqualifiedCalculatorToSpecCalculatorSpec() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("spec.CalculatorSpec", convention.specQualifiedName("Calculator"));
        assertEquals("spec.CalculatorSpecSupport", convention.supportQualifiedName("Calculator"));
    }

    @Test
    public void defaultConventionMapsSpecSourceRelativePathForUnqualifiedCalculator() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("spec" + java.io.File.separator + "CalculatorSpec.java",
                convention.specSourceRelativePath("Calculator"));
    }

    // ── Default convention round-trip ────────────────────────────────

    @Test
    public void defaultConventionRoundTripsSpecOrgExampleBookSpecBackToOrgExampleBook() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("org.example.Book", convention.describedQualifiedName("spec.org.example.BookSpec"));
    }

    @Test
    public void defaultConventionRoundTripsRootLevelCalculatorSpecBackToCalculator() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("Calculator", convention.describedQualifiedName("spec.CalculatorSpec"));
    }

    // ── Production prefix convention ────────────────────────────────

    @Test
    public void withProductionPrefixMapsOrgExampleBookToSpecBookSpec() {
        SpecNamingConvention convention = SpecNamingConvention.of("spec", "org.example");

        assertEquals("spec.BookSpec", convention.specQualifiedName("org.example.Book"));
        assertEquals("spec.BookSpecSupport", convention.supportQualifiedName("org.example.Book"));
    }

    @Test
    public void withProductionPrefixMapsOrgExampleDomainBookToSpecDomainBookSpec() {
        SpecNamingConvention convention = SpecNamingConvention.of("spec", "org.example");

        assertEquals("spec.domain.BookSpec", convention.specQualifiedName("org.example.domain.Book"));
        assertEquals("spec.domain.BookSpecSupport", convention.supportQualifiedName("org.example.domain.Book"));
    }

    @Test
    public void withProductionPrefixRoundTripsSpecDomainBookSpecToOrgExampleDomainBook() {
        SpecNamingConvention convention = SpecNamingConvention.of("spec", "org.example");

        assertEquals("org.example.domain.Book", convention.describedQualifiedName("spec.domain.BookSpec"));
    }

    @Test
    public void withProductionPrefixRejectsDescribedTypeOutsideConfiguredPrefix() {
        SpecNamingConvention convention = SpecNamingConvention.of("spec", "org.example");

        try {
            convention.specQualifiedName("com.other.Book");
            fail("Expected IllegalArgumentException for type outside production prefix");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not under production package prefix"));
        }
    }

    // ── Package name computation ────────────────────────────────────

    @Test
    public void defaultConventionComputesSpecPackageNameForPackagedType() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("spec.org.example", convention.specPackageName("org.example.Book"));
    }

    @Test
    public void defaultConventionComputesSpecPackageNameForUnqualifiedType() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("spec", convention.specPackageName("Calculator"));
    }

    @Test
    public void withProductionPrefixComputesSpecPackageNameForExactPrefixMatch() {
        SpecNamingConvention convention = SpecNamingConvention.of("spec", "org.example");

        assertEquals("spec", convention.specPackageName("org.example.Book"));
    }

    @Test
    public void withProductionPrefixComputesSpecPackageNameForSubPackage() {
        SpecNamingConvention convention = SpecNamingConvention.of("spec", "org.example");

        assertEquals("spec.domain", convention.specPackageName("org.example.domain.Book"));
    }

    // ── Source relative paths ───────────────────────────────────────

    @Test
    public void defaultConventionComputesSpecSourceRelativePath() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();
        String separator = java.io.File.separator;

        assertEquals("spec" + separator + "org" + separator + "example" + separator + "BookSpec.java",
                convention.specSourceRelativePath("org.example.Book"));
    }

    @Test
    public void withProductionPrefixComputesSpecSourceRelativePath() {
        SpecNamingConvention convention = SpecNamingConvention.of("spec", "org.example");
        String separator = java.io.File.separator;

        assertEquals("spec" + separator + "BookSpec.java",
                convention.specSourceRelativePath("org.example.Book"));
    }

    // ── Source path reverse mapping ─────────────────────────────────

    @Test
    public void defaultConventionReverseMapsSpecSourcePathToDescribedQualifiedName() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();
        String separator = java.io.File.separator;

        String described = convention.describedQualifiedNameForSpecSourcePath(
                "spec" + separator + "org" + separator + "example" + separator + "BookSpec.java");
        assertEquals("org.example.Book", described);
    }

    // ── Simple name checks ──────────────────────────────────────────

    @Test
    public void isSpecSimpleNameDetectsSpecSuffix() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertTrue(convention.isSpecSimpleName("BookSpec"));
        assertFalse(convention.isSpecSimpleName("Book"));
        assertFalse(convention.isSpecSimpleName("Spec"));
        assertFalse(convention.isSpecSimpleName(null));
    }

    @Test
    public void isSupportSimpleNameDetectsSupportSuffix() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertTrue(convention.isSupportSimpleName("BookSpecSupport"));
        assertFalse(convention.isSupportSimpleName("Book"));
        assertFalse(convention.isSupportSimpleName("SpecSupport"));
        assertFalse(convention.isSupportSimpleName(null));
    }

    @Test
    public void isSpecSourceFileNameDetectsSpecJavaFiles() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertTrue(convention.isSpecSourceFileName("BookSpec.java"));
        assertFalse(convention.isSpecSourceFileName("Book.java"));
        assertFalse(convention.isSpecSourceFileName("Spec.java"));
        assertFalse(convention.isSpecSourceFileName(null));
    }

    @Test
    public void isSupportSourceFileNameDetectsSupportJavaFiles() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertTrue(convention.isSupportSourceFileName("BookSpecSupport.java"));
        assertFalse(convention.isSupportSourceFileName("Book.java"));
        assertFalse(convention.isSupportSourceFileName("SpecSupport.java"));
        assertFalse(convention.isSupportSourceFileName(null));
    }

    // ── Spec qualified name for source path ─────────────────────────

    @Test
    public void specQualifiedNameForSourcePathReturnsCorrectQualifiedName() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();
        String separator = java.io.File.separator;

        String qualified = convention.specQualifiedNameForSourcePath(
                "spec" + separator + "org" + separator + "example" + separator + "BookSpec.java");
        assertEquals("spec.org.example.BookSpec", qualified);
    }

    @Test
    public void supportQualifiedNameForSourcePathReturnsCorrectQualifiedName() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();
        String separator = java.io.File.separator;

        String qualified = convention.supportQualifiedNameForSourcePath(
                "spec" + separator + "org" + separator + "example" + separator + "BookSpecSupport.java");
        assertEquals("spec.org.example.BookSpecSupport", qualified);
    }

    // ── Support round-trip ──────────────────────────────────────────

    @Test
    public void defaultConventionRoundTripsSupportQualifiedName() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("org.example.Book",
                convention.describedQualifiedNameForSupport("spec.org.example.BookSpecSupport"));
    }

    // ── Equality and hash ───────────────────────────────────────────

    @Test
    public void equalsBasedOnPackagePrefixes() {
        SpecNamingConvention a = SpecNamingConvention.defaults();
        SpecNamingConvention b = SpecNamingConvention.defaults();
        SpecNamingConvention c = SpecNamingConvention.of("spec", "org.example");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
    }

    // ── From suite configuration ────────────────────────────────────

    @Test
    public void fromSuiteConfigurationCreatesConvention() {
        org.javaspec.config.JavaspecSuiteConfiguration config =
                org.javaspec.config.JavaspecSuiteConfiguration.of(
                        "test-suite", "target/specs", "src/main/java",
                        "spec", "org.example",
                        java.util.Collections.<String>emptyList()
                );

        SpecNamingConvention convention = SpecNamingConvention.from(config);

        assertEquals("spec", convention.specPackagePrefix());
        assertEquals("org.example", convention.productionPackagePrefix());
    }

    // ── Simple name helpers ─────────────────────────────────────────

    @Test
    public void specSimpleNameReturnsSimpleNameWithSpecSuffix() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("BookSpec", convention.specSimpleName("org.example.Book"));
        assertEquals("CalculatorSpec", convention.specSimpleName("Calculator"));
    }

    @Test
    public void supportSimpleNameReturnsSimpleNameWithSupportSuffix() {
        SpecNamingConvention convention = SpecNamingConvention.defaults();

        assertEquals("BookSpecSupport", convention.supportSimpleName("org.example.Book"));
    }
}
