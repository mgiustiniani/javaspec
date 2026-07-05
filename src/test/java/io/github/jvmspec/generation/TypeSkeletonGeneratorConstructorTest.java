package io.github.jvmspec.generation;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypeSkeletonGeneratorConstructorTest {
    @Test
    public void rendersClassWithConstructor() {
        ConstructorDescriptor constructor = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Markdown",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(constructor)
        );

        String source = TypeSkeletonGenerator.render(type);

        assertTrue(source.contains("public class Markdown {"));
        assertTrue(source.contains("public Markdown(Writer writer) {"));
    }

    @Test
    public void rendersClassWithConstructorAndBody() {
        ConstructorDescriptor constructor = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                "this.writer = writer;"
        );
        DescribedType type = DescribedType.of(
                "com.example.Markdown",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(constructor)
        );

        String source = TypeSkeletonGenerator.render(type);

        assertTrue(source.contains("public class Markdown {"));
        assertTrue(source.contains("public Markdown(Writer writer) {"));
        assertTrue(source.contains("this.writer = writer;"));
    }

    @Test
    public void rendersClassWithMultipleConstructors() {
        ConstructorDescriptor c1 = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                "this.writer = writer;"
        );
        ConstructorDescriptor c2 = ConstructorDescriptor.of(
                Arrays.asList("String"),
                Arrays.asList("name"),
                "this.name = name;"
        );
        DescribedType type = DescribedType.of(
                "com.example.Markdown",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(c1, c2)
        );

        String source = TypeSkeletonGenerator.render(type);

        assertTrue(source.contains("public Markdown(Writer writer) {"));
        assertTrue(source.contains("this.writer = writer;"));
        assertTrue(source.contains("public Markdown(String name) {"));
        assertTrue(source.contains("this.name = name;"));
    }

    @Test
    public void rendersFinalClassWithConstructor() {
        ConstructorDescriptor constructor = ConstructorDescriptor.of(
                Arrays.asList("Shape"),
                Arrays.asList("shape"),
                "this.shape = shape;"
        );
        DescribedType type = DescribedType.of(
                "com.example.Circle",
                JavaTypeKind.FINAL_CLASS,
                Arrays.asList("com.example.Shape"),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(constructor)
        );

        String source = TypeSkeletonGenerator.render(type);

        assertTrue(source.contains("public final class Circle extends Shape {"));
        assertTrue(source.contains("public Circle(Shape shape) {"));
        assertTrue(source.contains("this.shape = shape;"));
    }

    @Test
    public void rendersClassWithoutConstructor() {
        DescribedType type = DescribedType.of("com.example.Generated", JavaTypeKind.CLASS);

        String source = TypeSkeletonGenerator.render(type);

        assertEquals("package com.example;\n\npublic class Generated { }\n", source);
    }

    @Test
    public void rendersSealedClassWithConstructorAndExplicitPermits() {
        ConstructorDescriptor constructor = ConstructorDescriptor.of(
                Arrays.asList("String"),
                Arrays.asList("name"),
                "this.name = name;"
        );
        DescribedType type = DescribedType.of(
                "com.example.Shape",
                JavaTypeKind.SEALED_CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList("com.example.Circle", "com.example.Rectangle"),
                Arrays.asList(constructor)
        );

        String source = TypeSkeletonGenerator.render(type);

        assertTrue(source.contains("public sealed class Shape permits Circle, Rectangle {"));
        assertTrue(source.contains("public Shape(String name) {"));
        assertTrue(source.contains("this.name = name;"));
    }

    @Test
    public void constructorRenderingRespectsSourceLocation() throws Exception {
        java.io.File sourceRoot = new java.io.File(".");
        ConstructorDescriptor constructor = ConstructorDescriptor.of(
                Arrays.asList("Writer"),
                Arrays.asList("writer"),
                ""
        );
        DescribedType type = DescribedType.of(
                "com.example.Markdown",
                JavaTypeKind.CLASS,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Arrays.asList(constructor)
        );

        TypeGenerationPlan plan = TypeSkeletonGenerator.plan(type, sourceRoot);

        assertEquals(type, plan.describedType());
        assertTrue(plan.sourceContent().contains("public class Markdown {"));
        assertTrue(plan.sourceContent().contains("public Markdown(Writer writer) {"));
    }
}
