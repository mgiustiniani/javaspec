# JavaSpec 1.0.0-RC5 API review

Status: published `v1.0.0-RC5` at production commit `ae9291f`. Remote Maven artifact and JLC-8
replay passed; Gradle Portal approval and stable-1.0 core Javadoc determinism remain.

Original reviewed implementation HEAD: `2eb26a7`
(`docs: complete constructor-safe restructuring milestone`).
Current pre-RC5 qualification base: `34e692b`. Owner-return discovery commit `008d254` and nested
record-component commit `114c832` change private implementation details and add no public/protected
Java signature.
Pre-cut comparison baseline: the prior `docs/history/api-baseline-1.0.0.md`, generated for
`1.0.0-RC1`. The committed inventory is now regenerated from aligned RC5 artifacts.

## Inventory procedure

After `scripts/verify-all.sh` built every aligned RC5 artifact, the pre-cut inventory was compared
with a generated candidate, then the committed baseline was regenerated twice:

```bash
scripts/generate-api-baseline.sh docs/history/api-baseline-1.0.0.md
scripts/generate-api-baseline.sh /tmp/javaspec-api-baseline-rc5-second.md
cmp docs/history/api-baseline-1.0.0.md /tmp/javaspec-api-baseline-rc5-second.md
```

The RC1-to-RC5 comparison reports 172 added public/protected declaration lines and no removed
public/protected declaration lines. The two RC5 generations are byte-identical. Public Java visibility is interpreted
through `docs/api-surface-1.0.md`; additions inside an `INTERNAL` package are inventory entries, not
supported API commitments.

## Classification of additions

### Internal core inventory

The largest inventory addition is under these explicitly `INTERNAL` packages:

- `io.github.jvmspec.internal.language`: Java-only frontend/backend seam, portable behavior contract,
  structured type projection, and immutable synchronization plans;
- `io.github.jvmspec.internal.type`: canonical constructor signatures, source-context type resolution,
  balanced Java syntax splitting, identifier checks, and deterministic import rendering;
- `io.github.jvmspec.cli.run`: generation activity accounting and pending-generation result details.

These types remain absent from the public extension SPI, CLI language options, configuration keys,
and ServiceLoader registration. They do not create a Kotlin or general language-support promise.

### Adapter inventory

`io.github.jvmspec.maven.JavaspecGenerateMojo` is the implementation entrypoint for the documented
`javaspec:generate` Maven goal introduced after RC1. The adapter contract is the goal name,
parameters, generated-test-source registration, failure behavior, and lifecycle integration rather
than direct construction of the Mojo class.

### Supported public API

No existing supported `PUBLIC_API` or `PUBLIC_SPI` JVM signature is removed or changed by the
RC1-to-current comparison. Generated-source and CLI/report behavior changed additively as documented
in the changelog, generation contract, report schema, and regression suite.

## RC5 cut actions

- [x] Change all aligned artifact versions from `1.0.0-RC4` to `1.0.0-RC5`.
- [x] Run initial `scripts/verify-all.sh` and `scripts/verify-release-dry-run.sh` sequentially.
- [x] Regenerate `docs/history/api-baseline-1.0.0.md` twice from fully built RC5 artifacts.
- [x] Confirm no supported-surface removal or incompatible descriptor change.
- [x] Repeat all local release gates from clean commit `8f93a46`, including Java 21/25 core,
  security, byte-reproducible release archives, external consumers, and Tasks downstream conformance.
- [x] Run the Java 8/11/17/21/25 CI matrix on pushed release commit `4d72aca`: run
  [`31261952121`](https://github.com/mgiustiniani/javaspec/actions/runs/31261952121) passed all six
  jobs with zero annotations.
- [x] Update release evidence with the RC5 commit, final local artifact hashes, and CI run URL.
- [x] Add immutable remote artifact replay evidence after publication: five clean Maven consumers
  passed; `magrathea-pki` commit `7c6c41e` passed CLI 4/4 twice and Java 21 Maven domain 4/4 with
  zero pending and no source mutation.
- [ ] Close the cross-environment core Javadoc index mismatch and obtain Gradle Portal approval
  before stable 1.0.
