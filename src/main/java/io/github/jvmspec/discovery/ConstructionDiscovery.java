package io.github.jvmspec.discovery;

import io.github.jvmspec.internal.type.ConstructorDiscoveryException;
import io.github.jvmspec.internal.type.ConstructorSignature;
import io.github.jvmspec.internal.type.JavaTypeRef;
import io.github.jvmspec.internal.type.JavaTypeResolutionContext;
import io.github.jvmspec.model.ConstructorDescriptor;

import java.util.ArrayList;
import java.util.List;

/** Package-private constructor identity and conflict handling used by the public discovery facade. */
final class ConstructionDiscovery {
    private ConstructionDiscovery() {
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
