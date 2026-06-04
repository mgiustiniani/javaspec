# Changelog

All notable changes to this project will be documented in this file.

## Unreleased

- Added the MIT `LICENSE` file and release-readiness scaffolding: version-alignment verification, release-artifact profiles, MIT license and maintainer publication metadata, and a release checklist; public publishing/deployment/signing/portal credentials remain postponed.
- Kept the repository layout non-reactor/non-multi-module: the core, Maven plugin, Gradle plugin, and JUnit Platform engine remain standalone builds.

## 0.1.0-SNAPSHOT

- Implemented the Java 8-compatible, zero-runtime-dependency javaspec core with CLI describe/run workflows, configuration, discovery, generation support, matchers, interface doubles, run controls, reports, and programmatic no-JUnit invocation.
- Added standalone optional adapters for Maven, Gradle, and JUnit Platform, all outside the core runtime artifact.
- Added aggregate local/CI verification for the core and standalone adapters while preserving the zero-runtime-dependency core policy.
