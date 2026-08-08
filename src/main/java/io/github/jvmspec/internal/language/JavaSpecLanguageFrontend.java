package io.github.jvmspec.internal.language;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecDiscovery;
import io.github.jvmspec.discovery.SpecDiscoveryRequest;

import java.util.List;

/** Java specification frontend preserving the pre-1.0 discovery behavior. */
public final class JavaSpecLanguageFrontend implements SpecLanguageFrontend {
    public SourceLanguage language() {
        return SourceLanguage.JAVA;
    }

    public List<DiscoveredSpec> discover(SpecDiscoveryRequest request) {
        return SpecDiscovery.discover(request);
    }
}
