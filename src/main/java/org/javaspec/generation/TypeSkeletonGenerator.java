package org.javaspec.generation;

import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Builds class-like type skeleton generation plans without linking to post-Java 8 APIs.
 */
public final class TypeSkeletonGenerator {
    private TypeSkeletonGenerator() {
    }

    public static TypeGenerationPlan plan(DescribedType describedType, File sourceRoot) {
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");

        File targetFile = new File(sourceRoot, describedType.sourceRelativePath());
        return TypeGenerationPlan.of(describedType, sourceRoot, targetFile, render(describedType));
    }

    public static String render(DescribedType describedType) {
        Objects.requireNonNull(describedType, "describedType must not be null");

        StringBuilder builder = new StringBuilder();
        if (describedType.hasPackage()) {
            builder.append("package ").append(describedType.packageName()).append(";\n\n");
        }
        appendDeclaration(builder, describedType);
        return builder.toString();
    }

    private static void appendDeclaration(StringBuilder builder, DescribedType describedType) {
        JavaTypeKind kind = describedType.kind();
        if (JavaTypeKind.RECORD.equals(kind)) {
            appendRecord(builder, describedType);
            return;
        }
        if (JavaTypeKind.SEALED_CLASS.equals(kind)) {
            appendSealedClass(builder, describedType);
            return;
        }
        if (JavaTypeKind.SEALED_INTERFACE.equals(kind)) {
            appendSealedInterface(builder, describedType);
            return;
        }
        if (JavaTypeKind.FINAL_CLASS.equals(kind)) {
            appendFinalClass(builder, describedType);
            return;
        }
        if (JavaTypeKind.CLASS.equals(kind)) {
            appendClass(builder, describedType);
            return;
        }
        if (JavaTypeKind.INTERFACE.equals(kind)) {
            appendInterface(builder, describedType);
            return;
        }
        if (JavaTypeKind.ENUM.equals(kind)) {
            appendEnum(builder, describedType);
            return;
        }
        builder.append("public ").append(kind.sourceKeyword()).append(" ").append(describedType.simpleName()).append(" { }\n");
    }

    private static void appendClass(StringBuilder builder, DescribedType describedType) {
        builder.append("public class ").append(describedType.simpleName());
        appendClassExtends(builder, describedType);
        appendImplements(builder, describedType);
        builder.append(" { }\n");
    }

    private static void appendFinalClass(StringBuilder builder, DescribedType describedType) {
        builder.append("public final class ").append(describedType.simpleName());
        appendClassExtends(builder, describedType);
        appendImplements(builder, describedType);
        builder.append(" { }\n");
    }

    private static void appendInterface(StringBuilder builder, DescribedType describedType) {
        builder.append("public interface ").append(describedType.simpleName());
        appendInterfaceExtends(builder, describedType);
        builder.append(" { }\n");
    }

    private static void appendEnum(StringBuilder builder, DescribedType describedType) {
        builder.append("public enum ").append(describedType.simpleName());
        appendImplements(builder, describedType);
        builder.append(" { }\n");
    }

    private static void appendRecord(StringBuilder builder, DescribedType describedType) {
        builder.append("public record ").append(describedType.simpleName()).append("()");
        appendImplements(builder, describedType);
        builder.append(" { }\n");
    }

    private static void appendSealedClass(StringBuilder builder, DescribedType describedType) {
        builder.append("public sealed class ").append(describedType.simpleName());
        appendClassExtends(builder, describedType);
        appendImplements(builder, describedType);
        if (describedType.hasPermittedTypes()) {
            builder.append(" permits ").append(typeList(describedType, describedType.permittedTypeNames())).append(" { }\n");
            return;
        }
        builder.append(" permits ").append(describedType.simpleName()).append(".Permitted {\n");
        builder.append("    static final class Permitted extends ").append(describedType.simpleName()).append(" { }\n");
        builder.append("}\n");
    }

    private static void appendSealedInterface(StringBuilder builder, DescribedType describedType) {
        builder.append("public sealed interface ").append(describedType.simpleName());
        appendInterfaceExtends(builder, describedType);
        if (describedType.hasPermittedTypes()) {
            builder.append(" permits ").append(nestedPermitsList(describedType)).append(" {\n");
            appendNestedInterfacePermittedTypes(builder, describedType);
            builder.append("}\n");
            return;
        }
        builder.append(" permits ").append(describedType.simpleName()).append(".Permitted {\n");
        builder.append("    final class Permitted implements ").append(describedType.simpleName()).append(" { }\n");
        builder.append("}\n");
    }

    private static void appendClassExtends(StringBuilder builder, DescribedType describedType) {
        if (describedType.hasExtendedTypes()) {
            builder.append(" extends ").append(sourceTypeName(describedType, describedType.extendedTypeNames().get(0)));
        }
    }

    private static void appendInterfaceExtends(StringBuilder builder, DescribedType describedType) {
        if (describedType.hasExtendedTypes()) {
            builder.append(" extends ").append(typeList(describedType, describedType.extendedTypeNames()));
        }
    }

    private static void appendImplements(StringBuilder builder, DescribedType describedType) {
        if (describedType.hasImplementedTypes()) {
            builder.append(" implements ").append(typeList(describedType, describedType.implementedTypeNames()));
        }
    }

    private static String nestedPermitsList(DescribedType describedType) {
        StringBuilder builder = new StringBuilder();
        List<String> typeNames = describedType.permittedTypeNames();
        for (int i = 0; i < typeNames.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(describedType.simpleName()).append(".").append(simpleName(typeNames.get(i)));
        }
        return builder.toString();
    }

    private static void appendNestedInterfacePermittedTypes(StringBuilder builder, DescribedType describedType) {
        List<String> typeNames = describedType.permittedTypeNames();
        for (int i = 0; i < typeNames.size(); i++) {
            builder.append("    final class ").append(simpleName(typeNames.get(i)))
                    .append(" implements ").append(describedType.simpleName()).append(" { }\n");
        }
    }

    private static String typeList(DescribedType describedType, List<String> typeNames) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < typeNames.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(sourceTypeName(describedType, typeNames.get(i)));
        }
        return builder.toString();
    }

    private static String sourceTypeName(DescribedType owner, String typeName) {
        String packageName = owner.packageName();
        if (packageName.length() > 0 && typeName.startsWith(packageName + ".")) {
            return typeName.substring(packageName.length() + 1);
        }
        return typeName;
    }

    private static String simpleName(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot < 0) {
            return typeName;
        }
        return typeName.substring(lastDot + 1);
    }
}
