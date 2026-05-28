package org.javaspec.generation;

import org.javaspec.model.ConstructorDescriptor;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.model.MethodDescriptor;

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
        appendBodyOrClose(builder, describedType);
    }

    private static void appendBodyOrClose(StringBuilder builder, DescribedType describedType) {
        if (describedType.hasConstructors() || describedType.hasMethods()) {
            builder.append(" {\n");
            appendMembers(builder, describedType);
            builder.append("}\n");
        } else {
            builder.append(" { }\n");
        }
    }

    private static void appendFinalClass(StringBuilder builder, DescribedType describedType) {
        builder.append("public final class ").append(describedType.simpleName());
        appendClassExtends(builder, describedType);
        appendImplements(builder, describedType);
        appendBodyOrClose(builder, describedType);
    }

    private static void appendInterface(StringBuilder builder, DescribedType describedType) {
        builder.append("public interface ").append(describedType.simpleName());
        appendInterfaceExtends(builder, describedType);
        builder.append(" { }\n");
    }

    private static void appendEnum(StringBuilder builder, DescribedType describedType) {
        builder.append("public enum ").append(describedType.simpleName());
        appendImplements(builder, describedType);
        if (describedType.hasMethods()) {
            builder.append(" {\n");
            builder.append("    ;\n\n");
            appendMethods(builder, describedType);
            builder.append("}\n");
        } else {
            builder.append(" { }\n");
        }
    }

    private static void appendRecord(StringBuilder builder, DescribedType describedType) {
        builder.append("public record ").append(describedType.simpleName()).append("()");
        appendImplements(builder, describedType);
        if (describedType.hasMethods()) {
            builder.append(" {\n");
            appendMethods(builder, describedType);
            builder.append("}\n");
        } else {
            builder.append(" { }\n");
        }
    }

    private static void appendSealedClass(StringBuilder builder, DescribedType describedType) {
        builder.append("public sealed class ").append(describedType.simpleName());
        appendClassExtends(builder, describedType);
        appendImplements(builder, describedType);
        if (describedType.hasPermittedTypes()) {
            builder.append(" permits ").append(typeList(describedType, describedType.permittedTypeNames()));
            appendBodyOrClose(builder, describedType);
            return;
        }
        builder.append(" permits ").append(describedType.simpleName()).append(".Permitted {\n");
        if (describedType.hasConstructors() || describedType.hasMethods()) {
            appendMembers(builder, describedType);
            builder.append("\n");
        }
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

    private static void appendMembers(StringBuilder builder, DescribedType describedType) {
        if (describedType.hasConstructors()) {
            appendConstructors(builder, describedType);
        }
        if (describedType.hasConstructors() && describedType.hasMethods()) {
            builder.append("\n");
        }
        if (describedType.hasMethods()) {
            appendMethods(builder, describedType);
        }
    }

    private static void appendConstructors(StringBuilder builder, DescribedType describedType) {
        List<ConstructorDescriptor> constructors = describedType.constructors();
        for (int ci = 0; ci < constructors.size(); ci++) {
            ConstructorDescriptor constructor = constructors.get(ci);
            builder.append("    public ").append(describedType.simpleName()).append("(");
            appendConstructorParameters(builder, constructor);
            builder.append(") {\n");
            if (constructor.hasBody()) {
                String body = constructor.bodyContent();
                String[] lines = body.split("\n");
                for (int li = 0; li < lines.length; li++) {
                    builder.append("        ").append(lines[li]).append("\n");
                }
            }
            builder.append("    }\n");
            if (ci < constructors.size() - 1) {
                builder.append("\n");
            }
        }
    }

    private static void appendConstructorParameters(StringBuilder builder, ConstructorDescriptor constructor) {
        List<String> types = constructor.parameterTypes();
        List<String> names = constructor.parameterNames();
        appendParameters(builder, types, names);
    }

    private static void appendMethods(StringBuilder builder, DescribedType describedType) {
        List<MethodDescriptor> methods = describedType.methods();
        for (int mi = 0; mi < methods.size(); mi++) {
            MethodDescriptor method = methods.get(mi);
            appendMethod(builder, describedType, method);
            if (mi < methods.size() - 1) {
                builder.append("\n");
            }
        }
    }

    static void appendMethod(StringBuilder builder, DescribedType owner, MethodDescriptor method) {
        builder.append("    public ");
        if (method.isStatic()) {
            builder.append("static ");
        }
        builder.append(sourceTypeName(owner, method.returnType())).append(" ")
                .append(method.methodName()).append("(");
        appendParameters(builder, method.parameterTypes(), method.parameterNames());
        builder.append(") {\n");
        if (!method.isVoid()) {
            builder.append("        ").append(defaultReturnStatement(owner, method)).append("\n");
        }
        builder.append("    }\n");
    }

    private static String defaultReturnStatement(DescribedType owner, MethodDescriptor method) {
        if (method.isStatic() && returnsOwnerType(owner, method) && canInstantiateOwner(owner)) {
            return "return " + factoryReturnExpression(owner, method) + ";";
        }
        return method.defaultReturnStatement();
    }

    private static String factoryReturnExpression(DescribedType owner, MethodDescriptor method) {
        ConstructorDescriptor matchingConstructor = matchingConstructor(owner, method.parameterTypes());
        if (matchingConstructor != null) {
            return "new " + owner.simpleName() + "(" + joined(method.parameterNames()) + ")";
        }
        if (!owner.hasConstructors() || hasNoArgConstructor(owner)) {
            return "new " + owner.simpleName() + "()";
        }
        ConstructorDescriptor fallbackConstructor = owner.constructors().get(0);
        return "new " + owner.simpleName() + "(" + defaultArguments(fallbackConstructor.parameterTypes()) + ")";
    }

    private static ConstructorDescriptor matchingConstructor(DescribedType owner, List<String> parameterTypes) {
        List<ConstructorDescriptor> constructors = owner.constructors();
        for (int i = 0; i < constructors.size(); i++) {
            ConstructorDescriptor constructor = constructors.get(i);
            if (constructor.parameterTypes().equals(parameterTypes)) {
                return constructor;
            }
        }
        return null;
    }

    private static boolean hasNoArgConstructor(DescribedType owner) {
        List<ConstructorDescriptor> constructors = owner.constructors();
        for (int i = 0; i < constructors.size(); i++) {
            if (!constructors.get(i).hasParameters()) {
                return true;
            }
        }
        return false;
    }

    private static String defaultArguments(List<String> parameterTypes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(defaultExpressionForType(parameterTypes.get(i)));
        }
        return builder.toString();
    }

    private static String defaultExpressionForType(String typeName) {
        String normalized = typeName.trim();
        if ("boolean".equals(normalized)) {
            return "false";
        }
        if ("long".equals(normalized)) {
            return "0L";
        }
        if ("float".equals(normalized)) {
            return "0.0f";
        }
        if ("double".equals(normalized)) {
            return "0.0d";
        }
        if ("char".equals(normalized)) {
            return "'\\0'";
        }
        if ("byte".equals(normalized) || "short".equals(normalized) || "int".equals(normalized)) {
            return "0";
        }
        return "null";
    }

    private static String joined(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static boolean returnsOwnerType(DescribedType owner, MethodDescriptor method) {
        String returnType = method.returnType();
        return owner.qualifiedName().equals(returnType) || owner.simpleName().equals(returnType);
    }

    private static boolean canInstantiateOwner(DescribedType owner) {
        JavaTypeKind kind = owner.kind();
        return JavaTypeKind.CLASS.equals(kind)
                || JavaTypeKind.FINAL_CLASS.equals(kind)
                || JavaTypeKind.SEALED_CLASS.equals(kind)
                || JavaTypeKind.RECORD.equals(kind);
    }

    private static void appendParameters(StringBuilder builder, List<String> types, List<String> names) {
        for (int pi = 0; pi < types.size(); pi++) {
            if (pi > 0) {
                builder.append(", ");
            }
            builder.append(types.get(pi)).append(" ").append(names.get(pi));
        }
    }
}
