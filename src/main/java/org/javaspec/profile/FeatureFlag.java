package org.javaspec.profile;

import org.javaspec.model.JavaTypeKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata-only feature flags understood by target profile checks.
 */
public enum FeatureFlag {
    CLASS("class", "Class", TargetProfile.JAVA8, "Ordinary class declarations."),
    FINAL_CLASS("final-class", "Final class", TargetProfile.JAVA8, "Final class declarations."),
    INTERFACE("interface", "Interface", TargetProfile.JAVA8, "Interface declarations."),
    ENUM("enum", "Enum", TargetProfile.JAVA8, "Enum declarations."),
    ANNOTATION("annotation", "Annotation", TargetProfile.JAVA8, "Annotation type declarations."),
    RECORD("record", "Record", TargetProfile.JAVA17, "Record data carriers are modeled as Java 17 LTS features."),
    SEALED_CLASS("sealed-class", "Sealed class", TargetProfile.JAVA17, "Sealed class hierarchy modeling."),
    SEALED_INTERFACE("sealed-interface", "Sealed interface", TargetProfile.JAVA17, "Sealed interface hierarchy modeling."),
    COLLECTION_FACTORY_APIS("collection-factory-apis", "Collection factory APIs", TargetProfile.JAVA11, "Unmodifiable and value-based collection factories and copy factories."),
    UNMODIFIABLE_COLLECTORS("unmodifiable-collectors", "Unmodifiable collectors", TargetProfile.JAVA11, "Collectors that produce unmodifiable collection results."),
    STREAM_TO_LIST("stream-to-list", "Stream toList", TargetProfile.JAVA17, "Stream terminal operation that returns an unmodifiable list result."),
    STREAM_MAP_MULTI("stream-map-multi", "Stream mapMulti", TargetProfile.JAVA17, "Stream mapMulti operations and related nested support types."),
    SEQUENCED_COLLECTIONS("sequenced-collections", "Sequenced collections", TargetProfile.JAVA21, "Sequenced collection interfaces, encounter-order operations, and reversed views."),
    STREAM_GATHERERS("stream-gatherers", "Stream gatherers", TargetProfile.JAVA25, "Gatherer-based stream traversal and transformation support.");

    private static final List<FeatureFlag> ORDERED_FLAGS = createOrderedFlags();
    private static final Map<String, FeatureFlag> FLAGS_BY_KEY = createFlagsByKey();

    private final String key;
    private final String displayName;
    private final TargetProfile introducedProfile;
    private final String notes;

    FeatureFlag(String key, String displayName, TargetProfile introducedProfile, String notes) {
        this.key = key;
        this.displayName = displayName;
        this.introducedProfile = introducedProfile;
        this.notes = notes;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public TargetProfile introducedProfile() {
        return introducedProfile;
    }

    public String notes() {
        return notes;
    }

    public boolean isSupportedBy(TargetProfile targetProfile) {
        Objects.requireNonNull(targetProfile, "targetProfile must not be null");
        return targetProfile.supports(introducedProfile);
    }

    public static List<FeatureFlag> orderedFlags() {
        return ORDERED_FLAGS;
    }

    public static Map<String, FeatureFlag> flagsByKey() {
        return FLAGS_BY_KEY;
    }

    public static FeatureFlag fromKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        if (key.length() == 0) {
            throw new IllegalArgumentException("Feature key must not be empty");
        }
        if (!key.equals(key.trim())) {
            throw new IllegalArgumentException("Feature key must not contain leading or trailing whitespace: " + key);
        }
        FeatureFlag flag = FLAGS_BY_KEY.get(key);
        if (flag == null) {
            throw new IllegalArgumentException("Unknown feature key: " + key);
        }
        return flag;
    }

    public static FeatureFlag forTypeKind(JavaTypeKind typeKind) {
        Objects.requireNonNull(typeKind, "typeKind must not be null");
        if (JavaTypeKind.CLASS.equals(typeKind)) {
            return CLASS;
        }
        if (JavaTypeKind.FINAL_CLASS.equals(typeKind)) {
            return FINAL_CLASS;
        }
        if (JavaTypeKind.INTERFACE.equals(typeKind)) {
            return INTERFACE;
        }
        if (JavaTypeKind.ENUM.equals(typeKind)) {
            return ENUM;
        }
        if (JavaTypeKind.ANNOTATION.equals(typeKind)) {
            return ANNOTATION;
        }
        if (JavaTypeKind.RECORD.equals(typeKind)) {
            return RECORD;
        }
        if (JavaTypeKind.SEALED_CLASS.equals(typeKind)) {
            return SEALED_CLASS;
        }
        if (JavaTypeKind.SEALED_INTERFACE.equals(typeKind)) {
            return SEALED_INTERFACE;
        }
        throw new IllegalArgumentException("Unsupported Java type kind: " + typeKind);
    }

    private static List<FeatureFlag> createOrderedFlags() {
        FeatureFlag[] values = values();
        List<FeatureFlag> flags = new ArrayList<FeatureFlag>();
        for (int i = 0; i < values.length; i++) {
            flags.add(values[i]);
        }
        return Collections.unmodifiableList(flags);
    }

    private static Map<String, FeatureFlag> createFlagsByKey() {
        Map<String, FeatureFlag> flagsByKey = new LinkedHashMap<String, FeatureFlag>();
        for (int i = 0; i < ORDERED_FLAGS.size(); i++) {
            FeatureFlag flag = ORDERED_FLAGS.get(i);
            flagsByKey.put(flag.key, flag);
        }
        return Collections.unmodifiableMap(flagsByKey);
    }
}
