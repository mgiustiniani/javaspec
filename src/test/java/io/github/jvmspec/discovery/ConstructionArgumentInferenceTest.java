package io.github.jvmspec.discovery;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ConstructionArgumentInferenceTest {
    @Test
    public void prefersNamedMethodParametersAndDerivesQualifiedConstantNames() {
        JavaExpressionTypeInference.MethodParameterInfo method =
                new JavaExpressionTypeInference.MethodParameterInfo(
                        Arrays.asList("java.lang.String", "int"),
                        Arrays.asList("name", "count"),
                        2
                );

        ConstructionArgumentInference.Arguments named = ConstructionArgumentInference.infer(
                Arrays.asList("name", "count"),
                method,
                Collections.<String, String>emptyMap(),
                "com.example"
        );
        ConstructionArgumentInference.Arguments constant = ConstructionArgumentInference.infer(
                Arrays.asList("Algorithm.EC_P256"),
                null,
                Collections.<String, String>emptyMap(),
                "com.example"
        );

        assertEquals(Arrays.asList("java.lang.String", "int"), named.parameterTypes);
        assertEquals(Arrays.asList("name", "count"), named.parameterNames);
        assertEquals(Arrays.asList("com.example.Algorithm"), constant.parameterTypes);
        assertEquals(Arrays.asList("algorithm"), constant.parameterNames);
    }

    @Test
    public void retainsLegacyPositionalFormalParameterEvidenceForExpressions() {
        JavaExpressionTypeInference.MethodParameterInfo method =
                new JavaExpressionTypeInference.MethodParameterInfo(
                        Arrays.asList("java.util.Map<String, String>"),
                        Arrays.asList("normalizedFields"),
                        1
                );

        ConstructionArgumentInference.Arguments arguments = ConstructionArgumentInference.infer(
                Arrays.asList("new java.util.LinkedHashMap<String, String>()"),
                method,
                Collections.<String, String>emptyMap(),
                "com.example"
        );

        assertEquals(Arrays.asList("java.util.Map<String, String>"), arguments.parameterTypes);
        assertEquals(Arrays.asList("normalizedFields"), arguments.parameterNames);
    }
}
