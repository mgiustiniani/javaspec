package org.javaspec.generation;

import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.model.MethodDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds Java 8-compatible, PHPSpec-inspired specification and support skeleton generation plans.
 */
public final class SpecSkeletonGenerator {
    private static final String SPEC_PACKAGE_PREFIX = "spec";
    private static final String SPEC_SUFFIX = "Spec";
    private static final String SUPPORT_SUFFIX = "SpecSupport";

    private SpecSkeletonGenerator() {
    }

    public static SpecGenerationPlan plan(DescribedClass describedClass, File specRoot) {
        return plan(DescribedType.of(describedClass), specRoot);
    }

    public static SpecGenerationPlan plan(DescribedType describedType, File specRoot) {
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(specRoot, "specRoot must not be null");

        File targetFile = new File(specRoot, sourceRelativePath(describedType.describedClass()));
        return SpecGenerationPlan.of(
                describedType,
                specQualifiedName(describedType.describedClass()),
                specSimpleName(describedType.describedClass()),
                specRoot,
                targetFile,
                render(describedType)
        );
    }

    public static SpecGenerationPlan supportPlan(DescribedClass describedClass, File specRoot) {
        return supportPlan(DescribedType.of(describedClass), specRoot);
    }

    public static SpecGenerationPlan supportPlan(DescribedType describedType, File specRoot) {
        Objects.requireNonNull(describedType, "describedType must not be null");
        Objects.requireNonNull(specRoot, "specRoot must not be null");

        File targetFile = new File(specRoot, supportSourceRelativePath(describedType.describedClass()));
        return SpecGenerationPlan.of(
                describedType,
                supportQualifiedName(describedType.describedClass()),
                supportSimpleName(describedType.describedClass()),
                specRoot,
                targetFile,
                renderSupport(describedType)
        );
    }

    public static String render(DescribedClass describedClass) {
        return render(DescribedType.of(describedClass));
    }

    public static String render(DescribedType describedType) {
        Objects.requireNonNull(describedType, "describedType must not be null");

        StringBuilder builder = new StringBuilder();
        String specPackageName = specPackageName(describedType.describedClass());
        if (specPackageName.length() > 0) {
            builder.append("package ").append(specPackageName).append(";\n\n");
        }
        appendImports(builder, describedType);
        if (hasImports(describedType)) {
            builder.append("\n");
        }
        builder.append("public class ").append(specSimpleName(describedType.describedClass()))
                .append(" extends ").append(supportSimpleName(describedType.describedClass())).append(" {\n");
        builder.append("    public void it_is_initializable() {\n");
        builder.append("        shouldHaveType(").append(describedType.simpleName()).append(".class);\n");
        builder.append("    }\n");
        appendKindMarker(builder, describedType.kind());
        appendRelationshipMarker(builder, "it_extends_expected_type", "shouldExtend", describedType.extendedTypeNames());
        appendRelationshipMarker(builder, "it_implements_expected_types", "shouldImplement", describedType.implementedTypeNames());
        appendRelationshipMarker(builder, "it_permits_expected_types", "shouldPermit", describedType.permittedTypeNames());
        builder.append("}\n");
        return builder.toString();
    }

    public static String renderSupport(DescribedType describedType) {
        Objects.requireNonNull(describedType, "describedType must not be null");

        StringBuilder builder = new StringBuilder();
        String specPackageName = specPackageName(describedType.describedClass());
        if (specPackageName.length() > 0) {
            builder.append("package ").append(specPackageName).append(";\n\n");
        }
        appendSubjectImport(builder, describedType);
        if (describedType.hasPackage()) {
            builder.append("\n");
        }
        builder.append("public class ").append(supportSimpleName(describedType.describedClass()))
                .append(" extends org.javaspec.api.ObjectBehavior<")
                .append(describedType.simpleName()).append("> {\n");
        builder.append("    public ").append(supportSimpleName(describedType.describedClass())).append("() {\n");
        builder.append("        super(").append(describedType.simpleName()).append(".class);\n");
        builder.append("    }\n");
        if (hasInstanceSubjectMethods(describedType)) {
            builder.append("\n");
            appendSupportProxyMethods(builder, describedType);
            builder.append("\n");
            appendThrowProxy(builder, describedType);
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
            if (!methods.get(i).isStatic()) {
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
            if (method.isStatic()) {
                continue;
            }
            if (appendedAny) {
                builder.append("\n");
            }
            appendSupportProxyMethod(builder, method);
            appendedAny = true;
        }
    }

    static void appendSupportProxyMethod(StringBuilder builder, MethodDescriptor method) {
        if (method.isStatic()) {
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
        builder.append("    protected org.javaspec.matcher.Matchable<")
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
        String throwType = describedType.simpleName() + "ThrowExpectation";
        builder.append("    @Override\n");
        builder.append("    public ").append(throwType).append(" shouldThrow(Class<? extends Throwable> expectedType) {\n");
        builder.append("        return new ").append(throwType).append("(expectedType);\n");
        builder.append("    }\n\n");
        builder.append("    protected class ").append(throwType)
                .append(" extends org.javaspec.api.ObjectBehavior.ThrowExpectation {\n");
        builder.append("        protected ").append(throwType).append("(Class<? extends Throwable> expectedType) {\n");
        builder.append("            super(").append(supportSimpleName(describedType.describedClass())).append(".this, expectedType);\n");
        builder.append("        }\n");
        List<MethodDescriptor> methods = describedType.methods();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            if (method.isStatic()) {
                continue;
            }
            builder.append("\n");
            appendDuringMethod(builder, method);
        }
        builder.append("    }\n");
    }

    static void appendDuringMethod(StringBuilder builder, MethodDescriptor method) {
        if (method.isStatic()) {
            return;
        }
        builder.append("        public void during").append(capitalize(method.methodName())).append("(");
        appendParameters(builder, method.parameterTypes(), method.parameterNames(), true);
        builder.append(") {\n");
        builder.append("            during(new org.javaspec.api.ObjectBehavior.ThrowingRunnable() {\n");
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
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        return describedClass.simpleName() + SPEC_SUFFIX;
    }

    public static String supportSimpleName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        return describedClass.simpleName() + SUPPORT_SUFFIX;
    }

    public static String specQualifiedName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        String specPackageName = specPackageName(describedClass);
        if (specPackageName.length() == 0) {
            return specSimpleName(describedClass);
        }
        return specPackageName + "." + specSimpleName(describedClass);
    }

    public static String supportQualifiedName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        String specPackageName = specPackageName(describedClass);
        if (specPackageName.length() == 0) {
            return supportSimpleName(describedClass);
        }
        return specPackageName + "." + supportSimpleName(describedClass);
    }

    public static String sourceRelativePath(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        String specPackageName = specPackageName(describedClass);
        if (specPackageName.length() == 0) {
            return specSimpleName(describedClass) + ".java";
        }
        return specPackageName.replace('.', File.separatorChar)
                + File.separator
                + specSimpleName(describedClass)
                + ".java";
    }

    public static String supportSourceRelativePath(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        String specPackageName = specPackageName(describedClass);
        if (specPackageName.length() == 0) {
            return supportSimpleName(describedClass) + ".java";
        }
        return specPackageName.replace('.', File.separatorChar)
                + File.separator
                + supportSimpleName(describedClass)
                + ".java";
    }

    public static String specPackageName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        if (!describedClass.hasPackage()) {
            return "";
        }
        return SPEC_PACKAGE_PREFIX + "." + describedClass.packageName();
    }
}
