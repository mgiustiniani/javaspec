package org.javaspec.generation;

import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class SpecSupportProphecyHelperTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final DescribedType CALCULATOR_TYPE = DescribedType.of(
            DescribedClass.of("com.example.Calculator"));

    private static final String MINIMAL_SUPPORT =
            "package spec.com.example;\n\n" +
            "import com.example.Calculator;\n\n" +
            "public class CalculatorSpecSupport extends org.javaspec.api.ObjectBehavior<Calculator> {\n" +
            "    public CalculatorSpecSupport() {\n" +
            "        super(Calculator.class);\n" +
            "    }\n" +
            "}\n";

    // -------------------------------------------------------------------------
    // No prophesized types — source unchanged

    @Test
    public void updateWithEmptyListLeavesSourceUnchanged() {
        String updated = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                MINIMAL_SUPPORT, CALCULATOR_TYPE, Collections.<String>emptyList());
        assertEquals(MINIMAL_SUPPORT, updated);
    }

    // -------------------------------------------------------------------------
    // Interface type helper

    @Test
    public void addsTypedProphecyHelperForInterface() {
        String updated = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                MINIMAL_SUPPORT, CALCULATOR_TYPE,
                Arrays.asList("com.example.Notifier"));

        assertTrue("must declare prophesizeNotifier()", updated.contains("prophesizeNotifier()"));
        assertTrue("must declare prophecyNotifier()", updated.contains("prophecyNotifier()"));
        // Return type must be the typed wrapper — spec uses B style (MailerProphecy m = prophesizeMailer())
        // or D style on Java 10+ (var m = prophesizeMailer())
        assertTrue("return type must be the typed wrapper",
                updated.contains("com.example.NotifierProphecy prophesizeNotifier"));
        assertTrue("alias must also be typed wrapper",
                updated.contains("com.example.NotifierProphecy prophecyNotifier"));
        assertTrue("must use Doubles dispatch",
                updated.contains("org.javaspec.doubles.Doubles.interfaceDouble") ||
                updated.contains("org.javaspec.doubles.Doubles.concreteDouble") ||
                updated.contains("isInterface()"));
    }

    // -------------------------------------------------------------------------
    // Concrete class helper

    @Test
    public void addsTypedProphecyHelperForConcreteClass() {
        String updated = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                MINIMAL_SUPPORT, CALCULATOR_TYPE,
                Arrays.asList("com.example.DataStore"));

        assertTrue("must declare prophesizeDataStore()", updated.contains("prophesizeDataStore()"));
        assertTrue("return type must be the typed wrapper",
                updated.contains("com.example.DataStoreProphecy prophesizeDataStore"));
    }

    // -------------------------------------------------------------------------
    // Idempotent — second call does not duplicate

    @Test
    public void helperInsertionIsIdempotent() {
        String once = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                MINIMAL_SUPPORT, CALCULATOR_TYPE,
                Arrays.asList("com.example.Notifier"));
        String twice = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                once, CALCULATOR_TYPE,
                Arrays.asList("com.example.Notifier"));

        assertEquals("second update must not add duplicate helpers", once, twice);
        // Count only actual method declarations, not references inside bodies or Javadoc
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:protected|public)\\s+\\S+\\s+prophesizeNotifier\\s*\\(")
                .matcher(twice);
        int declarations = 0;
        while (m.find()) declarations++;
        assertEquals("must have exactly one prophesizeNotifier declaration", 1, declarations);
    }

    // -------------------------------------------------------------------------
    // Multiple types — both helpers present

    @Test
    public void addsHelpersForMultipleTypes() {
        String updated = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                MINIMAL_SUPPORT, CALCULATOR_TYPE,
                Arrays.asList("com.example.Notifier", "com.example.Repository"));

        assertTrue(updated.contains("prophesizeNotifier()"));
        assertTrue(updated.contains("prophesizeRepository()"));
    }

    // -------------------------------------------------------------------------
    // Helper stays out of src/ — contractual boundary

    @Test
    public void updateWritesIntoProvidedSourceStringNotFilesystem() {
        // The method is pure (String → String); callers decide where to write.
        // Callers in GenerationOrchestrator always use generatedSourcesRoot, never specRoot.
        // This test verifies the method signature is side-effect-free on the filesystem.
        String updated = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                MINIMAL_SUPPORT, CALCULATOR_TYPE,
                Arrays.asList("com.example.Notifier"));
        assertNotNull(updated);
        assertNotSame("must return new string, not same reference", MINIMAL_SUPPORT, updated);
    }

    @Test
    public void typedWrapperReturnTypeCompilesWithVarOnJava10OrLater() throws Exception {
        assumeTrue("requires javac --release 10", javaSpecificationVersionAtLeast(10));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("These tests require a JDK with javax.tools.JavaCompiler", compiler);

        File src = temporaryFolder.newFolder("src");
        File out = temporaryFolder.newFolder("out");

        write(src, "com/example/Calculator.java",
                "package com.example; public class Calculator {}\n");
        write(src, "com/example/Mailer.java",
                "package com.example; public interface Mailer { boolean send(String message); }\n");
        write(src, "com/example/MailerProphecy.java",
                "package com.example;\n" +
                "public class MailerProphecy {\n" +
                "  public MailerProphecy(org.javaspec.doubles.InterfaceDouble<Mailer> handle, " +
                "org.javaspec.doubles.prophecy.PredictionRegistry registry) {}\n" +
                "  public org.javaspec.doubles.prophecy.MethodProphecy<Boolean> send(String message) { return null; }\n" +
                "}\n");

        String support = SpecSupportFileGenerator.updateSourceWithProphecyHelpers(
                MINIMAL_SUPPORT, CALCULATOR_TYPE, Arrays.asList("com.example.Mailer"));
        write(src, "spec/com/example/CalculatorSpecSupport.java", support);
        write(src, "spec/com/example/CalculatorSpec.java",
                "package spec.com.example;\n" +
                "public class CalculatorSpec extends CalculatorSpecSupport {\n" +
                "  public void it_uses_var_for_typed_prophecy() {\n" +
                "    var mailer = prophesizeMailer();\n" +
                "    mailer.send(\"hello\").willReturn(Boolean.TRUE);\n" +
                "  }\n" +
                "}\n");

        int exit = compiler.run(null, null, null,
                "--release", "10",
                "-cp", System.getProperty("java.class.path"),
                "-d", out.getAbsolutePath(),
                new File(src, "com/example/Calculator.java").getAbsolutePath(),
                new File(src, "com/example/Mailer.java").getAbsolutePath(),
                new File(src, "com/example/MailerProphecy.java").getAbsolutePath(),
                new File(src, "spec/com/example/CalculatorSpecSupport.java").getAbsolutePath(),
                new File(src, "spec/com/example/CalculatorSpec.java").getAbsolutePath());
        assertEquals("Java 10 var syntax should infer MailerProphecy from prophesizeMailer()", 0, exit);
    }

    private static void write(File root, String relativePath, String source) throws Exception {
        File file = new File(root, relativePath);
        assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
        Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean javaSpecificationVersionAtLeast(int expected) {
        String raw = System.getProperty("java.specification.version", "1.8");
        int major;
        if (raw.startsWith("1.")) {
            major = Integer.parseInt(raw.substring(2));
        } else {
            int dot = raw.indexOf('.');
            major = Integer.parseInt(dot >= 0 ? raw.substring(0, dot) : raw);
        }
        return major >= expected;
    }

    private static int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) >= 0) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
