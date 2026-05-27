package org.javaspec.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable description of a Java class-like type by qualified name, kind, and type relationships.
 */
public final class DescribedType {
    private final DescribedClass describedClass;
    private final JavaTypeKind kind;
    private final List<String> extendedTypeNames;
    private final List<String> implementedTypeNames;
    private final List<String> permittedTypeNames;

    private DescribedType(
            DescribedClass describedClass,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames
    ) {
        this.describedClass = describedClass;
        this.kind = kind;
        this.extendedTypeNames = extendedTypeNames;
        this.implementedTypeNames = implementedTypeNames;
        this.permittedTypeNames = permittedTypeNames;
    }

    public static DescribedType classNamed(String typeName) {
        return of(typeName, JavaTypeKind.CLASS);
    }

    public static DescribedType of(String typeName) {
        return classNamed(typeName);
    }

    public static DescribedType of(String typeName, JavaTypeKind kind) {
        return of(typeName, kind, empty(), empty(), empty());
    }

    public static DescribedType of(String typeName, JavaTypeKind kind, List<String> permittedTypeNames) {
        return of(typeName, kind, empty(), empty(), permittedTypeNames);
    }

    public static DescribedType of(
            String typeName,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames
    ) {
        return new DescribedType(
                DescribedClass.of(typeName),
                Objects.requireNonNull(kind, "kind must not be null"),
                validatedTypeNames(extendedTypeNames, "extendedTypeNames"),
                validatedTypeNames(implementedTypeNames, "implementedTypeNames"),
                validatedTypeNames(permittedTypeNames, "permittedTypeNames")
        );
    }

    public static DescribedType of(DescribedClass describedClass) {
        return of(describedClass, JavaTypeKind.CLASS);
    }

    public static DescribedType of(DescribedClass describedClass, JavaTypeKind kind) {
        return of(describedClass, kind, empty(), empty(), empty());
    }

    public static DescribedType of(DescribedClass describedClass, JavaTypeKind kind, List<String> permittedTypeNames) {
        return of(describedClass, kind, empty(), empty(), permittedTypeNames);
    }

    public static DescribedType of(
            DescribedClass describedClass,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames
    ) {
        return new DescribedType(
                Objects.requireNonNull(describedClass, "describedClass must not be null"),
                Objects.requireNonNull(kind, "kind must not be null"),
                validatedTypeNames(extendedTypeNames, "extendedTypeNames"),
                validatedTypeNames(implementedTypeNames, "implementedTypeNames"),
                validatedTypeNames(permittedTypeNames, "permittedTypeNames")
        );
    }

    public DescribedClass describedClass() {
        return describedClass;
    }

    public JavaTypeKind kind() {
        return kind;
    }

    public List<String> extendedTypeNames() {
        return extendedTypeNames;
    }

    public boolean hasExtendedTypes() {
        return !extendedTypeNames.isEmpty();
    }

    public List<String> implementedTypeNames() {
        return implementedTypeNames;
    }

    public boolean hasImplementedTypes() {
        return !implementedTypeNames.isEmpty();
    }

    public List<String> permittedTypeNames() {
        return permittedTypeNames;
    }

    public boolean hasPermittedTypes() {
        return !permittedTypeNames.isEmpty();
    }

    public String qualifiedName() {
        return describedClass.qualifiedName();
    }

    public String packageName() {
        return describedClass.packageName();
    }

    public String simpleName() {
        return describedClass.simpleName();
    }

    public boolean hasPackage() {
        return describedClass.hasPackage();
    }

    public String sourceRelativePath() {
        return describedClass.sourceRelativePath();
    }

    public boolean isClass() {
        return JavaTypeKind.CLASS.equals(kind);
    }

    public boolean isFinalClass() {
        return JavaTypeKind.FINAL_CLASS.equals(kind);
    }

    public boolean isInterface() {
        return JavaTypeKind.INTERFACE.equals(kind);
    }

    public boolean isEnum() {
        return JavaTypeKind.ENUM.equals(kind);
    }

    public boolean isAnnotation() {
        return JavaTypeKind.ANNOTATION.equals(kind);
    }

    public boolean isRecord() {
        return JavaTypeKind.RECORD.equals(kind);
    }

    public boolean isSealedClass() {
        return JavaTypeKind.SEALED_CLASS.equals(kind);
    }

    public boolean isSealedInterface() {
        return JavaTypeKind.SEALED_INTERFACE.equals(kind);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DescribedType)) {
            return false;
        }
        DescribedType that = (DescribedType) other;
        return describedClass.equals(that.describedClass)
                && kind == that.kind
                && extendedTypeNames.equals(that.extendedTypeNames)
                && implementedTypeNames.equals(that.implementedTypeNames)
                && permittedTypeNames.equals(that.permittedTypeNames);
    }

    @Override
    public int hashCode() {
        int result = describedClass.hashCode();
        result = 31 * result + kind.hashCode();
        result = 31 * result + extendedTypeNames.hashCode();
        result = 31 * result + implementedTypeNames.hashCode();
        result = 31 * result + permittedTypeNames.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return kind.displayName() + " " + qualifiedName();
    }

    private static List<String> empty() {
        return Collections.emptyList();
    }

    private static List<String> validatedTypeNames(List<String> typeNames, String parameterName) {
        Objects.requireNonNull(typeNames, parameterName + " must not be null");
        List<String> copy = new ArrayList<String>();
        for (int i = 0; i < typeNames.size(); i++) {
            String typeName = typeNames.get(i);
            copy.add(DescribedClass.of(typeName).qualifiedName());
        }
        return Collections.unmodifiableList(copy);
    }
}
