package io.github.jvmspec.doubles;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConcreteDoubleCapabilitiesTest {

    // -------------------------------------------------------------------------
    // isSupported()

    @Test
    public void nullNotSupported() {
        assertFalse(ConcreteDoubleCapabilities.isSupported(null));
    }

    @Test
    public void primitiveNotSupported() {
        assertFalse(ConcreteDoubleCapabilities.isSupported(int.class));
        assertFalse(ConcreteDoubleCapabilities.isSupported(boolean.class));
    }

    @Test
    public void arrayNotSupported() {
        assertFalse(ConcreteDoubleCapabilities.isSupported(String[].class));
    }

    @Test
    public void enumNotSupported() {
        assertFalse(ConcreteDoubleCapabilities.isSupported(SampleEnum.class));
    }

    @Test
    public void annotationNotSupported() {
        assertFalse(ConcreteDoubleCapabilities.isSupported(SampleAnnotation.class));
    }

    @Test
    public void interfaceNotSupportedByBytecode() {
        // Interfaces should use core Doubles.create() instead.
        assertFalse(ConcreteDoubleCapabilities.isSupported(SampleInterface.class));
    }

    @Test
    public void finalClassNotSupported() {
        assertFalse(ConcreteDoubleCapabilities.isSupported(String.class));
    }

    @Test
    public void nonFinalConcreteClassIsSupported() {
        assertTrue(ConcreteDoubleCapabilities.isSupported(ConcreteClass.class));
    }

    // -------------------------------------------------------------------------
    // describe()

    @Test
    public void describeNullReturnsExplanation() {
        String d = ConcreteDoubleCapabilities.describe(null);
        assertNotNull(d);
        assertTrue(d.length() > 0);
    }

    @Test
    public void describeFinalClassMentionsAgent() {
        String d = ConcreteDoubleCapabilities.describe(String.class);
        assertTrue("should mention agent", d.contains("agent") || d.contains("agent"));
        assertTrue("should mention final", d.contains("final") || d.contains("Final"));
    }

    @Test
    public void describeFinalClassIncludesWorkarounds() {
        String d = ConcreteDoubleCapabilities.describe(String.class);
        assertTrue("should include workaround hints",
                d.contains("interface") || d.contains("wrap") || d.contains("Wrap"));
    }

    @Test
    public void describeInterfaceSuggestsCoreCoreDoublesApi() {
        String d = ConcreteDoubleCapabilities.describe(SampleInterface.class);
        assertTrue("should mention core Doubles API",
                d.contains("Doubles.create") || d.contains("core"));
    }

    @Test
    public void describeNonFinalClassIndicatesSupported() {
        String d = ConcreteDoubleCapabilities.describe(ConcreteClass.class);
        assertTrue("should indicate supported",
                d.contains("supported") || d.contains("ByteBuddy"));
    }

    // -------------------------------------------------------------------------
    // Limitation notes

    @Test
    public void staticMethodLimitationNoteDescribesWorkarounds() {
        String note = ConcreteDoubleCapabilities.staticMethodLimitationNote();
        assertTrue(note.contains("Static") || note.contains("static"));
        assertTrue("should suggest interface workaround",
                note.contains("interface") || note.contains("Interface"));
    }

    @Test
    public void constructorInterceptionLimitationNoteDescribesInjection() {
        String note = ConcreteDoubleCapabilities.constructorInterceptionLimitationNote();
        assertTrue("should mention injection",
                note.contains("inject") || note.contains("Inject")
                        || note.contains("constructor") || note.contains("Constructor"));
    }

    // -------------------------------------------------------------------------
    // Enhanced error message from BytebuddyConcreteDoubleProvider

    @Test
    public void providerErrorMessageForFinalClassMentionsCapabilities() {
        BytebuddyConcreteDoubleProvider provider = new BytebuddyConcreteDoubleProvider();
        try {
            provider.createDouble(String.class);
            fail("should throw for final class");
        } catch (IllegalArgumentException ex) {
            // Error message should incorporate ConcreteDoubleCapabilities.describe()
            assertTrue("message should mention agent or workaround",
                    ex.getMessage().contains("agent") || ex.getMessage().contains("interface")
                            || ex.getMessage().contains("final"));
        }
    }

    // -------------------------------------------------------------------------
    // Test types

    public interface SampleInterface {
        void doSomething();
    }

    public static class ConcreteClass {
        public void action() { }
    }

    public enum SampleEnum { VALUE }

    public @interface SampleAnnotation { }
}
