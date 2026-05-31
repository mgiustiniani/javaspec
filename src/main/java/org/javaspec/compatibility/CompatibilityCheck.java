package org.javaspec.compatibility;

import org.javaspec.model.JavaTypeKind;
import org.javaspec.profile.ApiSymbol;
import org.javaspec.profile.FeatureFlag;
import org.javaspec.profile.ProfileCatalog;
import org.javaspec.profile.TargetProfile;

import java.util.Objects;

/**
 * Public compatibility-check facade for target profiles.
 */
public final class CompatibilityCheck {
    private static final CompatibilityCheck DEFAULT = new CompatibilityCheck(ProfileCompatibilityCheck.defaultCheck());

    private final ProfileCompatibilityCheck delegate;

    private CompatibilityCheck(ProfileCompatibilityCheck delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    public static CompatibilityCheck defaultCheck() {
        return DEFAULT;
    }

    public static CompatibilityCheck using(ProfileCatalog catalog) {
        return new CompatibilityCheck(ProfileCompatibilityCheck.using(catalog));
    }

    public ProfileCatalog catalog() {
        return delegate.catalog();
    }

    public boolean isTypeKindAllowed(TargetProfile targetProfile, JavaTypeKind typeKind) {
        return delegate.isTypeKindAllowed(targetProfile, typeKind);
    }

    public CompatibilityResult checkTypeKind(TargetProfile targetProfile, JavaTypeKind typeKind) {
        return delegate.checkTypeKind(targetProfile, typeKind);
    }

    public boolean isFeatureAllowed(TargetProfile targetProfile, FeatureFlag featureFlag) {
        return delegate.isFeatureAllowed(targetProfile, featureFlag);
    }

    public CompatibilityResult checkFeature(TargetProfile targetProfile, FeatureFlag featureFlag) {
        return delegate.checkFeature(targetProfile, featureFlag);
    }

    public boolean isApiSymbolAllowed(TargetProfile targetProfile, ApiSymbol symbol) {
        return delegate.isApiSymbolAllowed(targetProfile, symbol);
    }

    public CompatibilityResult checkApiSymbol(TargetProfile targetProfile, ApiSymbol symbol) {
        return delegate.checkApiSymbol(targetProfile, symbol);
    }

    public boolean isApiSymbolAllowed(TargetProfile targetProfile, String ownerQualifiedName, String memberName) {
        return delegate.isApiSymbolAllowed(targetProfile, ownerQualifiedName, memberName);
    }

    public CompatibilityResult checkApiSymbol(TargetProfile targetProfile, String ownerQualifiedName, String memberName) {
        return delegate.checkApiSymbol(targetProfile, ownerQualifiedName, memberName);
    }

    public boolean isApiOwnerKnown(String ownerQualifiedName) {
        return delegate.isApiOwnerKnown(ownerQualifiedName);
    }
}
