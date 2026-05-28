package org.javaspec.discovery;

import org.javaspec.model.ConstructorDescriptor;
import org.javaspec.model.DescribedType;
import org.javaspec.model.JavaTypeKind;
import org.javaspec.model.MethodDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers Java specification skeletons using the PHPSpec-inspired *Spec naming convention.
 */
public final class SpecDiscovery {
    private static final String JAVA_SUFFIX = ".java";
    private static final String SPEC_PACKAGE_PREFIX = "spec";
    private static final String SPEC_PACKAGE_PREFIX_WITH_DOT = SPEC_PACKAGE_PREFIX + ".";
    private static final String SPEC_SUFFIX = "Spec";
    private static final String SPEC_JAVA_SUFFIX = SPEC_SUFFIX + JAVA_SUFFIX;
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern SHOULD_EXTEND_PATTERN = Pattern.compile("shouldExtend\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_IMPLEMENT_PATTERN = Pattern.compile("shouldImplement\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_PERMIT_PATTERN = Pattern.compile("shouldPermit\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern CLASS_LITERAL_PATTERN = Pattern.compile("([A-Za-z_$][A-Za-z0-9_$.]*)\\s*\\.\\s*class");
    private static final Pattern BE_CONSTRUCTED_WITH_PATTERN = Pattern.compile("beConstructedWith\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern FACTORY_CONSTRUCTION_PATTERN = Pattern.compile("beConstructed(?:ThroughNamed|Through|Named)\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern METHOD_PATTERN = Pattern.compile("public\\s+void\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)\\s*\\{", Pattern.DOTALL);
    private static final Pattern PROXY_EXPECTATION_PATTERN = Pattern.compile(
            "\\b([a-z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*\\.\\s*"
                    + "(shouldReturn|shouldNotReturn|shouldBe|shouldNotBe|shouldEqual|shouldNotEqual|shouldBeLike|shouldBeEqualTo|shouldContain|shouldNotContain|shouldStartWith|shouldEndWith|shouldMatchPattern)"
                    + "\\s*\\(([^;{}]*)\\)",
            Pattern.DOTALL
    );
    private static final Pattern SHOULD_THROW_DURING_PATTERN = Pattern.compile(
            "shouldThrow\\s*\\([^;{}]*\\)\\s*\\.\\s*during([A-Z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)",
            Pattern.DOTALL
    );
    private static final Pattern SUBJECT_VOID_CALL_PATTERN = Pattern.compile(
            "subject\\s*\\(\\s*\\)\\s*\\.\\s*([a-z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*;",
            Pattern.DOTALL
    );
    private static final Pattern PLAIN_SETTER_CALL_PATTERN = Pattern.compile(
            "(?m)^\\s*(set[A-Z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*;"
    );

    private SpecDiscovery() {
    }

    public static List<DiscoveredSpec> discover(File specRoot) {
        Objects.requireNonNull(specRoot, "specRoot must not be null");

        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        if (!specRoot.isDirectory()) {
            return specs;
        }

        collect(specRoot, "", specs);
        return specs;
    }

    private static void collect(File directory, String packagePrefix, List<DiscoveredSpec> specs) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }

        Arrays.sort(children, new Comparator<File>() {
            public int compare(File left, File right) {
                return left.getName().compareTo(right.getName());
            }
        });

        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                collect(child, childPackagePrefix(packagePrefix, child.getName()), specs);
            } else if (child.isFile() && child.getName().endsWith(SPEC_JAVA_SUFFIX)) {
                addSpec(child, packagePrefix, specs);
            }
        }
    }

    private static String childPackagePrefix(String packagePrefix, String childName) {
        if (packagePrefix.length() == 0) {
            return childName;
        }
        return packagePrefix + "." + childName;
    }

    private static void addSpec(File specFile, String packageName, List<DiscoveredSpec> specs) {
        String fileName = specFile.getName();
        String specSimpleName = fileName.substring(0, fileName.length() - JAVA_SUFFIX.length());
        String describedSimpleName = specSimpleName.substring(0, specSimpleName.length() - SPEC_SUFFIX.length());
        if (describedSimpleName.length() == 0) {
            return;
        }

        String describedPackageName = describedPackageName(packageName);
        String describedQualifiedName;
        String specQualifiedName;
        if (describedPackageName.length() == 0) {
            describedQualifiedName = describedSimpleName;
        } else {
            describedQualifiedName = describedPackageName + "." + describedSimpleName;
        }
        if (packageName.length() == 0) {
            specQualifiedName = specSimpleName;
        } else {
            specQualifiedName = packageName + "." + specSimpleName;
        }

        try {
            String source = sourceOf(specFile);
            List<ConstructorDescriptor> constructors = extractConstructors(source, describedPackageName);
            List<MethodDescriptor> methods = extractMethods(source, describedPackageName, describedQualifiedName);
            specs.add(DiscoveredSpec.of(
                    specFile,
                    specQualifiedName,
                    DescribedType.of(
                            describedQualifiedName,
                            describedKind(source),
                            typeNames(source, describedPackageName, SHOULD_EXTEND_PATTERN),
                            typeNames(source, describedPackageName, SHOULD_IMPLEMENT_PATTERN),
                            typeNames(source, describedPackageName, SHOULD_PERMIT_PATTERN),
                            constructors,
                            methods
                    )
            ));
        } catch (IllegalArgumentException ignored) {
            // Ignore files that match the suffix convention but cannot be mapped to a valid Java type name.
        }
    }

    private static List<ConstructorDescriptor> extractConstructors(String source, String describedPackageName) {
        Map<String, String> imports = importsBySimpleName(source);
        List<ConstructorDescriptor> constructors = new ArrayList<ConstructorDescriptor>();

        Map<String, MethodParameterInfo> methods = parseMethods(source, imports, describedPackageName);

        Matcher withMatcher = BE_CONSTRUCTED_WITH_PATTERN.matcher(source);
        while (withMatcher.find()) {
            List<String> argNames = splitArguments(withMatcher.group(1).trim());
            ConstructionArguments arguments = inferConstructionArguments(argNames, source, withMatcher.start(), methods);
            constructors.add(ConstructorDescriptor.of(arguments.parameterTypes, arguments.parameterNames, ""));
        }

        return constructors;
    }

    private static ConstructionArguments inferConstructionArguments(
            List<String> argumentValues,
            String source,
            int position,
            Map<String, MethodParameterInfo> methods
    ) {
        List<String> paramTypes = new ArrayList<String>();
        List<String> paramNames = new ArrayList<String>();
        MethodParameterInfo info = null;
        String methodName = findEnclosingMethod(source, position, methods);
        if (methodName != null && methods.containsKey(methodName)) {
            info = methods.get(methodName);
        }
        for (int i = 0; i < argumentValues.size(); i++) {
            String argName = argumentValues.get(i).trim();
            if (info != null) {
                int parameterIndex = info.names.indexOf(argName);
                if (parameterIndex >= 0) {
                    paramTypes.add(info.types.get(parameterIndex));
                    paramNames.add(info.names.get(parameterIndex));
                    continue;
                }
                if (i < info.types.size()) {
                    paramTypes.add(info.types.get(i));
                    paramNames.add(info.names.get(i));
                    continue;
                }
            }
            paramTypes.add(inferLiteralType(argName));
            paramNames.add(parameterNameForArgument(argName, i));
        }
        return new ConstructionArguments(paramTypes, paramNames);
    }

    private static List<MethodDescriptor> extractMethods(String source, String describedPackageName, String describedQualifiedName) {
        Map<String, String> imports = importsBySimpleName(source);
        Map<String, MethodParameterInfo> specMethods = parseMethods(source, imports, describedPackageName);
        Map<String, MethodDescriptor> discovered = new LinkedHashMap<String, MethodDescriptor>();

        addFactoryMethodDescriptors(source, describedQualifiedName, specMethods, discovered);

        Matcher proxyMatcher = PROXY_EXPECTATION_PATTERN.matcher(source);
        while (proxyMatcher.find()) {
            String methodName = proxyMatcher.group(1);
            if (isIgnoredProxyCall(methodName)) {
                continue;
            }
            String argumentSource = proxyMatcher.group(2).trim();
            String matcherName = proxyMatcher.group(3);
            String expectationSource = proxyMatcher.group(4).trim();
            List<String> parameterTypes = inferArgumentTypes(argumentSource, source, proxyMatcher.start(), specMethods);
            List<String> parameterNames = parameterNamesFor(methodName, parameterTypes.size());
            String returnType = inferReturnType(matcherName, expectationSource);
            addMethod(discovered, MethodDescriptor.of(methodName, returnType, parameterTypes, parameterNames));
        }

        Matcher duringMatcher = SHOULD_THROW_DURING_PATTERN.matcher(source);
        while (duringMatcher.find()) {
            String duringTarget = duringMatcher.group(1);
            if ("Instantiation".equals(duringTarget)) {
                continue;
            }
            String methodName = decapitalize(duringTarget);
            String argumentSource = duringMatcher.group(2).trim();
            List<String> parameterTypes = inferArgumentTypes(argumentSource, source, duringMatcher.start(), specMethods);
            List<String> parameterNames = parameterNamesFor(methodName, parameterTypes.size());
            addMethod(discovered, MethodDescriptor.voidMethod(methodName, parameterTypes, parameterNames));
        }

        Matcher subjectVoidMatcher = SUBJECT_VOID_CALL_PATTERN.matcher(source);
        while (subjectVoidMatcher.find()) {
            String methodName = subjectVoidMatcher.group(1);
            if (isIgnoredProxyCall(methodName)) {
                continue;
            }
            String argumentSource = subjectVoidMatcher.group(2).trim();
            List<String> parameterTypes = inferArgumentTypes(argumentSource, source, subjectVoidMatcher.start(), specMethods);
            List<String> parameterNames = parameterNamesFor(methodName, parameterTypes.size());
            addMethod(discovered, MethodDescriptor.voidMethod(methodName, parameterTypes, parameterNames));
        }

        Matcher setterMatcher = PLAIN_SETTER_CALL_PATTERN.matcher(source);
        while (setterMatcher.find()) {
            String methodName = setterMatcher.group(1);
            String argumentSource = setterMatcher.group(2).trim();
            List<String> parameterTypes = inferArgumentTypes(argumentSource, source, setterMatcher.start(), specMethods);
            List<String> parameterNames = parameterNamesFor(methodName, parameterTypes.size());
            addMethod(discovered, MethodDescriptor.voidMethod(methodName, parameterTypes, parameterNames));
        }

        return new ArrayList<MethodDescriptor>(discovered.values());
    }

    private static void addFactoryMethodDescriptors(
            String source,
            String describedQualifiedName,
            Map<String, MethodParameterInfo> specMethods,
            Map<String, MethodDescriptor> discovered
    ) {
        Matcher matcher = FACTORY_CONSTRUCTION_PATTERN.matcher(source);
        while (matcher.find()) {
            List<String> markerArguments = splitArguments(matcher.group(1).trim());
            if (markerArguments.isEmpty()) {
                continue;
            }
            String methodName = stringLiteralValue(markerArguments.get(0));
            if (methodName == null || !isJavaIdentifier(methodName)) {
                continue;
            }
            List<String> factoryArguments = new ArrayList<String>();
            for (int i = 1; i < markerArguments.size(); i++) {
                factoryArguments.add(markerArguments.get(i));
            }
            ConstructionArguments arguments = inferConstructionArguments(factoryArguments, source, matcher.start(), specMethods);
            addMethod(discovered, MethodDescriptor.staticMethod(
                    methodName,
                    describedQualifiedName,
                    arguments.parameterTypes,
                    arguments.parameterNames
            ));
        }
    }

    private static String stringLiteralValue(String value) {
        if (!isStringLiteral(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.substring(1, trimmed.length() - 1);
    }

    private static boolean isIgnoredProxyCall(String methodName) {
        return "match".equals(methodName)
                || "subject".equals(methodName)
                || methodName.startsWith("should")
                || methodName.startsWith("beConstructed")
                || "matcherRegistry".equals(methodName);
    }

    private static void addMethod(Map<String, MethodDescriptor> methods, MethodDescriptor candidate) {
        String key = methodKey(candidate.methodName(), candidate.parameterTypes());
        MethodDescriptor existing = methods.get(key);
        if (existing == null || shouldReplace(existing, candidate)) {
            methods.put(key, candidate);
        }
    }

    private static boolean shouldReplace(MethodDescriptor existing, MethodDescriptor candidate) {
        if (existing.isStatic() != candidate.isStatic()) {
            return false;
        }
        if ("Object".equals(existing.returnType()) && !"Object".equals(candidate.returnType())) {
            return true;
        }
        return existing.isVoid() && !candidate.isVoid();
    }

    private static String methodKey(String methodName, List<String> parameterTypes) {
        StringBuilder builder = new StringBuilder(methodName);
        builder.append('(');
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(parameterTypes.get(i));
        }
        builder.append(')');
        return builder.toString();
    }

    private static String inferReturnType(String matcherName, String expectationSource) {
        List<String> expectationArgs = splitArguments(expectationSource);
        if ("shouldContain".equals(matcherName)
                || "shouldNotContain".equals(matcherName)
                || "shouldStartWith".equals(matcherName)
                || "shouldEndWith".equals(matcherName)
                || "shouldMatchPattern".equals(matcherName)) {
            if (!expectationArgs.isEmpty() && isStringLiteral(expectationArgs.get(0))) {
                return "String";
            }
            return "Object";
        }
        if (expectationArgs.isEmpty()) {
            return "Object";
        }
        return inferLiteralType(expectationArgs.get(0));
    }

    private static List<String> inferArgumentTypes(
            String argumentSource,
            String source,
            int position,
            Map<String, MethodParameterInfo> specMethods
    ) {
        List<String> argumentValues = splitArguments(argumentSource);
        List<String> types = new ArrayList<String>();
        MethodParameterInfo enclosingInfo = null;
        String enclosingMethod = findEnclosingMethod(source, position, specMethods);
        if (enclosingMethod != null) {
            enclosingInfo = specMethods.get(enclosingMethod);
        }
        for (int i = 0; i < argumentValues.size(); i++) {
            String argument = argumentValues.get(i).trim();
            String type = null;
            if (enclosingInfo != null) {
                int parameterIndex = enclosingInfo.names.indexOf(argument);
                if (parameterIndex >= 0) {
                    type = enclosingInfo.types.get(parameterIndex);
                }
            }
            if (type == null) {
                type = inferLiteralType(argument);
            }
            types.add(type);
        }
        return types;
    }

    private static List<String> parameterNamesFor(String methodName, int count) {
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

    private static String parameterNameForArgument(String argument, int index) {
        if (isJavaIdentifier(argument)) {
            return argument;
        }
        return "arg" + index;
    }

    private static String inferLiteralType(String expression) {
        String value = expression.trim();
        if (value.length() == 0) {
            return "Object";
        }
        if (isStringLiteral(value)) {
            return "String";
        }
        if (isCharLiteral(value)) {
            return "char";
        }
        if ("true".equals(value) || "false".equals(value)) {
            return "boolean";
        }
        if ("null".equals(value)) {
            return "Object";
        }
        if (value.matches("[-+]?\\d+[lL]")) {
            return "long";
        }
        if (value.matches("[-+]?\\d+[dDfF]")) {
            return "double";
        }
        if (value.matches("[-+]?(?:\\d+\\.\\d*|\\d*\\.\\d+)(?:[eE][-+]?\\d+)?[dDfF]?")) {
            return "double";
        }
        if (value.matches("[-+]?\\d+(?:[eE][-+]?\\d+)[dDfF]?")) {
            return "double";
        }
        if (value.matches("[-+]?\\d+")) {
            return "int";
        }
        return "Object";
    }

    private static boolean isStringLiteral(String value) {
        String trimmed = value.trim();
        return trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"';
    }

    private static boolean isCharLiteral(String value) {
        String trimmed = value.trim();
        return trimmed.length() >= 3 && trimmed.charAt(0) == '\'' && trimmed.charAt(trimmed.length() - 1) == '\'';
    }

    private static List<String> splitArguments(String arguments) {
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

    private static Map<String, MethodParameterInfo> parseMethods(String source, Map<String, String> imports, String describedPackageName) {
        Map<String, MethodParameterInfo> methods = new LinkedHashMap<String, MethodParameterInfo>();
        Matcher methodMatcher = METHOD_PATTERN.matcher(source);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            String paramsGroup = methodMatcher.group(2).trim();
            List<String> types = new ArrayList<String>();
            List<String> names = new ArrayList<String>();
            if (paramsGroup.length() > 0) {
                List<String> params = splitArguments(paramsGroup);
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
            methods.put(methodName, new MethodParameterInfo(types, names));
        }
        return methods;
    }

    private static String findEnclosingMethod(String source, int position, Map<String, MethodParameterInfo> methods) {
        Pattern methodDeclPattern = Pattern.compile("public\\s+void\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)\\s*\\{");
        Matcher matcher = methodDeclPattern.matcher(source);
        String lastMethod = null;
        while (matcher.find() && matcher.start() < position) {
            lastMethod = matcher.group(1);
        }
        return lastMethod;
    }

    private static final class ConstructionArguments {
        final List<String> parameterTypes;
        final List<String> parameterNames;

        ConstructionArguments(List<String> parameterTypes, List<String> parameterNames) {
            this.parameterTypes = parameterTypes;
            this.parameterNames = parameterNames;
        }
    }

    private static final class MethodParameterInfo {
        final List<String> types;
        final List<String> names;

        MethodParameterInfo(List<String> types, List<String> names) {
            this.types = types;
            this.names = names;
        }
    }

    private static JavaTypeKind describedKind(String source) {
        if (source.contains("shouldBeAFinalClass(")) {
            return JavaTypeKind.FINAL_CLASS;
        }
        if (source.contains("shouldBeAnAnnotation(")) {
            return JavaTypeKind.ANNOTATION;
        }
        if (source.contains("shouldBeAnEnum(")) {
            return JavaTypeKind.ENUM;
        }
        if (source.contains("shouldBeARecord(")) {
            return JavaTypeKind.RECORD;
        }
        if (source.contains("shouldBeASealedInterface(")) {
            return JavaTypeKind.SEALED_INTERFACE;
        }
        if (source.contains("shouldBeASealedClass(")) {
            return JavaTypeKind.SEALED_CLASS;
        }
        if (source.contains("shouldBeAnInterface(")) {
            return JavaTypeKind.INTERFACE;
        }
        return JavaTypeKind.CLASS;
    }

    private static List<String> typeNames(String source, String describedPackageName, Pattern markerPattern) {
        List<String> typeNames = new ArrayList<String>();
        Map<String, String> imports = importsBySimpleName(source);
        Matcher markerMatcher = markerPattern.matcher(source);
        while (markerMatcher.find()) {
            String arguments = markerMatcher.group(1);
            Matcher classLiteralMatcher = CLASS_LITERAL_PATTERN.matcher(arguments);
            while (classLiteralMatcher.find()) {
                String typeName = resolveTypeName(classLiteralMatcher.group(1), imports, describedPackageName);
                if (!typeNames.contains(typeName)) {
                    typeNames.add(typeName);
                }
            }
        }
        return typeNames;
    }

    private static Map<String, String> importsBySimpleName(String source) {
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

    private static String resolveTypeName(String typeName, Map<String, String> imports, String describedPackageName) {
        String normalized = typeName.trim();
        if (isPrimitiveOrVoid(normalized) || normalized.indexOf('<') >= 0 || normalized.endsWith("[]")) {
            return normalized;
        }
        if (normalized.indexOf('.') >= 0) {
            return normalized;
        }
        String importedName = imports.get(normalized);
        if (importedName != null) {
            return importedName;
        }
        if (isJavaLangType(normalized)) {
            return normalized;
        }
        if (describedPackageName.length() == 0) {
            return normalized;
        }
        return describedPackageName + "." + normalized;
    }

    private static boolean isPrimitiveOrVoid(String typeName) {
        return "boolean".equals(typeName)
                || "byte".equals(typeName)
                || "short".equals(typeName)
                || "int".equals(typeName)
                || "long".equals(typeName)
                || "float".equals(typeName)
                || "double".equals(typeName)
                || "char".equals(typeName)
                || "void".equals(typeName);
    }

    private static boolean isJavaLangType(String typeName) {
        return "String".equals(typeName)
                || "Object".equals(typeName)
                || "Boolean".equals(typeName)
                || "Byte".equals(typeName)
                || "Short".equals(typeName)
                || "Integer".equals(typeName)
                || "Long".equals(typeName)
                || "Float".equals(typeName)
                || "Double".equals(typeName)
                || "Character".equals(typeName)
                || "Class".equals(typeName);
    }

    private static boolean isJavaIdentifier(String value) {
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

    private static String decapitalize(String value) {
        if (value.length() == 0) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String sourceOf(File specFile) {
        try {
            return new String(Files.readAllBytes(specFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        } catch (SecurityException ignored) {
            return "";
        }
    }

    private static String describedPackageName(String specPackageName) {
        if (SPEC_PACKAGE_PREFIX.equals(specPackageName)) {
            return "";
        }
        if (specPackageName.startsWith(SPEC_PACKAGE_PREFIX_WITH_DOT)) {
            return specPackageName.substring(SPEC_PACKAGE_PREFIX_WITH_DOT.length());
        }
        return specPackageName;
    }
}
