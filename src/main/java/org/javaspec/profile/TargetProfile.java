package org.javaspec.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable Java LTS target profile metadata.
 */
public enum TargetProfile {
    JAVA8("java8", "Java 8", 8),
    JAVA11("java11", "Java 11", 11),
    JAVA17("java17", "Java 17", 17),
    JAVA21("java21", "Java 21", 21),
    JAVA25("java25", "Java 25", 25);

    private static final List<TargetProfile> ORDERED_PROFILES = createOrderedProfiles();
    private static final Map<String, TargetProfile> PROFILES_BY_KEY = createProfilesByKey();

    private final String key;
    private final String displayLabel;
    private final int majorVersion;

    TargetProfile(String key, String displayLabel, int majorVersion) {
        this.key = key;
        this.displayLabel = displayLabel;
        this.majorVersion = majorVersion;
    }

    public String key() {
        return key;
    }

    public String displayLabel() {
        return displayLabel;
    }

    public int majorVersion() {
        return majorVersion;
    }

    public boolean supports(TargetProfile introducedProfile) {
        Objects.requireNonNull(introducedProfile, "introducedProfile must not be null");
        return majorVersion >= introducedProfile.majorVersion;
    }

    public boolean supportsJavaVersion(int minimumJavaVersion) {
        if (minimumJavaVersion < 1) {
            throw new IllegalArgumentException("minimumJavaVersion must be positive: " + minimumJavaVersion);
        }
        return majorVersion >= minimumJavaVersion;
    }

    public boolean isBefore(TargetProfile other) {
        Objects.requireNonNull(other, "other must not be null");
        return majorVersion < other.majorVersion;
    }

    public boolean isAfter(TargetProfile other) {
        Objects.requireNonNull(other, "other must not be null");
        return majorVersion > other.majorVersion;
    }

    public static List<TargetProfile> orderedProfiles() {
        return ORDERED_PROFILES;
    }

    public static Map<String, TargetProfile> profilesByKey() {
        return PROFILES_BY_KEY;
    }

    public static TargetProfile fromKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        if (key.length() == 0) {
            throw new IllegalArgumentException("Profile key must not be empty");
        }
        if (!key.equals(key.trim())) {
            throw new IllegalArgumentException("Profile key must not contain leading or trailing whitespace: " + key);
        }
        TargetProfile profile = PROFILES_BY_KEY.get(key);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown target profile key: " + key);
        }
        return profile;
    }

    public static TargetProfile parse(String value) {
        Objects.requireNonNull(value, "value must not be null");
        String normalized = normalizedProfileKey(value);
        if (normalized.length() == 0) {
            throw new IllegalArgumentException("Profile value must not be empty");
        }
        TargetProfile profile = PROFILES_BY_KEY.get(normalized);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown target profile: " + value);
        }
        return profile;
    }

    public static boolean isKnownKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return PROFILES_BY_KEY.containsKey(key);
    }

    public static TargetProfile fromMajorVersion(int majorVersion) {
        for (int i = 0; i < ORDERED_PROFILES.size(); i++) {
            TargetProfile profile = ORDERED_PROFILES.get(i);
            if (profile.majorVersion == majorVersion) {
                return profile;
            }
        }
        throw new IllegalArgumentException("No target profile for Java major version: " + majorVersion);
    }

    public static TargetProfile minimumSupportingJavaVersion(int minimumJavaVersion) {
        if (minimumJavaVersion < 1) {
            throw new IllegalArgumentException("minimumJavaVersion must be positive: " + minimumJavaVersion);
        }
        for (int i = 0; i < ORDERED_PROFILES.size(); i++) {
            TargetProfile profile = ORDERED_PROFILES.get(i);
            if (profile.supportsJavaVersion(minimumJavaVersion)) {
                return profile;
            }
        }
        throw new IllegalArgumentException("No target profile supports Java version: " + minimumJavaVersion);
    }

    private static List<TargetProfile> createOrderedProfiles() {
        List<TargetProfile> profiles = new ArrayList<TargetProfile>();
        profiles.add(JAVA8);
        profiles.add(JAVA11);
        profiles.add(JAVA17);
        profiles.add(JAVA21);
        profiles.add(JAVA25);
        return Collections.unmodifiableList(profiles);
    }

    private static Map<String, TargetProfile> createProfilesByKey() {
        Map<String, TargetProfile> profilesByKey = new LinkedHashMap<String, TargetProfile>();
        for (int i = 0; i < ORDERED_PROFILES.size(); i++) {
            TargetProfile profile = ORDERED_PROFILES.get(i);
            profilesByKey.put(profile.key, profile);
        }
        return Collections.unmodifiableMap(profilesByKey);
    }

    private static String normalizedProfileKey(String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char character = trimmed.charAt(i);
            if (character != ' ' && character != '-' && character != '_') {
                builder.append(character);
            }
        }
        String normalized = builder.toString();
        if (isAllDigits(normalized)) {
            return "java" + normalized;
        }
        if (normalized.startsWith("jdk") && normalized.length() > 3) {
            return "java" + normalized.substring(3);
        }
        return normalized;
    }

    private static boolean isAllDigits(String value) {
        if (value.length() == 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
