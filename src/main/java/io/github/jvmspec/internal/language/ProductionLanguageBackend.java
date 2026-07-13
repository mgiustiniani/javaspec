package io.github.jvmspec.internal.language;

import io.github.jvmspec.generation.ConstructorPolicy;

import java.io.File;

/** Internal production-language inspection and synchronization seam. */
public interface ProductionLanguageBackend {
    SourceLanguage language();

    BehaviorContract refine(BehaviorContract contract, File sourceRoot);

    SourceSynchronizationPlan planSynchronization(
            String existingSource,
            BehaviorContract contract,
            ConstructorPolicy constructorPolicy
    );
}
