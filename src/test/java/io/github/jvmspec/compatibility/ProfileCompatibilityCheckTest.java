package io.github.jvmspec.compatibility;

import io.github.jvmspec.model.JavaTypeKind;
import io.github.jvmspec.profile.FeatureFlag;
import io.github.jvmspec.profile.TargetProfile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProfileCompatibilityCheckTest {
    @Test
    public void facadeDeniesRecordsBeforeJava17AndAllowsThemInJava17() {
        CompatibilityCheck check = CompatibilityCheck.defaultCheck();

        CompatibilityResult denied = check.checkTypeKind(TargetProfile.JAVA11, JavaTypeKind.RECORD);
        assertTrue(denied.isDenied());
        assertFalse(denied.isAllowed());
        assertEquals(TargetProfile.JAVA17, denied.requiredProfile());

        CompatibilityResult allowed = check.checkTypeKind(TargetProfile.JAVA17, JavaTypeKind.RECORD);
        assertTrue(allowed.isAllowed());
        assertEquals(TargetProfile.JAVA17, allowed.requiredProfile());
    }

    @Test
    public void profileCheckDeniesSealedClassesBeforeJava17AndAllowsThemInJava17() {
        ProfileCompatibilityCheck check = ProfileCompatibilityCheck.defaultCheck();

        CompatibilityResult denied = check.checkTypeKind(TargetProfile.JAVA11, JavaTypeKind.SEALED_CLASS);
        assertTrue(denied.isDenied());
        assertEquals(TargetProfile.JAVA17, denied.requiredProfile());

        CompatibilityResult allowed = check.checkTypeKind(TargetProfile.JAVA17, JavaTypeKind.SEALED_CLASS);
        assertTrue(allowed.isAllowed());
        assertEquals(TargetProfile.JAVA17, allowed.requiredProfile());
    }

    @Test
    public void collectionFactoryFeatureRequiresJava11() {
        ProfileCompatibilityCheck check = ProfileCompatibilityCheck.defaultCheck();

        CompatibilityResult denied = check.checkFeature(TargetProfile.JAVA8, FeatureFlag.COLLECTION_FACTORY_APIS);
        assertTrue(denied.isDenied());
        assertEquals(TargetProfile.JAVA11, denied.requiredProfile());
        assertFalse(check.isFeatureAllowed(TargetProfile.JAVA8, FeatureFlag.COLLECTION_FACTORY_APIS));

        CompatibilityResult allowed = check.checkFeature(TargetProfile.JAVA11, FeatureFlag.COLLECTION_FACTORY_APIS);
        assertTrue(allowed.isAllowed());
        assertEquals(TargetProfile.JAVA11, allowed.requiredProfile());
        assertTrue(check.isFeatureAllowed(TargetProfile.JAVA11, FeatureFlag.COLLECTION_FACTORY_APIS));
    }

    @Test
    public void listFactoryMemberRequiresJava11() {
        CompatibilityCheck check = CompatibilityCheck.defaultCheck();

        CompatibilityResult denied = check.checkApiSymbol(TargetProfile.JAVA8, "java.util.List", "of");
        assertTrue(denied.isDenied());
        assertEquals(TargetProfile.JAVA11, denied.requiredProfile());
        assertFalse(check.isApiSymbolAllowed(TargetProfile.JAVA8, "java.util.List", "of"));

        CompatibilityResult allowed = check.checkApiSymbol(TargetProfile.JAVA11, "java.util.List", "of");
        assertTrue(allowed.isAllowed());
        assertEquals(TargetProfile.JAVA11, allowed.requiredProfile());
        assertTrue(check.isApiSymbolAllowed(TargetProfile.JAVA11, "java.util.List", "of"));
    }

    @Test
    public void phase36ApiSymbolsRespectProfileBoundaries() {
        ProfileCompatibilityCheck check = ProfileCompatibilityCheck.defaultCheck();

        CompatibilityResult deniedHttpClient = check.checkApiSymbol(TargetProfile.JAVA8, "java.net.http.HttpClient", null);
        assertTrue(deniedHttpClient.isDenied());
        assertEquals(TargetProfile.JAVA11, deniedHttpClient.requiredProfile());
        assertFalse(check.isApiSymbolAllowed(TargetProfile.JAVA8, "java.net.http.HttpClient", null));

        CompatibilityResult allowedHttpClient = check.checkApiSymbol(TargetProfile.JAVA11, "java.net.http.HttpClient", null);
        assertTrue(allowedHttpClient.isAllowed());
        assertEquals(TargetProfile.JAVA11, allowedHttpClient.requiredProfile());

        CompatibilityResult deniedVirtualBuilder = check.checkApiSymbol(TargetProfile.JAVA17, "java.lang.Thread.Builder.OfVirtual", null);
        assertTrue(deniedVirtualBuilder.isDenied());
        assertEquals(TargetProfile.JAVA21, deniedVirtualBuilder.requiredProfile());

        CompatibilityResult allowedVirtualBuilder = check.checkApiSymbol(TargetProfile.JAVA21, "java.lang.Thread.Builder.OfVirtual", null);
        assertTrue(allowedVirtualBuilder.isAllowed());
        assertEquals(TargetProfile.JAVA21, allowedVirtualBuilder.requiredProfile());

        CompatibilityResult deniedGather = check.checkApiSymbol(TargetProfile.JAVA21, "java.util.stream.Stream", "gather");
        assertTrue(deniedGather.isDenied());
        assertEquals(TargetProfile.JAVA25, deniedGather.requiredProfile());
        assertFalse(check.isApiSymbolAllowed(TargetProfile.JAVA21, "java.util.stream.Stream", "gather"));

        CompatibilityResult allowedGather = check.checkApiSymbol(TargetProfile.JAVA25, "java.util.stream.Stream", "gather");
        assertTrue(allowedGather.isAllowed());
        assertEquals(TargetProfile.JAVA25, allowedGather.requiredProfile());
    }

    @Test
    public void unknownApiSymbolIsDeniedWithoutRequiredProfile() {
        CompatibilityCheck check = CompatibilityCheck.defaultCheck();

        CompatibilityResult result = check.checkApiSymbol(TargetProfile.JAVA8, "com.example.DoesNotExist", null);

        assertTrue(result.isDenied());
        assertFalse(result.hasRequiredProfile());
        assertNull(result.requiredProfile());
        assertEquals(TargetProfile.JAVA8, result.targetProfile());
        assertEquals("com.example.DoesNotExist", result.subject());
        assertTrue(result.message().contains("Unknown API symbol"));
    }
}
