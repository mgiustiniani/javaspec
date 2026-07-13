package io.github.jvmspec.internal.language;

/** Built-in language runtime. Additional language registration is deferred until after 1.0. */
public final class LanguageRuntime {
    private static final SpecLanguageFrontend JAVA_SPEC_FRONTEND =
            new JavaSpecLanguageFrontend();
    private static final ProductionLanguageBackend JAVA_PRODUCTION_BACKEND =
            new JavaProductionLanguageBackend();

    private LanguageRuntime() {
    }

    public static SpecLanguageFrontend javaSpecFrontend() {
        return JAVA_SPEC_FRONTEND;
    }

    public static ProductionLanguageBackend javaProductionBackend() {
        return JAVA_PRODUCTION_BACKEND;
    }
}
