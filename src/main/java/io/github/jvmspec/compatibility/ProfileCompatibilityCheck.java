package io.github.jvmspec.compatibility;

import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.profile.ApiSymbol;
import io.github.jvmspec.profile.FeatureFlag;
import io.github.jvmspec.profile.ProfileCatalog;
import io.github.jvmspec.profile.TargetProfile;

import java.util.List;
import java.util.Objects;

/**
 * Domain service for checking whether type kinds, features, and API symbols fit a target profile.
 */
public final class ProfileCompatibilityCheck {
    private static final ProfileCompatibilityCheck DEFAULT = new ProfileCompatibilityCheck(ProfileCatalog.defaultCatalog());

    private final ProfileCatalog catalog;

    private ProfileCompatibilityCheck(ProfileCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    public static ProfileCompatibilityCheck defaultCheck() {
        return DEFAULT;
    }

    public static ProfileCompatibilityCheck using(ProfileCatalog catalog) {
        return new ProfileCompatibilityCheck(catalog);
    }

    public ProfileCatalog catalog() {
        return catalog;
    }

    public boolean isTypeKindAllowed(TargetProfile targetProfile, JavaTypeKind typeKind) {
        return checkTypeKind(targetProfile, typeKind).isAllowed();
    }

    public CompatibilityResult checkTypeKind(TargetProfile targetProfile, JavaTypeKind typeKind) {
        Objects.requireNonNull(targetProfile, "targetProfile must not be null");
        Objects.requireNonNull(typeKind, "typeKind must not be null");
        TargetProfile requiredProfile = TargetProfile.minimumSupportingJavaVersion(typeKind.minimumJavaVersion());
        String subject = typeKind.displayName();
        if (targetProfile.supportsJavaVersion(typeKind.minimumJavaVersion())) {
            return CompatibilityResult.allowed(
                    targetProfile,
                    requiredProfile,
                    subject,
                    subject + " is allowed for " + targetProfile.displayLabel()
            );
        }
        return CompatibilityResult.denied(
                targetProfile,
                requiredProfile,
                subject,
                subject + " requires " + requiredProfile.displayLabel() + " but target profile is " + targetProfile.displayLabel()
        );
    }

    public boolean isFeatureAllowed(TargetProfile targetProfile, FeatureFlag featureFlag) {
        return checkFeature(targetProfile, featureFlag).isAllowed();
    }

    public CompatibilityResult checkFeature(TargetProfile targetProfile, FeatureFlag featureFlag) {
        Objects.requireNonNull(targetProfile, "targetProfile must not be null");
        Objects.requireNonNull(featureFlag, "featureFlag must not be null");
        String subject = featureFlag.key();
        if (catalog.isFeatureSupported(targetProfile, featureFlag)) {
            return CompatibilityResult.allowed(
                    targetProfile,
                    featureFlag.introducedProfile(),
                    subject,
                    featureFlag.displayName() + " is allowed for " + targetProfile.displayLabel()
            );
        }
        return CompatibilityResult.denied(
                targetProfile,
                featureFlag.introducedProfile(),
                subject,
                featureFlag.displayName() + " requires " + featureFlag.introducedProfile().displayLabel()
                        + " but target profile is " + targetProfile.displayLabel()
        );
    }

    public boolean isApiSymbolAllowed(TargetProfile targetProfile, ApiSymbol symbol) {
        return checkApiSymbol(targetProfile, symbol).isAllowed();
    }

    public CompatibilityResult checkApiSymbol(TargetProfile targetProfile, ApiSymbol symbol) {
        Objects.requireNonNull(targetProfile, "targetProfile must not be null");
        Objects.requireNonNull(symbol, "symbol must not be null");
        String subject = symbol.displayName();
        if (symbol.isAvailableIn(targetProfile)) {
            return CompatibilityResult.allowed(
                    targetProfile,
                    symbol.introducedProfile(),
                    subject,
                    subject + " is available in " + targetProfile.displayLabel()
            );
        }
        return CompatibilityResult.denied(
                targetProfile,
                symbol.introducedProfile(),
                subject,
                subject + " requires " + symbol.introducedProfile().displayLabel()
                        + " but target profile is " + targetProfile.displayLabel()
        );
    }

    public boolean isApiSymbolAllowed(TargetProfile targetProfile, String ownerQualifiedName, String memberName) {
        return checkApiSymbol(targetProfile, ownerQualifiedName, memberName).isAllowed();
    }

    public CompatibilityResult checkApiSymbol(TargetProfile targetProfile, String ownerQualifiedName, String memberName) {
        Objects.requireNonNull(targetProfile, "targetProfile must not be null");
        List<ApiSymbol> symbols = catalog.lookup(ownerQualifiedName, memberName);
        String subject = symbolDisplayName(ownerQualifiedName, memberName);
        if (symbols.isEmpty()) {
            return CompatibilityResult.denied(
                    targetProfile,
                    null,
                    subject,
                    "Unknown API symbol: " + subject
            );
        }
        ApiSymbol earliestRequired = symbols.get(0);
        for (int i = 0; i < symbols.size(); i++) {
            ApiSymbol symbol = symbols.get(i);
            if (symbol.introducedProfile().majorVersion() < earliestRequired.introducedProfile().majorVersion()) {
                earliestRequired = symbol;
            }
            if (symbol.isAvailableIn(targetProfile)) {
                return CompatibilityResult.allowed(
                        targetProfile,
                        symbol.introducedProfile(),
                        subject,
                        subject + " is available in " + targetProfile.displayLabel()
                );
            }
        }
        return CompatibilityResult.denied(
                targetProfile,
                earliestRequired.introducedProfile(),
                subject,
                subject + " requires " + earliestRequired.introducedProfile().displayLabel()
                        + " but target profile is " + targetProfile.displayLabel()
        );
    }

    public boolean isApiOwnerKnown(String ownerQualifiedName) {
        return !catalog.lookupOwner(ownerQualifiedName).isEmpty();
    }

    private static String symbolDisplayName(String ownerQualifiedName, String memberName) {
        Objects.requireNonNull(ownerQualifiedName, "ownerQualifiedName must not be null");
        if (memberName == null) {
            return ownerQualifiedName;
        }
        return ownerQualifiedName + "." + memberName;
    }
}
