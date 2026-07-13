package io.github.jvmspec.discovery;

import io.github.jvmspec.internal.type.ConstructorDiscoveryException;
import io.github.jvmspec.internal.type.ConstructorSignature;
import io.github.jvmspec.internal.type.JavaTypeRef;
import io.github.jvmspec.internal.type.JavaTypeResolutionContext;
import io.github.jvmspec.model.ConstructorDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.jvmspec.discovery.JavaExpressionTypeInference.importsBySimpleName;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.inferredExpressionType;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.isLegalJavaIdentifier;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.methodInfoFromScan;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.parseMethods;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.sameInferredType;
import static io.github.jvmspec.discovery.JavaExpressionTypeInference.splitArguments;

/** Constructor observation, identity, accessor evidence, and conflict handling. */
final class ConstructionDiscovery {
    private static final Pattern BE_CONSTRUCTED_WITH_PATTERN = Pattern.compile(
            "beConstructedWith\\s*\\(([^)]*)\\)", Pattern.DOTALL);

    private ConstructionDiscovery() {
    }

    static List<ConstructorDescriptor> discover(
            String source,
            String describedPackageName,
            String describedQualifiedName,
            SpecCallScanner.ScanResult scan,
            boolean recordSubject
    ) {
        Map<String, String> imports = importsBySimpleName(source);
        JavaTypeResolutionContext typeResolution = JavaTypeResolutionContext.fromSource(source);
        List<ConstructorDescriptor> constructors = new ArrayList<ConstructorDescriptor>();

        if (scan != null) {
            Map<String, JavaExpressionTypeInference.MethodParameterInfo> methods =
                    methodInfoFromScan(scan, imports, describedPackageName);
            for (int i = 0; i < scan.constructionCalls.size(); i++) {
                SpecCallScanner.Call call = scan.constructionCalls.get(i);
                JavaExpressionTypeInference.MethodParameterInfo methodInfo =
                        methods.get(call.enclosingMethod);
                ConstructionArgumentInference.Arguments arguments = ConstructionArgumentInference.infer(
                        call.argumentTexts, methodInfo, imports, describedPackageName);
                if (recordSubject) {
                    arguments = applyAccessorNamingEvidence(
                            arguments, call, scan, methodInfo, imports, describedPackageName);
                }
                ConstructorDescriptor candidate = ConstructorDescriptor.of(
                        arguments.parameterTypes, arguments.parameterNames, "");
                addBySignature(constructors, candidate, describedQualifiedName, typeResolution);
            }
            return constructors;
        }

        Map<String, JavaExpressionTypeInference.MethodParameterInfo> methods =
                parseMethods(source, imports, describedPackageName);
        Matcher matcher = BE_CONSTRUCTED_WITH_PATTERN.matcher(source);
        while (matcher.find()) {
            List<String> argumentTexts = splitArguments(matcher.group(1).trim());
            ConstructionArgumentInference.Arguments arguments = ConstructionArgumentInference.infer(
                    argumentTexts, source, matcher.start(), methods, imports, describedPackageName);
            ConstructorDescriptor candidate = ConstructorDescriptor.of(
                    arguments.parameterTypes, arguments.parameterNames, "");
            addBySignature(constructors, candidate, describedQualifiedName, typeResolution);
        }
        return constructors;
    }

    static void addBySignature(
            List<ConstructorDescriptor> constructors,
            ConstructorDescriptor candidate,
            String describedQualifiedName,
            JavaTypeResolutionContext typeResolution
    ) {
        ConstructorSignature candidateSignature = ConstructorSignature.of(
                describedQualifiedName,
                resolvedErasedTypes(candidate.parameterTypes(), typeResolution));
        for (int i = 0; i < constructors.size(); i++) {
            ConstructorDescriptor existing = constructors.get(i);
            if (!candidateSignature.equals(ConstructorSignature.of(
                    describedQualifiedName,
                    resolvedErasedTypes(existing.parameterTypes(), typeResolution)))) {
                continue;
            }
            if (sameStructuredParameterTypes(
                    existing.parameterTypes(), candidate.parameterTypes())) {
                return;
            }
            throw new ConstructorDiscoveryException(
                    "CONFLICTING_CONSTRUCTOR_SIGNATURE: subject " + describedQualifiedName
                            + ", erased signature " + candidateSignature
                            + ", incompatible parameter types " + existing.parameterTypes()
                            + " and " + candidate.parameterTypes() + "."
            );
        }
        constructors.add(candidate);
    }

    static List<ConstructorDescriptor> combine(
            List<ConstructorDescriptor> existing,
            List<ConstructorDescriptor> additional,
            String describedQualifiedName,
            JavaTypeResolutionContext typeResolution
    ) {
        List<ConstructorDescriptor> result = new ArrayList<ConstructorDescriptor>(existing);
        for (int i = 0; i < additional.size(); i++) {
            addBySignature(result, additional.get(i), describedQualifiedName, typeResolution);
        }
        return result;
    }

    private static ConstructionArgumentInference.Arguments applyAccessorNamingEvidence(
            ConstructionArgumentInference.Arguments arguments,
            SpecCallScanner.Call constructionCall,
            SpecCallScanner.ScanResult scan,
            JavaExpressionTypeInference.MethodParameterInfo methodInfo,
            Map<String, String> imports,
            String describedPackageName
    ) {
        List<String> names = new ArrayList<String>(arguments.parameterNames);
        for (int i = 0; i < constructionCall.argumentTexts.size(); i++) {
            String argumentText = constructionCall.argumentTexts.get(i).trim();
            String accessorName = uniqueAccessorForExampleValue(
                    argumentText,
                    arguments.parameterTypes.get(i),
                    arguments.parameterTypes,
                    constructionCall.enclosingMethod,
                    scan,
                    methodInfo,
                    imports,
                    describedPackageName
            );
            if (accessorName != null) {
                names.set(i, accessorName);
            }
        }
        return new ConstructionArgumentInference.Arguments(arguments.parameterTypes, names);
    }

    private static String uniqueAccessorForExampleValue(
            String argumentText,
            String argumentType,
            List<String> constructionTypes,
            String enclosingMethod,
            SpecCallScanner.ScanResult scan,
            JavaExpressionTypeInference.MethodParameterInfo methodInfo,
            Map<String, String> imports,
            String describedPackageName
    ) {
        List<SpecCallScanner.Expectation> expectations = new ArrayList<SpecCallScanner.Expectation>();
        expectations.addAll(scan.proxyExpectations);
        expectations.addAll(scan.matchSubjectExpectations);
        String exactCandidate = null;
        String typedCandidate = null;
        for (int i = 0; i < expectations.size(); i++) {
            SpecCallScanner.Expectation expectation = expectations.get(i);
            if (!Objects.equals(enclosingMethod, expectation.enclosingMethod)
                    || !expectation.argumentTexts.isEmpty()
                    || !"shouldReturn".equals(expectation.matcherName)
                    || expectation.expectationTexts.size() != 1
                    || !isLegalJavaIdentifier(expectation.name)) {
                continue;
            }
            String expectationText = expectation.expectationTexts.get(0).trim();
            String expectationType = inferredExpressionType(
                    expectationText, methodInfo, imports, describedPackageName);
            if (!sameInferredType(argumentType, expectationType)) {
                continue;
            }
            if (typedCandidate != null && !typedCandidate.equals(expectation.name)) {
                typedCandidate = "";
            } else if (typedCandidate == null) {
                typedCandidate = expectation.name;
            }
            if (!argumentText.equals(expectationText)) {
                continue;
            }
            if (exactCandidate != null && !exactCandidate.equals(expectation.name)) {
                return null;
            }
            exactCandidate = expectation.name;
        }
        if (exactCandidate != null) {
            return exactCandidate;
        }
        int compatibleConstructionSlots = 0;
        for (int i = 0; i < constructionTypes.size(); i++) {
            if (sameInferredType(argumentType, constructionTypes.get(i))) {
                compatibleConstructionSlots++;
            }
        }
        return compatibleConstructionSlots == 1 && typedCandidate != null && typedCandidate.length() > 0
                ? typedCandidate
                : null;
    }

    private static List<String> resolvedErasedTypes(
            List<String> parameterTypes,
            JavaTypeResolutionContext typeResolution
    ) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < parameterTypes.size(); i++) {
            result.add(typeResolution.resolveErased(parameterTypes.get(i)));
        }
        return result;
    }

    private static boolean sameStructuredParameterTypes(List<String> left, List<String> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            try {
                JavaTypeRef leftType = JavaTypeRef.parseCanonical(left.get(i));
                JavaTypeRef rightType = JavaTypeRef.parseCanonical(right.get(i));
                if (!leftType.structurallyEquivalent(rightType)) return false;
            } catch (IllegalArgumentException ex) {
                if (!left.get(i).replace("...", "[]").equals(
                        right.get(i).replace("...", "[]"))) return false;
            }
        }
        return true;
    }
}
