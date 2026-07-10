#!/usr/bin/env bash
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

DOC="docs/api-surface-1.0.md"
status=0

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  status=1
}

pass() {
  printf 'PASS: %s\n' "$1"
}

if [ ! -f "$DOC" ]; then
  fail "$DOC is missing"
  exit 1
fi

SOURCE_DIRS="src/main/java javaspec-maven-plugin/src/main/java javaspec-junit-platform-engine/src/main/java javaspec-bytecode-doubles/src/main/java javaspec-bytecode-agent/src/main/java javaspec-gradle-plugin/src/main/java"

packages="$(find $SOURCE_DIRS -name '*.java' -print \
  | xargs rg -n '^package ' \
  | sed 's/.*package //;s/;//' \
  | sort -u)"

missing=""
for package_name in $packages; do
  if ! rg -q "\`$package_name\`" "$DOC"; then
    missing="$missing $package_name"
  fi
done

if [ -n "$missing" ]; then
  for package_name in $missing; do
    fail "API surface classification missing for package: $package_name"
  done
else
  pass "all shipped Java packages are classified in $DOC"
fi

for term in PUBLIC_API PUBLIC_SPI ADAPTER_API GENERATED_API INTERNAL; do
  if rg -q "$term" "$DOC"; then
    pass "classification term present: $term"
  else
    fail "classification term missing: $term"
  fi
done

if rg -q '1.0 baseline procedure' "$DOC"; then
  pass "1.0 baseline procedure documented"
else
  fail "1.0 baseline procedure missing"
fi

if [ "$status" -eq 0 ]; then
  printf 'PASS: API surface classification checks passed.\n'
else
  printf 'FAIL: API surface classification checks failed.\n' >&2
fi

exit "$status"
