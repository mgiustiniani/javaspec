package org.javaspec.generation;

import org.javaspec.model.ConstructorDescriptor;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.model.MethodDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inserts missing public method skeletons into existing class-like source without rewriting the body.
 */
public final class ClassMethodUpdater {
    private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile(
            "(?m)(?:^|[\\s;{}])" +
            "(?:(?:public|protected|private)\\s+)?" +
            "(?:(?:static|final|synchronized|abstract|native|strictfp)\\s+)*" +
            "([A-Za-z_$][A-Za-z0-9_$.<>?\\[\\]]*(?:\\s*\\[\\])?)\\s+" +
            "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)"
    );
    private ClassMethodUpdater() {
    }

    public static String updateSource(String existingSource, DescribedType describedType) {
        Objects.requireNonNull(existingSource, "existingSource must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");

        if (!describedType.hasMethods()) {
            return existingSource;
        }

        List<MethodDescriptor> missingMethods = missingMethods(existingSource, describedType);
        if (missingMethods.isEmpty()) {
            return existingSource;
        }

        int closingBrace = findPrimaryTypeClosingBrace(existingSource, describedType.simpleName());
        if (closingBrace < 0) {
            return existingSource;
        }

        String indent = indentationBefore(existingSource, closingBrace) + "    ";
        String insertion = renderMissingMethods(missingMethods, describedType, indent);
        if (insertion.length() == 0) {
            return existingSource;
        }
        String prefix = existingSource.substring(0, closingBrace);
        String suffix = existingSource.substring(closingBrace);

        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        if (!prefix.endsWith("\n")) {
            builder.append("\n");
        }
        if (!endsWithBlankLine(builder)) {
            builder.append("\n");
        }
        builder.append(insertion);
        if (!insertion.endsWith("\n")) {
            builder.append("\n");
        }
        builder.append(suffix);
        return builder.toString();
    }

    public static boolean hasMissingMethods(String existingSource, DescribedType describedType) {
        Objects.requireNonNull(existingSource, "existingSource must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");
        return !missingMethods(existingSource, describedType).isEmpty();
    }

    public static String updateFile(File classFile, DescribedType describedType) throws IOException {
        Objects.requireNonNull(classFile, "classFile must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");

        String existingSource = new String(Files.readAllBytes(classFile.toPath()), StandardCharsets.UTF_8);
        String updatedSource = updateSource(existingSource, describedType);
        if (!existingSource.equals(updatedSource)) {
            Files.write(classFile.toPath(), updatedSource.getBytes(StandardCharsets.UTF_8));
        }
        return updatedSource;
    }

    private static boolean supportsMethodBodies(JavaTypeKind kind) {
        return JavaTypeKind.CLASS.equals(kind)
                || JavaTypeKind.FINAL_CLASS.equals(kind)
                || JavaTypeKind.SEALED_CLASS.equals(kind)
                || JavaTypeKind.ENUM.equals(kind)
                || JavaTypeKind.RECORD.equals(kind);
    }

    private static boolean supportsInterfaceDeclarations(JavaTypeKind kind) {
        return JavaTypeKind.INTERFACE.equals(kind);
    }

    private static boolean supportsAnnotationElements(JavaTypeKind kind) {
        return JavaTypeKind.ANNOTATION.equals(kind);
    }

    private static List<MethodDescriptor> missingMethods(String source, DescribedType describedType) {
        Set<String> existingSignatures = existingMethodSignatures(source);
        List<MethodDescriptor> missing = new ArrayList<MethodDescriptor>();
        Set<String> plannedSignatures = new LinkedHashSet<String>();
        List<MethodDescriptor> methods = eligibleMethods(describedType);
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            String key = signatureKey(method.methodName(), method.parameterTypes());
            if (existingSignatures.contains(key) || plannedSignatures.contains(key)) {
                continue;
            }
            missing.add(method);
            plannedSignatures.add(key);
        }
        return missing;
    }

    private static List<MethodDescriptor> eligibleMethods(DescribedType describedType) {
        JavaTypeKind kind = describedType.kind();
        if (supportsMethodBodies(kind)) {
            return describedType.methods();
        }
        if (supportsInterfaceDeclarations(kind)) {
            return interfaceMethods(describedType);
        }
        if (supportsAnnotationElements(kind)) {
            return annotationElementMethods(describedType);
        }
        return new ArrayList<MethodDescriptor>();
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

    private static Set<String> existingMethodSignatures(String source) {
        Set<String> signatures = new LinkedHashSet<String>();
        Matcher matcher = METHOD_SIGNATURE_PATTERN.matcher(source);
        while (matcher.find()) {
            String returnType = matcher.group(1);
            if (isStatementKeyword(returnType)) {
                continue;
            }
            String methodName = matcher.group(2);
            List<String> parameterTypes = parseParameterTypes(matcher.group(3));
            signatures.add(signatureKey(methodName, parameterTypes));
        }
        return signatures;
    }

    private static boolean isStatementKeyword(String value) {
        return "return".equals(value)
                || "new".equals(value)
                || "if".equals(value)
                || "for".equals(value)
                || "while".equals(value)
                || "switch".equals(value)
                || "catch".equals(value);
    }

    private static List<String> parseParameterTypes(String parameterSource) {
        List<String> types = new ArrayList<String>();
        String trimmed = parameterSource.trim();
        if (trimmed.length() == 0) {
            return types;
        }
        List<String> parameters = splitArguments(trimmed);
        for (int i = 0; i < parameters.size(); i++) {
            String parameter = stripParameterDecorators(parameters.get(i).trim());
            int lastSpace = parameter.lastIndexOf(' ');
            if (lastSpace < 0) {
                continue;
            }
            String type = parameter.substring(0, lastSpace).trim();
            if (type.endsWith("...")) {
                type = type.substring(0, type.length() - 3) + "[]";
            }
            types.add(type);
        }
        return types;
    }

    private static String stripParameterDecorators(String parameter) {
        String result = parameter;
        while (result.startsWith("@")) {
            int space = result.indexOf(' ');
            if (space < 0) {
                return result;
            }
            result = result.substring(space + 1).trim();
        }
        while (result.startsWith("final ")) {
            result = result.substring("final ".length()).trim();
        }
        return result;
    }

    private static List<String> splitArguments(String arguments) {
        List<String> result = new ArrayList<String>();
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
                if (c == '(' || c == '[' || c == '<') {
                    nesting++;
                } else if (c == ')' || c == ']' || c == '>') {
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
        if (current.length() > 0 || arguments.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private static String signatureKey(String methodName, List<String> parameterTypes) {
        StringBuilder builder = new StringBuilder(methodName);
        builder.append('(');
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(normalizeType(parameterTypes.get(i)));
        }
        builder.append(')');
        return builder.toString();
    }

    private static String normalizeType(String typeName) {
        String normalized = typeName.trim().replace("...", "[]").replaceAll("\\s+", "");
        int arrayIndex = normalized.indexOf("[]");
        String suffix = "";
        if (arrayIndex >= 0) {
            suffix = normalized.substring(arrayIndex);
            normalized = normalized.substring(0, arrayIndex);
        }
        int genericIndex = normalized.indexOf('<');
        String generic = "";
        if (genericIndex >= 0) {
            generic = normalized.substring(genericIndex);
            normalized = normalized.substring(0, genericIndex);
        }
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot >= 0) {
            normalized = normalized.substring(lastDot + 1);
        }
        return normalized + generic + suffix;
    }

    private static int findPrimaryTypeClosingBrace(String source, String simpleName) {
        Pattern pattern = Pattern.compile("\\b(?:class|interface|enum|record)\\s+" + Pattern.quote(simpleName) + "\\b[^\\{]*\\{", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return -1;
        }
        int openBrace = matcher.end() - 1;
        return findMatchingBrace(source, openBrace);
    }

    private static int findMatchingBrace(String source, int openBrace) {
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean escaped = false;
        for (int i = openBrace; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (escaped) {
                escaped = false;
                continue;
            }
            if ((inString || inChar) && c == '\\') {
                escaped = true;
                continue;
            }
            if (!inString && !inChar && c == '/' && next == '/') {
                inLineComment = true;
                i++;
                continue;
            }
            if (!inString && !inChar && c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            if (!inChar && c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString && c == '\'') {
                inChar = !inChar;
                continue;
            }
            if (inString || inChar) {
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String indentationBefore(String source, int position) {
        int index = position - 1;
        while (index >= 0 && source.charAt(index) != '\n' && source.charAt(index) != '\r') {
            index--;
        }
        int lineStart = index + 1;
        StringBuilder indentation = new StringBuilder();
        while (lineStart < position) {
            char c = source.charAt(lineStart);
            if (c == ' ' || c == '\t') {
                indentation.append(c);
                lineStart++;
                continue;
            }
            break;
        }
        return indentation.toString();
    }

    private static boolean endsWithBlankLine(StringBuilder builder) {
        int length = builder.length();
        if (length < 2) {
            return false;
        }
        int last = length - 1;
        if (builder.charAt(last) != '\n') {
            return false;
        }
        int previous = last - 1;
        while (previous >= 0 && (builder.charAt(previous) == ' ' || builder.charAt(previous) == '\t' || builder.charAt(previous) == '\r')) {
            previous--;
        }
        return previous >= 0 && builder.charAt(previous) == '\n';
    }

    private static String renderMissingMethods(List<MethodDescriptor> methods, DescribedType owner, String indent) {
        JavaTypeKind kind = owner.kind();
        if (supportsMethodBodies(kind)) {
            return renderMethods(methods, owner, indent);
        }
        if (supportsInterfaceDeclarations(kind)) {
            return renderInterfaceDeclarations(methods, owner, indent);
        }
        if (supportsAnnotationElements(kind)) {
            return renderAnnotationElements(methods, owner, indent);
        }
        return "";
    }

    private static String renderMethods(List<MethodDescriptor> methods, DescribedType owner, String indent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            appendMethod(builder, owner, method, indent);
            if (i < methods.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private static String renderInterfaceDeclarations(List<MethodDescriptor> methods, DescribedType owner, String indent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            appendInterfaceDeclaration(builder, owner, method, indent);
            if (i < methods.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private static String renderAnnotationElements(List<MethodDescriptor> methods, DescribedType owner, String indent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            builder.append(indent).append(sourceTypeName(owner, method.returnType())).append(" ")
                    .append(method.methodName()).append("();\n");
            if (i < methods.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private static void appendInterfaceDeclaration(StringBuilder builder, DescribedType owner, MethodDescriptor method, String indent) {
        builder.append(indent).append(sourceTypeName(owner, method.returnType())).append(" ")
                .append(method.methodName()).append("(");
        appendParameters(builder, method.parameterTypes(), method.parameterNames());
        builder.append(");\n");
    }

    private static void appendMethod(StringBuilder builder, DescribedType owner, MethodDescriptor method, String indent) {
        builder.append(indent).append("public ");
        if (method.isStatic()) {
            builder.append("static ");
        }
        builder.append(sourceTypeName(owner, method.returnType())).append(" ")
                .append(method.methodName()).append("(");
        appendParameters(builder, method.parameterTypes(), method.parameterNames());
        builder.append(") {\n");
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
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(types.get(i)).append(" ").append(names.get(i));
        }
    }

    private static String sourceTypeName(DescribedType owner, String typeName) {
        String packageName = owner.packageName();
        if (packageName.length() > 0 && typeName.startsWith(packageName + ".")) {
            return typeName.substring(packageName.length() + 1);
        }
        return typeName;
    }
}
