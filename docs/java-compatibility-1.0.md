# Java compatibility matrix — 1.0

javaspec core is Java 8 runtime-compatible and has zero third-party runtime dependencies. Optional
adapters may add their own dependencies while preserving javaspec semantics.

| Area | 1.0 contract |
|---|---|
| Core runtime | Java 8-compatible bytecode/source target; no third-party runtime dependencies. |
| Core compile/test toolchain | Uses the current JDK for local verification; generated/source code remains Java 8-compatible unless a selected target profile allows newer syntax. |
| CLI compilation | Opt-in through `--compile`; uses `javax.tools.JavaCompiler`; supports `--release <N>` on Java 9+ compilers. |
| Target profiles | `java8`, `java11`, `java17`, `java21`, and `java25` are conservative generation/update gates. |
| Records/sealed types | Java-adapted generation/update support when the target profile and compiler support the feature. |
| Maven plugin | Optional artifact; depends on javaspec core and Maven plugin APIs in the plugin artifact only. |
| Gradle plugin | Optional plugin artifact; depends on javaspec core in the Gradle plugin only. |
| JUnit Platform engine | Optional artifact; depends on JUnit Platform APIs outside core. |
| Bytecode doubles | Optional ByteBuddy subclass adapter outside core. |
| Bytecode agent | Optional ByteBuddy/java.lang.instrument adapter outside core. |

## Verification expectations

Before RC/final release, run:

```sh
scripts/check-version-alignment.sh
scripts/check-current-docs.sh
mvn -q verify
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
```

The default Maven `verify` gate runs Animal Sniffer against the Java 8 API signature so accidental
direct linkage to post-Java-8 APIs fails the build. The only explicit signature ignore is
`com.sun.source.*`, because javaspec's source scanner uses the javac tree API supplied by JDK 8
`tools.jar` and by the compiler module on newer JDKs.

The release checklist records per-JDK CI evidence when available. Local verification on one JDK does
not replace the release-candidate Java 8/11/17/21/25 matrix evidence.

Runtime/API evidence and source-language evidence are tracked separately. API symbols and profile
boundaries remain in `ProfileCatalogTest` and profile-enforcement tests. Final language constructs
and source-fidelity concerns are inventoried in
[`java-language-coverage-roadmap.md`](java-language-coverage-roadmap.md) and
`src/test/resources/java-language-matrix/coverage.tsv`. `JavaLanguageCoverageManifestTest` writes
the host result to `target/java-language-coverage-report.txt`; development mode reports outstanding
`PLANNED` fixtures, while `-Djavaspec.languageCoverage.strict=true` rejects any remaining planned
coverage at the stable gate.

## Dependency boundary

The root `io.github.jvmspec:javaspec` runtime dependency tree must stay empty. Dependency-heavy
capabilities live in optional artifacts so users can adopt them explicitly.
