package org.javaspec.generation;

import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

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
    public void rendersRecordSkeleton() {
        String source = TypeSkeletonGenerator.render(DescribedType.of("com.example.User", JavaTypeKind.RECORD));

        assertEquals("package com.example;\n\npublic record User() { }\n", source);
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
                Collections.<org.javaspec.model.ConstructorDescriptor>emptyList(),
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
                "        return 0;\n" +
                "    }\n" +
                "\n" +
                "    public boolean isEnabled() {\n" +
                "        return false;\n" +
                "    }\n" +
                "\n" +
                "    public long getCount() {\n" +
                "        return 0L;\n" +
                "    }\n" +
                "\n" +
                "    public float getRatio() {\n" +
                "        return 0.0f;\n" +
                "    }\n" +
                "\n" +
                "    public double getScore() {\n" +
                "        return 0.0d;\n" +
                "    }\n" +
                "\n" +
                "    public char getInitial() {\n" +
                "        return '\\0';\n" +
                "    }\n" +
                "\n" +
                "    public String getTitle() {\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    public void setRating(int rating) {\n" +
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
}
