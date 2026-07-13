package io.github.jvmspec.discovery;

import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MethodDiscoveryTest {
    @Test
    public void astAndLegacyDiscoveryProduceEquivalentCallableContracts() {
        String source = "package spec.com.example;\n"
                + "public class ServiceSpec {\n"
                + "  public void it_describes_calls(String value) {\n"
                + "    beConstructedThrough(\"create\", value);\n"
                + "    name().shouldReturn(value);\n"
                + "    shouldThrow(IllegalStateException.class).duringReset();\n"
                + "    subject().save(value);\n"
                + "    setEnabled(true);\n"
                + "    shouldBeReady();\n"
                + "  }\n"
                + "}\n";

        List<MethodDescriptor> ast = MethodDiscovery.discover(
                source, "com.example", "com.example.Service", SpecCallScanner.scan(source));
        List<MethodDescriptor> legacy = MethodDiscovery.discover(
                source, "com.example", "com.example.Service", null);

        assertDescriptor(ast, "create", "com.example.Service", Arrays.asList("String"), true);
        assertDescriptor(ast, "name", "String", Arrays.<String>asList(), false);
        assertDescriptor(ast, "reset", "void", Arrays.<String>asList(), false);
        assertDescriptor(ast, "save", "void", Arrays.asList("String"), false);
        assertDescriptor(ast, "setEnabled", "void", Arrays.asList("boolean"), false);
        assertDescriptor(ast, "isReady", "boolean", Arrays.<String>asList(), false);

        assertDescriptor(legacy, "create", "com.example.Service", Arrays.asList("String"), true);
        assertDescriptor(legacy, "name", "Object", Arrays.<String>asList(), false);
        assertDescriptor(legacy, "reset", "void", Arrays.<String>asList(), false);
        assertDescriptor(legacy, "save", "void", Arrays.asList("String"), false);
        assertDescriptor(legacy, "setEnabled", "void", Arrays.asList("boolean"), false);
        assertDescriptor(legacy, "isReady", "boolean", Arrays.<String>asList(), false);
    }

    @Test
    public void frameworkLifecycleCallsAreNotProductionMethods() {
        String source = "package spec.com.example;\n"
                + "public class ServiceSpec {\n"
                + "  public void it_continues_with_a_replacement(Service replacement) {\n"
                + "    setSubject(replacement);\n"
                + "    setMatcherRegistry(null);\n"
                + "  }\n"
                + "}\n";

        List<MethodDescriptor> ast = MethodDiscovery.discover(
                source, "com.example", "com.example.Service", SpecCallScanner.scan(source));
        List<MethodDescriptor> legacy = MethodDiscovery.discover(
                source, "com.example", "com.example.Service", null);

        assertTrue(ast.isEmpty());
        assertTrue(legacy.isEmpty());
    }

    @Test
    public void variableAssignmentsToSpecHelpersAreNotProductionMethods() {
        String source = "package spec.com.example;\n"
                + "public class ServiceSpec {\n"
                + "  public void it_uses_a_spec_helper() {\n"
                + "    var replacement = helper();\n"
                + "    setSubject(replacement);\n"
                + "  }\n"
                + "  private Service helper() { return null; }\n"
                + "}\n";

        List<MethodDescriptor> methods = MethodDiscovery.discover(
                source, "com.example", "com.example.Service", SpecCallScanner.scan(source));

        assertTrue(methods.isEmpty());
    }

    @Test
    public void knownTypeEvidenceReplacesUnknownEvidenceForEquivalentSignature() {
        String source = "package spec.com.example;\n"
                + "public class ServiceSpec {\n"
                + "  public void it_describes_transform() {\n"
                + "    transform(value -> value).shouldReturn(\"done\");\n"
                + "    transform((java.util.function.Function<String, String>) value -> value)"
                + ".shouldReturn(\"done\");\n"
                + "  }\n"
                + "}\n";

        List<MethodDescriptor> methods = MethodDiscovery.discover(
                source, "com.example", "com.example.Service", SpecCallScanner.scan(source));

        MethodDescriptor transform = descriptor(methods, "transform");
        assertNotNull(transform);
        assertEquals(1, transform.parameterTypes().size());
        assertEquals("java.util.function.Function<String, String>", transform.parameterTypes().get(0));
        assertTrue(!transform.isParameterTypeUnknown(0));
    }

    private static void assertDescriptor(
            List<MethodDescriptor> methods,
            String name,
            String returnType,
            List<String> parameterTypes,
            boolean isStatic
    ) {
        MethodDescriptor descriptor = descriptor(methods, name);
        assertNotNull(name, descriptor);
        assertEquals(returnType, descriptor.returnType());
        assertEquals(parameterTypes, descriptor.parameterTypes());
        assertEquals(isStatic, descriptor.isStatic());
    }

    private static MethodDescriptor descriptor(List<MethodDescriptor> methods, String name) {
        for (int i = 0; i < methods.size(); i++) {
            if (name.equals(methods.get(i).methodName())) {
                return methods.get(i);
            }
        }
        return null;
    }
}
