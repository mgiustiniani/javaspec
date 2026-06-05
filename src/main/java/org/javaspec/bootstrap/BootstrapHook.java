package org.javaspec.bootstrap;

/**
 * User-provided hook executed immediately before javaspec examples run.
 */
public interface BootstrapHook {
    void bootstrap(BootstrapContext context) throws Exception;
}
