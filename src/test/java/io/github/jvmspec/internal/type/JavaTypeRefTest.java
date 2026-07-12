package io.github.jvmspec.internal.type;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JavaTypeRefTest {
    @Test
    public void resolvesNestedParameterizedArraysAndWildcardBoundsRecursively() {
        Map<String, String> imports = new LinkedHashMap<String, String>();
        imports.put("Map", "java.util.Map");
        imports.put("List", "java.util.List");
        imports.put("KeyUsage", "com.example.KeyUsage");

        JavaTypeRef type = JavaTypeRef.resolve(
                "Map<String, List<? extends KeyUsage[]>>", imports, "spec.com.example");

        assertEquals("java.util.Map<String, java.util.List<? extends com.example.KeyUsage[]>>",
                type.canonicalName());
    }

    @Test
    public void structuralEquivalencePreservesEveryGenericArgument() {
        JavaTypeRef publicKeys = JavaTypeRef.parseCanonical(
                "java.util.List<com.example.SubjectPublicKeyProfile>");
        JavaTypeRef simplePublicKeys = JavaTypeRef.parseCanonical(
                "List<SubjectPublicKeyProfile>");
        JavaTypeRef keyUsages = JavaTypeRef.parseCanonical(
                "java.util.List<com.example.KeyUsage>");

        assertEquals(true, publicKeys.structurallyEquivalent(simplePublicKeys));
        assertEquals(false, publicKeys.structurallyEquivalent(keyUsages));
    }

    @Test
    public void structuralEquivalencePreservesNestedWildcardsAndArrays() {
        JavaTypeRef left = JavaTypeRef.parseCanonical(
                "java.util.Map<String, ? extends com.example.KeyUsage[]>[]");
        JavaTypeRef same = JavaTypeRef.parseCanonical(
                "Map<java.lang.String, ? extends KeyUsage[]>[]");
        JavaTypeRef different = JavaTypeRef.parseCanonical(
                "Map<String, ? super KeyUsage[]>[]");

        assertEquals(true, left.structurallyEquivalent(same));
        assertEquals(false, left.structurallyEquivalent(different));
    }

    @Test
    public void importPlanImportsNestedGenericArgumentsDeterministically() {
        JavaTypeImportPlan plan = JavaTypeImportPlan.forTypes(
                "spec.com.example",
                Arrays.asList("java.util.List<com.example.SubjectPublicKeyProfile>"));

        assertEquals(Arrays.asList("com.example.SubjectPublicKeyProfile", "java.util.List"), plan.imports());
        assertEquals("List<SubjectPublicKeyProfile>",
                plan.render("java.util.List<com.example.SubjectPublicKeyProfile>"));
    }

    @Test
    public void collidingSimpleNamesRemainFullyQualifiedAndAreNotImported() {
        JavaTypeImportPlan plan = JavaTypeImportPlan.forTypes(
                "spec.com.example",
                Arrays.asList("java.util.Map<com.alpha.Token, com.beta.Token>"));

        assertEquals(Arrays.asList("java.util.Map"), plan.imports());
        assertFalse(plan.imports().contains("com.alpha.Token"));
        assertFalse(plan.imports().contains("com.beta.Token"));
        assertEquals("Map<com.alpha.Token, com.beta.Token>",
                plan.render("java.util.Map<com.alpha.Token, com.beta.Token>"));
    }
}
