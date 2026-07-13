package io.github.jvmspec.discovery;

import io.github.jvmspec.internal.type.JavaSyntaxSplitter;
import io.github.jvmspec.internal.type.JavaTypeRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Java-specific expression, parameter, and source-name inference for spec discovery. */
final class JavaExpressionTypeInference {
    private static final List<String> RESERVED_IDENTIFIERS = Collections.unmodifiableList(Arrays.asList(
            "_", "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short",
            "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "true", "try", "void", "volatile", "while"
    ));
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern METHOD_PATTERN = Pattern.compile("public\\s+void\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)\\s*(?:throws\\s+[^\\{]+)?\\{", Pattern.DOTALL);

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
            if (localIndex >= 0) {
                return methodInfo.types.get(localIndex);
            }
        }
        return inferLiteralType(value, imports, describedPackageName).typeName;
    }

    static boolean sameInferredType(String left, String right) {
        return left.equals(right)
                || ("String".equals(left) && "java.lang.String".equals(right))
                || ("java.lang.String".equals(left) && "String".equals(right));
    }

    static String inferReturnType(
            String matcherName,
            String expectationSource,
            Map<String, String> imports,
            String describedPackageName
    ) {
        List<String> expectationArgs = splitArguments(expectationSource);
        if ("shouldContain".equals(matcherName)
                || "shouldNotContain".equals(matcherName)
                || "shouldStartWith".equals(matcherName)
                || "shouldNotStartWith".equals(matcherName)
                || "shouldEndWith".equals(matcherName)
                || "shouldNotEndWith".equals(matcherName)
                || "shouldMatchPattern".equals(matcherName)
                || "shouldNotMatchPattern".equals(matcherName)) {
            if (!expectationArgs.isEmpty() && isStringLiteral(expectationArgs.get(0))) {
                return "String";
            }
            return "Object";
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
        if (expectationArgs.isEmpty()) {
            return "Object";
        }
        return inferLiteralType(expectationArgs.get(0), imports, describedPackageName).typeName;
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
        if (enclosingMethod != null) {
            enclosingInfo = specMethods.get(enclosingMethod);
        }
        return inferArgumentTypesCore(splitArguments(argumentSource), enclosingInfo, imports, describedPackageName);
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
                if (parameterIndex >= 0) {
                    type = enclosingInfo.types.get(parameterIndex);
                }
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
        for (int i = 0; i < count; i++) {
            names.add("arg" + i);
        }
        return names;
    }

    static String typeDerivedParameterName(String argument) {
        String qualifier = qualifiedConstantReferenceType(argument.trim());
        if (qualifier == null) {
            return null;
        }
        int lastDot = qualifier.lastIndexOf('.');
        String simpleTypeName = lastDot < 0 ? qualifier : qualifier.substring(lastDot + 1);
        String derived = decapitalize(simpleTypeName);
        if (!isJavaIdentifier(derived) || derived.equals(simpleTypeName)) {
            return null;
        }
        return derived;
    }

    static String parameterNameForArgument(String argument, int index) {
        if (isLegalJavaIdentifier(argument)) {
            return argument;
        }
        return "arg" + index;
    }

    static boolean isLegalJavaIdentifier(String value) {
        return isJavaIdentifier(value) && !RESERVED_IDENTIFIERS.contains(value);
    }

    static InferredType inferLiteralType(String expression, Map<String, String> imports, String describedPackageName) {
        String value = expression.trim();
        if (value.length() == 0) {
            return InferredType.unknownObject();
        }
        String castType = castType(value);
        if (castType != null) {
            return InferredType.known(resolveTypeName(castType, imports, describedPackageName));
        }
        if (isStringLiteral(value)) {
            return InferredType.known("String");
        }
        if (isCharLiteral(value)) {
            return InferredType.known("char");
        }
        if ("true".equals(value) || "false".equals(value)) {
            return InferredType.known("boolean");
        }
        if ("null".equals(value)) {
            return InferredType.unknownObject();
        }
        if (isClassLiteral(value)) {
            return InferredType.known("Class");
        }
        String arrayCreationType = arrayCreationType(value);
        if (arrayCreationType != null) {
            return InferredType.known(resolveTypeName(arrayCreationType, imports, describedPackageName));
        }
        String constructedType = constructedType(value);
        if (constructedType != null) {
            return InferredType.known(resolveTypeName(constructedType, imports, describedPackageName));
        }
        String parameterizedFactoryType = parameterizedCollectionFactoryType(
                value, imports, describedPackageName);
        if (parameterizedFactoryType != null) {
            return InferredType.known(parameterizedFactoryType);
        }
        String staticFactoryType = staticFactoryReceiverType(value);
        if (staticFactoryType != null) {
            return InferredType.known(resolveTypeName(staticFactoryType, imports, describedPackageName));
        }
        if (value.matches("[-+]?\\d+[lL]")) {
            return InferredType.known("long");
        }
        if (value.matches("[-+]?\\d+[dDfF]")) {
            return InferredType.known("double");
        }
        if (value.matches("[-+]?(?:\\d+\\.\\d*|\\d*\\.\\d+)(?:[eE][-+]?\\d+)?[dDfF]?")) {
            return InferredType.known("double");
        }
        if (value.matches("[-+]?\\d+(?:[eE][-+]?\\d+)[dDfF]?")) {
            return InferredType.known("double");
        }
        if (value.matches("[-+]?\\d+")) {
            return InferredType.known("int");
        }
        String qualifiedConstantType = qualifiedConstantReferenceType(value);
        if (qualifiedConstantType != null) {
            return InferredType.known(resolveTypeName(qualifiedConstantType, imports, describedPackageName));
        }
        return InferredType.unknownObject();
    }

    static String arrayCreationType(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("new ")) {
            return null;
        }
        int firstBracket = trimmed.indexOf('[', 4);
        if (firstBracket < 0) {
            return null;
        }
        int constructorOpen = trimmed.indexOf('(', 4);
        if (constructorOpen >= 0 && constructorOpen < firstBracket) {
            return null;
        }
        String componentType = trimmed.substring(4, firstBracket).trim();
        if (!isLikelyTypeName(componentType)) {
            return null;
        }
        int dimensions = 0;
        int index = firstBracket;
        while (index < trimmed.length() && trimmed.charAt(index) == '[') {
            int close = trimmed.indexOf(']', index + 1);
            if (close < 0) {
                return null;
            }
            dimensions++;
            index = close + 1;
            while (index < trimmed.length() && Character.isWhitespace(trimmed.charAt(index))) {
                index++;
            }
        }
        if (dimensions == 0) {
            return null;
        }
        StringBuilder typeName = new StringBuilder(componentType);
        for (int i = 0; i < dimensions; i++) {
            typeName.append("[]");
        }
        return typeName.toString();
    }

    static String constructedType(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("new ")) {
            return null;
        }
        int open = trimmed.indexOf('(');
        if (open < 0) {
            return null;
        }
        String typeName = trimmed.substring(4, open).trim();
        int genericStart = typeName.indexOf('<');
        if (genericStart >= 0) {
            typeName = typeName.substring(0, genericStart).trim();
        }
        if (!isLikelyTypeName(typeName)) {
            return null;
        }
        return typeName;
    }

    static String parameterizedCollectionFactoryType(
            String value,
            Map<String, String> imports,
            String describedPackageName
    ) {
        String trimmed = value.trim();
        int open = trimmed.indexOf('(');
        int close = trimmed.lastIndexOf(')');
        if (open < 0 || close != trimmed.length() - 1 || close <= open) {
            return null;
        }
        String invocation = trimmed.substring(0, open).trim();
        int methodSeparator = invocation.lastIndexOf('.');
        if (methodSeparator < 0 || !"of".equals(invocation.substring(methodSeparator + 1))) {
            return null;
        }
        String receiver = invocation.substring(0, methodSeparator);
        String resolvedReceiver = resolveTypeName(receiver, imports, describedPackageName);
        if (!"java.util.List".equals(resolvedReceiver)) {
            return null;
        }
        List<String> elements = splitArguments(trimmed.substring(open + 1, close));
        if (elements.isEmpty()) {
            return null;
        }
        String elementType = null;
        for (int i = 0; i < elements.size(); i++) {
            InferredType inferred = inferLiteralType(elements.get(i), imports, describedPackageName);
            if (inferred.unknown || "Object".equals(inferred.typeName)) {
                return null;
            }
            if (elementType == null) {
                elementType = inferred.typeName;
            } else if (!sameInferredType(elementType, inferred.typeName)) {
                return null;
            }
        }
        return resolvedReceiver + "<" + elementType + ">";
    }

    static String staticFactoryReceiverType(String value) {
        Matcher matcher = Pattern.compile("^([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*)\\.([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(.*\\)$").matcher(value.trim());
        if (!matcher.matches()) {
            return null;
        }
        String receiver = matcher.group(1);
        String methodName = matcher.group(2);
        if (!isLikelyStaticFactoryMethod(methodName)) {
            return null;
        }
        String simpleReceiver = simpleName(receiver);
        if (simpleReceiver.length() == 0 || !Character.isUpperCase(simpleReceiver.charAt(0))) {
            return null;
        }
        return receiver;
    }

    static String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot < 0 ? qualifiedName : qualifiedName.substring(lastDot + 1);
    }

    static boolean isLikelyStaticFactoryMethod(String methodName) {
        return "of".equals(methodName)
                || "ofNullable".equals(methodName)
                || "from".equals(methodName)
                || "fromString".equals(methodName)
                || "fromValue".equals(methodName)
                || "parse".equals(methodName)
                || "valueOf".equals(methodName)
                || "create".equals(methodName)
                || "named".equals(methodName);
    }

    static String castType(String value) {
        if (!value.startsWith("(")) {
            return null;
        }
        int close = value.indexOf(')');
        if (close <= 1 || close == value.length() - 1) {
            return null;
        }
        String typeName = value.substring(1, close).trim();
        String expression = value.substring(close + 1).trim();
        if (!isLikelyTypeName(typeName)) {
            return null;
        }
        if (expression.length() == 0) {
            return null;
        }
        return typeName;
    }

    static boolean isLikelyTypeName(String value) {
        String trimmed = value.trim();
        String erased = eraseGenericArguments(trimmed);
        if (erased == null) {
            return false;
        }
        if (erased.matches("(?:byte|short|int|long|float|double|boolean|char)(?:\\s*\\[\\s*\\])*")) {
            return true;
        }
        return erased.matches("[A-Z_$][A-Za-z0-9_$]*(?:\\s*\\[\\s*\\])*")
                || erased.matches("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+(?:\\s*\\[\\s*\\])*");
    }

    static String eraseGenericArguments(String typeName) {
        StringBuilder erased = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < typeName.length(); i++) {
            char current = typeName.charAt(i);
            if (current == '<') {
                depth++;
                continue;
            }
            if (current == '>') {
                depth--;
                if (depth < 0) {
                    return null;
                }
                continue;
            }
            if (depth == 0) {
                erased.append(current);
            }
        }
        return depth == 0 ? erased.toString().trim() : null;
    }

    static String qualifiedConstantReferenceType(String value) {
        if (!value.matches("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+")) {
            return null;
        }
        int lastDot = value.lastIndexOf('.');
        return value.substring(0, lastDot);
    }

    static boolean isClassLiteral(String value) {
        String trimmed = value.trim();
        if (!trimmed.endsWith(".class")) {
            return false;
        }
        String target = trimmed.substring(0, trimmed.length() - ".class".length()).trim();
        return isLikelyTypeName(target);
    }

    static boolean isStringLiteral(String value) {
        String trimmed = value.trim();
        return trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"';
    }

    static boolean isCharLiteral(String value) {
        String trimmed = value.trim();
        return trimmed.length() >= 3 && trimmed.charAt(0) == '\'' && trimmed.charAt(trimmed.length() - 1) == '\'';
    }

    static List<String> splitArguments(String arguments) {
        List<String> result = new ArrayList<String>();
        if (arguments == null || arguments.trim().length() == 0) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        int nesting = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int i = 0; i < arguments.length(); i++) {
            char c = arguments.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\' && (inString || inChar)) {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '"' && !inChar) {
                inString = !inString;
                current.append(c);
                continue;
            }
            if (c == '\'' && !inString) {
                inChar = !inChar;
                current.append(c);
                continue;
            }
            if (!inString && !inChar) {
                if (c == '(' || c == '[' || c == '{') {
                    nesting++;
                } else if (c == ')' || c == ']' || c == '}') {
                    if (nesting > 0) {
                        nesting--;
                    }
                } else if (c == ',' && nesting == 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    static Map<String, MethodParameterInfo> parseMethods(String source, Map<String, String> imports, String describedPackageName) {
        Map<String, MethodParameterInfo> methods = new LinkedHashMap<String, MethodParameterInfo>();
        Matcher methodMatcher = METHOD_PATTERN.matcher(source);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            String paramsGroup = methodMatcher.group(2).trim();
            List<String> types = new ArrayList<String>();
            List<String> names = new ArrayList<String>();
            if (paramsGroup.length() > 0) {
                List<String> params = JavaSyntaxSplitter.splitTopLevel(paramsGroup, ',');
                for (int i = 0; i < params.size(); i++) {
                    String param = params.get(i).trim();
                    if (param.length() > 0) {
                        int lastSpace = param.lastIndexOf(' ');
                        if (lastSpace >= 0) {
                            String typeName = param.substring(0, lastSpace).trim();
                            String varName = param.substring(lastSpace + 1).trim();
                            typeName = resolveTypeName(typeName, imports, describedPackageName);
                            types.add(typeName);
                            names.add(varName);
                        }
                    }
                }
            }
            methods.put(methodName, new MethodParameterInfo(types, names, types.size()));
        }
        return methods;
    }

    static String findEnclosingMethod(String source, int position, Map<String, MethodParameterInfo> methods) {
        Matcher matcher = METHOD_PATTERN.matcher(source);
        String lastMethod = null;
        while (matcher.find() && matcher.start() < position) {
            lastMethod = matcher.group(1);
        }
        return lastMethod;
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

    static Map<String, MethodParameterInfo> methodInfoFromScan(
            SpecCallScanner.ScanResult scan,
            Map<String, String> imports,
            String describedPackageName
    ) {
        Map<String, MethodParameterInfo> methods = new LinkedHashMap<String, MethodParameterInfo>();
        for (Map.Entry<String, SpecCallScanner.SpecMethodParams> entry : scan.specMethods.entrySet()) {
            SpecCallScanner.SpecMethodParams parameters = entry.getValue();
            List<String> types = new ArrayList<String>();
            for (int i = 0; i < parameters.typeTexts.size(); i++) {
                String typeText = parameters.typeTexts.get(i);
                String initializerText = i < parameters.initializerTexts.size()
                        ? parameters.initializerTexts.get(i)
                        : null;
                if ("var".equals(typeText) && initializerText != null) {
                    types.add(inferLiteralType(
                            initializerText, imports, describedPackageName).typeName);
                } else {
                    types.add(resolveTypeName(typeText, imports, describedPackageName));
                }
            }
            methods.put(entry.getKey(), new MethodParameterInfo(
                    types, parameters.names, parameters.formalParameterCount));
        }
        return methods;
    }

    static Map<String, String> importsBySimpleName(String source) {
        Map<String, String> imports = new LinkedHashMap<String, String>();
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        while (matcher.find()) {
            String qualifiedName = matcher.group(1);
            int lastDot = qualifiedName.lastIndexOf('.');
            if (lastDot >= 0) {
                imports.put(qualifiedName.substring(lastDot + 1), qualifiedName);
            }
        }
        return imports;
    }

    static String resolveTypeName(
            String typeName,
            Map<String, String> imports,
            String describedPackageName
    ) {
        return JavaTypeRef.resolve(typeName, imports, describedPackageName).canonicalName();
    }

    static boolean isJavaIdentifier(String value) {
        if (value == null || value.length() == 0) {
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

    static String decapitalize(String value) {
        if (value.length() == 0) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
