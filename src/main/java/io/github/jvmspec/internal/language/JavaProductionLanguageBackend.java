package io.github.jvmspec.internal.language;

import io.github.jvmspec.discovery.ProductionSignatureReader;
import io.github.jvmspec.generation.ClassConstructorUpdater;
import io.github.jvmspec.generation.ClassMethodUpdater;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.model.DescribedType;

import java.io.File;

/** Java production backend preserving the existing source-planning order. */
public final class JavaProductionLanguageBackend implements ProductionLanguageBackend {
    public SourceLanguage language() {
        return SourceLanguage.JAVA;
    }

    public BehaviorContract refine(BehaviorContract contract, File sourceRoot) {
        return BehaviorContract.from(ProductionSignatureReader.refine(
                contract.describedType(), sourceRoot));
    }

    public SourceSynchronizationPlan planSynchronization(
            String existingSource,
            BehaviorContract contract,
            ConstructorPolicy constructorPolicy
    ) {
        DescribedType describedType = contract.describedType();
        String proposedSource = existingSource;
        boolean constructorChange = false;
        boolean methodChange = false;

        if (describedType.hasConstructors()) {
            String constructorSource = ClassConstructorUpdater.updateSource(
                    proposedSource, describedType, constructorPolicy);
            constructorChange = !proposedSource.equals(constructorSource);
            proposedSource = constructorSource;
        }
        if (describedType.hasMethods()) {
            String methodSource = ClassMethodUpdater.updateSource(proposedSource, describedType);
            methodChange = !proposedSource.equals(methodSource);
            proposedSource = methodSource;
        }
        return SourceSynchronizationPlan.of(
                existingSource, proposedSource, constructorChange, methodChange);
    }
}
