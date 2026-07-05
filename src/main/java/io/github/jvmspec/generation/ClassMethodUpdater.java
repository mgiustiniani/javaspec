package io.github.jvmspec.generation;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import io.github.jvmspec.generation.parser.JavaSourceParserLoader;
import io.github.jvmspec.generation.parser.ParsedSource;

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
 *
 * <p>For sealed interfaces, missing non-static method declarations are inserted into the sealed
 * root body, and missing method implementations are inserted into nested permitted implementations
 * declared in the same source file. Permitted types declared in other source files are
 * deliberately left untouched: the updater deterministically modifies only the sealed root and its
 * in-file nested permitted implementations.</p>
 */
public final class ClassMethodUpdater {
    private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile(
            "(?m)(?:^|[\\s;{}])" +
            "(?:(?:public|protected|private)\\s+)?" +
            "(?:(?:static|final|synchronized|abstract|native|strictfp)\\s+)*" +
            "([A-Za-z_$][A-Za-z0-9_$.<>?\\[\\]]*(?:\\s*\\[\\])?)\\s+" +
            "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)"
    );
    private static final Pattern NESTED_TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b"
    );
    private static final Pattern PERMITS_KEYWORD_PATTERN = Pattern.compile("\\bpermits\\b");
    private ClassMethodUpdater() {
    }

    public static String updateSource(String existingSource, DescribedType describedType) {
        Objects.requireNonNull(existingSource, "existingSource must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");

        if (!describedType.hasMethods()) {
            return existingSource;
        }
        if (JavaTypeKind.SEALED_INTERFACE.equals(describedType.kind())) {
            return updateSealedInterfaceSource(existingSource, describedType);
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
        return insertBeforeClosingBrace(existingSource, closingBrace, insertion);
    }

    public static boolean hasMissingMethods(String existingSource, DescribedType describedType) {
        Objects.requireNonNull(existingSource, "existingSource must not be null");
        Objects.requireNonNull(describedType, "describedType must not be null");
        if (JavaTypeKind.SEALED_INTERFACE.equals(describedType.kind())) {
            return !updateSealedInterfaceSource(existingSource, describedType).equals(existingSource);
        }
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

    /**
     * Inserts missing sealed-interface members source-preservingly.
     *
     * <p>Missing non-static method declarations are inserted into the sealed root body. Nested
     * permitted implementations declared inside the same source file (nested types named in the
     * root {@code permits} clause, named by the described permitted types, or the generated
     * default nested {@code Permitted} implementation) receive missing method implementations
     * with Java default returns; permitted entries that are themselves nested interfaces receive
     * method declarations instead. Permitted types declared in other source files are out of
     * scope and deliberately left untouched.</p>
     */
    private static String updateSealedInterfaceSource(String existingSource, DescribedType describedType) {
        List<MethodDescriptor> methods = interfaceMethods(describedType);
        if (methods.isEmpty()) {
            return existingSource;
        }
        String masked = maskNonCode(existingSource);
        int rootOpenBrace = findPrimaryTypeOpenBrace(masked, describedType.simpleName());
        if (rootOpenBrace < 0) {
            return existingSource;
        }
        int rootClosingBrace = findMatchingBraceInMasked(masked, rootOpenBrace);
        if (rootClosingBrace < 0) {
            return existingSource;
        }
        List<NestedTypeRegion> nestedRegions = nestedTypeRegions(masked, rootOpenBrace, rootClosingBrace);
        Set<String> permittedNames = permittedSimpleNames(masked, describedType);

        String result = existingSource;

        // The root closing brace is the highest insertion offset, so it is applied first; nested
        // regions are then applied from last to first so earlier offsets stay valid.
        List<MethodDescriptor> rootMissing = missingMethodsInScope(
                rootScopeText(masked, rootOpenBrace, rootClosingBrace, nestedRegions), methods);
        if (!rootMissing.isEmpty()) {
            String indent = indentationBefore(existingSource, rootClosingBrace) + "    ";
            String insertion = renderInterfaceDeclarations(rootMissing, describedType, indent);
            result = insertBeforeClosingBraceKeepingIndent(result, rootClosingBrace, insertion);
        }
        for (int i = nestedRegions.size() - 1; i >= 0; i--) {
            NestedTypeRegion region = nestedRegions.get(i);
            if (!permittedNames.contains(region.simpleName)) {
                continue;
            }
            List<MethodDescriptor> nestedMissing = missingMethodsInScope(
                    masked.substring(region.openBrace + 1, region.closingBrace), methods);
            if (nestedMissing.isEmpty()) {
                continue;
            }
            String indent = indentationBefore(existingSource, region.closingBrace) + "    ";
            String insertion = "interface".equals(region.keyword)
                    ? renderInterfaceDeclarations(nestedMissing, describedType, indent)
                    : renderMethods(nestedMissing, describedType, indent);
            result = insertBeforeClosingBraceKeepingIndent(result, region.closingBrace, insertion);
        }
        return result;
    }

    /**
     * Inserts before a closing brace while keeping the brace's indentation, both when the brace
     * sits on its own indented line and when it closes a single-line {@code { }} body.
     */
    private static String insertBeforeClosingBraceKeepingIndent(String source, int closingBrace, String insertion) {
        int cutPosition = insertionCutPosition(source, closingBrace);
        String closingIndent = cutPosition == closingBrace ? indentationBefore(source, closingBrace) : "";
        return insertBeforeClosingBrace(source, cutPosition, insertion, closingIndent);
    }

    /**
     * Finds nested type declarations placed directly inside the sealed root body.
     */
    private static List<NestedTypeRegion> nestedTypeRegions(String masked, int rootOpenBrace, int rootClosingBrace) {
        List<NestedTypeRegion> regions = new ArrayList<NestedTypeRegion>();
        Matcher matcher = NESTED_TYPE_PATTERN.matcher(masked);
        matcher.region(rootOpenBrace + 1, rootClosingBrace);
        while (matcher.find()) {
            if (braceDepthBetween(masked, rootOpenBrace + 1, matcher.start()) != 0) {
                continue;
            }
            int openBrace = masked.indexOf('{', matcher.end());
            if (openBrace < 0 || openBrace >= rootClosingBrace) {
                continue;
            }
            int closingBrace = findMatchingBraceInMasked(masked, openBrace);
            if (closingBrace < 0 || closingBrace >= rootClosingBrace) {
                continue;
            }
            regions.add(new NestedTypeRegion(matcher.group(1), matcher.group(2), matcher.start(), openBrace, closingBrace));
        }
        return regions;
    }

    private static int braceDepthBetween(String masked, int start, int end) {
        int depth = 0;
        for (int i = start; i < end; i++) {
            char c = masked.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return depth;
    }

    /**
     * Returns the masked sealed root body with nested type declarations blanked out, so that
     * signature de-duplication for the root scope ignores nested implementation members.
     */
    private static String rootScopeText(String masked, int rootOpenBrace, int rootClosingBrace, List<NestedTypeRegion> nestedRegions) {
        char[] scope = masked.substring(rootOpenBrace + 1, rootClosingBrace).toCharArray();
        for (int r = 0; r < nestedRegions.size(); r++) {
            NestedTypeRegion region = nestedRegions.get(r);
            int from = Math.max(0, region.declarationStart - rootOpenBrace - 1);
            int to = Math.min(scope.length, region.closingBrace + 1 - rootOpenBrace - 1);
            for (int i = from; i < to; i++) {
                scope[i] = blankedChar(scope[i]);
            }
        }
        return new String(scope);
    }

    /**
     * Collects the simple names of permitted implementations targeted for in-file updates: names
     * from the source {@code permits} clause, names from the described permitted types, and the
     * generated default nested {@code Permitted} implementation name.
     */
    private static Set<String> permittedSimpleNames(String masked, DescribedType describedType) {
        Set<String> names = new LinkedHashSet<String>();
        Matcher headerMatcher = primaryTypePattern(describedType.simpleName()).matcher(masked);
        if (headerMatcher.find()) {
            String header = headerMatcher.group();
            Matcher permitsMatcher = PERMITS_KEYWORD_PATTERN.matcher(header);
            if (permitsMatcher.find()) {
                String clause = header.substring(permitsMatcher.end(), header.length() - 1);
                String[] entries = clause.split(",");
                for (int i = 0; i < entries.length; i++) {
                    String entry = entries[i].trim();
                    if (entry.length() > 0) {
                        names.add(simpleNameOf(entry));
                    }
                }
            }
        }
        List<String> permittedTypeNames = describedType.permittedTypeNames();
        for (int i = 0; i < permittedTypeNames.size(); i++) {
            names.add(simpleNameOf(permittedTypeNames.get(i)));
        }
        names.add("Permitted");
        return names;
    }

    private static String simpleNameOf(String typeName) {
        String trimmed = typeName.trim();
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot < 0) {
            return trimmed;
        }
        return trimmed.substring(lastDot + 1);
    }

    /**
     * Moves the insertion cut to the start of the closing-brace line when the brace is preceded
     * only by indentation, so indented nested closing braces keep their indentation.
     */
    private static int insertionCutPosition(String source, int closingBrace) {
        int index = closingBrace - 1;
        while (index >= 0 && (source.charAt(index) == ' ' || source.charAt(index) == '\t')) {
            index--;
        }
        if (index >= 0 && source.charAt(index) == '\n') {
            return index + 1;
        }
        return closingBrace;
    }

    private static String insertBeforeClosingBrace(String source, int cutPosition, String insertion) {
        return insertBeforeClosingBrace(source, cutPosition, insertion, "");
    }

    private static String insertBeforeClosingBrace(String source, int cutPosition, String insertion, String closingIndent) {
        String prefix = source.substring(0, cutPosition);
        String suffix = source.substring(cutPosition);

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
        builder.append(closingIndent);
        builder.append(suffix);
        return builder.toString();
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
        return missingMethodsInScope(source, eligibleMethods(describedType));
    }

    private static List<MethodDescriptor> missingMethodsInScope(String scopeSource, List<MethodDescriptor> methods) {
        // Use the comment-stripping parser (SPI) to detect existing methods, eliminating false
        // positives from method names that appear inside comments or string literals.
        ParsedSource parsed = JavaSourceParserLoader.select(effectiveParserClassLoader()).parse(scopeSource);
        Set<String> existingSignatures = existingMethodSignatures(scopeSource);
        List<MethodDescriptor> missing = new ArrayList<MethodDescriptor>();
        Set<String> plannedSignatures = new LinkedHashSet<String>();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor method = methods.get(i);
            String key = signatureKey(method.methodName(), method.parameterTypes());
            boolean existsBySignatureKey = existingSignatures.contains(key);
            boolean existsByParser = parsed.hasMethod(method.methodName(), method.parameterTypes());
            if (existsBySignatureKey || existsByParser || plannedSignatures.contains(key)) {
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
            if (JavaTypeKind.ENUM.equals(kind)) {
                return nonImplicitEnumMethods(describedType.methods());
            }
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

    /**
     * Filters out methods that are implicitly defined on all enum types (values(), valueOf(String), name(), ordinal())
     * to prevent javaspec from inserting redundant stubs.
     */
    private static List<MethodDescriptor> nonImplicitEnumMethods(List<MethodDescriptor> methods) {
        List<MethodDescriptor> result = new ArrayList<MethodDescriptor>();
        for (int i = 0; i < methods.size(); i++) {
            MethodDescriptor m = methods.get(i);
            if (isImplicitEnumMethod(m)) {
                continue;
            }
            result.add(m);
        }
        return result;
    }

    private static boolean isImplicitEnumMethod(MethodDescriptor m) {
        // valueOf(String) is implicitly defined on every enum
        if ("valueOf".equals(m.methodName()) && m.parameterTypes().size() == 1
                && "String".equals(m.parameterTypes().get(0))) {
            return true;
        }
        // values() is implicitly defined on every enum
        if ("values".equals(m.methodName()) && m.parameterTypes().isEmpty()) {
            return true;
        }
        // name() and ordinal() are inherited from Enum
        if (("name".equals(m.methodName()) || "ordinal".equals(m.methodName()))
                && m.parameterTypes().isEmpty()) {
            return true;
        }
        return false;
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
        String codeOnlySource = maskNonCode(source);
        Matcher matcher = METHOD_SIGNATURE_PATTERN.matcher(codeOnlySource);
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

    private static ClassLoader effectiveParserClassLoader() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            return context;
        }
        return ClassMethodUpdater.class.getClassLoader();
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
        int openBrace = findPrimaryTypeOpenBrace(source, simpleName);
        if (openBrace < 0) {
            return -1;
        }
        return findMatchingBrace(source, openBrace);
    }

    private static int findPrimaryTypeOpenBrace(String source, String simpleName) {
        Matcher matcher = primaryTypePattern(simpleName).matcher(source);
        if (!matcher.find()) {
            return -1;
        }
        return matcher.end() - 1;
    }

    private static Pattern primaryTypePattern(String simpleName) {
        return Pattern.compile("\\b(?:class|interface|enum|record)\\s+" + Pattern.quote(simpleName) + "\\b[^\\{]*\\{", Pattern.DOTALL);
    }

    private static int findMatchingBrace(String source, int openBrace) {
        return findMatchingBraceInMasked(maskNonCode(source), openBrace);
    }

    private static int findMatchingBraceInMasked(String masked, int openBrace) {
        int depth = 0;
        for (int i = openBrace; i < masked.length(); i++) {
            char c = masked.charAt(i);
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

    /**
     * Returns a copy of the source where comments, string literals, and char literals are blanked
     * with spaces while offsets and line breaks are preserved, so that structural scanning can
     * tolerate braces and keywords inside non-code text.
     */
    private static String maskNonCode(String source) {
        char[] chars = source.toCharArray();
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean escaped = false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            char next = i + 1 < chars.length ? chars[i + 1] : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                } else {
                    chars[i] = blankedChar(c);
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    chars[i] = ' ';
                    chars[i + 1] = ' ';
                    i++;
                } else {
                    chars[i] = blankedChar(c);
                }
                continue;
            }
            if (escaped) {
                chars[i] = ' ';
                escaped = false;
                continue;
            }
            if ((inString || inChar) && c == '\\') {
                chars[i] = ' ';
                escaped = true;
                continue;
            }
            if (!inString && !inChar && c == '/' && next == '/') {
                inLineComment = true;
                chars[i] = ' ';
                chars[i + 1] = ' ';
                i++;
                continue;
            }
            if (!inString && !inChar && c == '/' && next == '*') {
                inBlockComment = true;
                chars[i] = ' ';
                chars[i + 1] = ' ';
                i++;
                continue;
            }
            if (!inChar && c == '"') {
                inString = !inString;
                chars[i] = ' ';
                continue;
            }
            if (!inString && c == '\'') {
                inChar = !inChar;
                chars[i] = ' ';
                continue;
            }
            if (inString || inChar) {
                chars[i] = blankedChar(c);
            }
        }
        return new String(chars);
    }

    private static char blankedChar(char c) {
        return c == '\n' || c == '\r' ? c : ' ';
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
        builder.append(indent).append("    ").append(StubMarkerScanner.STUB_MARKER).append("\n");
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

    /**
     * Region of a nested type declaration found inside the sealed root body.
     */
    private static final class NestedTypeRegion {
        private final String keyword;
        private final String simpleName;
        private final int declarationStart;
        private final int openBrace;
        private final int closingBrace;

        private NestedTypeRegion(String keyword, String simpleName, int declarationStart, int openBrace, int closingBrace) {
            this.keyword = keyword;
            this.simpleName = simpleName;
            this.declarationStart = declarationStart;
            this.openBrace = openBrace;
            this.closingBrace = closingBrace;
        }
    }
}
