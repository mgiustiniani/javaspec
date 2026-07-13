package io.github.jvmspec.discovery;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JavaInferenceComponentsTest {
    @Test
    public void expressionArgumentsDoNotTreatRelationalAnglesAsGenericDelimiters() {
        assertEquals(
                Arrays.asList("left < right", "choose(first, second)", "tail"),
                JavaExpressionArguments.split(
                        "left < right, choose(first, second), tail")
        );
    }

    @Test
    public void sourceContextTreatsAnglesAsGenericDelimitersInDeclarations() {
        String source = "package spec.com.example;\n"
                + "import java.util.Map;\n"
                + "public class SubjectSpec {\n"
                + "  public void it_uses(Map<String, Integer> values, String[] names) { }\n"
                + "}\n";
        Map<String, String> imports = JavaSourceContext.importsBySimpleName(source);

        JavaExpressionTypeInference.MethodParameterInfo method =
                JavaSourceContext.parseMethods(source, imports, "com.example").get("it_uses");

        assertEquals(Arrays.asList("java.util.Map<String, Integer>", "String[]"), method.types);
        assertEquals(Arrays.asList("values", "names"), method.names);
    }

    @Test
    public void literalInferenceSeparatesKnownFactoriesConstantsAndUnknownValues() {
        Map<String, String> imports = Collections.singletonMap("Widget", "com.acme.Widget");

        JavaExpressionTypeInference.InferredType factory = JavaLiteralTypeInference.infer(
                "Widget.create()", imports, "com.example");
        JavaExpressionTypeInference.InferredType constant = JavaLiteralTypeInference.infer(
                "Widget.READY", imports, "com.example");
        JavaExpressionTypeInference.InferredType unknown = JavaLiteralTypeInference.infer(
                "lookup()", imports, "com.example");

        assertEquals("com.acme.Widget", factory.typeName);
        assertFalse(factory.unknown);
        assertEquals("com.acme.Widget", constant.typeName);
        assertFalse(constant.unknown);
        assertEquals("Object", unknown.typeName);
        assertTrue(unknown.unknown);
    }
}
