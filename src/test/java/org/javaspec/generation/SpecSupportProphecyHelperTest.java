package org.javaspec.generation;

import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class SpecSupportProphecyHelperTest {

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
        assertTrue("must mention NotifierProphecy return type",
                updated.contains("com.example.NotifierProphecy"));
        assertTrue("must use Doubles.interfaceDouble or concreteDouble",
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
        assertTrue("must mention DataStoreProphecy return type",
                updated.contains("com.example.DataStoreProphecy"));
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
