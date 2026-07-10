#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"
cd "$repo_root"

status=0

fail() {
  printf 'FAIL: %s\n' "$1"
  status=1
}

pass() {
  printf 'PASS: %s\n' "$1"
}

extract_maven_project_version() {
  awk '
    /^[[:space:]]*<version>[[:space:]]*[^<]+[[:space:]]*<\/version>[[:space:]]*$/ {
      line = $0
      sub(/^[[:space:]]*<version>[[:space:]]*/, "", line)
      sub(/[[:space:]]*<\/version>[[:space:]]*$/, "", line)
      print line
      exit
    }
  ' pom.xml
}

root_version="$(extract_maven_project_version)"
if [ -z "$root_version" ]; then
  fail "root pom.xml project version was not found"
elif [[ "$root_version" =~ ^1\.0\.0(-RC[0-9]+)?$ ]]; then
  pass "release version is $root_version"
else
  fail "release version must be 1.0.0-RC<N> or 1.0.0; got '$root_version'"
fi

expected_tag="v${root_version}"
actual_tag="${JAVASPEC_RELEASE_TAG:-${GITHUB_REF_NAME:-}}"
if [ -n "$actual_tag" ]; then
  if [ "$actual_tag" = "$expected_tag" ]; then
    pass "release tag matches version: $actual_tag"
  else
    fail "release tag '$actual_tag' does not match expected tag '$expected_tag'"
  fi
else
  fail "release tag was not provided; set JAVASPEC_RELEASE_TAG or run from a GitHub tag context"
fi

snapshot_hits="$(find . \
  -path './.git' -prune -o \
  -path './target' -prune -o \
  -path './*/target' -prune -o \
  -path './*/build' -prune -o \
  \( -name 'pom.xml' -o -name 'build.gradle' -o -name 'settings.gradle' \) \
  -print0 | xargs -0 rg -n 'SNAPSHOT' 2>/dev/null || true)"

if [ -n "$snapshot_hits" ]; then
  printf '%s\n' "$snapshot_hits"
  fail "SNAPSHOT references remain in release build files"
else
  pass "no SNAPSHOT references in release build files"
fi

if [ "$status" -eq 0 ]; then
  printf 'PASS: release preflight checks passed.\n'
else
  printf 'FAIL: release preflight checks failed.\n'
fi

exit "$status"
