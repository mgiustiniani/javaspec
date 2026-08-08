#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"
cd "$repo_root"

status=0
languages=(en it es de fr ch)
required_tokens=(
  '.TH JAVASPEC 1'
  'javaspec describe'
  'javaspec run'
  'javaspec prophesize'
  'javaspec list-extensions'
  '\-\-launcher-version'
  '\-\-launcher-fingerprint'
  '\-\-resolve-pom'
  '\-\-release'
  '\-\-generation-report'
  '// javaspec:stub'
)

for language in "${languages[@]}"; do
  page="docs/man/${language}/man1/javaspec.1"
  if [ ! -s "$page" ]; then
    printf 'FAIL: manual page is missing or empty: %s\n' "$page"
    status=1
    continue
  fi

  for token in "${required_tokens[@]}"; do
    if ! grep -Fq -- "$token" "$page"; then
      printf 'FAIL: %s is missing contract token: %s\n' "$page" "$token"
      status=1
    fi
  done

  if command -v groff >/dev/null 2>&1; then
    if LC_ALL=C.UTF-8 groff -Kutf8 -man -Tutf8 "$page" >/dev/null; then
      printf 'PASS: rendered manual page: %s\n' "$page"
    else
      printf 'FAIL: groff could not render manual page: %s\n' "$page"
      status=1
    fi
  else
    printf 'SKIP: groff is unavailable; token checks passed for %s\n' "$page"
  fi
done

if [ "$status" -eq 0 ]; then
  printf 'PASS: multilingual manual pages are complete and aligned.\n'
else
  printf 'FAIL: multilingual manual-page checks failed.\n'
fi

exit "$status"
