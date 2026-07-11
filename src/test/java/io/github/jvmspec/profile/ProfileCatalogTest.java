package io.github.jvmspec.profile;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ProfileCatalogTest {
    @Test
    public void defaultCatalogExposesDeterministicProfileAndSymbolOrder() {
        ProfileCatalog catalog = ProfileCatalog.defaultCatalog();
        List<TargetProfile> expectedProfiles = Arrays.asList(
                TargetProfile.JAVA8,
                TargetProfile.JAVA11,
                TargetProfile.JAVA17,
                TargetProfile.JAVA21,
                TargetProfile.JAVA25
        );

        assertEquals(expectedProfiles, catalog.profiles());
        assertEquals(expectedProfiles, new ArrayList<TargetProfile>(catalog.profilesByKey().values()));
        assertEquals(expectedProfiles, new ArrayList<TargetProfile>(catalog.symbolsIntroducedByProfile().keySet()));
        assertEquals(expectedProfiles, new ArrayList<TargetProfile>(catalog.symbolsAvailableByProfile().keySet()));
        assertEquals("java.lang.Iterable", catalog.symbols().get(0).ownerQualifiedName());
        assertEquals("java.util.stream.Gatherers", catalog.symbols().get(catalog.symbols().size() - 1).ownerQualifiedName());
    }

    @Test
    public void defaultCatalogExposesImmutableCollections() {
        final ProfileCatalog catalog = ProfileCatalog.defaultCatalog();

        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.profiles().add(TargetProfile.JAVA8);
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.profilesByKey().put("custom", TargetProfile.JAVA8);
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.symbols().add(sampleSymbol());
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.symbolsIntroducedByProfile().put(TargetProfile.JAVA8, new ArrayList<ApiSymbol>());
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.symbolsIntroducedIn(TargetProfile.JAVA8).add(sampleSymbol());
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.symbolsAvailableByProfile().get(TargetProfile.JAVA8).add(sampleSymbol());
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.featureSupport(TargetProfile.JAVA8).put(FeatureFlag.STREAM_GATHERERS, Boolean.TRUE);
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.symbolsByOwnerAndMember().put(ApiSymbolKey.of("com.example.Sample"), new ArrayList<ApiSymbol>());
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.symbolsByOwnerAndMember().get(ApiSymbolKey.of("java.util.ArrayList")).add(sampleSymbol());
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.symbolsByOwner().put("com.example.Sample", new ArrayList<ApiSymbol>());
            }
        });
        assertThrows(UnsupportedOperationException.class, new ThrowingRunnable() {
            public void run() {
                catalog.symbolsByOwner().get("java.util.ArrayList").add(sampleSymbol());
            }
        });
    }

    @Test
    public void catalogContainsRepresentativeJava8Symbols() {
        ProfileCatalog catalog = ProfileCatalog.defaultCatalog();

        assertSymbol(catalog, "java.util.ArrayList", null, ApiSymbolKind.TYPE, ApiSymbolCategory.LIST, TargetProfile.JAVA8);
        assertSymbol(catalog, "java.util.Map.Entry", null, ApiSymbolKind.NESTED_TYPE, ApiSymbolCategory.MAP, TargetProfile.JAVA8);
        assertSymbol(catalog, "java.util.Collections", "emptyList", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA8);
        assertSymbol(catalog, "java.util.stream.Stream", null, ApiSymbolKind.TYPE, ApiSymbolCategory.STREAM, TargetProfile.JAVA8);
    }

    @Test
    public void catalogContainsRepresentativeJava11Symbols() {
        ProfileCatalog catalog = ProfileCatalog.defaultCatalog();

        assertSymbol(catalog, "java.util.List", "of", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11);
        assertSymbol(catalog, "java.util.Map", "ofEntries", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.COLLECTION_FACTORY, TargetProfile.JAVA11);
        assertSymbol(catalog, "java.util.Optional", "stream", ApiSymbolKind.METHOD, ApiSymbolCategory.OPTIONAL, TargetProfile.JAVA11);
        assertSymbol(catalog, "java.util.stream.Stream", "ofNullable", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.STREAM, TargetProfile.JAVA11);
        assertSymbol(catalog, "java.nio.file.Files", "readString", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.FILE_IO, TargetProfile.JAVA11);
        assertSymbol(catalog, "java.nio.file.Files", "writeString", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.FILE_IO, TargetProfile.JAVA11);
        assertSymbol(catalog, "java.net.http.HttpClient", null, ApiSymbolKind.TYPE, ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11);
        assertSymbol(catalog, "java.net.http.HttpClient.Builder", null, ApiSymbolKind.NESTED_TYPE, ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11);
        assertSymbol(catalog, "java.net.http.HttpClient", "newHttpClient", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.HTTP_CLIENT, TargetProfile.JAVA11);
    }

    @Test
    public void catalogContainsRepresentativeJava17Symbols() {
        ProfileCatalog catalog = ProfileCatalog.defaultCatalog();

        assertSymbol(catalog, "java.util.stream.Stream", "toList", ApiSymbolKind.METHOD, ApiSymbolCategory.STREAM, TargetProfile.JAVA17);
        assertSymbol(catalog, "record", null, ApiSymbolKind.LANGUAGE_FEATURE, ApiSymbolCategory.LANGUAGE_MODELING, TargetProfile.JAVA17);
        assertSymbol(catalog, "java.util.HexFormat", null, ApiSymbolKind.TYPE, ApiSymbolCategory.HEX_FORMAT, TargetProfile.JAVA17);
        assertSymbol(catalog, "java.time.InstantSource", null, ApiSymbolKind.TYPE, ApiSymbolCategory.TIME_SOURCE, TargetProfile.JAVA17);
        assertSymbol(catalog, "java.time.InstantSource", "system", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.TIME_SOURCE, TargetProfile.JAVA17);
        assertSymbol(catalog, "java.util.random.RandomGenerator", null, ApiSymbolKind.TYPE, ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17);
        assertSymbol(catalog, "java.util.random.RandomGeneratorFactory", null, ApiSymbolKind.TYPE, ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17);
        assertSymbol(catalog, "java.util.random.RandomGeneratorFactory", "all", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17);
        assertSymbol(catalog, "java.util.random.RandomGeneratorFactory", "getDefault", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.RANDOM_GENERATOR, TargetProfile.JAVA17);
    }

    @Test
    public void catalogContainsRepresentativeJava21Symbols() {
        ProfileCatalog catalog = ProfileCatalog.defaultCatalog();

        assertSymbol(catalog, "java.util.SequencedCollection", null, ApiSymbolKind.TYPE, ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21);
        assertSymbol(catalog, "java.util.SequencedCollection", "reversed", ApiSymbolKind.METHOD, ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21);
        assertSymbol(catalog, "java.util.SequencedCollection", "getFirst", ApiSymbolKind.METHOD, ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21);
        assertSymbol(catalog, "java.util.SequencedMap", "putFirst", ApiSymbolKind.METHOD, ApiSymbolCategory.SEQUENCED_COLLECTION, TargetProfile.JAVA21);
        assertSymbol(catalog, "java.lang.Thread.Builder", null, ApiSymbolKind.NESTED_TYPE, ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21);
        assertSymbol(catalog, "java.lang.Thread.Builder.OfVirtual", null, ApiSymbolKind.NESTED_TYPE, ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21);
        assertSymbol(catalog, "java.lang.Thread", "ofVirtual", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21);
        assertSymbol(catalog, "java.lang.Thread", "startVirtualThread", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21);
        assertSymbol(catalog, "java.lang.Thread", "isVirtual", ApiSymbolKind.METHOD, ApiSymbolCategory.VIRTUAL_THREAD, TargetProfile.JAVA21);
    }

    @Test
    public void catalogContainsRepresentativeJava25Symbols() {
        ProfileCatalog catalog = ProfileCatalog.defaultCatalog();

        assertSymbol(catalog, "java.util.stream.Stream", "gather", ApiSymbolKind.METHOD, ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25);
        assertSymbol(catalog, "java.util.stream.Gatherer", null, ApiSymbolKind.TYPE, ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25);
        assertSymbol(catalog, "java.util.stream.Gatherer", "of", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25);
        assertSymbol(catalog, "java.util.stream.Gatherer", "ofSequential", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25);
        assertSymbol(catalog, "java.util.stream.Gatherer", "initializer", ApiSymbolKind.METHOD, ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25);
        assertSymbol(catalog, "java.util.stream.Gatherers", null, ApiSymbolKind.TYPE, ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25);
        assertSymbol(catalog, "java.util.stream.Gatherers", "fold", ApiSymbolKind.STATIC_METHOD, ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25);
        assertSymbol(catalog, "java.util.stream.Gatherer.Integrator", null, ApiSymbolKind.NESTED_TYPE, ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25);
        assertSymbol(catalog, "java.util.stream.Gatherer.Integrator.Greedy", null, ApiSymbolKind.NESTED_TYPE, ApiSymbolCategory.STREAM_GATHERER, TargetProfile.JAVA25);
    }

    @Test
    public void availableSymbolsRespectProfileSupportOrdering() {
        ProfileCatalog catalog = ProfileCatalog.defaultCatalog();
        List<ApiSymbol> java8Symbols = catalog.symbolsAvailableIn(TargetProfile.JAVA8);
        for (int i = 0; i < java8Symbols.size(); i++) {
            assertEquals(TargetProfile.JAVA8, java8Symbols.get(i).introducedProfile());
        }
        assertFalse(containsSymbol(java8Symbols, "java.util.List", "of", TargetProfile.JAVA11));
        assertFalse(containsSymbol(java8Symbols, "java.util.stream.Stream", "toList", TargetProfile.JAVA17));

        List<ApiSymbol> java25Symbols = catalog.symbolsAvailableIn(TargetProfile.JAVA25);
        assertEquals(catalog.symbols().size(), java25Symbols.size());
        assertTrue(java25Symbols.containsAll(catalog.symbols()));
    }

    private static void assertSymbol(
            ProfileCatalog catalog,
            String owner,
            String member,
            ApiSymbolKind kind,
            ApiSymbolCategory category,
            TargetProfile profile
    ) {
        List<ApiSymbol> symbols = catalog.lookup(owner, member);
        for (int i = 0; i < symbols.size(); i++) {
            ApiSymbol symbol = symbols.get(i);
            if (kind.equals(symbol.kind())
                    && category.equals(symbol.category())
                    && profile.equals(symbol.introducedProfile())) {
                return;
            }
        }
        throw new AssertionError("Expected symbol in catalog: " + owner + "#" + member + " introduced in " + profile.key());
    }

    private static boolean containsSymbol(List<ApiSymbol> symbols, String owner, String member, TargetProfile profile) {
        for (int i = 0; i < symbols.size(); i++) {
            ApiSymbol symbol = symbols.get(i);
            if (owner.equals(symbol.ownerQualifiedName())
                    && profile.equals(symbol.introducedProfile())
                    && ((member == null && symbol.memberName() == null) || (member != null && member.equals(symbol.memberName())))) {
                return true;
            }
        }
        return false;
    }

    private static ApiSymbol sampleSymbol() {
        return ApiSymbol.type("com.example.Sample", ApiSymbolCategory.UTILITY_CONTAINER, TargetProfile.JAVA8, "Sample metadata.");
    }
}
