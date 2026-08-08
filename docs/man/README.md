# javaspec manual pages

This directory contains the maintained section 1 manual pages for the javaspec CLI and repository launcher.

| Language | Repository key | Man page | User manual |
|---|---|---|---|
| English | `en` | [`en/man1/javaspec.1`](en/man1/javaspec.1) | [`../usermanual/Home.md`](../usermanual/Home.md) |
| Italian | `it` | [`it/man1/javaspec.1`](it/man1/javaspec.1) | [`../usermanual/it/Home.md`](../usermanual/it/Home.md) |
| Spanish | `es` | [`es/man1/javaspec.1`](es/man1/javaspec.1) | [`../usermanual/es/Home.md`](../usermanual/es/Home.md) |
| German | `de` | [`de/man1/javaspec.1`](de/man1/javaspec.1) | [`../usermanual/de/Home.md`](../usermanual/de/Home.md) |
| French | `fr` | [`fr/man1/javaspec.1`](fr/man1/javaspec.1) | [`../usermanual/fr/Home.md`](../usermanual/fr/Home.md) |
| Simplified Chinese | `ch` | [`ch/man1/javaspec.1`](ch/man1/javaspec.1) | [`../usermanual/ch/Home.md`](../usermanual/ch/Home.md) |

`ch` is retained as the repository language key requested for this first documentation slice. The
standard installation locale for Simplified Chinese is normally `zh_CN`; packaging may add that
locale alias without changing the maintained source page.

The pages are UTF-8 roff documents and intentionally avoid pinning an artifact version so they can
follow the active 1.0 release line. Command names, launcher provenance options, option names,
defaults, exit codes, and generation-safety behavior must remain aligned across all translations.

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
scripts/check-usermanuals.sh
scripts/check-current-docs.sh
```

The guard checks all six pages for valid roff rendering and shared command/launcher-contract tokens.
The English page is the semantic source of truth; all translations and concise localized user
manuals must be updated in the same change when behavior changes.
