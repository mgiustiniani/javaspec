package io.github.jvmspec.generation;

import io.github.jvmspec.internal.type.JavaTypeImportPlan;
import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.io.File;
import java.util.ArrayList;
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
        if (JavaTypeKind.RECORD.equals(describedType.kind())) {
            JavaTypeImportPlan importPlan = recordImportPlan(describedType);
            appendImports(builder, importPlan);
            appendRecord(builder, describedType, importPlan);
            return builder.toString();
        }
        appendDeclaration(builder, describedType);
        return builder.toString();
    }

    private static void appendDeclaration(StringBuilder builder, DescribedType describedType) {
        JavaTypeKind kind = describedType.kind();
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
        if (JavaTypeKind.ANNOTATION.equals(kind)) {
            appendAnnotation(builder, describedType);
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
        appendInterfaceBodyOrClose(builder, describedType);
    }

    private static void appendAnnotation(StringBuilder builder, DescribedType describedType) {
        List<MethodDescriptor> elements = annotationElementMethods(describedType);
        builder.append("public @interface ").append(describedType.simpleName());
        if (elements.isEmpty()) {
            builder.append(" { }\n");
            return;
        }
        builder.append(" {\n");
        appendAnnotationElements(builder, describedType, elements);
        builder.append("}\n");
    }

    private static void appendInterfaceBodyOrClose(StringBuilder builder, DescribedType describedType) {
        List<MethodDescriptor> methods = interfaceMethods(describedType);
        if (methods.isEmpty()) {
            builder.append(" { }\n");
            return;
        }
        builder.append(" {\n");
        appendMethodDeclarations(builder, describedType, methods);
        builder.append("}\n");
    }

    private static void appendEnum(StringBuilder builder, DescribedType describedType) {
        builder.append("public enum ").append(describedType.simpleName());
        appendImplements(builder, describedType);
        List<DescribedType.EnumConstantInfo> constants = describedType.enumConstants();
        boolean hasConstants = !constants.isEmpty();
        boolean hasConstructors = describedType.hasConstructors();
        boolean hasMethods = describedType.hasMethods();
        if (hasConstants || hasConstructors || hasMethods) {
            builder.append(" {\n");
            if (hasConstants) {
                for (int ci = 0; ci < constants.size(); ci++) {
                    DescribedType.EnumConstantInfo ciInfo = constants.get(ci);
                    builder.append("    ").append(ciInfo.name());
                    if (ciInfo.hasParameters()) {
                        builder.append("(");
                        appendEnumConstantArgs(builder, ciInfo);
                        builder.append(")");
                    }
                    if (ci < constants.size() - 1) {
                        builder.append(",");
                    } else {
                        builder.append(";");
                    }
                    builder.append("\n");
                }
            } else {
                builder.append("    ;\n");
            }
            if (hasConstructors || hasMethods) {
                builder.append("\n");
            }
            if (hasConstructors) {
                appendConstructors(builder, describedType);
            }
            if (hasConstructors && hasMethods) {
                builder.append("\n");
            }
            if (hasMethods) {
                appendMethods(builder, describedType);
            }
            builder.append("}\n");
        } else {
            builder.append(" { }\n");
        }
    }

    private static void appendRecord(
            StringBuilder builder,
            DescribedType describedType,
            JavaTypeImportPlan importPlan
    ) {
        List<RecordComponentPlanner.Component> components = RecordComponentPlanner.componentsFor(describedType);
        builder.append("public record ").append(describedType.simpleName()).append("(");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) builder.append(", ");
            RecordComponentPlanner.Component component = components.get(i);
            builder.append(importPlan.render(component.type())).append(" ").append(component.name());
        }
        builder.append(")");
        appendImplements(builder, describedType);
        List<MethodDescriptor> explicitMethods = explicitRecordMethods(describedType.methods(), components);
        if (!explicitMethods.isEmpty()) {
            builder.append(" {\n");
            appendMethods(builder, describedType, explicitMethods, "    ");
            builder.append("}\n");
        } else {
            builder.append(" { }\n");
        }
    }

    private static JavaTypeImportPlan recordImportPlan(DescribedType describedType) {
        List<String> types = new ArrayList<String>();
        List<RecordComponentPlanner.Component> components = RecordComponentPlanner.componentsFor(describedType);
        for (int i = 0; i < components.size(); i++) {
            types.add(components.get(i).type());
        }
        return JavaTypeImportPlan.forTypes(describedType.packageName(), types);
    }

    private static void appendImports(StringBuilder builder, JavaTypeImportPlan importPlan) {
        List<String> imports = importPlan.imports();
        for (int i = 0; i < imports.size(); i++) {
            builder.append("import ").append(imports.get(i)).append(";\n");
        }
        if (!imports.isEmpty()) builder.append("\n");
    }

    private static List<MethodDescriptor> explicitRecordMethods(
            List<MethodDescriptor> methods,
            List<RecordComponentPlanner.Component> components
    ) {
        List<MethodDescriptor> explicit = new ArrayList<MethodDescriptor>();
        for (int methodIndex = 0; methodIndex < methods.size(); methodIndex++) {
            MethodDescriptor method = methods.get(methodIndex);
            boolean implicitAccessor = false;
            for (int componentIndex = 0; componentIndex < components.size(); componentIndex++) {
                if (RecordComponentPlanner.isImplicitAccessor(components.get(componentIndex), method)) {
                    implicitAccessor = true;
                    break;
                }
            }
            if (!implicitAccessor) {
                explicit.add(method);
            }
        }
        return explicit;
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
        List<MethodDescriptor> methods = interfaceMethods(describedType);
        builder.append("public sealed interface ").append(describedType.simpleName());
        appendInterfaceExtends(builder, describedType);
        if (describedType.hasPermittedTypes()) {
            builder.append(" permits ").append(nestedPermitsList(describedType)).append(" {\n");
            appendSealedInterfaceMethods(builder, describedType, methods);
            appendNestedInterfacePermittedTypes(builder, describedType, methods);
            builder.append("}\n");
            return;
        }
        builder.append(" permits ").append(describedType.simpleName()).append(".Permitted {\n");
        appendSealedInterfaceMethods(builder, describedType, methods);
        appendNestedInterfacePermittedType(builder, describedType, "Permitted", methods);
        builder.append("}\n");
    }

    private static void appendSealedInterfaceMethods(StringBuilder builder, DescribedType describedType, List<MethodDescriptor> methods) {
        if (methods.isEmpty()) {
            return;
        }
        appendMethodDeclarations(builder, describedType, methods);
        builder.append("\n");
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

    private static void appendNestedInterfacePermittedTypes(
            StringBuilder builder,
            DescribedType describedType,
            List<MethodDescriptor> methods
    ) {
        List<String> typeNames = describedType.permittedTypeNames();
        for (int i = 0; i < typeNames.size(); i++) {
            appendNestedInterfacePermittedType(builder, describedType, simpleName(typeNames.get(i)), methods);
        }
    }

    private static void appendNestedInterfacePermittedType(
            StringBuilder builder,
            DescribedType describedType,
            String simpleName,
            List<MethodDescriptor> methods
    ) {
        builder.append("    final class ").append(simpleName)
                .append(" implements ").append(describedType.simpleName());
        if (methods.isEmpty()) {
            builder.append(" { }\n");
            return;
        }
        builder.append(" {\n");
        appendMethods(builder, describedType, methods, "        ");
        builder.append("    }\n");
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
        String modifier = JavaTypeKind.ENUM.equals(describedType.kind()) ? "private" : "public";
        List<ConstructorDescriptor> constructors = describedType.constructors();
        for (int ci = 0; ci < constructors.size(); ci++) {
            ConstructorDescriptor constructor = constructors.get(ci);
            builder.append("    ").append(modifier).append(" ").append(describedType.simpleName()).append("(");
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
        appendMethods(builder, describedType, describedType.methods(), "    ");
    }

    private static void appendMethods(
            StringBuilder builder,
            DescribedType describedType,
            List<MethodDescriptor> methods,
            String indent
    ) {
        for (int mi = 0; mi < methods.size(); mi++) {
            MethodDescriptor method = methods.get(mi);
            appendMethod(builder, describedType, method, indent);
            if (mi < methods.size() - 1) {
                builder.append("\n");
            }
        }
    }

    private static void appendMethodDeclarations(StringBuilder builder, DescribedType describedType, List<MethodDescriptor> methods) {
        for (int mi = 0; mi < methods.size(); mi++) {
            MethodDescriptor method = methods.get(mi);
            appendMethodDeclaration(builder, describedType, method);
            if (mi < methods.size() - 1) {
                builder.append("\n");
            }
        }
    }

    private static void appendMethodDeclaration(StringBuilder builder, DescribedType owner, MethodDescriptor method) {
        builder.append("    ").append(sourceTypeName(owner, method.returnType())).append(" ")
                .append(method.methodName()).append("(");
        appendParameters(builder, method.parameterTypes(), method.parameterNames());
        builder.append(");\n");
    }

    private static void appendAnnotationElements(StringBuilder builder, DescribedType describedType, List<MethodDescriptor> methods) {
        for (int mi = 0; mi < methods.size(); mi++) {
            MethodDescriptor method = methods.get(mi);
            builder.append("    ").append(sourceTypeName(describedType, method.returnType())).append(" ")
                    .append(method.methodName()).append("();\n");
            if (mi < methods.size() - 1) {
                builder.append("\n");
            }
        }
    }

    private static List<MethodDescriptor> interfaceMethods(DescribedType describedType) {
        List<MethodDescriptor> result = new ArrayList<MethodDescriptor>();
        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (!method.isStatic()) {
                result.add(method);
            }
        }
        return result;
    }

    private static List<MethodDescriptor> annotationElementMethods(DescribedType describedType) {
        List<MethodDescriptor> result = new ArrayList<MethodDescriptor>();
        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (isCompatibleAnnotationElement(method)) {
                result.add(method);
            }
        }
        return result;
    }

    private static boolean isCompatibleAnnotationElement(MethodDescriptor method) {
        return !method.isStatic()
                && !method.hasParameters()
                && isCompatibleAnnotationElementReturnType(method.returnType());
    }

    private static boolean isCompatibleAnnotationElementReturnType(String returnType) {
        String normalized = returnType.trim().replace(" ", "");
        int arrayDimensions = 0;
        while (normalized.endsWith("[]")) {
            arrayDimensions++;
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        if (arrayDimensions > 1
                || "void".equals(normalized)
                || isKnownInvalidAnnotationElementType(normalized)) {
            return false;
        }
        if (isPrimitiveType(normalized)
                || "String".equals(normalized)
                || "java.lang.String".equals(normalized)
                || "Class".equals(normalized)
                || "java.lang.Class".equals(normalized)
                || isClassElementType(normalized)) {
            return true;
        }
        if (normalized.indexOf('<') >= 0 || normalized.indexOf('?') >= 0) {
            return false;
        }
        return isQualifiedTypeName(normalized);
    }

    private static boolean isPrimitiveType(String typeName) {
        return "boolean".equals(typeName)
                || "byte".equals(typeName)
                || "short".equals(typeName)
                || "int".equals(typeName)
                || "long".equals(typeName)
                || "float".equals(typeName)
                || "double".equals(typeName)
                || "char".equals(typeName);
    }

    private static boolean isKnownInvalidAnnotationElementType(String typeName) {
        return "Object".equals(typeName)
                || "java.lang.Object".equals(typeName)
                || "Void".equals(typeName)
                || "java.lang.Void".equals(typeName)
                || "Boolean".equals(typeName)
                || "java.lang.Boolean".equals(typeName)
                || "Byte".equals(typeName)
                || "java.lang.Byte".equals(typeName)
                || "Short".equals(typeName)
                || "java.lang.Short".equals(typeName)
                || "Integer".equals(typeName)
                || "java.lang.Integer".equals(typeName)
                || "Long".equals(typeName)
                || "java.lang.Long".equals(typeName)
                || "Float".equals(typeName)
                || "java.lang.Float".equals(typeName)
                || "Double".equals(typeName)
                || "java.lang.Double".equals(typeName)
                || "Character".equals(typeName)
                || "java.lang.Character".equals(typeName);
    }

    private static boolean isClassElementType(String typeName) {
        return (typeName.startsWith("Class<") || typeName.startsWith("java.lang.Class<"))
                && typeName.endsWith(">");
    }

    private static boolean isQualifiedTypeName(String typeName) {
        if (typeName.length() == 0) {
            return false;
        }
        String[] parts = typeName.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if (!isJavaIdentifier(parts[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJavaIdentifier(String value) {
        if (value.length() == 0) {
            return false;
        }
        int index = 0;
        int firstCodePoint = value.codePointAt(index);
        if (!Character.isJavaIdentifierStart(firstCodePoint)) {
            return false;
        }
        index += Character.charCount(firstCodePoint);
        while (index < value.length()) {
            int currentCodePoint = value.codePointAt(index);
            if (!Character.isJavaIdentifierPart(currentCodePoint)) {
                return false;
            }
            index += Character.charCount(currentCodePoint);
        }
        return true;
    }

    static void appendMethod(StringBuilder builder, DescribedType owner, MethodDescriptor method) {
        appendMethod(builder, owner, method, "    ");
    }

    private static void appendMethod(
            StringBuilder builder,
            DescribedType owner,
            MethodDescriptor method,
            String indent
    ) {
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
        for (int pi = 0; pi < types.size(); pi++) {
            if (pi > 0) {
                builder.append(", ");
            }
            builder.append(types.get(pi)).append(" ").append(names.get(pi));
        }
    }

    /**
     * Appends enum constant arguments using original literal values from the spec when available,
     * otherwise falls back to default values by type.
     */
    private static void appendEnumConstantArgs(StringBuilder builder, DescribedType.EnumConstantInfo ciInfo) {
        List<String> types = ciInfo.parameterTypes();
        List<String> values = ciInfo.hasParameterValues() ? ciInfo.parameterValues() : null;
        for (int pi = 0; pi < types.size(); pi++) {
            if (pi > 0) {
                builder.append(", ");
            }
            if (values != null) {
                builder.append(values.get(pi));
            } else {
                builder.append(defaultValueForType(types.get(pi)));
            }
        }
    }

    private static String defaultValueForType(String typeName) {
        if ("int".equals(typeName) || "long".equals(typeName) || "short".equals(typeName) || "byte".equals(typeName)) return "0";
        if ("double".equals(typeName) || "float".equals(typeName)) return "0.0";
        if ("boolean".equals(typeName)) return "false";
        if ("char".equals(typeName)) return "'\\0'";
        return "null";
    }
}
