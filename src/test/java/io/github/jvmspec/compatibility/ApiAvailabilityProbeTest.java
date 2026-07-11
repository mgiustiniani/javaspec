package io.github.jvmspec.compatibility;

import io.github.jvmspec.profile.ApiSymbol;
import io.github.jvmspec.profile.ApiSymbolCategory;
import io.github.jvmspec.profile.TargetProfile;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApiAvailabilityProbeTest {
    @Test
    public void verifiesJava8ClassesAndMethodsByName() {
        ApiAvailabilityProbe probe = ApiAvailabilityProbe.usingContextClassLoader();

        assertTrue(probe.isClassPresent("java.util.ArrayList"));
        assertTrue(probe.isMethodPresent("java.util.Collections", "emptyList"));
        assertTrue(probe.isOwnerPresent(ApiSymbol.type(
                "java.util.ArrayList",
                ApiSymbolCategory.LIST,
                TargetProfile.JAVA8,
                "Resizable array list."
        )));
        assertTrue(probe.isSymbolPresent(ApiSymbol.staticMethod(
                "java.util.Collections",
                "emptyList",
                ApiSymbolCategory.COLLECTION_FACTORY,
                TargetProfile.JAVA8,
                "Empty list factory."
        )));
    }

    @Test
    public void nonexistentClassAndMembersReturnFalse() {
        ApiAvailabilityProbe probe = ApiAvailabilityProbe.usingContextClassLoader();

        assertFalse(probe.isClassPresent("com.example.DoesNotExist"));
        assertFalse(probe.isMethodPresent("com.example.DoesNotExist", "missing"));
        assertFalse(probe.isMethodPresent("java.util.Collections", "missing"));
        assertFalse(probe.isFieldPresent("java.lang.System", "missing"));
        assertFalse(probe.isSymbolPresent(ApiSymbol.staticMethod(
                "java.util.Collections",
                "missing",
                ApiSymbolCategory.COLLECTION_FACTORY,
                TargetProfile.JAVA8,
                "Missing method."
        )));
    }

    @Test
    public void arrayTypeNamesReturnPresentWithoutLoadingArrayClasses() {
        ApiAvailabilityProbe probe = ApiAvailabilityProbe.usingContextClassLoader();

        assertTrue(probe.isClassPresent("int[]"));
        assertTrue(probe.isClassPresent("java.lang.String[]"));
        assertTrue(probe.isOwnerPresent(ApiSymbol.arrayType(
                "boolean[]",
                ApiSymbolCategory.ARRAY,
                TargetProfile.JAVA8,
                "Primitive boolean array."
        )));
    }
}
