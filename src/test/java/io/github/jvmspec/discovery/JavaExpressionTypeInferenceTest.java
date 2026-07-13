package io.github.jvmspec.discovery;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JavaExpressionTypeInferenceTest {
    @Test
    public void splitsTopLevelJavaArgumentsWithoutSplittingNestedExpressions() {
        List<String> arguments = JavaExpressionTypeInference.splitArguments(
                "call(1, 2), new int[] { 3, 4 }, \"a,b\", value -> consume(value, 5)"
        );

        assertEquals(Arrays.asList(
                "call(1, 2)",
                "new int[] { 3, 4 }",
                "\"a,b\"",
                "value -> consume(value, 5)"
        ), arguments);
    }

    @Test
    public void infersResolvedStructuredJavaTypesAndPreservesUnknownEvidence() {
        Map<String, String> imports = Collections.singletonMap("List", "java.util.List");

        JavaExpressionTypeInference.InferredType list =
                JavaExpressionTypeInference.inferLiteralType(
                        "List." + "of(\"first\", \"second\")", imports, "com.example");
        JavaExpressionTypeInference.InferredType array =
                JavaExpressionTypeInference.inferLiteralType(
                        "new Widget[2][]", Collections.<String, String>emptyMap(), "com.example");
        JavaExpressionTypeInference.InferredType unknown =
                JavaExpressionTypeInference.inferLiteralType(
                        "computeValue()", imports, "com.example");

        assertEquals("java.util.List<String>", list.typeName);
        assertFalse(list.unknown);
        assertEquals("com.example.Widget[][]", array.typeName);
        assertFalse(array.unknown);
        assertEquals("Object", unknown.typeName);
        assertTrue(unknown.unknown);
    }

    @Test
    public void parsesMethodParameterContextWithQualifiedGenericAndArrayTypes() {
        String source = "package spec.com.example;\n"
                + "import java.util.Map;\n"
                + "public class SubjectSpec {\n"
                + "  public void it_uses(Map<String, Integer> values, Widget[] widgets) { }\n"
                + "}\n";
        Map<String, String> imports = JavaExpressionTypeInference.importsBySimpleName(source);

        Map<String, JavaExpressionTypeInference.MethodParameterInfo> methods =
                JavaExpressionTypeInference.parseMethods(source, imports, "com.example");
        JavaExpressionTypeInference.MethodParameterInfo info = methods.get("it_uses");

        assertEquals(Arrays.asList(
                "java.util.Map<String, Integer>",
                "com.example.Widget[]"
        ), info.types);
        assertEquals(Arrays.asList("values", "widgets"), info.names);
        assertEquals(2, info.formalParameterCount);
    }
}
