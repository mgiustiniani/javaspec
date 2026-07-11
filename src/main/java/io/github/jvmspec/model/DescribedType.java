package io.github.jvmspec.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable description of a Java class-like type by qualified name, kind, relationships, constructors, and methods.
 */
public final class DescribedType {

    /**
     * Information about an enum constant declared in a spec via {@code shouldHaveConstant()}.
     */
    public static final class EnumConstantInfo {
        private final String name;
        private final List<String> parameterTypes;
        private final List<String> parameterNames;
        /** Original literal expressions from the spec, e.g. {"\"secp256r1\"", "256"} */
        private final List<String> parameterValues;

        private EnumConstantInfo(String name, List<String> parameterTypes, List<String> parameterNames, List<String> parameterValues) {
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.parameterTypes = validatedTypes(parameterTypes);
            this.parameterNames = validatedNames(parameterNames);
            this.parameterValues = validatedValues(parameterValues);
        }

        /**
         * Creates an enum constant info with no constructor parameters.
         */
        public static EnumConstantInfo of(String name) {
            return new EnumConstantInfo(name, Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList());
        }

        /**
         * Creates an enum constant info with the given constructor parameter types and names.
         */
        public static EnumConstantInfo of(String name, List<String> parameterTypes, List<String> parameterNames) {
            return new EnumConstantInfo(name, parameterTypes, parameterNames, Collections.<String>emptyList());
        }

        /**
         * Creates an enum constant info with the given constructor parameter types, names, and literal values.
         */
        public static EnumConstantInfo of(String name, List<String> parameterTypes, List<String> parameterNames, List<String> parameterValues) {
            return new EnumConstantInfo(name, parameterTypes, parameterNames, parameterValues);
        }

        public String name() {
            return name;
        }

        public boolean hasParameters() {
            return !parameterTypes.isEmpty();
        }

        public List<String> parameterTypes() {
            return parameterTypes;
        }

        public List<String> parameterNames() {
            return parameterNames;
        }

        public List<String> parameterValues() {
            return parameterValues;
        }

        public boolean hasParameterValues() {
            return parameterValues.size() == parameterTypes.size() && !parameterValues.isEmpty();
        }

        private static List<String> validatedTypes(List<String> types) {
            Objects.requireNonNull(types, "parameterTypes must not be null");
            List<String> copy = new ArrayList<String>(types);
            for (int i = 0; i < copy.size(); i++) {
                Objects.requireNonNull(copy.get(i), "parameterTypes[" + i + "] must not be null");
            }
            return Collections.unmodifiableList(copy);
        }

        private static List<String> validatedNames(List<String> names) {
            Objects.requireNonNull(names, "parameterNames must not be null");
            List<String> copy = new ArrayList<String>(names);
            for (int i = 0; i < copy.size(); i++) {
                Objects.requireNonNull(copy.get(i), "parameterNames[" + i + "] must not be null");
            }
            return Collections.unmodifiableList(copy);
        }

        private static List<String> validatedValues(List<String> values) {
            if (values == null) return Collections.unmodifiableList(Collections.<String>emptyList());
            List<String> copy = new ArrayList<String>(values);
            return Collections.unmodifiableList(copy);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof EnumConstantInfo)) return false;
            EnumConstantInfo that = (EnumConstantInfo) other;
            return name.equals(that.name) && parameterTypes.equals(that.parameterTypes) && parameterNames.equals(that.parameterNames);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + parameterTypes.hashCode();
            result = 31 * result + parameterNames.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "EnumConstant(" + name + ")";
        }
    }
    private final DescribedClass describedClass;
    private final JavaTypeKind kind;
    private final List<String> extendedTypeNames;
    private final List<String> implementedTypeNames;
    private final List<String> permittedTypeNames;
    private final List<ConstructorDescriptor> constructors;
    private final List<MethodDescriptor> methods;
    private final List<EnumConstantInfo> enumConstants;

    private DescribedType(
            DescribedClass describedClass,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames,
            List<ConstructorDescriptor> constructors,
            List<MethodDescriptor> methods,
            List<EnumConstantInfo> enumConstants
    ) {
        this.describedClass = describedClass;
        this.kind = kind;
        this.extendedTypeNames = extendedTypeNames;
        this.implementedTypeNames = implementedTypeNames;
        this.permittedTypeNames = permittedTypeNames;
        this.constructors = constructors;
        this.methods = methods;
        this.enumConstants = enumConstants;
    }

    public static DescribedType classNamed(String typeName) {
        return of(typeName, JavaTypeKind.CLASS);
    }

    public static DescribedType of(String typeName) {
        return classNamed(typeName);
    }

    public static DescribedType of(String typeName, JavaTypeKind kind) {
        return of(DescribedClass.of(typeName), kind);
    }

    public static DescribedType of(String typeName, JavaTypeKind kind, List<String> permittedTypeNames) {
        return of(DescribedClass.of(typeName), kind, permittedTypeNames);
    }

    public static DescribedType of(
            String typeName,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames
    ) {
        return of(DescribedClass.of(typeName), kind, extendedTypeNames, implementedTypeNames, permittedTypeNames);
    }

    public static DescribedType of(
            String typeName,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames,
            List<ConstructorDescriptor> constructors
    ) {
        return of(DescribedClass.of(typeName), kind, extendedTypeNames, implementedTypeNames, permittedTypeNames, constructors);
    }

    public static DescribedType of(
            String typeName,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames,
            List<ConstructorDescriptor> constructors,
            List<MethodDescriptor> methods
    ) {
        return of(DescribedClass.of(typeName), kind, extendedTypeNames, implementedTypeNames, permittedTypeNames, constructors, methods);
    }

    public static DescribedType of(DescribedClass describedClass) {
        return of(describedClass, JavaTypeKind.CLASS);
    }

    public static DescribedType of(DescribedClass describedClass, JavaTypeKind kind) {
        return of(describedClass, kind, empty(), empty(), empty(), constructorsEmpty(), methodsEmpty(), enumConstantsEmpty());
    }

    public static DescribedType of(DescribedClass describedClass, JavaTypeKind kind, List<String> permittedTypeNames) {
        return of(describedClass, kind, empty(), empty(), permittedTypeNames, constructorsEmpty(), methodsEmpty(), enumConstantsEmpty());
    }

    public static DescribedType of(
            DescribedClass describedClass,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames
    ) {
        return of(describedClass, kind, extendedTypeNames, implementedTypeNames, permittedTypeNames, constructorsEmpty(), methodsEmpty(), enumConstantsEmpty());
    }

    public static DescribedType of(
            DescribedClass describedClass,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames,
            List<ConstructorDescriptor> constructors
    ) {
        return of(describedClass, kind, extendedTypeNames, implementedTypeNames, permittedTypeNames, constructors, methodsEmpty(), enumConstantsEmpty());
    }

    public static DescribedType of(
            DescribedClass describedClass,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames,
            List<ConstructorDescriptor> constructors,
            List<MethodDescriptor> methods
    ) {
        return new DescribedType(
                Objects.requireNonNull(describedClass, "describedClass must not be null"),
                Objects.requireNonNull(kind, "kind must not be null"),
                validatedTypeNames(extendedTypeNames, "extendedTypeNames"),
                validatedTypeNames(implementedTypeNames, "implementedTypeNames"),
                validatedTypeNames(permittedTypeNames, "permittedTypeNames"),
                validatedConstructors(constructors),
                validatedMethods(methods),
                enumConstantsEmpty()
        );
    }

    public static DescribedType of(
            DescribedClass describedClass,
            JavaTypeKind kind,
            List<String> extendedTypeNames,
            List<String> implementedTypeNames,
            List<String> permittedTypeNames,
            List<ConstructorDescriptor> constructors,
            List<MethodDescriptor> methods,
            List<EnumConstantInfo> enumConstants
    ) {
        return new DescribedType(
                Objects.requireNonNull(describedClass, "describedClass must not be null"),
                Objects.requireNonNull(kind, "kind must not be null"),
                validatedTypeNames(extendedTypeNames, "extendedTypeNames"),
                validatedTypeNames(implementedTypeNames, "implementedTypeNames"),
                validatedTypeNames(permittedTypeNames, "permittedTypeNames"),
                validatedConstructors(constructors),
                validatedMethods(methods),
                validatedEnumConstants(enumConstants)
        );
    }

    public DescribedType withMethods(List<MethodDescriptor> methods) {
        return of(describedClass, kind, extendedTypeNames, implementedTypeNames, permittedTypeNames, constructors, methods, enumConstants);
    }

    public DescribedType withConstructors(List<ConstructorDescriptor> constructors) {
        return of(describedClass, kind, extendedTypeNames, implementedTypeNames, permittedTypeNames, constructors, methods, enumConstants);
    }

    public DescribedType withKind(JavaTypeKind kind) {
        return of(describedClass, kind, extendedTypeNames, implementedTypeNames, permittedTypeNames, constructors, methods, enumConstants);
    }

    public DescribedType withEnumConstants(List<EnumConstantInfo> enumConstants) {
        return of(describedClass, kind, extendedTypeNames, implementedTypeNames, permittedTypeNames, constructors, methods, enumConstants);
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

    public List<ConstructorDescriptor> constructors() {
        return constructors;
    }

    public boolean hasConstructors() {
        return !constructors.isEmpty();
    }

    public List<MethodDescriptor> methods() {
        return methods;
    }

    public boolean hasMethods() {
        return !methods.isEmpty();
    }

    public List<EnumConstantInfo> enumConstants() {
        return enumConstants;
    }

    public boolean hasEnumConstants() {
        return !enumConstants.isEmpty();
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
                && permittedTypeNames.equals(that.permittedTypeNames)
                && constructors.equals(that.constructors)
                && methods.equals(that.methods)
                && enumConstants.equals(that.enumConstants);
    }

    @Override
    public int hashCode() {
        int result = describedClass.hashCode();
        result = 31 * result + kind.hashCode();
        result = 31 * result + extendedTypeNames.hashCode();
        result = 31 * result + implementedTypeNames.hashCode();
        result = 31 * result + permittedTypeNames.hashCode();
        result = 31 * result + constructors.hashCode();
        result = 31 * result + methods.hashCode();
        result = 31 * result + enumConstants.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return kind.displayName() + " " + qualifiedName();
    }

    private static List<String> empty() {
        return Collections.emptyList();
    }

    private static List<ConstructorDescriptor> constructorsEmpty() {
        return Collections.emptyList();
    }

    private static List<MethodDescriptor> methodsEmpty() {
        return Collections.emptyList();
    }

    private static List<EnumConstantInfo> enumConstantsEmpty() {
        return Collections.emptyList();
    }

    private static List<EnumConstantInfo> validatedEnumConstants(List<EnumConstantInfo> constants) {
        Objects.requireNonNull(constants, "enumConstants must not be null");
        List<EnumConstantInfo> copy = new ArrayList<EnumConstantInfo>(constants);
        for (int i = 0; i < copy.size(); i++) {
            Objects.requireNonNull(copy.get(i), "enumConstants[" + i + "] must not be null");
        }
        return Collections.unmodifiableList(copy);
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

    private static List<ConstructorDescriptor> validatedConstructors(List<ConstructorDescriptor> constructors) {
        Objects.requireNonNull(constructors, "constructors must not be null");
        List<ConstructorDescriptor> copy = new ArrayList<ConstructorDescriptor>(constructors);
        for (int i = 0; i < copy.size(); i++) {
            Objects.requireNonNull(copy.get(i), "constructors[" + i + "] must not be null");
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<MethodDescriptor> validatedMethods(List<MethodDescriptor> methods) {
        Objects.requireNonNull(methods, "methods must not be null");
        List<MethodDescriptor> copy = new ArrayList<MethodDescriptor>();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = Objects.requireNonNull(methods.get(i), "methods[" + i + "] must not be null");
            addMethod(copy, method);
        }
        return Collections.unmodifiableList(copy);
    }

    private static void addMethod(List<MethodDescriptor> methods, MethodDescriptor candidate) {
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor existing = methods.get(i);
            if (!existing.hasEquivalentSignature(candidate)) {
                continue;
            }
            if (shouldReplace(existing, candidate)) {
                methods.set(i, candidate);
            }
            return;
        }
        methods.add(candidate);
    }

    private static boolean shouldReplace(MethodDescriptor existing, MethodDescriptor candidate) {
        if ("Object".equals(existing.returnType()) && !"Object".equals(candidate.returnType())) {
            return true;
        }
        if (hasLessSpecificUnknownParameters(existing, candidate)) {
            return true;
        }
        return existing.isVoid() && !candidate.isVoid();
    }

    private static boolean hasLessSpecificUnknownParameters(MethodDescriptor existing, MethodDescriptor candidate) {
        if (existing.parameterTypes().size() != candidate.parameterTypes().size()) {
            return false;
        }
        boolean lessSpecific = false;
        for (int i = 0; i < existing.parameterTypes().size(); i++) {
            if (existing.isParameterTypeUnknown(i) && !candidate.isParameterTypeUnknown(i)) {
                lessSpecific = true;
                continue;
            }
            if (!existing.isParameterTypeUnknown(i) && candidate.isParameterTypeUnknown(i)) {
                return false;
            }
        }
        return lessSpecific;
    }

}
