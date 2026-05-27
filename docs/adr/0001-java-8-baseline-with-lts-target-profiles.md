# 0001 — Java 8 Baseline with LTS Target Profiles

## Status

Accepted

## Context

javaspec must compile and run on Java 8 while also modeling target Java LTS profiles for Java 8 and later LTS releases available as of 2026-05-27: 8, 11, 17, 21, and 25.

A Java 8-compatible binary cannot directly reference classes, methods, or interfaces introduced after Java 8. Direct references would create linkage failures when running on Java 8 even if those code paths were not intended to execute.

The project also needs to understand modern Java collection and language capabilities so specifications can target newer LTS profiles.

## Decision

javaspec will use Java 8 as the production source, bytecode, and runtime compatibility baseline.

The project will model target JDK capabilities through explicit LTS profiles:

- `java8`
- `java11`
- `java17`
- `java21`
- `java25`

APIs introduced after Java 8 will be represented as metadata strings, symbolic descriptors, or reflected conditionally through a compatibility boundary. Production source must not import or directly call post-Java 8 JDK APIs.

Java 25-specific public data-structure additions will not be assumed until maintainers verify the target JDK 25 API documentation.

## Consequences

Positive consequences:

- The runtime artifact can execute on Java 8.
- A single binary can model multiple Java LTS targets.
- Version-specific behavior remains explicit and testable.
- Linkage errors from accidental Java 9+ references are easier to prevent.

Negative consequences:

- The implementation cannot use newer Java language features in production code.
- Reflection and metadata require extra tests and careful diagnostics.
- Some newer APIs cannot be used ergonomically inside the core implementation.

Follow-up actions:

- Add build checks for Java 8 source/target compatibility.
- Add tests that run on Java 8 and newer LTS runtimes where available.
- Add audits for accidental Java 9+ compile-time references.
- Keep the LTS data-structure catalog synchronized with verified JDK API docs.
