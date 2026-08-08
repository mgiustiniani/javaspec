package io.github.jvmspec.internal.language;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecDiscoveryRequest;

import java.util.List;

/** Internal specification-language discovery seam. */
public interface SpecLanguageFrontend {
    SourceLanguage language();

    List<DiscoveredSpec> discover(SpecDiscoveryRequest request);
}
