package org.javaspec.invocation;

import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecDiscovery;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.runner.RunResult;
import org.javaspec.runner.SpecRunner;

import java.util.List;
import java.util.Objects;

/**
 * Programmatic launcher over the canonical javaspec discovery and reflection runner.
 */
public final class JavaspecLauncher {
    public JavaspecLauncher() {
    }

    public static JavaspecInvocationResult launch(JavaspecInvocation invocation) {
        return run(invocation);
    }

    public static JavaspecInvocationResult run(JavaspecInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation must not be null");
        List<DiscoveredSpec> specs = invocation.hasDiscoveredSpecs()
                ? invocation.discoveredSpecs()
                : SpecDiscovery.discover(invocation.discoveryRequest());
        RunResult runResult = SpecRunner.run(specs, invocation.classLoader(), invocation.stopOnFailure());
        return JavaspecInvocationResult.of(specs, runResult, exitCodeFor(runResult));
    }

    public static JavaspecInvocationResult invoke(JavaspecInvocation invocation) {
        return run(invocation);
    }

    public static JavaspecInvocationResult run(SpecDiscoveryRequest discoveryRequest, ClassLoader classLoader) {
        return run(JavaspecInvocation.of(discoveryRequest, classLoader));
    }

    public static JavaspecInvocationResult run(
            SpecDiscoveryRequest discoveryRequest,
            ClassLoader classLoader,
            boolean stopOnFailure
    ) {
        return run(JavaspecInvocation.of(discoveryRequest, classLoader).withStopOnFailure(stopOnFailure));
    }

    public static JavaspecInvocationResult run(List<DiscoveredSpec> specs, ClassLoader classLoader) {
        return run(JavaspecInvocation.of(specs, classLoader));
    }

    public static JavaspecInvocationResult run(List<DiscoveredSpec> specs, ClassLoader classLoader, boolean stopOnFailure) {
        return run(JavaspecInvocation.of(specs, classLoader).withStopOnFailure(stopOnFailure));
    }

    public static int exitCodeFor(RunResult runResult) {
        return JavaspecExitCode.from(runResult);
    }
}
