# Migration guide — 1.0

This guide is for users moving from early javaspec snapshots to the 1.0 line.

## Coordinates and package names

Use the 1.0 coordinates under `io.github.jvmspec`:

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

Update imports to `io.github.jvmspec.*`. Historical package names are not part of the current API.

## Generated sources

Generated support and Prophecy wrapper sources are written under
`target/generated-sources/javaspec` by default. Do not rely on generation mutating `src/` unless an
explicit command/config path says so. Add the generated-sources directory to IDE/build-tool test
sources when needed.

Generated production stubs include `// javaspec:stub`. Compiled runs with remaining stub markers add
a synthetic `BROKEN` generation result so skeletons cannot silently become final GREEN.

## Subject-centric authoring

Prefer PHPSpec-style subject-centric examples:

```java
public final class CalculatorSpec extends ObjectBehavior<Calculator> {
    public CalculatorSpec() { super(Calculator.class); }

    public void it_adds_numbers() {
        match(subject().add(2, 3)).shouldReturn(5);
    }
}
```

Generated typed support methods are preferred where available:

```java
add(2, 3).shouldReturn(5);
```

## Collaborators and Prophecy

Prefer generated typed `*Prophecy` wrappers over reflective `method("...")` calls. The reflective API
remains a bootstrap/fallback surface, but typed wrappers are the canonical 1.0 style.

Collaborator injection is supported for ordinary interfaces and generated typed prophecy wrappers on
`let`, example methods, and `letGo`. It is not a general dependency-injection container.

## Example data

Use `row(...)` and `examples(...)` for small PHPSpec-style data tables. Rows execute inline inside the
owning example. JUnit Platform row selectors filter adapter descriptors/events but do not isolate row
execution; see `docs/example-data-contract-1.0.md`.

## Adapters

Optional Maven, Gradle, JUnit Platform, bytecode-doubles, and bytecode-agent artifacts are standalone.
Install or publish each artifact explicitly when using local snapshots. `scripts/verify-all.sh` and
`scripts/verify-release-dry-run.sh` exercise the full artifact set.

## Custom matchers

1.0 supports programmatic custom matcher registration through `MatcherRegistry` and
`match(actual).shouldMatch("name", args...)`. Configuration-file and inline dynamic custom matcher
conveniences are deferred; see `docs/matcher-contract-1.0.md`.
