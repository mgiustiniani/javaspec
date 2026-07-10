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

root_version="$(awk '
  /^[[:space:]]*<version>[[:space:]]*[^<]+[[:space:]]*<\/version>[[:space:]]*$/ {
    line = $0
    sub(/^[[:space:]]*<version>[[:space:]]*/, "", line)
    sub(/[[:space:]]*<\/version>[[:space:]]*$/, "", line)
    print line
    exit
  }
' pom.xml)"

case "$root_version" in
  1.0.0-SNAPSHOT|1.0.0-RC[0-9]*|1.0.0)
    pass "release-line version is $root_version"
    ;;
  *)
    fail "release-line version must be 1.0.0-SNAPSHOT, 1.0.0-RC<N>, or 1.0.0; got '$root_version'"
    ;;
esac

if [ -f docs/release-notes-1.0.0.md ]; then
  pass "docs/release-notes-1.0.0.md exists"
else
  fail "docs/release-notes-1.0.0.md is missing"
fi

if [ -f docs/phpspec-compatibility-matrix.md ]; then
  pass "docs/phpspec-compatibility-matrix.md exists"
else
  fail "docs/phpspec-compatibility-matrix.md is missing"
fi

if [ -f docs/extension-spi-1.0.md ]; then
  pass "docs/extension-spi-1.0.md exists"
else
  fail "docs/extension-spi-1.0.md is missing"
fi

if [ -f docs/example-data-contract-1.0.md ]; then
  pass "docs/example-data-contract-1.0.md exists"
else
  fail "docs/example-data-contract-1.0.md is missing"
fi

if [ -f docs/matcher-contract-1.0.md ]; then
  pass "docs/matcher-contract-1.0.md exists"
else
  fail "docs/matcher-contract-1.0.md is missing"
fi

required_current_docs=(
  docs/junit-platform-contract-1.0.md
  docs/java-compatibility-1.0.md
  docs/migration-guide-1.0.md
  docs/junit-to-javaspec-guide.md
  docs/cucumber-boundary.md
  docs/troubleshooting.md
  docs/compatibility-policy-1.0.md
)
for required_doc in "${required_current_docs[@]}"; do
  if [ -f "$required_doc" ]; then
    pass "$required_doc exists"
  else
    fail "$required_doc is missing"
  fi
done

if [ -e docs/release-notes-0.1.1-SNAPSHOT.md ]; then
  fail "obsolete docs/release-notes-0.1.1-SNAPSHOT.md still exists"
else
  pass "obsolete 0.1.1 release-notes file is absent"
fi

current_paths=(
  PLAN.md
  README.md
  RELEASING.md
  docs/CAPABILITIES.md
  docs/release-1.0-audit.md
  docs/release-1.0-checklist.md
  docs/release-notes-1.0.0.md
  docs/extension-spi-1.0.md
  docs/example-data-contract-1.0.md
  docs/matcher-contract-1.0.md
  docs/junit-platform-contract-1.0.md
  docs/java-compatibility-1.0.md
  docs/migration-guide-1.0.md
  docs/junit-to-javaspec-guide.md
  docs/cucumber-boundary.md
  docs/troubleshooting.md
  docs/compatibility-policy-1.0.md
  docs/usermanual
  docs/arc42
  docs/bytecode-doubles.md
  examples
  javaspec-gradle-plugin
  javaspec-maven-plugin
  javaspec-junit-platform-engine
  javaspec-bytecode-doubles
  javaspec-bytecode-agent
  scripts
  .github
)

stale_version_hits="$(rg -n '0\.1\.1|release-notes-0\.1\.1|0\.1\.0-SNAPSHOT|<version>0\.1\.0</version>' "${current_paths[@]}" \
  -g '!**/target/**' -g '!**/build/**' -g '!scripts/check-current-docs.sh' 2>/dev/null || true)"
if [ -n "$stale_version_hits" ]; then
  printf '%s\n' "$stale_version_hits"
  fail "current docs/config still contain stale pre-1.0 snapshot references"
else
  pass "no stale pre-1.0 snapshot references in current docs/config"
fi

obsolete_package_hits="$(rg -n 'org\.javaspec' "${current_paths[@]}" \
  -g '!**/target/**' -g '!**/build/**' -g '!scripts/check-current-docs.sh' 2>/dev/null || true)"
if [ -n "$obsolete_package_hits" ]; then
  printf '%s\n' "$obsolete_package_hits"
  fail "current docs/config still contain obsolete org.javaspec references"
else
  pass "no obsolete org.javaspec references in current docs/config"
fi

if [ -f docs/phpspec-compatibility-matrix.md ]; then
  unspecified_hits="$(rg -n '^\|.*\| UNSPECIFIED \|' docs/phpspec-compatibility-matrix.md 2>/dev/null || true)"
  if [ -n "$unspecified_hits" ]; then
    printf '%s\n' "$unspecified_hits"
    fail "PHPSpec compatibility matrix still has UNSPECIFIED entries"
  else
    pass "PHPSpec compatibility matrix has no UNSPECIFIED entries"
  fi
fi

if [ "$status" -eq 0 ]; then
  printf 'PASS: current documentation/version checks passed.\n'
else
  printf 'FAIL: current documentation/version checks failed.\n'
fi

exit "$status"
