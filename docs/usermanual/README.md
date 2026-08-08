# javaspec user manual — language index

The detailed English manual is the semantic source of truth. Concise maintained editions provide the
installation, first-specification, safe-generation, execution, reporting, adapter, native-preview,
and release-status paths in the same six languages as the section 1 manual pages.

| Language | User manual | Section 1 manual page |
|---|---|---|
| English | [`Home.md`](Home.md) | [`../man/en/man1/javaspec.1`](../man/en/man1/javaspec.1) |
| Italiano | [`it/Home.md`](it/Home.md) | [`../man/it/man1/javaspec.1`](../man/it/man1/javaspec.1) |
| Español | [`es/Home.md`](es/Home.md) | [`../man/es/man1/javaspec.1`](../man/es/man1/javaspec.1) |
| Deutsch | [`de/Home.md`](de/Home.md) | [`../man/de/man1/javaspec.1`](../man/de/man1/javaspec.1) |
| Français | [`fr/Home.md`](fr/Home.md) | [`../man/fr/man1/javaspec.1`](../man/fr/man1/javaspec.1) |
| 简体中文 | [`ch/Home.md`](ch/Home.md) | [`../man/ch/man1/javaspec.1`](../man/ch/man1/javaspec.1) |

The localized editions intentionally stay concise instead of duplicating the entire long-form API
reference. Command names, option names, coordinates, defaults, safety rules, exit codes, and release
availability are normative and validated across every edition. Detailed API/SPI contracts remain in
English under [`../`](../README.md).

Run the documentation guards from the repository root:

```sh
scripts/check-usermanuals.sh
scripts/check-man-pages.sh
scripts/check-current-docs.sh
```

When the standard CLI behavior changes, update the detailed English manual, all five concise
translations, all six manual pages, and the shared token guards in the same commit. The generated
project-native executable has its own intentionally narrower option set in
[`../native-image.md`](../native-image.md); native-only changes update all user-manual editions but do
not add unsupported options to `javaspec(1)`.
