package io.github.jvmspec.internal.language;

import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.model.MethodDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Internal language-neutral handoff between specification discovery and production planning.
 * The frozen public descriptor is retained as a Java compatibility bridge while portable shape,
 * type, construction, relationship, and callable evidence is exposed independently.
 */
public final class BehaviorContract {
    private final DescribedType describedType;
    private final String subjectQualifiedName;
    private final BehaviorTypeShape subjectShape;
    private final List<BehaviorTypeRef> extendedTypes;
    private final List<BehaviorTypeRef> implementedTypes;
    private final List<BehaviorTypeRef> permittedTypes;
    private final List<ConstructionContract> constructions;
    private final List<CallableContract> callables;

    private BehaviorContract(DescribedType describedType) {
        this.describedType = Objects.requireNonNull(
                describedType, "describedType must not be null");
        this.subjectQualifiedName = describedType.qualifiedName();
        this.subjectShape = shapeOf(describedType.kind());
        String defaultPackage = describedType.describedClass().packageName();
        this.extendedTypes = typeRefs(describedType.extendedTypeNames(), defaultPackage);
        this.implementedTypes = typeRefs(describedType.implementedTypeNames(), defaultPackage);
        this.permittedTypes = typeRefs(describedType.permittedTypeNames(), defaultPackage);
        this.constructions = constructionsOf(describedType, defaultPackage);
        this.callables = callablesOf(describedType, defaultPackage);
    }

    public static BehaviorContract from(DescribedType describedType) {
        return new BehaviorContract(describedType);
    }

    /** Java compatibility bridge used by the sole pre-1.0 production backend. */
    public DescribedType describedType() {
        return describedType;
    }

    public String subjectQualifiedName() {
        return subjectQualifiedName;
    }

    public BehaviorTypeShape subjectShape() {
        return subjectShape;
    }

    public List<BehaviorTypeRef> extendedTypes() {
        return extendedTypes;
    }

    public List<BehaviorTypeRef> implementedTypes() {
        return implementedTypes;
    }

    public List<BehaviorTypeRef> permittedTypes() {
        return permittedTypes;
    }

    public List<ConstructionContract> constructions() {
        return constructions;
    }

    public List<CallableContract> callables() {
        return callables;
    }

    public boolean portableEquivalent(BehaviorContract other) {
        return other != null
                && subjectQualifiedName.equals(other.subjectQualifiedName)
                && subjectShape.equals(other.subjectShape)
                && extendedTypes.equals(other.extendedTypes)
                && implementedTypes.equals(other.implementedTypes)
                && permittedTypes.equals(other.permittedTypes)
                && constructions.equals(other.constructions)
                && callables.equals(other.callables);
    }

    public boolean isPortable() {
        return portableTypes(extendedTypes)
                && portableTypes(implementedTypes)
                && portableTypes(permittedTypes)
                && portableConstructions()
                && portableCallables();
    }

    private boolean portableConstructions() {
        for (int i = 0; i < constructions.size(); i++) {
            List<BehaviorParameter> parameters = constructions.get(i).parameters();
            for (int j = 0; j < parameters.size(); j++) {
                if (!parameters.get(j).type().isPortable()) return false;
            }
        }
        return true;
    }

    private boolean portableCallables() {
        for (int i = 0; i < callables.size(); i++) {
            CallableContract callable = callables.get(i);
            if (!callable.returnType().isPortable()) return false;
            for (int j = 0; j < callable.parameters().size(); j++) {
                if (!callable.parameters().get(j).type().isPortable()) return false;
            }
        }
        return true;
    }

    private static boolean portableTypes(List<BehaviorTypeRef> types) {
        for (int i = 0; i < types.size(); i++) {
            if (!types.get(i).isPortable()) return false;
        }
        return true;
    }

    private static List<ConstructionContract> constructionsOf(
            DescribedType describedType,
            String defaultPackage
    ) {
        List<ConstructionContract> result = new ArrayList<ConstructionContract>();
        for (int i = 0; i < describedType.constructors().size(); i++) {
            ConstructorDescriptor descriptor = describedType.constructors().get(i);
            List<BehaviorParameter> parameters = new ArrayList<BehaviorParameter>();
            for (int j = 0; j < descriptor.parameterTypes().size(); j++) {
                parameters.add(BehaviorParameter.of(
                        descriptor.parameterNames().get(j),
                        BehaviorTypeRef.fromJava(
                                descriptor.parameterTypes().get(j), defaultPackage),
                        false));
            }
            result.add(ConstructionContract.of(parameters));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<CallableContract> callablesOf(
            DescribedType describedType,
            String defaultPackage
    ) {
        List<CallableContract> result = new ArrayList<CallableContract>();
        for (int i = 0; i < describedType.methods().size(); i++) {
            MethodDescriptor descriptor = describedType.methods().get(i);
            List<BehaviorParameter> parameters = new ArrayList<BehaviorParameter>();
            for (int j = 0; j < descriptor.parameterTypes().size(); j++) {
                parameters.add(BehaviorParameter.of(
                        descriptor.parameterNames().get(j),
                        BehaviorTypeRef.fromJava(
                                descriptor.parameterTypes().get(j), defaultPackage),
                        descriptor.isParameterTypeUnknown(j)));
            }
            result.add(CallableContract.of(
                    descriptor.methodName(),
                    BehaviorTypeRef.fromJava(descriptor.returnType(), defaultPackage),
                    parameters,
                    descriptor.isStatic()
                            ? CallableContract.InvocationKind.TYPE
                            : CallableContract.InvocationKind.INSTANCE));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<BehaviorTypeRef> typeRefs(
            List<String> sourceTypes,
            String defaultPackage
    ) {
        List<BehaviorTypeRef> result = new ArrayList<BehaviorTypeRef>();
        for (int i = 0; i < sourceTypes.size(); i++) {
            result.add(BehaviorTypeRef.fromJava(sourceTypes.get(i), defaultPackage));
        }
        return Collections.unmodifiableList(result);
    }

    private static BehaviorTypeShape shapeOf(JavaTypeKind kind) {
        if (JavaTypeKind.FINAL_CLASS.equals(kind)) {
            return BehaviorTypeShape.FINAL_REFERENCE_CLASS;
        }
        if (JavaTypeKind.INTERFACE.equals(kind)) return BehaviorTypeShape.INTERFACE;
        if (JavaTypeKind.ENUM.equals(kind)) return BehaviorTypeShape.ENUMERATION;
        if (JavaTypeKind.ANNOTATION.equals(kind)) return BehaviorTypeShape.METADATA_TYPE;
        if (JavaTypeKind.RECORD.equals(kind)) return BehaviorTypeShape.PRODUCT_TYPE;
        if (JavaTypeKind.SEALED_CLASS.equals(kind)) {
            return BehaviorTypeShape.SEALED_REFERENCE_CLASS;
        }
        if (JavaTypeKind.SEALED_INTERFACE.equals(kind)) {
            return BehaviorTypeShape.SEALED_INTERFACE;
        }
        return BehaviorTypeShape.REFERENCE_CLASS;
    }
}
