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
  docs/release-1.0-rc-evidence.md
  docs/README.md
  docs/usermanual/README.md
  docs/agent/javaspec-guided-development-assistant.md
  docs/native-image.md
  docs/adr/0027-standalone-bytecode-agent-adapter.md
  docs/adr/0028-project-specific-native-image-preview.md
  docs/arc42/README.md
  docs/man/README.md
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

versioned_user_docs=(
  README.md
  docs/CAPABILITIES.md
  docs/usermanual/Home.md
  docs/usermanual/it/Home.md
  docs/usermanual/es/Home.md
  docs/usermanual/de/Home.md
  docs/usermanual/fr/Home.md
  docs/usermanual/ch/Home.md
  docs/migration-guide-1.0.md
  docs/native-image.md
  docs/bytecode-doubles.md
  javaspec-gradle-plugin/README.md
  javaspec-junit-platform-engine/README.md
)
if [[ "$root_version" == 1.0.0-RC* ]]; then
  stale_rc_hits="$(grep -n -H -E '1\.0\.0-RC[0-9]+' "${versioned_user_docs[@]}" \
    | grep -v "1.0.0-RC${root_version##*RC}" || true)"
  if [ -n "$stale_rc_hits" ]; then
    printf '%s\n' "$stale_rc_hits"
    fail "current user documentation contains release candidates other than $root_version"
  else
    pass "current user documentation uses release candidate $root_version"
  fi
fi

for required_token in '--generation-report' 'PROPOSED' 'appliedWrites'; do
  if grep -q -- "$required_token" README.md docs/usermanual/Home.md; then
    pass "current user documentation contains $required_token"
  else
    fail "current user documentation is missing $required_token"
  fi
done

for native_token in \
  'javaspec:native-prepare' \
  'NativeImageLauncher' \
  'project-specific' \
  'after immutable tag `v1.0.0-RC5`' \
  'No runtime source discovery'; do
  if grep -Fq -- "$native_token" docs/native-image.md; then
    pass "native preview documentation contains $native_token"
  else
    fail "native preview documentation is missing $native_token"
  fi
done

if grep -Eq 'RC5 (includes|provides|ships).*native|native.*(available|published).*RC5' docs/native-image.md README.md; then
  fail "documentation incorrectly attributes the post-RC5 native preview to published RC5"
else
  pass "documentation separates the native preview from published RC5"
fi

if python3 - <<'PY'
from pathlib import Path
import re
import sys

text = Path("README.md").read_text(encoding="utf-8")
lines = text.splitlines()
in_java_fence = False
current_heading = ""
fallback_heading = "### Explicit matcher fallback (advanced)"
foreign = re.compile(
    r"\b(?:assertEquals|assertTrue|assertFalse|assertNull|assertNotNull|assertSame|assertThrows|assertThat)\s*\("
    r"|\bAssertions\."
    r"|^\s*import\s+(?:static\s+)?(?:org\.junit|org\.testng|org\.assertj|org\.hamcrest)"
)
hits = []
for number, line in enumerate(lines, 1):
    stripped = line.strip()
    if not in_java_fence and stripped.startswith("#"):
        current_heading = stripped
    if stripped == "```java":
        in_java_fence = True
        continue
    if in_java_fence and stripped == "```":
        in_java_fence = False
        continue
    if not in_java_fence:
        continue
    if foreign.search(line):
        hits.append((number, "foreign assertion syntax", stripped))
    if "match(" in line and current_heading != fallback_heading:
        hits.append((number, "non-canonical match(...) outside the advanced fallback", stripped))

for required in (
    "total(10.0, 2.5).shouldReturn(12.5);",
    "add(2, 3).shouldReturn(5);",
    "normalize(input).shouldReturn(expected)",
):
    if required not in text:
        hits.append((0, "missing canonical generated-proxy example", required))

for number, reason, line in hits:
    location = f"README.md:{number}" if number else "README.md"
    print(f"{location}: {reason}: {line}")
sys.exit(1 if hits else 0)
PY
then
  pass "README Java examples use canonical generated proxies outside the explicit advanced fallback"
else
  fail "README Java examples contain non-canonical or external assertion syntax"
fi

if grep -q '(migration-guide.md)' docs/bytecode-doubles.md; then
  fail "docs/bytecode-doubles.md links to obsolete migration-guide.md"
else
  pass "bytecode doubles guide links to the current migration guide"
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
  docs/release-1.0-rc-evidence.md
  docs/usermanual
  docs/man
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

stale_version_hits="$(grep -R -n -E \
  --exclude='check-current-docs.sh' --exclude-dir=target --exclude-dir=build \
  '0\.1\.1|release-notes-0\.1\.1|0\.1\.0-SNAPSHOT|<version>0\.1\.0</version>' \
  "${current_paths[@]}" 2>/dev/null || true)"
if [ -n "$stale_version_hits" ]; then
  printf '%s\n' "$stale_version_hits"
  fail "current docs/config still contain stale pre-1.0 snapshot references"
else
  pass "no stale pre-1.0 snapshot references in current docs/config"
fi

obsolete_package_hits="$(grep -R -n -E \
  --exclude='check-current-docs.sh' --exclude-dir=target --exclude-dir=build \
  'org\.javaspec' "${current_paths[@]}" 2>/dev/null || true)"
if [ -n "$obsolete_package_hits" ]; then
  printf '%s\n' "$obsolete_package_hits"
  fail "current docs/config still contain obsolete org.javaspec references"
else
  pass "no obsolete org.javaspec references in current docs/config"
fi

stale_portal_claims="$(grep -R -n -E \
  --exclude='check-current-docs.sh' --exclude-dir=target --exclude-dir=build --exclude-dir=history \
  'published on the Gradle Plugin Portal|Gradle plugin is published on the Gradle' \
  README.md CHANGELOG.md docs examples javaspec-gradle-plugin javaspec-junit-platform-engine 2>/dev/null || true)"
if [ -n "$stale_portal_claims" ]; then
  printf '%s\n' "$stale_portal_claims"
  fail "documentation claims Gradle Portal publication before marker verification"
else
  pass "documentation distinguishes Gradle submission from Portal availability"
fi

if scripts/check-doc-links.sh; then
  pass "local documentation links resolve"
else
  fail "local documentation link guard failed"
fi

if scripts/check-usermanuals.sh; then
  pass "multilingual user-manual guard passed"
else
  fail "multilingual user-manual guard failed"
fi

if scripts/check-man-pages.sh; then
  pass "multilingual manual-page guard passed"
else
  fail "multilingual manual-page guard failed"
fi

agent_doc=docs/agent/javaspec-guided-development-assistant.md
for agent_token in \
  'name: javaspec-spec-driven' \
  'Prompt version: `javaspec-example-spec-driven-v2`' \
  'bin/javaspec --launcher-fingerprint' \
  'FRAMEWORK_INCOHERENCE' \
  'semantic'; do
  if grep -Fq -- "$agent_token" "$agent_doc"; then
    pass "example agent contains $agent_token"
  else
    fail "example agent is missing $agent_token"
  fi
done

if [ -f docs/phpspec-compatibility-matrix.md ]; then
  unspecified_hits="$(grep -n -E '^\|.*\| UNSPECIFIED \|' docs/phpspec-compatibility-matrix.md 2>/dev/null || true)"
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
