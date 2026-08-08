package io.github.jvmspec.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Coordinates Java expression, argument, literal, and source-context inference. */
final class JavaExpressionTypeInference {
    private static final List<String> RESERVED_IDENTIFIERS = Collections.unmodifiableList(Arrays.asList(
            "_", "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short",
            "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "true", "try", "void", "volatile", "while"
    ));

    private JavaExpressionTypeInference() {
    }

    static String inferredExpressionType(
            String expression,
            MethodParameterInfo methodInfo,
            Map<String, String> imports,
            String describedPackageName
    ) {
        String value = expression.trim();
        if (methodInfo != null) {
            int localIndex = methodInfo.names.indexOf(value);
            if (localIndex >= 0) return methodInfo.types.get(localIndex);
        }
        return inferLiteralType(value, imports, describedPackageName).typeName;
    }

    static boolean sameInferredType(String left, String right) {
        return JavaLiteralTypeInference.sameType(left, right);
    }

    static String inferReturnType(
            String matcherName,
            String expectationSource,
            Map<String, String> imports,
            String describedPackageName
    ) {
        List<String> expectationArguments = splitArguments(expectationSource);
        if ("shouldContain".equals(matcherName)
                || "shouldNotContain".equals(matcherName)
                || "shouldStartWith".equals(matcherName)
                || "shouldNotStartWith".equals(matcherName)
                || "shouldEndWith".equals(matcherName)
                || "shouldNotEndWith".equals(matcherName)
                || "shouldMatchPattern".equals(matcherName)
                || "shouldNotMatchPattern".equals(matcherName)) {
            return !expectationArguments.isEmpty() && isStringLiteral(expectationArguments.get(0))
                    ? "String"
                    : "Object";
        }
        if ("shouldBeApproximately".equals(matcherName)
                || "shouldReturnApproximately".equals(matcherName)
                || "shouldNotBeApproximately".equals(matcherName)
                || "shouldNotReturnApproximately".equals(matcherName)) {
            return "Number";
        }
        if ("shouldHaveType".equals(matcherName)
                || "shouldBeAnInstanceOf".equals(matcherName)
                || "shouldReturnAnInstanceOf".equals(matcherName)
                || "shouldImplement".equals(matcherName)
                || "shouldHaveCount".equals(matcherName)
                || "shouldBeEmpty".equals(matcherName)
                || "shouldNotBeEmpty".equals(matcherName)
                || "shouldHaveKey".equals(matcherName)
                || "shouldNotHaveKey".equals(matcherName)
                || "shouldHaveValue".equals(matcherName)
                || "shouldNotHaveValue".equals(matcherName)) {
            return "Object";
        }
        if (expectationArguments.isEmpty()) return "Object";
        return inferLiteralType(
                expectationArguments.get(0), imports, describedPackageName).typeName;
    }

    static InferredArguments inferArgumentTypes(
            String argumentSource,
            String source,
            int position,
            Map<String, MethodParameterInfo> specMethods,
            Map<String, String> imports,
            String describedPackageName
    ) {
        MethodParameterInfo enclosingInfo = null;
        String enclosingMethod = findEnclosingMethod(source, position, specMethods);
        if (enclosingMethod != null) enclosingInfo = specMethods.get(enclosingMethod);
        return inferArgumentTypesCore(
                splitArguments(argumentSource), enclosingInfo, imports, describedPackageName);
    }

    static InferredArguments inferArgumentTypesCore(
            List<String> argumentValues,
            MethodParameterInfo enclosingInfo,
            Map<String, String> imports,
            String describedPackageName
    ) {
        List<String> types = new ArrayList<String>();
        List<Boolean> unknowns = new ArrayList<Boolean>();
        for (int i = 0; i < argumentValues.size(); i++) {
            String argument = argumentValues.get(i).trim();
            String type = null;
            if (enclosingInfo != null) {
                int parameterIndex = enclosingInfo.names.indexOf(argument);
                if (parameterIndex >= 0) type = enclosingInfo.types.get(parameterIndex);
            }
            if (type != null) {
                types.add(type);
                unknowns.add(Boolean.FALSE);
            } else {
                InferredType inferred = inferLiteralType(argument, imports, describedPackageName);
                types.add(inferred.typeName);
                unknowns.add(Boolean.valueOf(inferred.unknown));
            }
        }
        return new InferredArguments(types, unknowns);
    }

    static List<String> parameterNamesFor(String methodName, int count) {
        List<String> names = new ArrayList<String>();
        if (count == 1 && methodName.startsWith("set") && methodName.length() > 3) {
            names.add(decapitalize(methodName.substring(3)));
            return names;
        }
        for (int i = 0; i < count; i++) names.add("arg" + i);
        return names;
    }

    static String typeDerivedParameterName(String argument) {
        String qualifier = JavaLiteralTypeInference.qualifiedConstantReferenceType(argument.trim());
        if (qualifier == null) return null;
        int lastDot = qualifier.lastIndexOf('.');
        String simpleTypeName = lastDot < 0 ? qualifier : qualifier.substring(lastDot + 1);
        String derived = decapitalize(simpleTypeName);
        return isJavaIdentifier(derived) && !derived.equals(simpleTypeName) ? derived : null;
    }

    static String parameterNameForArgument(String argument, int index) {
        return isLegalJavaIdentifier(argument) ? argument : "arg" + index;
    }

    static boolean isLegalJavaIdentifier(String value) {
        return isJavaIdentifier(value) && !RESERVED_IDENTIFIERS.contains(value);
    }

    static InferredType inferLiteralType(
            String expression,
            Map<String, String> imports,
            String describedPackageName
    ) {
        return JavaLiteralTypeInference.infer(expression, imports, describedPackageName);
    }

    static boolean isStringLiteral(String value) {
        return JavaLiteralTypeInference.isStringLiteral(value);
    }

    static List<String> splitArguments(String arguments) {
        return JavaExpressionArguments.split(arguments);
    }

    static Map<String, MethodParameterInfo> parseMethods(
            String source,
            Map<String, String> imports,
            String describedPackageName
    ) {
        return JavaSourceContext.parseMethods(source, imports, describedPackageName);
    }

    static String findEnclosingMethod(
            String source,
            int position,
            Map<String, MethodParameterInfo> methods
    ) {
        return JavaSourceContext.findEnclosingMethod(source, position);
    }

    static Map<String, MethodParameterInfo> methodInfoFromScan(
            SpecCallScanner.ScanResult scan,
            Map<String, String> imports,
            String describedPackageName
    ) {
        return JavaSourceContext.methodInfoFromScan(scan, imports, describedPackageName);
    }

    static Map<String, String> importsBySimpleName(String source) {
        return JavaSourceContext.importsBySimpleName(source);
    }

    static String resolveTypeName(
            String typeName,
            Map<String, String> imports,
            String describedPackageName
    ) {
        return JavaSourceContext.resolveTypeName(typeName, imports, describedPackageName);
    }

    static boolean isJavaIdentifier(String value) {
        return JavaSourceContext.isJavaIdentifier(value);
    }

    static String decapitalize(String value) {
        return JavaSourceContext.decapitalize(value);
    }

    static final class InferredArguments {
        final List<String> types;
        final List<Boolean> unknowns;

        InferredArguments(List<String> types, List<Boolean> unknowns) {
            this.types = types;
            this.unknowns = unknowns;
        }

        int size() {
            return types.size();
        }
    }

    static final class InferredType {
        final String typeName;
        final boolean unknown;

        private InferredType(String typeName, boolean unknown) {
            this.typeName = typeName;
            this.unknown = unknown;
        }

        static InferredType known(String typeName) {
            return new InferredType(typeName, false);
        }

        static InferredType unknownObject() {
            return new InferredType("Object", true);
        }
    }

    static final class MethodParameterInfo {
        final List<String> types;
        final List<String> names;
        final int formalParameterCount;

        MethodParameterInfo(List<String> types, List<String> names, int formalParameterCount) {
            this.types = types;
            this.names = names;
            this.formalParameterCount = formalParameterCount;
        }
    }
}
