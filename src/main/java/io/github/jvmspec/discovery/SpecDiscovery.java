package io.github.jvmspec.discovery;

import static io.github.jvmspec.discovery.JavaExpressionTypeInference.InferredArguments;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.InferredType;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.MethodParameterInfo;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.decapitalize;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.importsBySimpleName;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferArgumentTypes;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferArgumentTypesCore;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferLiteralType;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferReturnType;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.isJavaIdentifier;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.isStringLiteral;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.methodInfoFromScan;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.parameterNamesFor;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.parseMethods;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.splitArguments;

import io.github.jvmspec.internal.type.JavaTypeResolutionContext;
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
    private static final String UNRESOLVED_FUNCTIONAL_PARAMETER_PREFIX = "__javaspecFunctionalArg";
    private static final Pattern SHOULD_EXTEND_PATTERN = Pattern.compile("(?<!\\.)\\bshouldExtend\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_IMPLEMENT_PATTERN = Pattern.compile("(?<!\\.)\\bshouldImplement\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_PERMIT_PATTERN = Pattern.compile("(?<!\\.)\\bshouldPermit\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern CLASS_LITERAL_PATTERN = Pattern.compile("([A-Za-z_$][A-Za-z0-9_$.]*)\\s*\\.\\s*class");
    private static final Pattern FACTORY_CONSTRUCTION_PATTERN = Pattern.compile("beConstructed(?:ThroughNamed|Through|Named)\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_HAVE_CONSTANT_PATTERN = Pattern.compile("shouldHaveConstant\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern PROXY_EXPECTATION_PATTERN = Pattern.compile(
            "\\b([a-z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*\\.\\s*"
                    + "(shouldReturn|shouldNotReturn|shouldBe|shouldNotBe|shouldEqual|shouldNotEqual|shouldBeLike|shouldNotBeLike|shouldBeEqualTo|shouldNotBeEqualTo|shouldBeApproximately|shouldReturnApproximately|shouldNotBeApproximately|shouldNotReturnApproximately|shouldHaveType|shouldBeAnInstanceOf|shouldReturnAnInstanceOf|shouldImplement|shouldContain|shouldNotContain|shouldStartWith|shouldNotStartWith|shouldEndWith|shouldNotEndWith|shouldMatchPattern|shouldNotMatchPattern|shouldHaveCount|shouldBeEmpty|shouldNotBeEmpty|shouldHaveKey|shouldNotHaveKey|shouldHaveValue|shouldNotHaveValue)"
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
    private static final Pattern STATE_EXPECTATION_PATTERN = Pattern.compile(
            "(?m)^\\s*((?:shouldBe|shouldNotBe|shouldHave|shouldNotHave)[A-Z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*;"
    );
    private static final Pattern MATCH_SUBJECT_PROXY_PATTERN = Pattern.compile(
            "match\\s*\\(\\s*subject\\s*\\(\\s*\\)\\s*\\.\\s*([a-z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*\\)\\s*\\.\\s*"
                    + "(shouldReturn|shouldNotReturn|shouldBe|shouldNotBe|shouldEqual|shouldNotEqual|shouldBeLike|shouldNotBeLike|shouldBeEqualTo|shouldNotBeEqualTo|shouldBeApproximately|shouldReturnApproximately|shouldNotBeApproximately|shouldNotReturnApproximately|shouldHaveType|shouldBeAnInstanceOf|shouldReturnAnInstanceOf|shouldImplement|shouldContain|shouldNotContain|shouldStartWith|shouldNotStartWith|shouldEndWith|shouldNotEndWith|shouldMatchPattern|shouldNotMatchPattern|shouldHaveCount|shouldBeEmpty|shouldNotBeEmpty|shouldHaveKey|shouldNotHaveKey|shouldHaveValue|shouldNotHaveValue)"
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
            List<SpecExample> examples = request.filterExamples(ExampleDiscovery.discover(source));
            if (request.hasExampleFilters() && examples.isEmpty()) {
                return;
            }
            SpecCallScanner.ScanResult scan = scanSpecCalls(source);
            JavaTypeKind kind = describedKind(source);
            List<ConstructorDescriptor> constructors = ConstructionDiscovery.discover(
                    source, describedPackageName, describedQualifiedName, scan,
                    JavaTypeKind.RECORD.equals(kind));
            List<MethodDescriptor> methods = extractMethods(source, describedPackageName, describedQualifiedName, scan);
            List<DescribedType.EnumConstantInfo> enumConstants = extractEnumConstants(source, describedPackageName);
            // When enum constants have constructor arguments, infer the enum constructor
            if (kind.equals(JavaTypeKind.ENUM) && !enumConstants.isEmpty()) {
                List<ConstructorDescriptor> enumConstructors = enumConstructorsFromConstants(enumConstants);
                constructors = ConstructionDiscovery.combine(
                        constructors, enumConstructors, describedQualifiedName,
                        JavaTypeResolutionContext.fromSource(source));
            }
            specs.add(DiscoveredSpec.of(
                    specFile,
                    specQualifiedName,
                    DescribedType.of(
                            DescribedClass.of(describedQualifiedName),
                            kind,
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

        Matcher stateExpectationMatcher = STATE_EXPECTATION_PATTERN.matcher(source);
        while (stateExpectationMatcher.find()) {
            String expectationName = stateExpectationMatcher.group(1);
            if (isKnownMatcherName(expectationName)) {
                continue;
            }
            String argumentSource = stateExpectationMatcher.group(2).trim();
            InferredArguments arguments = inferArgumentTypes(argumentSource, source, stateExpectationMatcher.start(), specMethods, imports, describedPackageName);
            addStateExpectationMethod(expectationName, arguments, discovered);
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
            ConstructionArgumentInference.Arguments arguments = ConstructionArgumentInference.infer(
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

        for (int i = 0; i < scan.stateExpectationStatements.size(); i++) {
            SpecCallScanner.Call call = scan.stateExpectationStatements.get(i);
            InferredArguments arguments = inferArgumentTypesCore(
                    call.argumentTexts, specMethods.get(call.enclosingMethod), imports, describedPackageName);
            addStateExpectationMethod(call.name, arguments, discovered);
        }
    }

    private static void addStateExpectationMethod(
            String expectationName,
            InferredArguments arguments,
            Map<String, MethodDescriptor> discovered
    ) {
        StateExpectationTarget target = stateExpectationTarget(expectationName, arguments);
        if (target == null) {
            return;
        }
        addMethod(discovered, MethodDescriptor.of(target.methodName, target.returnType));
    }

    private static StateExpectationTarget stateExpectationTarget(String expectationName, InferredArguments arguments) {
        if (hasStateExpectationPrefix(expectationName, "shouldNotBe")) {
            if (arguments.size() == 0) {
                return new StateExpectationTarget("is" + expectationName.substring("shouldNotBe".length()), "boolean");
            }
            return null;
        }
        if (hasStateExpectationPrefix(expectationName, "shouldBe")) {
            if (arguments.size() == 0) {
                return new StateExpectationTarget("is" + expectationName.substring("shouldBe".length()), "boolean");
            }
            return null;
        }
        if (hasStateExpectationPrefix(expectationName, "shouldNotHave")) {
            String property = expectationName.substring("shouldNotHave".length());
            if (arguments.size() == 0) {
                return new StateExpectationTarget("has" + property, "boolean");
            }
            if (arguments.size() == 1) {
                return new StateExpectationTarget("get" + property, arguments.types.get(0));
            }
            return null;
        }
        if (hasStateExpectationPrefix(expectationName, "shouldHave")) {
            String property = expectationName.substring("shouldHave".length());
            if (arguments.size() == 0) {
                return new StateExpectationTarget("has" + property, "boolean");
            }
            if (arguments.size() == 1) {
                return new StateExpectationTarget("get" + property, arguments.types.get(0));
            }
        }
        return null;
    }

    private static boolean hasStateExpectationPrefix(String name, String prefix) {
        return name.startsWith(prefix)
                && name.length() > prefix.length()
                && Character.isUpperCase(name.charAt(prefix.length()));
    }

    private static boolean isKnownMatcherName(String name) {
        return "shouldReturn".equals(name)
                || "shouldNotReturn".equals(name)
                || "shouldBe".equals(name)
                || "shouldNotBe".equals(name)
                || "shouldEqual".equals(name)
                || "shouldNotEqual".equals(name)
                || "shouldBeLike".equals(name)
                || "shouldNotBeLike".equals(name)
                || "shouldBeEqualTo".equals(name)
                || "shouldNotBeEqualTo".equals(name)
                || "shouldBeApproximately".equals(name)
                || "shouldReturnApproximately".equals(name)
                || "shouldNotBeApproximately".equals(name)
                || "shouldNotReturnApproximately".equals(name)
                || "shouldHaveType".equals(name)
                || "shouldBeAnInstanceOf".equals(name)
                || "shouldReturnAnInstanceOf".equals(name)
                || "shouldImplement".equals(name)
                || "shouldContain".equals(name)
                || "shouldNotContain".equals(name)
                || "shouldStartWith".equals(name)
                || "shouldNotStartWith".equals(name)
                || "shouldEndWith".equals(name)
                || "shouldNotEndWith".equals(name)
                || "shouldMatchPattern".equals(name)
                || "shouldNotMatchPattern".equals(name)
                || "shouldHaveCount".equals(name)
                || "shouldBeEmpty".equals(name)
                || "shouldNotBeEmpty".equals(name)
                || "shouldHaveKey".equals(name)
                || "shouldNotHaveKey".equals(name)
                || "shouldHaveValue".equals(name)
                || "shouldNotHaveValue".equals(name)
                || "shouldBeAClass".equals(name)
                || "shouldBeAFinalClass".equals(name)
                || "shouldBeAnInterface".equals(name)
                || "shouldBeAnEnum".equals(name)
                || "shouldBeAnAnnotation".equals(name)
                || "shouldBeARecord".equals(name)
                || "shouldBeASealedClass".equals(name)
                || "shouldBeASealedInterface".equals(name)
                || "shouldExtend".equals(name)
                || "shouldPermit".equals(name)
                || "shouldHaveConstant".equals(name);
    }

    private static final class StateExpectationTarget {
        private final String methodName;
        private final String returnType;

        private StateExpectationTarget(String methodName, String returnType) {
            this.methodName = methodName;
            this.returnType = returnType;
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
        List<String> parameterNames = functionalAwareParameterNames(
                expectation.argumentTexts, arguments, parameterNamesFor(expectation.name, arguments.size()));
        String returnType = inferExpectationReturnType(
                expectation, specMethods.get(expectation.enclosingMethod), imports, describedPackageName);
        addMethod(discovered, methodDescriptor(expectation.name, returnType, arguments, parameterNames));
    }

    private static List<String> functionalAwareParameterNames(
            List<String> argumentTexts,
            InferredArguments arguments,
            List<String> fallbackNames
    ) {
        List<String> names = new ArrayList<String>(fallbackNames);
        for (int i = 0; i < argumentTexts.size() && i < arguments.unknowns.size(); i++) {
            if (Boolean.TRUE.equals(arguments.unknowns.get(i)) && isFunctionalExpression(argumentTexts.get(i))) {
                names.set(i, UNRESOLVED_FUNCTIONAL_PARAMETER_PREFIX + i);
            }
        }
        return names;
    }

    private static boolean isFunctionalExpression(String expression) {
        return expression.indexOf("->") >= 0 || expression.indexOf("::") >= 0;
    }

    private static String inferExpectationReturnType(
            SpecCallScanner.Expectation expectation,
            MethodParameterInfo methodInfo,
            Map<String, String> imports,
            String describedPackageName
    ) {
        if (expectation.expectationTexts.size() == 1 && methodInfo != null) {
            String expression = expectation.expectationTexts.get(0).trim();
            int localIndex = methodInfo.names.indexOf(expression);
            if (localIndex >= 0) {
                return methodInfo.types.get(localIndex);
            }
        }
        return inferReturnType(
                expectation.matcherName,
                joinArguments(expectation.expectationTexts),
                imports,
                describedPackageName
        );
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
            ConstructionArgumentInference.Arguments arguments = ConstructionArgumentInference.infer(
                    factoryArguments, source, matcher.start(), specMethods, imports, describedPackageName);
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





    /**
     * Derives an idiomatic parameter name from a qualified constant argument: for
     * {@code Algorithm.EC_P256} the qualifier {@code Algorithm} yields {@code algorithm}.
     * Returns {@code null} when the argument is not a qualified constant reference, so callers
     * keep the positional {@code argN} fallback.
     */













    /**
     * Recognizes a qualified constant reference such as {@code Algorithm.EC_P256} or
     * {@code com.example.Algorithm.EC_P256} (an enum constant or a {@code public static final}
     * field access, with no method call or array access involved) and returns the qualifier as the
     * inferred type, e.g. {@code Algorithm} or {@code com.example.Algorithm}. Returns {@code null}
     * when the expression does not match this shape, so callers fall back to {@code Object}.
     */











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


    static String resolveTypeName(String typeName, Map<String, String> imports, String describedPackageName) {
        return JavaExpressionTypeInference.resolveTypeName(typeName, imports, describedPackageName);
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
