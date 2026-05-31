package org.javaspec.profile;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ApiSymbolTest {
    @Test
    public void createsMethodSymbolsWithDisplayNameAndKey() {
        ApiSymbol symbol = ApiSymbol.staticMethod(
                "java.util.Collections",
                "emptyList",
                ApiSymbolCategory.COLLECTION_FACTORY,
                TargetProfile.JAVA8,
                "Empty list factory."
        );

        assertEquals("java.util.Collections", symbol.ownerQualifiedName());
        assertTrue(symbol.hasMemberName());
        assertEquals("emptyList", symbol.memberName());
        assertEquals(ApiSymbolKind.STATIC_METHOD, symbol.kind());
        assertEquals(ApiSymbolCategory.COLLECTION_FACTORY, symbol.category());
        assertEquals(TargetProfile.JAVA8, symbol.introducedProfile());
        assertEquals("Empty list factory.", symbol.notes());
        assertEquals("java.util.Collections.emptyList", symbol.displayName());
        assertEquals(ApiSymbolKey.of("java.util.Collections", "emptyList"), symbol.key());
        assertEquals("java.util.Collections.emptyList [static method, java8]", symbol.toString());
    }

    @Test
    public void createsTypeSymbolsWithoutMemberName() {
        ApiSymbol symbol = ApiSymbol.type(
                "java.util.ArrayList",
                ApiSymbolCategory.LIST,
                TargetProfile.JAVA8,
                "Resizable array list."
        );

        assertFalse(symbol.hasMemberName());
        assertEquals(null, symbol.memberName());
        assertEquals("java.util.ArrayList", symbol.displayName());
        assertEquals(ApiSymbolKey.of("java.util.ArrayList"), symbol.key());
        assertEquals("java.util.ArrayList [type, java8]", symbol.toString());
    }

    @Test
    public void equalityAndHashCodeIncludeAllFields() {
        ApiSymbol first = ApiSymbol.method(
                "java.util.Optional",
                "stream",
                ApiSymbolCategory.OPTIONAL,
                TargetProfile.JAVA11,
                "Optional-to-stream bridge."
        );
        ApiSymbol same = ApiSymbol.method(
                "java.util.Optional",
                "stream",
                ApiSymbolCategory.OPTIONAL,
                TargetProfile.JAVA11,
                "Optional-to-stream bridge."
        );
        ApiSymbol differentProfile = ApiSymbol.method(
                "java.util.Optional",
                "stream",
                ApiSymbolCategory.OPTIONAL,
                TargetProfile.JAVA17,
                "Optional-to-stream bridge."
        );

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertFalse(first.equals(differentProfile));
        assertFalse(first.equals("java.util.Optional.stream"));
    }

    @Test
    public void validatesRequiredOwnerKindCategoryProfileAndNotes() {
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.type(null, ApiSymbolCategory.LIST, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.type("", ApiSymbolCategory.LIST, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.type("   ", ApiSymbolCategory.LIST, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.type(" java.util.List", ApiSymbolCategory.LIST, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.of("java.util.List", null, null, ApiSymbolCategory.LIST, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.type("java.util.List", null, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.type("java.util.List", ApiSymbolCategory.LIST, null, "notes");
            }
        });
        assertThrows(NullPointerException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.type("java.util.List", ApiSymbolCategory.LIST, TargetProfile.JAVA8, null);
            }
        });
    }

    @Test
    public void validatesRequiredMemberNameForMemberSymbols() {
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.method("java.util.List", null, ApiSymbolCategory.LIST, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.staticMethod("java.util.List", null, ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "notes");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.field("java.lang.System", null, ApiSymbolCategory.UTILITY_CONTAINER, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.staticMethod("java.util.List", "", ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11, "notes");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.field("java.lang.System", "   ", ApiSymbolCategory.UTILITY_CONTAINER, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.field("java.lang.System", " out", ApiSymbolCategory.UTILITY_CONTAINER, TargetProfile.JAVA8, "notes");
            }
        });
    }

    @Test
    public void rejectsMemberNameForTypeAndLanguageFeatureSymbols() {
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.of("java.util.List", "size", ApiSymbolKind.TYPE, ApiSymbolCategory.LIST, TargetProfile.JAVA8, "notes");
            }
        });
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            public void run() {
                ApiSymbol.of("record", "component", ApiSymbolKind.LANGUAGE_FEATURE, ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17, "notes");
            }
        });
    }

    @Test
    public void availabilityUsesIntroducedProfile() {
        ApiSymbol symbol = ApiSymbol.staticMethod(
                "java.util.List",
                "of",
                ApiSymbolCategory.COLLECTION_FACTORY,
                TargetProfile.JAVA11,
                "Unmodifiable list factory."
        );

        assertFalse(symbol.isAvailableIn(TargetProfile.JAVA8));
        assertTrue(symbol.isAvailableIn(TargetProfile.JAVA11));
        assertTrue(symbol.isAvailableIn(TargetProfile.JAVA25));
    }
}
