package io.github.jvmspec.discovery;

import io.github.jvmspec.model.MethodDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.jvmspec.discovery.JavaExpressionTypeInference.InferredArguments;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.MethodParameterInfo;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.decapitalize;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.importsBySimpleName;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferArgumentTypes;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferArgumentTypesCore;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferReturnType;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.isJavaIdentifier;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.isStringLiteral;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.methodInfoFromScan;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.parameterNamesFor;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.parseMethods;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.splitArguments;

/** Discovers production callable contracts from Java specification behavior. */
final class MethodDiscovery {
    private static final String UNRESOLVED_FUNCTIONAL_PARAMETER_PREFIX =
            "__javaspecFunctionalArg";
    private static final Pattern FACTORY_CONSTRUCTION_PATTERN = Pattern.compile(
            "beConstructed(?:ThroughNamed|Through|Named)\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern PROXY_EXPECTATION_PATTERN = Pattern.compile(
            "\\b([a-z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*\\.\\s*"
                    + "(shouldReturn|shouldNotReturn|shouldBe|shouldNotBe|shouldEqual|shouldNotEqual|shouldBeLike|shouldNotBeLike|shouldBeEqualTo|shouldNotBeEqualTo|shouldBeApproximately|shouldReturnApproximately|shouldNotBeApproximately|shouldNotReturnApproximately|shouldHaveType|shouldBeAnInstanceOf|shouldReturnAnInstanceOf|shouldImplement|shouldContain|shouldNotContain|shouldStartWith|shouldNotStartWith|shouldEndWith|shouldNotEndWith|shouldMatchPattern|shouldNotMatchPattern|shouldHaveCount|shouldBeEmpty|shouldNotBeEmpty|shouldHaveKey|shouldNotHaveKey|shouldHaveValue|shouldNotHaveValue)"
                    + "\\s*\\(([^;{}]*)\\)", Pattern.DOTALL);
    private static final Pattern SHOULD_THROW_DURING_PATTERN = Pattern.compile(
            "shouldThrow\\s*\\([^;{}]*\\)\\s*\\.\\s*during([A-Z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)",
            Pattern.DOTALL);
    private static final Pattern SUBJECT_VOID_CALL_PATTERN = Pattern.compile(
            "subject\\s*\\(\\s*\\)\\s*\\.\\s*([a-z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*;",
            Pattern.DOTALL);
    private static final Pattern PLAIN_SETTER_CALL_PATTERN = Pattern.compile(
            "(?m)^\\s*(set[A-Z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*;");
    private static final Pattern STATE_EXPECTATION_PATTERN = Pattern.compile(
            "(?m)^\\s*((?:shouldBe|shouldNotBe|shouldHave|shouldNotHave)[A-Z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*;");
    private static final Pattern MATCH_SUBJECT_PROXY_PATTERN = Pattern.compile(
            "match\\s*\\(\\s*subject\\s*\\(\\s*\\)\\s*\\.\\s*([a-z][A-Za-z0-9_$]*)\\s*\\(([^;{}]*)\\)\\s*\\)\\s*\\.\\s*"
                    + "(shouldReturn|shouldNotReturn|shouldBe|shouldNotBe|shouldEqual|shouldNotEqual|shouldBeLike|shouldNotBeLike|shouldBeEqualTo|shouldNotBeEqualTo|shouldBeApproximately|shouldReturnApproximately|shouldNotBeApproximately|shouldNotReturnApproximately|shouldHaveType|shouldBeAnInstanceOf|shouldReturnAnInstanceOf|shouldImplement|shouldContain|shouldNotContain|shouldStartWith|shouldNotStartWith|shouldEndWith|shouldNotEndWith|shouldMatchPattern|shouldNotMatchPattern|shouldHaveCount|shouldBeEmpty|shouldNotBeEmpty|shouldHaveKey|shouldNotHaveKey|shouldHaveValue|shouldNotHaveValue)"
                    + "\\s*\\(([^;{}]*)\\)", Pattern.DOTALL);

    private MethodDiscovery() {
    }

    static List<MethodDescriptor> discover(String source, String describedPackageName, String describedQualifiedName, SpecCallScanner.ScanResult scan) {
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
            if (SpecCallScanner.isFrameworkMethodName(methodName)) {
                continue;
            }
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
        return methodName.startsWith("should")
                || methodName.startsWith("beConstructed")
                || SpecCallScanner.isFrameworkMethodName(methodName);
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
}
