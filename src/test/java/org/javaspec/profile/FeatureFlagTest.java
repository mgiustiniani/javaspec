package org.javaspec.profile;

import org.javaspec.model.JavaTypeKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FeatureFlagTest {
    @Test
    public void java8TypeKindsMapToJava8Features() {
        assertFeature(JavaTypeKind.CLASS, FeatureFlag.CLASS, TargetProfile.JAVA8);
        assertFeature(JavaTypeKind.FINAL_CLASS, FeatureFlag.FINAL_CLASS, TargetProfile.JAVA8);
        assertFeature(JavaTypeKind.INTERFACE, FeatureFlag.INTERFACE, TargetProfile.JAVA8);
        assertFeature(JavaTypeKind.ENUM, FeatureFlag.ENUM, TargetProfile.JAVA8);
        assertFeature(JavaTypeKind.ANNOTATION, FeatureFlag.ANNOTATION, TargetProfile.JAVA8);
    }

    @Test
    public void postJava8TypeKindsMapToJava17Features() {
        assertFeature(JavaTypeKind.RECORD, FeatureFlag.RECORD, TargetProfile.JAVA17);
        assertFeature(JavaTypeKind.SEALED_CLASS, FeatureFlag.SEALED_CLASS, TargetProfile.JAVA17);
        assertFeature(JavaTypeKind.SEALED_INTERFACE, FeatureFlag.SEALED_INTERFACE, TargetProfile.JAVA17);
    }

    @Test
    public void collectionFactoryApisRequireJava11() {
        assertEquals(TargetProfile.JAVA11, FeatureFlag.COLLECTION_FACTORY_APIS.introducedProfile());
        assertFalse(FeatureFlag.COLLECTION_FACTORY_APIS.isSupportedBy(TargetProfile.JAVA8));
        assertTrue(FeatureFlag.COLLECTION_FACTORY_APIS.isSupportedBy(TargetProfile.JAVA11));
    }

    @Test
    public void sequencedCollectionsRequireJava21() {
        assertEquals(TargetProfile.JAVA21, FeatureFlag.SEQUENCED_COLLECTIONS.introducedProfile());
        assertFalse(FeatureFlag.SEQUENCED_COLLECTIONS.isSupportedBy(TargetProfile.JAVA17));
        assertTrue(FeatureFlag.SEQUENCED_COLLECTIONS.isSupportedBy(TargetProfile.JAVA21));
    }

    @Test
    public void streamGatherersRequireJava25() {
        assertEquals(TargetProfile.JAVA25, FeatureFlag.STREAM_GATHERERS.introducedProfile());
        assertFalse(FeatureFlag.STREAM_GATHERERS.isSupportedBy(TargetProfile.JAVA21));
        assertTrue(FeatureFlag.STREAM_GATHERERS.isSupportedBy(TargetProfile.JAVA25));
    }

    private static void assertFeature(JavaTypeKind typeKind, FeatureFlag expectedFlag, TargetProfile expectedProfile) {
        FeatureFlag actualFlag = FeatureFlag.forTypeKind(typeKind);
        assertEquals(expectedFlag, actualFlag);
        assertEquals(expectedProfile, actualFlag.introducedProfile());
    }
}
