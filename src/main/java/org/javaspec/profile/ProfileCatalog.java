package org.javaspec.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic immutable catalog of target profiles, API symbols, and feature flags.
 */
public final class ProfileCatalog {
    private static final ProfileCatalog DEFAULT = new ProfileCatalog(DefaultProfileCatalogSymbols.create());

    private final List<TargetProfile> profiles;
    private final Map<String, TargetProfile> profilesByKey;
    private final List<ApiSymbol> symbols;
    private final Map<TargetProfile, List<ApiSymbol>> symbolsIntroducedByProfile;
    private final Map<TargetProfile, List<ApiSymbol>> symbolsAvailableByProfile;
    private final Map<TargetProfile, List<FeatureFlag>> featuresAvailableByProfile;
    private final Map<TargetProfile, Map<FeatureFlag, Boolean>> featureSupportByProfile;
    private final Map<ApiSymbolKey, List<ApiSymbol>> symbolsByOwnerAndMember;
    private final Map<String, List<ApiSymbol>> symbolsByOwner;

    private ProfileCatalog(List<ApiSymbol> symbols) {
        this.profiles = TargetProfile.orderedProfiles();
        this.profilesByKey = TargetProfile.profilesByKey();
        this.symbols = validatedSymbols(symbols);
        this.symbolsIntroducedByProfile = buildSymbolsIntroducedByProfile(this.symbols, this.profiles);
        this.symbolsAvailableByProfile = buildSymbolsAvailableByProfile(this.symbols, this.profiles);
        this.featuresAvailableByProfile = buildFeaturesAvailableByProfile(this.profiles);
        this.featureSupportByProfile = buildFeatureSupportByProfile(this.profiles);
        this.symbolsByOwnerAndMember = buildSymbolsByOwnerAndMember(this.symbols);
        this.symbolsByOwner = buildSymbolsByOwner(this.symbols);
    }

    public static ProfileCatalog defaultCatalog() {
        return DEFAULT;
    }

    public static ProfileCatalog of(List<ApiSymbol> symbols) {
        return new ProfileCatalog(symbols);
    }

    public List<TargetProfile> profiles() {
        return profiles;
    }

    public Map<String, TargetProfile> profilesByKey() {
        return profilesByKey;
    }

    public List<ApiSymbol> symbols() {
        return symbols;
    }

    public List<ApiSymbol> symbolsIntroducedIn(TargetProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        List<ApiSymbol> result = symbolsIntroducedByProfile.get(profile);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    public List<ApiSymbol> symbolsAvailableIn(TargetProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        List<ApiSymbol> result = symbolsAvailableByProfile.get(profile);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    public Map<TargetProfile, List<ApiSymbol>> symbolsIntroducedByProfile() {
        return symbolsIntroducedByProfile;
    }

    public Map<TargetProfile, List<ApiSymbol>> symbolsAvailableByProfile() {
        return symbolsAvailableByProfile;
    }

    public List<FeatureFlag> featuresAvailableIn(TargetProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        List<FeatureFlag> result = featuresAvailableByProfile.get(profile);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    public Map<FeatureFlag, Boolean> featureSupport(TargetProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        Map<FeatureFlag, Boolean> result = featureSupportByProfile.get(profile);
        if (result == null) {
            return Collections.emptyMap();
        }
        return result;
    }

    public boolean isFeatureSupported(TargetProfile profile, FeatureFlag featureFlag) {
        Objects.requireNonNull(featureFlag, "featureFlag must not be null");
        return Boolean.TRUE.equals(featureSupport(profile).get(featureFlag));
    }

    public Map<ApiSymbolKey, List<ApiSymbol>> symbolsByOwnerAndMember() {
        return symbolsByOwnerAndMember;
    }

    public Map<String, List<ApiSymbol>> symbolsByOwner() {
        return symbolsByOwner;
    }

    public List<ApiSymbol> lookup(String ownerQualifiedName) {
        return lookup(ownerQualifiedName, null);
    }

    public List<ApiSymbol> lookup(String ownerQualifiedName, String memberName) {
        ApiSymbolKey key = ApiSymbolKey.of(ownerQualifiedName, memberName);
        List<ApiSymbol> result = symbolsByOwnerAndMember.get(key);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    public List<ApiSymbol> lookupOwner(String ownerQualifiedName) {
        String owner = validateRequired(ownerQualifiedName, "ownerQualifiedName");
        List<ApiSymbol> result = symbolsByOwner.get(owner);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    private static List<ApiSymbol> validatedSymbols(List<ApiSymbol> symbols) {
        Objects.requireNonNull(symbols, "symbols must not be null");
        List<ApiSymbol> copy = new ArrayList<ApiSymbol>(symbols);
        for (int i = 0; i < copy.size(); i++) {
            Objects.requireNonNull(copy.get(i), "symbols[" + i + "] must not be null");
        }
        return Collections.unmodifiableList(copy);
    }

    private static Map<TargetProfile, List<ApiSymbol>> buildSymbolsIntroducedByProfile(List<ApiSymbol> symbols, List<TargetProfile> profiles) {
        Map<TargetProfile, List<ApiSymbol>> mutable = newProfileSymbolListMap(profiles);
        for (int i = 0; i < symbols.size(); i++) {
            ApiSymbol symbol = symbols.get(i);
            mutable.get(symbol.introducedProfile()).add(symbol);
        }
        return immutableProfileSymbolListMap(mutable, profiles);
    }

    private static Map<TargetProfile, List<ApiSymbol>> buildSymbolsAvailableByProfile(List<ApiSymbol> symbols, List<TargetProfile> profiles) {
        Map<TargetProfile, List<ApiSymbol>> mutable = newProfileSymbolListMap(profiles);
        for (int profileIndex = 0; profileIndex < profiles.size(); profileIndex++) {
            TargetProfile profile = profiles.get(profileIndex);
            List<ApiSymbol> available = mutable.get(profile);
            for (int symbolIndex = 0; symbolIndex < symbols.size(); symbolIndex++) {
                ApiSymbol symbol = symbols.get(symbolIndex);
                if (symbol.isAvailableIn(profile)) {
                    available.add(symbol);
                }
            }
        }
        return immutableProfileSymbolListMap(mutable, profiles);
    }

    private static Map<TargetProfile, List<FeatureFlag>> buildFeaturesAvailableByProfile(List<TargetProfile> profiles) {
        Map<TargetProfile, List<FeatureFlag>> result = new LinkedHashMap<TargetProfile, List<FeatureFlag>>();
        List<FeatureFlag> flags = FeatureFlag.orderedFlags();
        for (int profileIndex = 0; profileIndex < profiles.size(); profileIndex++) {
            TargetProfile profile = profiles.get(profileIndex);
            List<FeatureFlag> available = new ArrayList<FeatureFlag>();
            for (int flagIndex = 0; flagIndex < flags.size(); flagIndex++) {
                FeatureFlag flag = flags.get(flagIndex);
                if (flag.isSupportedBy(profile)) {
                    available.add(flag);
                }
            }
            result.put(profile, Collections.unmodifiableList(available));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<TargetProfile, Map<FeatureFlag, Boolean>> buildFeatureSupportByProfile(List<TargetProfile> profiles) {
        Map<TargetProfile, Map<FeatureFlag, Boolean>> result = new LinkedHashMap<TargetProfile, Map<FeatureFlag, Boolean>>();
        List<FeatureFlag> flags = FeatureFlag.orderedFlags();
        for (int profileIndex = 0; profileIndex < profiles.size(); profileIndex++) {
            TargetProfile profile = profiles.get(profileIndex);
            Map<FeatureFlag, Boolean> support = new LinkedHashMap<FeatureFlag, Boolean>();
            for (int flagIndex = 0; flagIndex < flags.size(); flagIndex++) {
                FeatureFlag flag = flags.get(flagIndex);
                support.put(flag, Boolean.valueOf(flag.isSupportedBy(profile)));
            }
            result.put(profile, Collections.unmodifiableMap(support));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<ApiSymbolKey, List<ApiSymbol>> buildSymbolsByOwnerAndMember(List<ApiSymbol> symbols) {
        Map<ApiSymbolKey, List<ApiSymbol>> mutable = new LinkedHashMap<ApiSymbolKey, List<ApiSymbol>>();
        for (int i = 0; i < symbols.size(); i++) {
            ApiSymbol symbol = symbols.get(i);
            ApiSymbolKey key = symbol.key();
            List<ApiSymbol> bucket = mutable.get(key);
            if (bucket == null) {
                bucket = new ArrayList<ApiSymbol>();
                mutable.put(key, bucket);
            }
            bucket.add(symbol);
        }
        Map<ApiSymbolKey, List<ApiSymbol>> result = new LinkedHashMap<ApiSymbolKey, List<ApiSymbol>>();
        for (Map.Entry<ApiSymbolKey, List<ApiSymbol>> entry : mutable.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<ApiSymbol>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, List<ApiSymbol>> buildSymbolsByOwner(List<ApiSymbol> symbols) {
        Map<String, List<ApiSymbol>> mutable = new LinkedHashMap<String, List<ApiSymbol>>();
        for (int i = 0; i < symbols.size(); i++) {
            ApiSymbol symbol = symbols.get(i);
            String owner = symbol.ownerQualifiedName();
            List<ApiSymbol> bucket = mutable.get(owner);
            if (bucket == null) {
                bucket = new ArrayList<ApiSymbol>();
                mutable.put(owner, bucket);
            }
            bucket.add(symbol);
        }
        Map<String, List<ApiSymbol>> result = new LinkedHashMap<String, List<ApiSymbol>>();
        for (Map.Entry<String, List<ApiSymbol>> entry : mutable.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<ApiSymbol>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<TargetProfile, List<ApiSymbol>> newProfileSymbolListMap(List<TargetProfile> profiles) {
        Map<TargetProfile, List<ApiSymbol>> result = new LinkedHashMap<TargetProfile, List<ApiSymbol>>();
        for (int i = 0; i < profiles.size(); i++) {
            result.put(profiles.get(i), new ArrayList<ApiSymbol>());
        }
        return result;
    }

    private static Map<TargetProfile, List<ApiSymbol>> immutableProfileSymbolListMap(
            Map<TargetProfile, List<ApiSymbol>> mutable,
            List<TargetProfile> profiles
    ) {
        Map<TargetProfile, List<ApiSymbol>> result = new LinkedHashMap<TargetProfile, List<ApiSymbol>>();
        for (int i = 0; i < profiles.size(); i++) {
            TargetProfile profile = profiles.get(i);
            List<ApiSymbol> symbols = mutable.get(profile);
            result.put(profile, Collections.unmodifiableList(new ArrayList<ApiSymbol>(symbols)));
        }
        return Collections.unmodifiableMap(result);
    }

    private static String validateRequired(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        if (value.trim().length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not contain leading or trailing whitespace: " + value);
        }
        return value;
    }
}
