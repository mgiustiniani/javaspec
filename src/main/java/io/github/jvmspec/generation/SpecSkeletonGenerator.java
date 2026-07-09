package io.github.jvmspec.generation;

import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.model.DescribedClass;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds Java 8-compatible, PHPSpec-inspired specification and support skeleton generation plans.
 */
public final class SpecSkeletonGenerator {
    private static final File DEFAULT_SUPPORT_ROOT = new File("target/generated-sources/javaspec");

    private SpecSkeletonGenerator() {
    }

    public static SpecGenerationPlan plan(DescribedClass describedClass, File specRoot) {
        return plan(describedClass, specRoot, SpecNamingConvention.defaults());
    }

    public static SpecGenerationPlan plan(DescribedClass describedClass, File specRoot, SpecNamingConvention namingConvention) {
        return plan(DescribedType.of(describedClass), specRoot, namingConvention);
    }

    public static SpecGenerationPlan plan(DescribedType describedType, File specRoot) {
        return plan(describedType, specRoot, SpecNamingConvention.defaults());
    }

    public static SpecGenerationPlan plan(DescribedType describedType, File specRoot, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(specRoot, "specRoot must not be null");
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");

        File targetFile = new File(specRoot, namingConvention.specSourceRelativePath(describedType.describedClass()));
        return SpecGenerationPlan.of(
                describedType,
                namingConvention.specQualifiedName(describedType.describedClass()),
                namingConvention.specSimpleName(describedType.describedClass()),
                specRoot,
                targetFile,
                render(describedType, namingConvention)
        );
    }

    public static SpecGenerationPlan supportPlan(DescribedClass describedClass, File specRoot) {
        return supportPlan(describedClass, specRoot, SpecNamingConvention.defaults());
    }

    public static SpecGenerationPlan supportPlan(DescribedClass describedClass, File specRoot, SpecNamingConvention namingConvention) {
        return supportPlan(DescribedType.of(describedClass), specRoot, namingConvention);
    }

    public static SpecGenerationPlan supportPlan(DescribedType describedType, File specRoot) {
        return supportPlan(describedType, specRoot, SpecNamingConvention.defaults());
    }

    public static SpecGenerationPlan supportPlan(DescribedType describedType, File specRoot, SpecNamingConvention namingConvention) {
        return supportPlan(describedType, specRoot, DEFAULT_SUPPORT_ROOT, namingConvention);
    }

    /**
     * Creates a support plan, writing the target file to {@code supportRoot} instead of {@code specRoot}.
     *
     * @param describedType the described type
     * @param specRoot      the spec root directory (used for package resolution)
     * @param supportRoot   the root directory for the generated support file (e.g. target/generated-sources/javaspec)
     * @param namingConvention the spec naming convention
     * @return a spec generation plan for the support file
     */
    public static SpecGenerationPlan supportPlan(DescribedType describedType, File specRoot, File supportRoot, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(specRoot, "specRoot must not be null");
        Objects.requireNonNull(supportRoot, "supportRoot must not be null");
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");

        File targetFile = new File(supportRoot, namingConvention.supportSourceRelativePath(describedType.describedClass()));
        return SpecGenerationPlan.of(
                describedType,
                namingConvention.supportQualifiedName(describedType.describedClass()),
                namingConvention.supportSimpleName(describedType.describedClass()),
                specRoot,
                targetFile,
                renderSupport(describedType, namingConvention)
        );
    }

    public static String render(DescribedClass describedClass) {
        return render(describedClass, SpecNamingConvention.defaults());
    }

    public static String render(DescribedClass describedClass, SpecNamingConvention namingConvention) {
        return render(DescribedType.of(describedClass), namingConvention);
    }

    public static String render(DescribedType describedType) {
        return render(describedType, SpecNamingConvention.defaults());
    }

    public static String render(DescribedType describedType, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");

        StringBuilder builder = new StringBuilder();
        String specPackageName = namingConvention.specPackageName(describedType.describedClass());
        if (specPackageName.length() > 0) {
            builder.append("package ").append(specPackageName).append(";\n\n");
        }
        appendImports(builder, describedType);
        if (hasImports(describedType)) {
            builder.append("\n");
        }
        builder.append("public class ").append(namingConvention.specSimpleName(describedType.describedClass()))
                .append(" extends ").append(namingConvention.supportSimpleName(describedType.describedClass())).append(" {\n");
        builder.append("    public void it_is_initializable() {\n");
        builder.append("        shouldHaveType(").append(describedType.simpleName()).append(".class);\n");
        builder.append("    }\n");
        appendKindMarker(builder, describedType.kind());
        if (JavaTypeKind.ENUM.equals(describedType.kind())) {
            appendEnumConstantPlaceholder(builder, describedType);
        }
        appendRelationshipMarker(builder, "it_extends_expected_type", "shouldExtend", describedType.extendedTypeNames());
        appendRelationshipMarker(builder, "it_implements_expected_types", "shouldImplement", describedType.implementedTypeNames());
        appendRelationshipMarker(builder, "it_permits_expected_types", "shouldPermit", describedType.permittedTypeNames());
        builder.append("}\n");
        return builder.toString();
    }

    public static String renderSupport(DescribedType describedType) {
        return renderSupport(describedType, SpecNamingConvention.defaults());
    }

    public static String renderSupport(DescribedType describedType, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");

        StringBuilder builder = new StringBuilder();
        String specPackageName = namingConvention.specPackageName(describedType.describedClass());
        if (specPackageName.length() > 0) {
            builder.append("package ").append(specPackageName).append(";\n\n");
        }
        appendSubjectImport(builder, describedType);
        if (describedType.hasPackage()) {
            builder.append("\n");
        }
        builder.append("public class ").append(namingConvention.supportSimpleName(describedType.describedClass()))
                .append(" extends io.github.jvmspec.api.ObjectBehavior<")
                .append(describedType.simpleName()).append("> {\n");
        builder.append("    public ").append(namingConvention.supportSimpleName(describedType.describedClass())).append("() {\n");
        builder.append("        super(").append(describedType.simpleName()).append(".class);\n");
        builder.append("    }\n");
        if (hasInstanceSubjectMethods(describedType)) {
            builder.append("\n");
            appendSupportProxyMethods(builder, describedType);
            builder.append("\n");
            appendThrowProxy(builder, describedType, namingConvention);
        }
        builder.append("}\n");
        return builder.toString();
    }

    private static void appendImports(StringBuilder builder, DescribedType describedType) {
        List<String> imports = new ArrayList<String>();
        addImport(imports, describedType.qualifiedName());
        addImports(imports, describedType.extendedTypeNames());
        addImports(imports, describedType.implementedTypeNames());
        addImports(imports, describedType.permittedTypeNames());
        for (int i = 0; i < imports.size(); i++) {
            builder.append("import ").append(imports.get(i)).append(";\n");
        }
    }

    private static boolean hasImports(DescribedType describedType) {
        return describedType.qualifiedName().indexOf('.') >= 0
                || !describedType.extendedTypeNames().isEmpty()
                || !describedType.implementedTypeNames().isEmpty()
                || !describedType.permittedTypeNames().isEmpty();
    }

    private static void appendSubjectImport(StringBuilder builder, DescribedType describedType) {
        if (describedType.qualifiedName().indexOf('.') >= 0) {
            builder.append("import ").append(describedType.qualifiedName()).append(";\n");
        }
    }

    private static void addImports(List<String> imports, List<String> typeNames) {
        for (int i = 0; i < typeNames.size(); i++) {
            addImport(imports, typeNames.get(i));
        }
    }

    private static void addImport(List<String> imports, String typeName) {
        if (typeName.indexOf('.') < 0) {
            return;
        }
        if (!imports.contains(typeName)) {
            imports.add(typeName);
        }
    }

    private static void appendKindMarker(StringBuilder builder, JavaTypeKind kind) {
        String marker = kindMarker(kind);
        if (marker == null) {
            return;
        }
        builder.append("\n");
        builder.append("    public void it_is_a_").append(kind.displayName().replace(' ', '_')).append("() {\n");
        builder.append("        ").append(marker).append("();\n");
        builder.append("    }\n");
    }

    private static String kindMarker(JavaTypeKind kind) {
        if (JavaTypeKind.FINAL_CLASS.equals(kind)) {
            return "shouldBeAFinalClass";
        }
        if (JavaTypeKind.INTERFACE.equals(kind)) {
            return "shouldBeAnInterface";
        }
        if (JavaTypeKind.ENUM.equals(kind)) {
            return "shouldBeAnEnum";
        }
        if (JavaTypeKind.ANNOTATION.equals(kind)) {
            return "shouldBeAnAnnotation";
        }
        if (JavaTypeKind.RECORD.equals(kind)) {
            return "shouldBeARecord";
        }
        if (JavaTypeKind.SEALED_CLASS.equals(kind)) {
            return "shouldBeASealedClass";
        }
        if (JavaTypeKind.SEALED_INTERFACE.equals(kind)) {
            return "shouldBeASealedInterface";
        }
        return null;
    }

    private static void appendEnumConstantPlaceholder(StringBuilder builder, DescribedType describedType) {
        builder.append("\n");
        builder.append("    public void it_has_expected_constants() {\n");
        builder.append("        // Declare enum constants via shouldHaveConstant(\"CONSTANT_NAME\")\n");
        builder.append("        // Example: shouldHaveConstant(\"EC_P256\");\n");
        builder.append("    }\n");
    }

    private static void appendRelationshipMarker(StringBuilder builder, String methodName, String markerName, List<String> typeNames) {
        if (typeNames.isEmpty()) {
            return;
        }
        builder.append("\n");
        builder.append("    public void ").append(methodName).append("() {\n");
        builder.append("        ").append(markerName).append("(");
        for (int i = 0; i < typeNames.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(simpleName(typeNames.get(i))).append(".class");
        }
        builder.append(");\n");
        builder.append("    }\n");
    }

    static boolean hasInstanceSubjectMethods(DescribedType describedType) {
        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            if (isSupportSubjectMethod(methods.get(i))) {
                return true;
            }
        }
        return false;
    }

    static void appendSupportProxyMethods(StringBuilder builder, DescribedType describedType) {
        List<MethodDescriptor> methods = describedType.methods();
        boolean appendedAny = false;
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (!isSupportSubjectMethod(method)) {
                continue;
            }
            if (appendedAny) {
                builder.append("\n");
            }
            appendSupportProxyMethod(builder, method);
            appendedAny = true;
        }
    }

    static boolean isSupportSubjectMethod(MethodDescriptor method) {
        return !method.isStatic() && !isObjectOverrideMethod(method);
    }

    private static boolean isObjectOverrideMethod(MethodDescriptor method) {
        return "equals".equals(method.methodName()) && method.parameterTypes().size() == 1
                && isObjectType(method.parameterTypes().get(0))
                || "hashCode".equals(method.methodName()) && !method.hasParameters()
                || "toString".equals(method.methodName()) && !method.hasParameters();
    }

    private static boolean isObjectType(String typeName) {
        return "Object".equals(typeName) || "java.lang.Object".equals(typeName);
    }

    static void appendSupportProxyMethod(StringBuilder builder, MethodDescriptor method) {
        if (!isSupportSubjectMethod(method)) {
            return;
        }
        if (method.isVoid()) {
            builder.append("    protected void ").append(method.methodName()).append("(");
            appendParameters(builder, method.parameterTypes(), method.parameterNames(), false);
            builder.append(") {\n");
            builder.append("        subject().").append(method.methodName()).append("(");
            appendArgumentNames(builder, method.parameterNames());
            builder.append(");\n");
            builder.append("    }\n");
            return;
        }
        builder.append("    protected io.github.jvmspec.matcher.Matchable<")
                .append(boxedType(method.returnType())).append("> ")
                .append(method.methodName()).append("(");
        appendParameters(builder, method.parameterTypes(), method.parameterNames(), false);
        builder.append(") {\n");
        builder.append("        return match(subject().").append(method.methodName()).append("(");
        appendArgumentNames(builder, method.parameterNames());
        builder.append("));\n");
        builder.append("    }\n");
    }

    static void appendThrowProxy(StringBuilder builder, DescribedType describedType) {
        appendThrowProxy(builder, describedType, SpecNamingConvention.defaults());
    }

    static void appendThrowProxy(StringBuilder builder, DescribedType describedType, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");
        String throwType = describedType.simpleName() + "ThrowExpectation";
        builder.append("    @Override\n");
        builder.append("    public ").append(throwType).append(" shouldThrow(Class<? extends Throwable> expectedType) {\n");
        builder.append("        return new ").append(throwType).append("(expectedType);\n");
        builder.append("    }\n\n");
        builder.append("    protected class ").append(throwType)
                .append(" extends io.github.jvmspec.api.ObjectBehavior.ThrowExpectation {\n");
        builder.append("        protected ").append(throwType).append("(Class<? extends Throwable> expectedType) {\n");
        builder.append("            super(").append(namingConvention.supportSimpleName(describedType.describedClass())).append(".this, expectedType);\n");
        builder.append("        }\n");
        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (!isSupportSubjectMethod(method)) {
                continue;
            }
            builder.append("\n");
            appendDuringMethod(builder, method);
        }
        builder.append("    }\n");
    }

    static void appendDuringMethod(StringBuilder builder, MethodDescriptor method) {
        if (!isSupportSubjectMethod(method)) {
            return;
        }
        builder.append("        public void during").append(capitalize(method.methodName())).append("(");
        appendParameters(builder, method.parameterTypes(), method.parameterNames(), true);
        builder.append(") {\n");
        builder.append("            during(new io.github.jvmspec.api.ObjectBehavior.ThrowingRunnable() {\n");
        builder.append("                @Override\n");
        builder.append("                public void run() throws Throwable {\n");
        builder.append("                    subject().").append(method.methodName()).append("(");
        appendArgumentNames(builder, method.parameterNames());
        builder.append(");\n");
        builder.append("                }\n");
        builder.append("            });\n");
        builder.append("        }\n");
    }

    static String boxedType(String typeName) {
        if ("boolean".equals(typeName)) return "Boolean";
        if ("byte".equals(typeName)) return "Byte";
        if ("short".equals(typeName)) return "Short";
        if ("int".equals(typeName)) return "Integer";
        if ("long".equals(typeName)) return "Long";
        if ("float".equals(typeName)) return "Float";
        if ("double".equals(typeName)) return "Double";
        if ("char".equals(typeName)) return "Character";
        return typeName;
    }

    private static void appendParameters(StringBuilder builder, List<String> types, List<String> names, boolean finalParameters) {
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            if (finalParameters) {
                builder.append("final ");
            }
            builder.append(types.get(i)).append(" ").append(names.get(i));
        }
    }

    private static void appendArgumentNames(StringBuilder builder, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(names.get(i));
        }
    }

    static String capitalize(String value) {
        if (value.length() == 0) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot < 0) {
            return qualifiedName;
        }
        return qualifiedName.substring(lastDot + 1);
    }

    public static String specSimpleName(DescribedClass describedClass) {
        return specSimpleName(describedClass, SpecNamingConvention.defaults());
    }

    public static String specSimpleName(DescribedClass describedClass, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");
        return namingConvention.specSimpleName(describedClass);
    }

    public static String supportSimpleName(DescribedClass describedClass) {
        return supportSimpleName(describedClass, SpecNamingConvention.defaults());
    }

    public static String supportSimpleName(DescribedClass describedClass, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");
        return namingConvention.supportSimpleName(describedClass);
    }

    public static String specQualifiedName(DescribedClass describedClass) {
        return specQualifiedName(describedClass, SpecNamingConvention.defaults());
    }

    public static String specQualifiedName(DescribedClass describedClass, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");
        return namingConvention.specQualifiedName(describedClass);
    }

    public static String supportQualifiedName(DescribedClass describedClass) {
        return supportQualifiedName(describedClass, SpecNamingConvention.defaults());
    }

    public static String supportQualifiedName(DescribedClass describedClass, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");
        return namingConvention.supportQualifiedName(describedClass);
    }

    public static String sourceRelativePath(DescribedClass describedClass) {
        return sourceRelativePath(describedClass, SpecNamingConvention.defaults());
    }

    public static String sourceRelativePath(DescribedClass describedClass, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");
        return namingConvention.specSourceRelativePath(describedClass);
    }

    public static String supportSourceRelativePath(DescribedClass describedClass) {
        return supportSourceRelativePath(describedClass, SpecNamingConvention.defaults());
    }

    public static String supportSourceRelativePath(DescribedClass describedClass, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");
        return namingConvention.supportSourceRelativePath(describedClass);
    }

    public static String specPackageName(DescribedClass describedClass) {
        return specPackageName(describedClass, SpecNamingConvention.defaults());
    }

    public static String specPackageName(DescribedClass describedClass, SpecNamingConvention namingConvention) {
        Objects.requireNonNull(namingConvention, "namingConvention must not be null");
        return namingConvention.specPackageName(describedClass);
    }
}
