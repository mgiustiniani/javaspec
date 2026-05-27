package org.javaspec.generation;

import org.javaspec.model.DescribedClass;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds Java 8-compatible, PHPSpec-inspired specification skeleton generation plans.
 */
public final class SpecSkeletonGenerator {
    private static final String SPEC_PACKAGE_PREFIX = "spec";
    private static final String SPEC_SUFFIX = "Spec";

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
        builder.append("import org.javaspec.api.ObjectBehavior;\n\n");
        builder.append("public class ").append(specSimpleName(describedType.describedClass()))
                .append(" extends ObjectBehavior<").append(describedType.simpleName()).append("> {\n");
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

    public static String specQualifiedName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        String specPackageName = specPackageName(describedClass);
        if (specPackageName.length() == 0) {
            return specSimpleName(describedClass);
        }
        return specPackageName + "." + specSimpleName(describedClass);
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

    public static String specPackageName(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        if (!describedClass.hasPackage()) {
            return "";
        }
        return SPEC_PACKAGE_PREFIX + "." + describedClass.packageName();
    }
}
