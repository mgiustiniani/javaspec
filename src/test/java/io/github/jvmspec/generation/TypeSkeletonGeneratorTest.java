package io.github.jvmspec.generation;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TypeSkeletonGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void rendersClassSkeleton() {
        String source = TypeSkeletonGenerator.render(DescribedType.of("com.example.Generated", JavaTypeKind.CLASS));

        assertEquals("package com.example;\n\npublic class Generated { }\n", source);
    }

    @Test
    public void rendersFinalClassSkeleton() {
        String source = TypeSkeletonGenerator.render(DescribedType.of(
                "com.example.Circle",
                JavaTypeKind.FINAL_CLASS,
                Arrays.asList("com.example.Shape"),
                Arrays.<String>asList(),
                Arrays.<String>asList()
        ));

        assertEquals("package com.example;\n\npublic final class Circle extends Shape { }\n", source);
    }

    @Test
    public void rendersInterfaceSkeleton() {
        String source = TypeSkeletonGenerator.render(DescribedType.of("com.example.PaymentGateway", JavaTypeKind.INTERFACE));

        assertEquals("package com.example;\n\npublic interface PaymentGateway { }\n", source);
    }

    @Test
    public void rendersInterfaceMethodDeclarationsAndSkipsStaticDescriptors() {
        DescribedType type = DescribedType.of(
                "com.example.PaymentGateway",
                JavaTypeKind.INTERFACE,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("status", "String"),
                        MethodDescriptor.of(
                                "charge",
                                "boolean",
                                Arrays.asList("String", "int"),
                                Arrays.asList("accountId", "cents")
                        ),
                        MethodDescriptor.staticMethod("named", "com.example.PaymentGateway")
                )
        );

        String source = TypeSkeletonGenerator.render(type);

        assertEquals("package com.example;\n\n" +
                "public interface PaymentGateway {\n" +
                "    String status();\n" +
                "\n" +
                "    boolean charge(String accountId, int cents);\n" +
                "}\n", source);
    }

    @Test
    public void rendersEnumSkeleton() {
        String source = TypeSkeletonGenerator.render(DescribedType.of("com.example.OrderStatus", JavaTypeKind.ENUM));

        assertEquals("package com.example;\n\npublic enum OrderStatus { }\n", source);
    }

    @Test
    public void rendersAnnotationSkeleton() {
        String source = TypeSkeletonGenerator.render(DescribedType.of("com.example.Experimental", JavaTypeKind.ANNOTATION));

        assertEquals("package com.example;\n\npublic @interface Experimental { }\n", source);
    }

    @Test
    public void rendersAnnotationElementsAndSkipsIncompatibleDescriptors() {
        DescribedType type = DescribedType.of(
                "com.example.GeneratedTag",
                JavaTypeKind.ANNOTATION,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("value", "String"),
                        MethodDescriptor.of("priority", "int"),
                        MethodDescriptor.of("tags", "String[]"),
                        MethodDescriptor.of(
                                "withParameter",
                                "String",
                                Arrays.asList("String"),
                                Arrays.asList("value")
                        ),
                        MethodDescriptor.staticMethod("staticValue", "String"),
                        MethodDescriptor.of("objectValue", "Object"),
                        MethodDescriptor.voidMethod("nothing")
                )
        );

        String source = TypeSkeletonGenerator.render(type);

        assertEquals("package com.example;\n\n" +
                "public @interface GeneratedTag {\n" +
                "    String value();\n" +
                "\n" +
                "    int priority();\n" +
                "\n" +
                "    String[] tags();\n" +
                "}\n", source);
    }

    @Test
    public void rendersRecordSkeleton() {
        String source = TypeSkeletonGenerator.render(DescribedType.of("com.example.User", JavaTypeKind.RECORD));

        assertEquals("package com.example;\n\npublic record User() { }\n", source);
    }

    @Test
    public void rendersRecordComponentFromConstructorAndExplicitAccessorStub() {
        DescribedType type = DescribedType.of(
                "com.example.UserId",
                JavaTypeKind.RECORD,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(ConstructorDescriptor.of(
                        Arrays.asList("String"),
                        Arrays.asList("arg0"),
                        "")),
                Arrays.asList(MethodDescriptor.of("value", "String"))
        );

        String source = TypeSkeletonGenerator.render(type);

        assertEquals("package com.example;\n\n" +
                "public record UserId(String value) {\n" +
                "    public String value() {\n" +
                "        // javaspec:stub\n" +
                "        return null;\n" +
                "    }\n" +
                "}\n", source);
    }

    @Test
    public void rendersSealedClassSkeleton() {
        String source = TypeSkeletonGenerator.render(DescribedType.of("com.example.Shape", JavaTypeKind.SEALED_CLASS));

        assertEquals("package com.example;\n\n" +
                "public sealed class Shape permits Shape.Permitted {\n" +
                "    static final class Permitted extends Shape { }\n" +
                "}\n", source);
    }

    @Test
    public void rendersSealedClassSkeletonWithExplicitPermits() {
        String source = TypeSkeletonGenerator.render(DescribedType.of(
                "com.example.Shape",
                JavaTypeKind.SEALED_CLASS,
                Arrays.asList("com.example.Circle", "com.example.Rectangle")
        ));

        assertEquals("package com.example;\n\npublic sealed class Shape permits Circle, Rectangle { }\n", source);
    }

    @Test
    public void rendersSealedInterfaceSkeleton() {
        String source = TypeSkeletonGenerator.render(DescribedType.of("com.example.Shape", JavaTypeKind.SEALED_INTERFACE));

        assertEquals("package com.example;\n\n" +
                "public sealed interface Shape permits Shape.Permitted {\n" +
                "    final class Permitted implements Shape { }\n" +
                "}\n", source);
    }

    @Test
    public void rendersSealedInterfaceSkeletonWithExplicitPermits() {
        String source = TypeSkeletonGenerator.render(DescribedType.of(
                "com.example.Message",
                JavaTypeKind.SEALED_INTERFACE,
                Arrays.asList("com.example.EmailMessage", "com.example.SmsMessage")
        ));

        assertEquals("package com.example;\n\n" +
                "public sealed interface Message permits Message.EmailMessage, Message.SmsMessage {\n" +
                "    final class EmailMessage implements Message { }\n" +
                "    final class SmsMessage implements Message { }\n" +
                "}\n", source);
    }

    @Test
    public void rendersSealedInterfaceMethodsWithNestedDefaultImplementationsThatCompileAsJava17Source() throws Exception {
        DescribedType type = DescribedType.of(
                "com.example.Shape",
                JavaTypeKind.SEALED_INTERFACE,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList("com.example.Circle", "com.example.Rectangle"),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("sides", "int"),
                        MethodDescriptor.of("name", "String"),
                        MethodDescriptor.of("enabled", "boolean")
                )
        );

        String source = TypeSkeletonGenerator.render(type);

        assertEquals("package com.example;\n\n" +
                "public sealed interface Shape permits Shape.Circle, Shape.Rectangle {\n" +
                "    int sides();\n" +
                "\n" +
                "    String name();\n" +
                "\n" +
                "    boolean enabled();\n" +
                "\n" +
                "    final class Circle implements Shape {\n" +
                "        public int sides() {\n" +
                "            // javaspec:stub\n" +
                "            return 0;\n" +
                "        }\n" +
                "\n" +
                "        public String name() {\n" +
                "            // javaspec:stub\n" +
                "            return null;\n" +
                "        }\n" +
                "\n" +
                "        public boolean enabled() {\n" +
                "            // javaspec:stub\n" +
                "            return false;\n" +
                "        }\n" +
                "    }\n" +
                "    final class Rectangle implements Shape {\n" +
                "        public int sides() {\n" +
                "            // javaspec:stub\n" +
                "            return 0;\n" +
                "        }\n" +
                "\n" +
                "        public String name() {\n" +
                "            // javaspec:stub\n" +
                "            return null;\n" +
                "        }\n" +
                "\n" +
                "        public boolean enabled() {\n" +
                "            // javaspec:stub\n" +
                "            return false;\n" +
                "        }\n" +
                "    }\n" +
                "}\n", source);
        assertCompilesAsJava17Source(source, "com/example/Shape.java");
    }

    @Test
    public void rendersClassSkeletonWithExtendsAndImplements() {
        String source = TypeSkeletonGenerator.render(DescribedType.of(
                "com.example.Service",
                JavaTypeKind.CLASS,
                Arrays.asList("com.example.BaseService"),
                Arrays.asList("com.example.RunnableService", "com.example.CloseableService"),
                Arrays.<String>asList()
        ));

        assertEquals("package com.example;\n\n" +
                "public class Service extends BaseService implements RunnableService, CloseableService { }\n", source);
    }

    @Test
    public void rendersInterfaceSkeletonWithExtends() {
        String source = TypeSkeletonGenerator.render(DescribedType.of(
                "com.example.PaymentGateway",
                JavaTypeKind.INTERFACE,
                Arrays.asList("com.example.Port", "com.example.Gateway"),
                Arrays.<String>asList(),
                Arrays.<String>asList()
        ));

        assertEquals("package com.example;\n\npublic interface PaymentGateway extends Port, Gateway { }\n", source);
    }

    @Test
    public void rendersClassWithMethodsUsingJava8CompatibleDefaultReturns() {
        DescribedType type = DescribedType.of(
                "com.example.Book",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<io.github.jvmspec.model.ConstructorDescriptor>emptyList(),
                Arrays.asList(
                        MethodDescriptor.of("getRating", "int"),
                        MethodDescriptor.of("isEnabled", "boolean"),
                        MethodDescriptor.of("getCount", "long"),
                        MethodDescriptor.of("getRatio", "float"),
                        MethodDescriptor.of("getScore", "double"),
                        MethodDescriptor.of("getInitial", "char"),
                        MethodDescriptor.of("getTitle", "String"),
                        MethodDescriptor.voidMethod("setRating", Arrays.asList("int"), Arrays.asList("rating"))
                )
        );

        String source = TypeSkeletonGenerator.render(type);

        assertEquals("package com.example;\n\n" +
                "public class Book {\n" +
                "    public int getRating() {\n" +
                "        // javaspec:stub\n" +
                "        return 0;\n" +
                "    }\n" +
                "\n" +
                "    public boolean isEnabled() {\n" +
                "        // javaspec:stub\n" +
                "        return false;\n" +
                "    }\n" +
                "\n" +
                "    public long getCount() {\n" +
                "        // javaspec:stub\n" +
                "        return 0L;\n" +
                "    }\n" +
                "\n" +
                "    public float getRatio() {\n" +
                "        // javaspec:stub\n" +
                "        return 0.0f;\n" +
                "    }\n" +
                "\n" +
                "    public double getScore() {\n" +
                "        // javaspec:stub\n" +
                "        return 0.0d;\n" +
                "    }\n" +
                "\n" +
                "    public char getInitial() {\n" +
                "        // javaspec:stub\n" +
                "        return '\\0';\n" +
                "    }\n" +
                "\n" +
                "    public String getTitle() {\n" +
                "        // javaspec:stub\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    public void setRating(int rating) {\n" +
                "        // javaspec:stub\n" +
                "    }\n" +
                "}\n", source);
    }

    @Test
    public void createsPlanWithTargetPathAndSkeletonContent() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("source-root");
        DescribedType describedType = DescribedType.of("com.example.PaymentGateway", JavaTypeKind.INTERFACE);

        TypeGenerationPlan plan = TypeSkeletonGenerator.plan(describedType, sourceRoot);

        assertEquals(describedType, plan.describedType());
        assertEquals(sourceRoot, plan.sourceRoot());
        assertEquals(new File(sourceRoot, "com" + File.separator + "example" + File.separator + "PaymentGateway.java"), plan.targetFile());
        assertEquals("package com.example;\n\npublic interface PaymentGateway { }\n", plan.sourceContent());
    }

    private void assertCompilesAsJava17Source(String source, String sourceRelativePath) throws Exception {
        if (!supportsJavaSpecificationVersion(17)) {
            return;
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("A JDK compiler is required to verify Java 17 generated source", compiler);

        File sourceRoot = temporaryFolder.newFolder("generated-java17-source");
        File sourceFile = new File(sourceRoot, sourceRelativePath);
        File parent = sourceFile.getParentFile();
        assertTrue(parent.mkdirs() || parent.isDirectory());
        Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));

        File classOutput = temporaryFolder.newFolder("generated-java17-classes");
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        try {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile));
            List<String> options = Arrays.asList("--release", "17", "-d", classOutput.getAbsolutePath());
            Boolean success = compiler.getTask(null, fileManager, null, options, null, units).call();
            assertTrue("Generated source did not compile as Java 17:\n" + source, success.booleanValue());
        } finally {
            fileManager.close();
        }
    }

    private static boolean supportsJavaSpecificationVersion(int minimumVersion) {
        String version = System.getProperty("java.specification.version");
        if (version == null) {
            return false;
        }
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dot = version.indexOf('.');
        if (dot >= 0) {
            version = version.substring(0, dot);
        }
        try {
            return Integer.parseInt(version) >= minimumVersion;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
