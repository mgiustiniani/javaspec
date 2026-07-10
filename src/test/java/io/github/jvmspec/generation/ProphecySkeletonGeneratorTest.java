package io.github.jvmspec.generation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.Assert.*;

public class ProphecySkeletonGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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

    public interface EdgeCaseCollaborator {
        int score(int value);
        void sendAll(String[] recipients);
        void varargs(String prefix, String... values);
    }

    public interface BoundedGenericCollaborator<T extends Number> {
        T load();
        void save(T value);
    }

    public static class GenericBase<T> {
        public void store(T value) {
        }
    }

    public static class StringGenericChild extends GenericBase<String> {
        @Override
        public void store(String value) {
        }
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
    public void tokenOverloadEdgeCasesCoverPrimitivesArraysVarargsAndGenerics() throws Exception {
        String source = ProphecySkeletonGenerator.render(EdgeCaseCollaborator.class, "spec.com.example");

        assertTrue(source.contains("public MethodProphecy<Integer> score(int arg0)"));
        assertTrue(source.contains("public MethodProphecy<Object> score(Object arg0)"));
        assertTrue(source.contains("public MethodProphecy<Void> sendAll(java.lang.String[] arg0)"));
        assertTrue(source.contains("public MethodProphecy<Object> sendAll(Object arg0)"));
        assertTrue("varargs are generated as Java array parameters at the wrapper boundary",
                source.contains("public MethodProphecy<Void> varargs(java.lang.String arg0, java.lang.String[] arg1)"));
        assertTrue(source.contains("public MethodProphecy<Object> varargs(Object arg0, Object arg1)"));

        String genericSource = ProphecySkeletonGenerator.render(BoundedGenericCollaborator.class, "spec.com.example");
        assertTrue("bounded generic arguments use erased reflection/source signatures",
                genericSource.contains("public MethodProphecy<Void> save(java.lang.Number arg0)"));
        assertTrue(genericSource.contains("public MethodProphecy<Object> save(Object arg0)"));
        assertTrue(genericSource.contains("public MethodProphecy<java.lang.Number> load()"));

        assertGeneratedWrapperAndTokenClientCompile("EdgeCaseCollaboratorProphecy", source,
                "package spec.com.example;\n" +
                        "import io.github.jvmspec.doubles.prophecy.Argument;\n" +
                        "final class EdgeCaseTokenClient {\n" +
                        "    void configure(EdgeCaseCollaboratorProphecy prophecy) {\n" +
                        "        prophecy.score(1);\n" +
                        "        prophecy.score(Argument.any(int.class));\n" +
                        "        prophecy.sendAll(new String[] {\"a\"});\n" +
                        "        prophecy.sendAll(Argument.any(String[].class));\n" +
                        "        prophecy.varargs(\"prefix\", new String[] {\"a\"});\n" +
                        "        prophecy.varargs(\"prefix\", Argument.any(String[].class));\n" +
                        "    }\n" +
                        "}\n");
    }

    @Test
    public void skipsBridgeAndSyntheticMethodsBeforeGeneratingTokenOverloads() throws Exception {
        String source = ProphecySkeletonGenerator.render(StringGenericChild.class, "spec.com.example");

        assertTrue(source.contains("public MethodProphecy<Void> store(java.lang.String arg0)"));
        assertFalse("synthetic bridge method store(Object) would collide with the token overload",
                source.contains("public MethodProphecy<Void> store(java.lang.Object arg0)"));
        assertEquals(1, countOccurrences(source, "public MethodProphecy<Object> store(Object arg0)"));
        assertGeneratedWrapperAndTokenClientCompile("StringGenericChildProphecy", source,
                "package spec.com.example;\n" +
                        "import io.github.jvmspec.doubles.prophecy.Argument;\n" +
                        "final class BridgeTokenClient {\n" +
                        "    void configure(StringGenericChildProphecy prophecy) {\n" +
                        "        prophecy.store(\"literal\");\n" +
                        "        prophecy.store(Argument.any(String.class));\n" +
                        "    }\n" +
                        "}\n");
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

    private void assertGeneratedWrapperAndTokenClientCompile(
            String wrapperSimpleName,
            String wrapperSource,
            String clientSource
    ) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("These tests require a JDK with javax.tools.JavaCompiler", compiler);
        File sourceRoot = temporaryFolder.newFolder("prophecy-source-" + System.nanoTime());
        File outputRoot = temporaryFolder.newFolder("prophecy-classes-" + System.nanoTime());
        File packageDirectory = new File(sourceRoot, "spec/com/example");
        assertTrue(packageDirectory.isDirectory() || packageDirectory.mkdirs());
        File wrapperFile = new File(packageDirectory, wrapperSimpleName + ".java");
        File clientFile = new File(packageDirectory, "TokenClient" + System.nanoTime() + ".java");
        Files.write(wrapperFile.toPath(), wrapperSource.getBytes(StandardCharsets.UTF_8));
        Files.write(clientFile.toPath(), clientSource.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
        int exitCode = compiler.run(
                null,
                compilerOutput,
                compilerOutput,
                "-d", outputRoot.getAbsolutePath(),
                "-classpath", System.getProperty("java.class.path"),
                "-source", "1.8",
                "-target", "1.8",
                wrapperFile.getAbsolutePath(),
                clientFile.getAbsolutePath()
        );
        assertEquals(new String(compilerOutput.toByteArray(), StandardCharsets.UTF_8), 0, exitCode);
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
