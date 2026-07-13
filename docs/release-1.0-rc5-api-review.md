# JavaSpec 1.0.0-RC5 API review

Status: prepared on `develop` after M12; version cut pending.

Reviewed HEAD: `2eb26a7` (`docs: complete constructor-safe restructuring milestone`).
Comparison baseline: `docs/history/api-baseline-1.0.0.md`, generated for `1.0.0-RC1`.

## Inventory procedure

After `scripts/verify-all.sh` built every shipped artifact:

```bash
scripts/generate-api-baseline.sh /tmp/javaspec-current-api.md
diff -u docs/history/api-baseline-1.0.0.md /tmp/javaspec-current-api.md
```

The comparison reports 172 added public/protected declaration lines and no removed public/protected
declaration lines. Public Java visibility is interpreted through `docs/api-surface-1.0.md`; additions
inside an `INTERNAL` package are inventory entries, not supported API commitments.

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

1. Change all aligned artifact versions from `1.0.0-RC4` to `1.0.0-RC5`.
2. Run `scripts/verify-all.sh` and `scripts/verify-release-dry-run.sh` sequentially.
3. Regenerate `docs/history/api-baseline-1.0.0.md` from the fully built RC5 artifacts.
4. Confirm the regenerated inventory has no unreviewed supported-surface removal or incompatible
   descriptor change.
5. Run the Java 8/11/17/21/25 CI matrix and security profile.
6. Update release notes/evidence with the RC5 commit, artifact hashes, and CI run URL before tagging.

Do not regenerate the committed baseline from an RC4-named development build: the baseline update is
part of the aligned RC5 version cut so the inventory remains commit- and artifact-qualified.
