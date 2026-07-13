package io.github.jvmspec.discovery;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.jvmspec.discovery.JavaExpressionTypeInference.importsBySimpleName;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferLiteralType;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.resolveTypeName;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.splitArguments;

/** Discovers subject kind, relationships, and enum declarations from Java specifications. */
final class SubjectDeclarationDiscovery {
    private static final Pattern SHOULD_EXTEND_PATTERN = Pattern.compile(
            "(?<!\\.)\\bshouldExtend\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_IMPLEMENT_PATTERN = Pattern.compile(
            "(?<!\\.)\\bshouldImplement\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_PERMIT_PATTERN = Pattern.compile(
            "(?<!\\.)\\bshouldPermit\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern CLASS_LITERAL_PATTERN = Pattern.compile(
            "([A-Za-z_$][A-Za-z0-9_$.]*)\\s*\\.\\s*class");
    private static final Pattern SHOULD_HAVE_CONSTANT_PATTERN = Pattern.compile(
            "shouldHaveConstant\\s*\\(([^)]*)\\)", Pattern.DOTALL);

    private SubjectDeclarationDiscovery() {
    }

    static Declaration discover(String source, String describedPackageName) {
        JavaTypeKind kind = describedKind(source);
        List<DescribedType.EnumConstantInfo> enumConstants =
                enumConstants(source, describedPackageName);
        return new Declaration(
                kind,
                typeNames(source, describedPackageName, SHOULD_EXTEND_PATTERN),
                typeNames(source, describedPackageName, SHOULD_IMPLEMENT_PATTERN),
                typeNames(source, describedPackageName, SHOULD_PERMIT_PATTERN),
                enumConstants,
                enumConstructors(enumConstants)
        );
    }

    private static JavaTypeKind describedKind(String source) {
        if (source.contains("shouldBeAFinalClass(")) return JavaTypeKind.FINAL_CLASS;
        if (source.contains("shouldBeAnAnnotation(")) return JavaTypeKind.ANNOTATION;
        if (source.contains("shouldBeAnEnum(")) return JavaTypeKind.ENUM;
        if (source.contains("shouldBeARecord(")) return JavaTypeKind.RECORD;
        if (source.contains("shouldBeASealedInterface(")) return JavaTypeKind.SEALED_INTERFACE;
        if (source.contains("shouldBeASealedClass(")) return JavaTypeKind.SEALED_CLASS;
        if (source.contains("shouldBeAnInterface(")) return JavaTypeKind.INTERFACE;
        return JavaTypeKind.CLASS;
    }

    private static List<String> typeNames(
            String source,
            String describedPackageName,
            Pattern markerPattern
    ) {
        List<String> typeNames = new ArrayList<String>();
        Map<String, String> imports = importsBySimpleName(source);
        Matcher markerMatcher = markerPattern.matcher(source);
        while (markerMatcher.find()) {
            Matcher classLiteralMatcher = CLASS_LITERAL_PATTERN.matcher(markerMatcher.group(1));
            while (classLiteralMatcher.find()) {
                String typeName = resolveTypeName(
                        classLiteralMatcher.group(1), imports, describedPackageName);
                if (!typeNames.contains(typeName)) {
                    typeNames.add(typeName);
                }
            }
        }
        return typeNames;
    }

    private static List<DescribedType.EnumConstantInfo> enumConstants(
            String source,
            String describedPackageName
    ) {
        Map<String, String> imports = importsBySimpleName(source);
        List<DescribedType.EnumConstantInfo> constants =
                new ArrayList<DescribedType.EnumConstantInfo>();
        Matcher matcher = SHOULD_HAVE_CONSTANT_PATTERN.matcher(source);
        while (matcher.find()) {
            List<String> arguments = splitArguments(matcher.group(1).trim());
            if (arguments.isEmpty()) {
                continue;
            }
            String constantName = unquoted(arguments.get(0).trim());
            List<String> parameterTypes = new ArrayList<String>();
            List<String> parameterNames = new ArrayList<String>();
            List<String> parameterValues = new ArrayList<String>();
            for (int i = 1; i < arguments.size(); i++) {
                String argument = arguments.get(i).trim();
                parameterTypes.add(inferLiteralType(
                        argument, imports, describedPackageName).typeName);
                parameterNames.add("arg" + (i - 1));
                parameterValues.add(argument);
            }
            constants.add(DescribedType.EnumConstantInfo.of(
                    constantName, parameterTypes, parameterNames, parameterValues));
        }
        return constants;
    }

    private static String unquoted(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static List<ConstructorDescriptor> enumConstructors(
            List<DescribedType.EnumConstantInfo> constants
    ) {
        for (int i = 0; i < constants.size(); i++) {
            DescribedType.EnumConstantInfo constant = constants.get(i);
            if (constant.hasParameters()) {
                return Collections.singletonList(ConstructorDescriptor.of(
                        constant.parameterTypes(), constant.parameterNames(), ""));
            }
        }
        return Collections.emptyList();
    }

    static final class Declaration {
        final JavaTypeKind kind;
        final List<String> extendedTypes;
        final List<String> implementedTypes;
        final List<String> permittedTypes;
        final List<DescribedType.EnumConstantInfo> enumConstants;
        final List<ConstructorDescriptor> enumConstructors;

        Declaration(
                JavaTypeKind kind,
                List<String> extendedTypes,
                List<String> implementedTypes,
                List<String> permittedTypes,
                List<DescribedType.EnumConstantInfo> enumConstants,
                List<ConstructorDescriptor> enumConstructors
        ) {
            this.kind = kind;
            this.extendedTypes = extendedTypes;
            this.implementedTypes = implementedTypes;
            this.permittedTypes = permittedTypes;
            this.enumConstants = enumConstants;
            this.enumConstructors = enumConstructors;
        }
    }
}
