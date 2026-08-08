# javaspec manual pages

This directory contains the initial section 1 manual pages for the javaspec CLI.

| Language | Repository key | Page |
|---|---|---|
| English | `en` | [`en/man1/javaspec.1`](en/man1/javaspec.1) |
| Italian | `it` | [`it/man1/javaspec.1`](it/man1/javaspec.1) |
| Spanish | `es` | [`es/man1/javaspec.1`](es/man1/javaspec.1) |
| German | `de` | [`de/man1/javaspec.1`](de/man1/javaspec.1) |
| French | `fr` | [`fr/man1/javaspec.1`](fr/man1/javaspec.1) |
| Simplified Chinese | `ch` | [`ch/man1/javaspec.1`](ch/man1/javaspec.1) |

`ch` is retained as the repository language key requested for this first documentation slice. The
standard installation locale for Simplified Chinese is normally `zh_CN`; packaging may add that
locale alias without changing the maintained source page.

The pages are UTF-8 roff documents and intentionally avoid pinning an artifact version so they can
follow the active 1.0 release-candidate line. Command names, option names, defaults, exit codes, and
generation-safety behavior must remain aligned across all translations.

## Preview

Preview a page directly without installing it:

```sh
man -l docs/man/en/man1/javaspec.1
man -l docs/man/it/man1/javaspec.1
```

Render to a UTF-8 terminal with groff:

```sh
groff -Kutf8 -man -Tutf8 docs/man/de/man1/javaspec.1 | less -R
```

## Validate

Run the documentation guard:

```sh
scripts/check-man-pages.sh
scripts/check-current-docs.sh
```

The guard checks all six pages for valid roff rendering and for the shared command-contract tokens.
The English page is the semantic source of truth; translations should be updated in the same change
when command behavior changes.
