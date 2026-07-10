package io.github.jvmspec.discovery;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedClass;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final String JAVA_SUFFIX = SpecNamingConvention.JAVA_SUFFIX;
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
    private static final Pattern SHOULD_EXTEND_PATTERN = Pattern.compile("(?<!\\.)\\bshouldExtend\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_IMPLEMENT_PATTERN = Pattern.compile("(?<!\\.)\\bshouldImplement\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_PERMIT_PATTERN = Pattern.compile("(?<!\\.)\\bshouldPermit\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern CLASS_LITERAL_PATTERN = Pattern.compile("([A-Za-z_$][A-Za-z0-9_$.]*)\\s*\\.\\s*class");
    private static final Pattern BE_CONSTRUCTED_WITH_PATTERN = Pattern.compile("beConstructedWith\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern FACTORY_CONSTRUCTION_PATTERN = Pattern.compile("beConstructed(?:ThroughNamed|Through|Named)\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_HAVE_CONSTANT_PATTERN = Pattern.compile("shouldHaveConstant\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern METHOD_PATTERN = Pattern.compile("public\\s+void\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)\\s*(?:throws\\s+[^\\{]+)?\\{", Pattern.DOTALL);
    private static final Pattern PROXY_EXPECTATION_PATTERN = Pattern.compile(
            "\\b([a-z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*\\.\\s*"
                    + "(shouldReturn|shouldNotReturn|shouldBe|shouldNotBe|shouldEqual|shouldNotEqual|shouldBeLike|shouldNotBeLike|shouldBeEqualTo|shouldNotBeEqualTo|shouldHaveType|shouldBeAnInstanceOf|shouldReturnAnInstanceOf|shouldImplement|shouldContain|shouldNotContain|shouldStartWith|shouldNotStartWith|shouldEndWith|shouldNotEndWith|shouldMatchPattern|shouldNotMatchPattern|shouldHaveCount|shouldBeEmpty|shouldNotBeEmpty|shouldHaveKey|shouldNotHaveKey|shouldHaveValue|shouldNotHaveValue)"
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
    private static final Pattern MATCH_SUBJECT_PROXY_PATTERN = Pattern.compile(
            "match\\s*\\(\\s*subject\\s*\\(\\s*\\)\\s*\\.\\s*([a-z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*\\)\\s*\\.\\s*"
                    + "(shouldReturn|shouldNotReturn|shouldBe|shouldNotBe|shouldEqual|shouldNotEqual|shouldBeLike|shouldNotBeLike|shouldBeEqualTo|shouldNotBeEqualTo|shouldHaveType|shouldBeAnInstanceOf|shouldReturnAnInstanceOf|shouldImplement|shouldContain|shouldNotContain|shouldStartWith|shouldNotStartWith|shouldEndWith|shouldNotEndWith|shouldMatchPattern|shouldNotMatchPattern|shouldHaveCount|shouldBeEmpty|shouldNotBeEmpty|shouldHaveKey|shouldNotHaveKey|shouldHaveValue|shouldNotHaveValue)"
                    + "\\s*\\(([^;{}]*)\\)",
            Pattern.DOTALL
    );

    private SpecDiscovery() {
    }

    public static List<DiscoveredSpec> discover(File specRoot) {
        return discover(SpecDiscoveryRequest.of(specRoot));
    }

    public static List<DiscoveredSpec> discover(File specRoot, SpecNamingConvention namingConvention) {
        return discover(SpecDiscoveryRequest.of(specRoot, namingConvention));
    }

    public static List<DiscoveredSpec> discover(SpecDiscoveryRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        if (!request.matchesSuite() || !request.specRoot().isDirectory()) {
            return specs;
        }

        collect(request.specRoot(), "", specs, request);
        return specs;
    }

    private static void collect(File directory, String packagePrefix, List<DiscoveredSpec> specs, SpecDiscoveryRequest request) {
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
                collect(child, childPackagePrefix(packagePrefix, child.getName()), specs, request);
            } else if (child.isFile() && request.namingConvention().isSpecSourceFileName(child.getName())) {
                addSpec(child, packagePrefix, specs, request);
            }
        }
    }

    private static String childPackagePrefix(String packagePrefix, String childName) {
        if (packagePrefix.length() == 0) {
            return childName;
        }
        return packagePrefix + "." + childName;
    }

    private static void addSpec(File specFile, String packageName, List<DiscoveredSpec> specs, SpecDiscoveryRequest request) {
        String fileName = specFile.getName();
        String specSimpleName = fileName.substring(0, fileName.length() - JAVA_SUFFIX.length());
        String specQualifiedName;
        if (packageName.length() == 0) {
            specQualifiedName = specSimpleName;
        } else {
            specQualifiedName = packageName + "." + specSimpleName;
        }

        try {
            DescribedClass describedClass = request.namingConvention().describedClassForSpec(specQualifiedName);
            if (!request.matchesClass(describedClass, specQualifiedName)) {
                return;
            }

            String describedPackageName = describedClass.packageName();
            String describedQualifiedName = describedClass.qualifiedName();
            String source = sourceOf(specFile);
            List<SpecExample> examples = request.filterExamples(extractExamples(source));
            if (request.hasExampleFilters() && examples.isEmpty()) {
                return;
            }
            SpecCallScanner.ScanResult scan = scanSpecCalls(source);
            List<ConstructorDescriptor> constructors = extractConstructors(source, describedPackageName, scan);
            List<MethodDescriptor> methods = extractMethods(source, describedPackageName, describedQualifiedName, scan);
            List<DescribedType.EnumConstantInfo> enumConstants = extractEnumConstants(source, describedPackageName);
            // When enum constants have constructor arguments, infer the enum constructor
            if (describedKind(source).equals(JavaTypeKind.ENUM) && !enumConstants.isEmpty()) {
                List<ConstructorDescriptor> enumConstructors = enumConstructorsFromConstants(enumConstants);
                constructors = combineConstructors(constructors, enumConstructors);
            }
            specs.add(DiscoveredSpec.of(
                    specFile,
                    specQualifiedName,
                    DescribedType.of(
                            DescribedClass.of(describedQualifiedName),
                            describedKind(source),
                            typeNames(source, describedPackageName, SHOULD_EXTEND_PATTERN),
                            typeNames(source, describedPackageName, SHOULD_IMPLEMENT_PATTERN),
                            typeNames(source, describedPackageName, SHOULD_PERMIT_PATTERN),
                            constructors,
                            methods,
                            enumConstants
                    ),
                    examples
            ));
        } catch (IllegalArgumentException ignored) {
            // Ignore files that match the suffix convention but cannot be mapped to a valid Java type name.
        }
    }

    private static SpecCallScanner.ScanResult scanSpecCalls(String source) {
        try {
            return SpecCallScanner.scan(source);
        } catch (LinkageError ex) {
            // On Java 8, com.sun.source.* lives in tools.jar and may be absent from the
            // runtime classpath. Fall back to legacy text-based extraction instead of failing.
            return null;
        }
    }

    private static List<ConstructorDescriptor> extractConstructors(String source, String describedPackageName, SpecCallScanner.ScanResult scan) {
        Map<String, String> imports = importsBySimpleName(source);
        List<ConstructorDescriptor> constructors = new ArrayList<ConstructorDescriptor>();

        if (scan != null) {
            Map<String, MethodParameterInfo> methods = methodInfoFromScan(scan, imports, describedPackageName);
            for (int i = 0; i < scan.constructionCalls.size(); i++) {
                SpecCallScanner.Call call = scan.constructionCalls.get(i);
                ConstructionArguments arguments = inferConstructionArgumentsCore(
                        call.argumentTexts, methods.get(call.enclosingMethod), imports, describedPackageName);
                ConstructorDescriptor cd = ConstructorDescriptor.of(arguments.parameterTypes, arguments.parameterNames, "");
                if (!constructors.contains(cd)) {
                    constructors.add(cd);
                }
            }
            return constructors;
        }

        Map<String, MethodParameterInfo> methods = parseMethods(source, imports, describedPackageName);

        Matcher withMatcher = BE_CONSTRUCTED_WITH_PATTERN.matcher(source);
        while (withMatcher.find()) {
            List<String> argNames = splitArguments(withMatcher.group(1).trim());
            ConstructionArguments arguments = inferConstructionArguments(argNames, source, withMatcher.start(), methods, imports, describedPackageName);
            ConstructorDescriptor cd = ConstructorDescriptor.of(arguments.parameterTypes, arguments.parameterNames, "");
            if (!constructors.contains(cd)) {
                constructors.add(cd);
            }
        }

        return constructors;
    }

    private static ConstructionArguments inferConstructionArguments(
            List<String> argumentValues,
            String source,
            int position,
            Map<String, MethodParameterInfo> methods,
            Map<String, String> imports,
            String describedPackageName
    ) {
        MethodParameterInfo info = null;
        String methodName = findEnclosingMethod(source, position, methods);
        if (methodName != null && methods.containsKey(methodName)) {
            info = methods.get(methodName);
        }
        return inferConstructionArgumentsCore(argumentValues, info, imports, describedPackageName);
    }

    private static ConstructionArguments inferConstructionArgumentsCore(
            List<String> argumentValues,
            MethodParameterInfo info,
            Map<String, String> imports,
            String describedPackageName
    ) {
        List<String> paramTypes = new ArrayList<String>();
        List<String> paramNames = new ArrayList<String>();
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
            paramTypes.add(inferLiteralType(argName, imports, describedPackageName).typeName);
            String parameterName = parameterNameForArgument(argName, i);
            if (parameterName.equals("arg" + i)) {
                String derivedName = typeDerivedParameterName(argName);
                if (derivedName != null && !paramNames.contains(derivedName)) {
                    parameterName = derivedName;
                }
            }
            paramNames.add(parameterName);
        }
        return new ConstructionArguments(paramTypes, paramNames);
    }

    private static List<MethodDescriptor> extractMethods(String source, String describedPackageName, String describedQualifiedName, SpecCallScanner.ScanResult scan) {
        Map<String, String> imports = importsBySimpleName(source);
        Map<String, MethodDescriptor> discovered = new LinkedHashMap<String, MethodDescriptor>();

        if (scan != null) {
            extractMethodsFromScan(scan, describedQualifiedName, discovered, imports, describedPackageName);
            return new ArrayList<MethodDescriptor>(discovered.values());
        }

        Map<String, MethodParameterInfo> specMethods = parseMethods(source, imports, describedPackageName);

        addFactoryMethodDescriptors(source, describedQualifiedName, specMethods, discovered, imports, describedPackageName);

        Matcher proxyMatcher = PROXY_EXPECTATION_PATTERN.matcher(source);
        while (proxyMatcher.find()) {
            String methodName = proxyMatcher.group(1);
            if (isIgnoredProxyCall(methodName)) {
                continue;
            }
            String argumentSource = proxyMatcher.group(2).trim();
            String matcherName = proxyMatcher.group(3);
            String expectationSource = proxyMatcher.group(4).trim();
            InferredArguments arguments = inferArgumentTypes(argumentSource, source, proxyMatcher.start(), specMethods, imports, describedPackageName);
            List<String> parameterNames = parameterNamesFor(methodName, arguments.size());
            String returnType = inferReturnType(matcherName, expectationSource, imports, describedPackageName);
            addMethod(discovered, methodDescriptor(methodName, returnType, arguments, parameterNames));
        }

        Matcher duringMatcher = SHOULD_THROW_DURING_PATTERN.matcher(source);
        while (duringMatcher.find()) {
            String duringTarget = duringMatcher.group(1);
            if ("Instantiation".equals(duringTarget)) {
                continue;
            }
            String methodName = decapitalize(duringTarget);
            String argumentSource = duringMatcher.group(2).trim();
            InferredArguments arguments = inferArgumentTypes(argumentSource, source, duringMatcher.start(), specMethods, imports, describedPackageName);
            List<String> parameterNames = parameterNamesFor(methodName, arguments.size());
            addMethod(discovered, voidMethodDescriptor(methodName, arguments, parameterNames));
        }

        Matcher subjectVoidMatcher = SUBJECT_VOID_CALL_PATTERN.matcher(source);
        while (subjectVoidMatcher.find()) {
            String methodName = subjectVoidMatcher.group(1);
            if (isIgnoredProxyCall(methodName)) {
                continue;
            }
            if (!startsStatement(source, subjectVoidMatcher.start())) {
                // The subject() call is nested inside a larger expression (e.g. an assignment or a
                // match(...).shouldReturn(subject().x()) argument); it is not a standalone void
                // statement, so no void method descriptor must be synthesized for it.
                continue;
            }
            String argumentSource = subjectVoidMatcher.group(2).trim();
            if (!hasBalancedParentheses(argumentSource)) {
                // Greedy regex capture swallowed parentheses belonging to an enclosing expression;
                // treating that fragment as an argument list would fabricate phantom parameters.
                continue;
            }
            InferredArguments arguments = inferArgumentTypes(argumentSource, source, subjectVoidMatcher.start(), specMethods, imports, describedPackageName);
            List<String> parameterNames = parameterNamesFor(methodName, arguments.size());
            addMethod(discovered, voidMethodDescriptor(methodName, arguments, parameterNames));
        }

        Matcher matchSubjectMatcher = MATCH_SUBJECT_PROXY_PATTERN.matcher(source);
        while (matchSubjectMatcher.find()) {
            String methodName = matchSubjectMatcher.group(1);
            if (isIgnoredProxyCall(methodName)) {
                continue;
            }
            String argumentSource = matchSubjectMatcher.group(2).trim();
            String matcherName = matchSubjectMatcher.group(3);
            String expectationSource = matchSubjectMatcher.group(4).trim();
            InferredArguments arguments = inferArgumentTypes(argumentSource, source, matchSubjectMatcher.start(), specMethods, imports, describedPackageName);
            List<String> parameterNames = parameterNamesFor(methodName, arguments.size());
            String returnType = inferReturnType(matcherName, expectationSource, imports, describedPackageName);
            addMethod(discovered, methodDescriptor(methodName, returnType, arguments, parameterNames));
        }

        Matcher setterMatcher = PLAIN_SETTER_CALL_PATTERN.matcher(source);
        while (setterMatcher.find()) {
            String methodName = setterMatcher.group(1);
            String argumentSource = setterMatcher.group(2).trim();
            InferredArguments arguments = inferArgumentTypes(argumentSource, source, setterMatcher.start(), specMethods, imports, describedPackageName);
            List<String> parameterNames = parameterNamesFor(methodName, arguments.size());
            addMethod(discovered, voidMethodDescriptor(methodName, arguments, parameterNames));
        }

        return new ArrayList<MethodDescriptor>(discovered.values());
    }

    private static void extractMethodsFromScan(
            SpecCallScanner.ScanResult scan,
            String describedQualifiedName,
            Map<String, MethodDescriptor> discovered,
            Map<String, String> imports,
            String describedPackageName
    ) {
        Map<String, MethodParameterInfo> specMethods = methodInfoFromScan(scan, imports, describedPackageName);

        for (int i = 0; i < scan.factoryCalls.size(); i++) {
            SpecCallScanner.Call call = scan.factoryCalls.get(i);
            if (call.argumentTexts.isEmpty()) {
                continue;
            }
            String methodName = stringLiteralValue(call.argumentTexts.get(0));
            if (methodName == null || !isJavaIdentifier(methodName)) {
                continue;
            }
            List<String> factoryArguments = new ArrayList<String>();
            for (int j = 1; j < call.argumentTexts.size(); j++) {
                factoryArguments.add(call.argumentTexts.get(j));
            }
            ConstructionArguments arguments = inferConstructionArgumentsCore(
                    factoryArguments, specMethods.get(call.enclosingMethod), imports, describedPackageName);
            addMethod(discovered, MethodDescriptor.staticMethod(
                    methodName,
                    describedQualifiedName,
                    arguments.parameterTypes,
                    arguments.parameterNames
            ));
        }

        for (int i = 0; i < scan.proxyExpectations.size(); i++) {
            SpecCallScanner.Expectation expectation = scan.proxyExpectations.get(i);
            addExpectationMethod(expectation, specMethods, discovered, imports, describedPackageName);
        }

        for (int i = 0; i < scan.throwDuringCalls.size(); i++) {
            SpecCallScanner.Call call = scan.throwDuringCalls.get(i);
            if ("Instantiation".equals(call.name)) {
                continue;
            }
            String methodName = decapitalize(call.name);
            InferredArguments arguments = inferArgumentTypesCore(
                    call.argumentTexts, specMethods.get(call.enclosingMethod), imports, describedPackageName);
            addMethod(discovered, voidMethodDescriptor(
                    methodName, arguments, parameterNamesFor(methodName, arguments.size())));
        }

        for (int i = 0; i < scan.subjectVoidStatements.size(); i++) {
            SpecCallScanner.Call call = scan.subjectVoidStatements.get(i);
            InferredArguments arguments = inferArgumentTypesCore(
                    call.argumentTexts, specMethods.get(call.enclosingMethod), imports, describedPackageName);
            addMethod(discovered, voidMethodDescriptor(
                    call.name, arguments, parameterNamesFor(call.name, arguments.size())));
        }

        for (int i = 0; i < scan.matchSubjectExpectations.size(); i++) {
            SpecCallScanner.Expectation expectation = scan.matchSubjectExpectations.get(i);
            addExpectationMethod(expectation, specMethods, discovered, imports, describedPackageName);
        }

        for (int i = 0; i < scan.setterStatements.size(); i++) {
            SpecCallScanner.Call call = scan.setterStatements.get(i);
            InferredArguments arguments = inferArgumentTypesCore(
                    call.argumentTexts, specMethods.get(call.enclosingMethod), imports, describedPackageName);
            addMethod(discovered, voidMethodDescriptor(
                    call.name, arguments, parameterNamesFor(call.name, arguments.size())));
        }
    }

    private static void addExpectationMethod(
            SpecCallScanner.Expectation expectation,
            Map<String, MethodParameterInfo> specMethods,
            Map<String, MethodDescriptor> discovered,
            Map<String, String> imports,
            String describedPackageName
    ) {
        InferredArguments arguments = inferArgumentTypesCore(
                expectation.argumentTexts, specMethods.get(expectation.enclosingMethod), imports, describedPackageName);
        List<String> parameterNames = parameterNamesFor(expectation.name, arguments.size());
        String returnType = inferReturnType(
                expectation.matcherName, joinArguments(expectation.expectationTexts), imports, describedPackageName);
        addMethod(discovered, methodDescriptor(expectation.name, returnType, arguments, parameterNames));
    }

    private static MethodDescriptor methodDescriptor(
            String methodName,
            String returnType,
            InferredArguments arguments,
            List<String> parameterNames
    ) {
        return MethodDescriptor.of(methodName, returnType, arguments.types, parameterNames)
                .withUnknownParameterTypes(arguments.unknowns);
    }

    private static MethodDescriptor voidMethodDescriptor(
            String methodName,
            InferredArguments arguments,
            List<String> parameterNames
    ) {
        return MethodDescriptor.voidMethod(methodName, arguments.types, parameterNames)
                .withUnknownParameterTypes(arguments.unknowns);
    }

    private static String joinArguments(List<String> argumentTexts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < argumentTexts.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(argumentTexts.get(i));
        }
        return builder.toString();
    }

    private static Map<String, MethodParameterInfo> methodInfoFromScan(
            SpecCallScanner.ScanResult scan,
            Map<String, String> imports,
            String describedPackageName
    ) {
        Map<String, MethodParameterInfo> methods = new LinkedHashMap<String, MethodParameterInfo>();
        for (Map.Entry<String, SpecCallScanner.SpecMethodParams> entry : scan.specMethods.entrySet()) {
            SpecCallScanner.SpecMethodParams params = entry.getValue();
            List<String> types = new ArrayList<String>();
            for (int i = 0; i < params.typeTexts.size(); i++) {
                String typeText = params.typeTexts.get(i);
                String initializerText = i < params.initializerTexts.size() ? params.initializerTexts.get(i) : null;
                if ("var".equals(typeText) && initializerText != null) {
                    types.add(inferLiteralType(initializerText, imports, describedPackageName).typeName);
                } else {
                    types.add(resolveTypeName(typeText, imports, describedPackageName));
                }
            }
            methods.put(entry.getKey(), new MethodParameterInfo(types, params.names));
        }
        return methods;
    }

    private static void addFactoryMethodDescriptors(
            String source,
            String describedQualifiedName,
            Map<String, MethodParameterInfo> specMethods,
            Map<String, MethodDescriptor> discovered,
            Map<String, String> imports,
            String describedPackageName
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
            ConstructionArguments arguments = inferConstructionArguments(factoryArguments, source, matcher.start(), specMethods, imports, describedPackageName);
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

    /**
     * Returns {@code true} when the character preceding {@code position} (ignoring whitespace) is
     * a statement boundary ({@code ;}, <code>{</code> or <code>}</code>) or the start of the
     * source, i.e. the match at {@code position} begins a new statement rather than being nested
     * inside a larger expression.
     */
    private static boolean startsStatement(String source, int position) {
        for (int i = position - 1; i >= 0; i--) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            return c == ';' || c == '{' || c == '}';
        }
        return true;
    }

    private static boolean hasBalancedParentheses(String value) {
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    private static boolean isIgnoredProxyCall(String methodName) {
        return "match".equals(methodName)
                || "subject".equals(methodName)
                || methodName.startsWith("should")
                || methodName.startsWith("beConstructed")
                || "matcherRegistry".equals(methodName);
    }

    private static void addMethod(Map<String, MethodDescriptor> methods, MethodDescriptor candidate) {
        String matchingKey = null;
        MethodDescriptor existing = null;
        for (Map.Entry<String, MethodDescriptor> entry : methods.entrySet()) {
            if (entry.getValue().hasEquivalentSignature(candidate)) {
                matchingKey = entry.getKey();
                existing = entry.getValue();
                break;
            }
        }
        if (existing == null) {
            methods.put(candidate.normalizedSignatureKey(), candidate);
            return;
        }
        if (shouldReplace(existing, candidate)) {
            methods.remove(matchingKey);
            methods.put(candidate.normalizedSignatureKey(), candidate);
        }
    }

    private static boolean shouldReplace(MethodDescriptor existing, MethodDescriptor candidate) {
        if (existing.isStatic() != candidate.isStatic()) {
            return false;
        }
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

    private static String inferReturnType(
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

    private static InferredArguments inferArgumentTypes(
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

    private static InferredArguments inferArgumentTypesCore(
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

    /**
     * Derives an idiomatic parameter name from a qualified constant argument: for
     * {@code Algorithm.EC_P256} the qualifier {@code Algorithm} yields {@code algorithm}.
     * Returns {@code null} when the argument is not a qualified constant reference, so callers
     * keep the positional {@code argN} fallback.
     */
    private static String typeDerivedParameterName(String argument) {
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

    private static String parameterNameForArgument(String argument, int index) {
        if (isJavaIdentifier(argument)) {
            return argument;
        }
        return "arg" + index;
    }

    private static InferredType inferLiteralType(String expression, Map<String, String> imports, String describedPackageName) {
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

    private static String arrayCreationType(String value) {
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

    private static String constructedType(String value) {
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

    private static String staticFactoryReceiverType(String value) {
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

    private static String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot < 0 ? qualifiedName : qualifiedName.substring(lastDot + 1);
    }

    private static boolean isLikelyStaticFactoryMethod(String methodName) {
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

    private static String castType(String value) {
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

    private static boolean isLikelyTypeName(String value) {
        String trimmed = value.trim();
        if (trimmed.matches("(?:byte|short|int|long|float|double|boolean|char)(?:\\s*\\[\\s*\\])*")) {
            return true;
        }
        return trimmed.matches("[A-Z_$][A-Za-z0-9_$]*(?:\\s*\\[\\s*\\])*")
                || trimmed.matches("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+(?:\\s*\\[\\s*\\])*");
    }

    /**
     * Recognizes a qualified constant reference such as {@code Algorithm.EC_P256} or
     * {@code com.example.Algorithm.EC_P256} (an enum constant or a {@code public static final}
     * field access, with no method call or array access involved) and returns the qualifier as the
     * inferred type, e.g. {@code Algorithm} or {@code com.example.Algorithm}. Returns {@code null}
     * when the expression does not match this shape, so callers fall back to {@code Object}.
     */
    private static String qualifiedConstantReferenceType(String value) {
        if (!value.matches("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+")) {
            return null;
        }
        int lastDot = value.lastIndexOf('.');
        return value.substring(0, lastDot);
    }

    private static boolean isClassLiteral(String value) {
        String trimmed = value.trim();
        if (!trimmed.endsWith(".class")) {
            return false;
        }
        String target = trimmed.substring(0, trimmed.length() - ".class".length()).trim();
        return isLikelyTypeName(target);
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

    private static List<SpecExample> extractExamples(String source) {
        List<SpecExample> examples = new ArrayList<SpecExample>();
        Matcher methodMatcher = METHOD_PATTERN.matcher(source);
        int orderIndex = 0;
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            String paramsGroup = methodMatcher.group(2).trim();
            if (SpecExample.isExampleMethodName(methodName)) {
                examples.add(SpecExample.of(methodName, orderIndex, lineNumberAt(source, methodMatcher.start())));
                orderIndex++;
            }
        }
        return examples;
    }

    private static int lineNumberAt(String source, int position) {
        int safePosition = Math.max(0, Math.min(position, source.length()));
        int lineNumber = 1;
        for (int i = 0; i < safePosition; i++) {
            char character = source.charAt(i);
            if (character == '\n') {
                lineNumber++;
            } else if (character == '\r') {
                lineNumber++;
                if (i + 1 < safePosition && source.charAt(i + 1) == '\n') {
                    i++;
                }
            }
        }
        return lineNumber;
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
        Matcher matcher = METHOD_PATTERN.matcher(source);
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

    private static final class InferredArguments {
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

    private static final class InferredType {
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

    private static List<DescribedType.EnumConstantInfo> extractEnumConstants(String source, String describedPackageName) {
        Map<String, String> imports = importsBySimpleName(source);
        List<DescribedType.EnumConstantInfo> constants = new ArrayList<DescribedType.EnumConstantInfo>();
        Matcher matcher = SHOULD_HAVE_CONSTANT_PATTERN.matcher(source);
        while (matcher.find()) {
            String arguments = matcher.group(1).trim();
            if (arguments.length() == 0) {
                continue;
            }
            List<String> args = splitArguments(arguments);
            if (args.isEmpty()) {
                continue;
            }
            String nameArg = args.get(0).trim();
            String constantName = nameArg;
            if (constantName.startsWith("\"") && constantName.endsWith("\"")) {
                constantName = constantName.substring(1, constantName.length() - 1);
            }
            List<String> paramTypes = new ArrayList<String>();
            List<String> paramNames = new ArrayList<String>();
            List<String> paramValues = new ArrayList<String>();
            for (int i = 1; i < args.size(); i++) {
                String arg = args.get(i).trim();
                String type = inferLiteralType(arg, imports, describedPackageName).typeName;
                paramTypes.add(type);
                paramNames.add("arg" + (i - 1));
                paramValues.add(arg);
            }
            constants.add(DescribedType.EnumConstantInfo.of(constantName, paramTypes, paramNames, paramValues));
        }
        return constants;
    }

    private static List<ConstructorDescriptor> enumConstructorsFromConstants(List<DescribedType.EnumConstantInfo> constants) {
        // All enum constants with constructor args should share the same constructor signature.
        // Use the first constant's args to infer the constructor.
        for (int i = 0; i < constants.size(); i++) {
            DescribedType.EnumConstantInfo ci = constants.get(i);
            if (ci.hasParameters()) {
                List<ConstructorDescriptor> result = new ArrayList<ConstructorDescriptor>();
                result.add(ConstructorDescriptor.of(ci.parameterTypes(), ci.parameterNames(), ""));
                return result;
            }
        }
        return Collections.emptyList();
    }

    private static List<ConstructorDescriptor> combineConstructors(
            List<ConstructorDescriptor> existing,
            List<ConstructorDescriptor> additional
    ) {
        List<ConstructorDescriptor> result = new ArrayList<ConstructorDescriptor>(existing);
        for (int i = 0; i < additional.size(); i++) {
            ConstructorDescriptor cd = additional.get(i);
            if (!result.contains(cd)) {
                result.add(cd);
            }
        }
        return result;
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

    static String resolveTypeName(String typeName, Map<String, String> imports, String describedPackageName) {
        String normalized = typeName.trim();
        if (normalized.endsWith("[]")) {
            return resolveArrayTypeName(normalized, imports, describedPackageName);
        }
        if (isPrimitiveOrVoid(normalized) || normalized.indexOf('<') >= 0) {
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

    private static String resolveArrayTypeName(String typeName, Map<String, String> imports, String describedPackageName) {
        String component = typeName;
        StringBuilder suffix = new StringBuilder();
        while (component.endsWith("[]")) {
            component = component.substring(0, component.length() - 2).trim();
            suffix.append("[]");
        }
        if (component.indexOf('<') >= 0) {
            return typeName;
        }
        return resolveTypeName(component, imports, describedPackageName) + suffix.toString();
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

}
