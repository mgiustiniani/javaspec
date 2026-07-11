package io.github.jvmspec.doubles;

import java.util.ServiceLoader;

final class ConcreteDoubleRegistry {
    private ConcreteDoubleRegistry() {}

    static ConcreteDoubleProvider findProvider(Class<?> type) {
        ServiceLoader<ConcreteDoubleProvider> loader =
                ServiceLoader.load(ConcreteDoubleProvider.class,
                        Thread.currentThread().getContextClassLoader());
        for (ConcreteDoubleProvider provider : loader) {
            if (provider.supports(type)) {
                return provider;
            }
        }
        return null;
    }
}
