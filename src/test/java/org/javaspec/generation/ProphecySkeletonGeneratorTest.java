package org.javaspec.generation;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProphecySkeletonGeneratorTest {

    // -------------------------------------------------------------------------
    // Interface type — unchanged behaviour

    public interface Mailer {
        boolean send(String recipient, String body);
        void flush();
    }

    @Test
    public void rendersInterfaceWrapperExtendingObjectProphecy() {
        String source = ProphecySkeletonGenerator.render(Mailer.class, "spec.com.example");

        assertTrue("must extend ObjectProphecy not BaseObjectProphecy",
                source.contains("extends ObjectProphecy<"));
        assertFalse(source.contains("extends BaseObjectProphecy<"));
        assertTrue(source.contains("import org.javaspec.doubles.prophecy.ObjectProphecy;"));
        assertTrue(source.contains("public final class MailerProphecy extends ObjectProphecy<"));
        assertTrue(source.contains("public MethodProphecy<Boolean> send("));
        assertTrue(source.contains("public MethodProphecy<Void> flush("));
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
}
