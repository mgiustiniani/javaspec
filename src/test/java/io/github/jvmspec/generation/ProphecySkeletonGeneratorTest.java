package io.github.jvmspec.generation;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProphecySkeletonGeneratorTest {

    // -------------------------------------------------------------------------
    // Interface type — unchanged behaviour

    public interface Mailer {
        boolean send(String recipient, String body);
        void flush();
    }

    public interface ObjectOnlyCollaborator {
        void receive(Object value);
    }

    public interface OverloadedCollaborator {
        boolean convert(String value);
        int convert(Integer value);
    }

    @Test
    public void rendersInterfaceWrapperExtendingObjectProphecy() {
        String source = ProphecySkeletonGenerator.render(Mailer.class, "spec.com.example");

        assertTrue("must extend ObjectProphecy not BaseObjectProphecy",
                source.contains("extends ObjectProphecy<"));
        assertFalse(source.contains("extends BaseObjectProphecy<"));
        assertTrue(source.contains("import io.github.jvmspec.doubles.prophecy.ObjectProphecy;"));
        assertTrue(source.contains("public final class MailerProphecy extends ObjectProphecy<"));
        assertTrue(source.contains("public MethodProphecy<Boolean> send("));
        assertTrue(source.contains("public MethodProphecy<Void> flush("));
    }

    @Test
    public void rendersArgumentTokenOverloadsForTypedWrapperMethods() {
        String source = ProphecySkeletonGenerator.render(Mailer.class, "spec.com.example");

        assertTrue("exact typed overload must remain for literal/exact arguments",
                source.contains("public MethodProphecy<Boolean> send(java.lang.String arg0, java.lang.String arg1)"));
        assertTrue("Object overload lets generated wrappers accept Argument.any()/matching()/in() tokens",
                source.contains("public MethodProphecy<Object> send(Object arg0, Object arg1)"));
        assertFalse("zero-argument methods do not need token overloads",
                source.contains("public MethodProphecy<Object> flush(Object"));
    }

    @Test
    public void skipsArgumentTokenOverloadWhenExactObjectSignatureAlreadyAcceptsTokens() {
        String source = ProphecySkeletonGenerator.render(ObjectOnlyCollaborator.class, "spec.com.example");

        assertEquals(1, countOccurrences(source, "receive(java.lang.Object arg0)"));
        assertFalse(source.contains("public MethodProphecy<Object> receive(Object arg0)"));
    }

    @Test
    public void rendersOnlyOneArgumentTokenOverloadForSameArityOverloads() {
        String source = ProphecySkeletonGenerator.render(OverloadedCollaborator.class, "spec.com.example");

        assertTrue(source.contains("public MethodProphecy<Boolean> convert(java.lang.String arg0)"));
        assertTrue(source.contains("public MethodProphecy<Integer> convert(java.lang.Integer arg0)"));
        assertEquals(1, countOccurrences(source, "public MethodProphecy<Object> convert(Object arg0)"));
    }

    @Test
    public void interfaceWrapperConstructorTakesDoubleHandle() {
        String source = ProphecySkeletonGenerator.render(Mailer.class, "spec.com.example");

        assertTrue(source.contains("InterfaceDouble<"));
        assertTrue(source.contains("doubleHandle, registry"));
    }

    @Test
    public void interfaceWrapperSkipsStaticMethods() {
        String source = ProphecySkeletonGenerator.render(Mailer.class, "spec.com.example");

        assertFalse("static methods must be skipped", source.contains("static"));
    }

    // -------------------------------------------------------------------------
    // Concrete class — new behaviour

    public static class GreeterService {
        public String greet(String name) { return "Hello " + name; }
        public void log(String msg) { }
    }

    public static final class FinalGreeter {
        public String greet(String name) { return "Hi " + name; }
    }

    @Test
    public void rendersConcreteClassWrapperExtendingObjectProphecy() {
        String source = ProphecySkeletonGenerator.render(GreeterService.class, "spec.com.example");

        assertTrue("concrete class wrapper must extend ObjectProphecy",
                source.contains("extends ObjectProphecy<"));
        assertTrue(source.contains("public final class GreeterServiceProphecy"));
        assertTrue(source.contains("public MethodProphecy<"));
        assertTrue(source.contains("greet("));
    }

    @Test
    public void rendersFinalClassWrapper() {
        String source = ProphecySkeletonGenerator.render(FinalGreeter.class, "spec.com.example");

        assertTrue(source.contains("FinalGreeterProphecy"));
        assertTrue("nested classes must use Java source names, not binary $ names",
                source.contains("ProphecySkeletonGeneratorTest.FinalGreeter"));
        assertFalse("generated source must not reference nested classes with binary names",
                source.contains("ProphecySkeletonGeneratorTest$FinalGreeter"));
        assertTrue(source.contains("greet("));
    }

    @Test
    public void concreteWrapperSkipsObjectMethods() {
        String source = ProphecySkeletonGenerator.render(GreeterService.class, "spec.com.example");

        assertFalse("Object methods must not appear", source.contains("equals("));
        assertFalse("Object methods must not appear", source.contains("hashCode("));
        assertFalse("Object methods must not appear", source.contains("toString("));
    }

    @Test
    public void concreteWrapperSkipsStaticMethods() {
        String source = ProphecySkeletonGenerator.render(GreeterService.class, "spec.com.example");
        assertFalse("static methods must not appear in prophecy wrapper", source.contains("static "));
    }

    // -------------------------------------------------------------------------
    // Rejected types

    @Test(expected = IllegalArgumentException.class)
    public void primitiveTypeIsRejected() {
        ProphecySkeletonGenerator.render(int.class, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void arrayTypeIsRejected() {
        ProphecySkeletonGenerator.render(String[].class, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void enumTypeIsRejected() {
        ProphecySkeletonGenerator.render(Thread.State.class, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void annotationTypeIsRejected() {
        ProphecySkeletonGenerator.render(SuppressWarnings.class, "");
    }

    private static int countOccurrences(String text, String fragment) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(fragment, index)) >= 0) {
            count++;
            index += fragment.length();
        }
        return count;
    }
}
