package io.github.jvmspec.generation;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.util.List;

/** Renders deterministic Java method, interface, annotation, and factory skeletons. */
final class JavaMethodRenderer {
    private JavaMethodRenderer() {
    }

    static String renderMissingMethods(List<MethodDescriptor> methods, DescribedType owner, String indent) {
        JavaTypeKind kind = owner.kind();
        if (JavaTypeMethodCapabilities.supportsBodies(kind)) {
            return renderMethods(methods, owner, indent);
        }
        if (JavaTypeMethodCapabilities.supportsInterfaceDeclarations(kind)) {
            return renderInterfaceDeclarations(methods, owner, indent);
        }
        if (JavaTypeMethodCapabilities.supportsAnnotationElements(kind)) {
            return renderAnnotationElements(methods, owner, indent);
        }
        return "";
    }

    static String renderMethods(List<MethodDescriptor> methods, DescribedType owner, String indent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            appendMethod(builder, owner, method, indent);
            if (i < methods.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    static String renderInterfaceDeclarations(List<MethodDescriptor> methods, DescribedType owner, String indent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            appendInterfaceDeclaration(builder, owner, method, indent);
            if (i < methods.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    static String renderAnnotationElements(List<MethodDescriptor> methods, DescribedType owner, String indent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            builder.append(indent).append(sourceTypeName(owner, method.returnType())).append(" ")
                    .append(method.methodName()).append("();\n");
            if (i < methods.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private static void appendInterfaceDeclaration(StringBuilder builder, DescribedType owner, MethodDescriptor method, String indent) {
        builder.append(indent).append(sourceTypeName(owner, method.returnType())).append(" ")
                .append(method.methodName()).append("(");
        appendParameters(builder, method.parameterTypes(), method.parameterNames());
        builder.append(");\n");
    }

    private static void appendMethod(StringBuilder builder, DescribedType owner, MethodDescriptor method, String indent) {
        builder.append(indent).append("public ");
        if (method.isStatic()) {
            builder.append("static ");
        }
        builder.append(sourceTypeName(owner, method.returnType())).append(" ")
                .append(method.methodName()).append("(");
        appendParameters(builder, method.parameterTypes(), method.parameterNames());
        builder.append(") {\n");
        builder.append(indent).append("    ").append(StubMarkerScanner.STUB_MARKER).append("\n");
        if (!method.isVoid()) {
            builder.append(indent).append("    ").append(defaultReturnStatement(owner, method)).append("\n");
        }
        builder.append(indent).append("}\n");
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
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(types.get(i)).append(" ").append(names.get(i));
        }
    }

    private static String sourceTypeName(DescribedType owner, String typeName) {
        String packageName = owner.packageName();
        if (packageName.length() > 0 && typeName.startsWith(packageName + ".")) {
            return typeName.substring(packageName.length() + 1);
        }
        return typeName;
    }
}
