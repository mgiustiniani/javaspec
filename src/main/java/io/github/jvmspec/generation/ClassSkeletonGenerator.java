package io.github.jvmspec.generation;

import io.github.jvmspec.model.DescribedClass;

import java.io.File;
import java.util.Objects;

/**
 * Builds Java 8-compatible class skeleton generation plans.
 */
public final class ClassSkeletonGenerator {
    public static ClassGenerationPlan plan(DescribedClass describedClass, File sourceRoot) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");

        File targetFile = new File(sourceRoot, describedClass.sourceRelativePath());
        return ClassGenerationPlan.of(describedClass, sourceRoot, targetFile, render(describedClass));
    }

    public static String render(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");

        StringBuilder builder = new StringBuilder();
        if (describedClass.hasPackage()) {
            builder.append("package ").append(describedClass.packageName()).append(";\n\n");
        }
        builder.append("public class ").append(describedClass.simpleName()).append(" { }\n");
        return builder.toString();
    }
}
