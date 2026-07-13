package io.github.jvmspec.discovery;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Infers Java types from literal, construction, constant, array, cast, and factory expressions. */
final class JavaLiteralTypeInference {
    private static final Pattern STATIC_FACTORY = Pattern.compile(
            "^([A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*)"
                    + "\\.([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(.*\\)$");

    private JavaLiteralTypeInference() {
    }

    static JavaExpressionTypeInference.InferredType infer(
            String expression,
            Map<String, String> imports,
            String describedPackageName
    ) {
        String value = expression.trim();
        if (value.length() == 0) return unknown();
        String castType = castType(value);
        if (castType != null) return known(resolve(castType, imports, describedPackageName));
        if (isStringLiteral(value)) return known("String");
        if (isCharLiteral(value)) return known("char");
        if ("true".equals(value) || "false".equals(value)) return known("boolean");
        if ("null".equals(value)) return unknown();
        if (isClassLiteral(value)) return known("Class");
        String arrayType = arrayCreationType(value);
        if (arrayType != null) return known(resolve(arrayType, imports, describedPackageName));
        String constructed = constructedType(value);
        if (constructed != null) return known(resolve(constructed, imports, describedPackageName));
        String collection = parameterizedCollectionFactoryType(value, imports, describedPackageName);
        if (collection != null) return known(collection);
        String factory = staticFactoryReceiverType(value);
        if (factory != null) return known(resolve(factory, imports, describedPackageName));
        if (value.matches("[-+]?\\d+[lL]")) return known("long");
        if (value.matches("[-+]?\\d+[dDfF]")) return known("double");
        if (value.matches("[-+]?(?:\\d+\\.\\d*|\\d*\\.\\d+)(?:[eE][-+]?\\d+)?[dDfF]?")) {
            return known("double");
        }
        if (value.matches("[-+]?\\d+(?:[eE][-+]?\\d+)[dDfF]?")) return known("double");
        if (value.matches("[-+]?\\d+")) return known("int");
        String constantType = qualifiedConstantReferenceType(value);
        if (constantType != null) return known(resolve(constantType, imports, describedPackageName));
        return unknown();
    }

    static boolean sameType(String left, String right) {
        return left.equals(right)
                || ("String".equals(left) && "java.lang.String".equals(right))
                || ("java.lang.String".equals(left) && "String".equals(right));
    }

    static String arrayCreationType(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("new ")) return null;
        int firstBracket = trimmed.indexOf('[', 4);
        if (firstBracket < 0) return null;
        int constructorOpen = trimmed.indexOf('(', 4);
        if (constructorOpen >= 0 && constructorOpen < firstBracket) return null;
        String componentType = trimmed.substring(4, firstBracket).trim();
        if (!isLikelyTypeName(componentType)) return null;
        int dimensions = 0;
        int index = firstBracket;
        while (index < trimmed.length() && trimmed.charAt(index) == '[') {
            int close = trimmed.indexOf(']', index + 1);
            if (close < 0) return null;
            dimensions++;
            index = close + 1;
            while (index < trimmed.length() && Character.isWhitespace(trimmed.charAt(index))) index++;
        }
        if (dimensions == 0) return null;
        StringBuilder typeName = new StringBuilder(componentType);
        for (int i = 0; i < dimensions; i++) typeName.append("[]");
        return typeName.toString();
    }

    static String constructedType(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("new ")) return null;
        int open = trimmed.indexOf('(');
        if (open < 0) return null;
        String typeName = trimmed.substring(4, open).trim();
        int genericStart = typeName.indexOf('<');
        if (genericStart >= 0) typeName = typeName.substring(0, genericStart).trim();
        return isLikelyTypeName(typeName) ? typeName : null;
    }

    static String parameterizedCollectionFactoryType(
            String value,
            Map<String, String> imports,
            String describedPackageName
    ) {
        String trimmed = value.trim();
        int open = trimmed.indexOf('(');
        int close = trimmed.lastIndexOf(')');
        if (open < 0 || close != trimmed.length() - 1 || close <= open) return null;
        String invocation = trimmed.substring(0, open).trim();
        int separator = invocation.lastIndexOf('.');
        if (separator < 0 || !"of".equals(invocation.substring(separator + 1))) return null;
        String receiver = resolve(invocation.substring(0, separator), imports, describedPackageName);
        if (!"java.util.List".equals(receiver)) return null;
        List<String> elements = JavaExpressionArguments.split(trimmed.substring(open + 1, close));
        if (elements.isEmpty()) return null;
        String elementType = null;
        for (int i = 0; i < elements.size(); i++) {
            JavaExpressionTypeInference.InferredType inferred =
                    infer(elements.get(i), imports, describedPackageName);
            if (inferred.unknown || "Object".equals(inferred.typeName)) return null;
            if (elementType == null) {
                elementType = inferred.typeName;
            } else if (!sameType(elementType, inferred.typeName)) {
                return null;
            }
        }
        return receiver + "<" + elementType + ">";
    }

    static String staticFactoryReceiverType(String value) {
        Matcher matcher = STATIC_FACTORY.matcher(value.trim());
        if (!matcher.matches() || !isLikelyStaticFactoryMethod(matcher.group(2))) return null;
        String receiver = matcher.group(1);
        String simpleReceiver = simpleName(receiver);
        if (simpleReceiver.length() == 0 || !Character.isUpperCase(simpleReceiver.charAt(0))) return null;
        return receiver;
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
        if (!value.startsWith("(")) return null;
        int close = value.indexOf(')');
        if (close <= 1 || close == value.length() - 1) return null;
        String typeName = value.substring(1, close).trim();
        String expression = value.substring(close + 1).trim();
        return isLikelyTypeName(typeName) && expression.length() > 0 ? typeName : null;
    }

    static boolean isLikelyTypeName(String value) {
        String erased = eraseGenericArguments(value.trim());
        if (erased == null) return false;
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
            } else if (current == '>') {
                depth--;
                if (depth < 0) return null;
            } else if (depth == 0) {
                erased.append(current);
            }
        }
        return depth == 0 ? erased.toString().trim() : null;
    }

    static String qualifiedConstantReferenceType(String value) {
        if (!value.matches("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+")) {
            return null;
        }
        return value.substring(0, value.lastIndexOf('.'));
    }

    static boolean isClassLiteral(String value) {
        String trimmed = value.trim();
        if (!trimmed.endsWith(".class")) return false;
        return isLikelyTypeName(trimmed.substring(0, trimmed.length() - ".class".length()).trim());
    }

    static boolean isStringLiteral(String value) {
        String trimmed = value.trim();
        return trimmed.length() >= 2
                && trimmed.charAt(0) == '"'
                && trimmed.charAt(trimmed.length() - 1) == '"';
    }

    static boolean isCharLiteral(String value) {
        String trimmed = value.trim();
        return trimmed.length() >= 3
                && trimmed.charAt(0) == '\''
                && trimmed.charAt(trimmed.length() - 1) == '\'';
    }

    private static String resolve(
            String typeName,
            Map<String, String> imports,
            String describedPackageName
    ) {
        return JavaSourceContext.resolveTypeName(typeName, imports, describedPackageName);
    }

    private static String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot < 0 ? qualifiedName : qualifiedName.substring(lastDot + 1);
    }

    private static JavaExpressionTypeInference.InferredType known(String typeName) {
        return JavaExpressionTypeInference.InferredType.known(typeName);
    }

    private static JavaExpressionTypeInference.InferredType unknown() {
        return JavaExpressionTypeInference.InferredType.unknownObject();
    }
}
