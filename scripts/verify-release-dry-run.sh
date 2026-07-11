#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"
cd "$repo_root"

MAVEN_BIN="${MAVEN_BIN:-mvn}"

gradle_cmd=()
if [ -n "${JAVASPEC_GRADLE_BIN:-}" ]; then
  gradle_cmd=("${JAVASPEC_GRADLE_BIN}")
elif [ -x "${repo_root}/gradlew" ]; then
  gradle_cmd=("${repo_root}/gradlew")
elif [ -x "/tmp/gradle-8.8/bin/gradle" ]; then
  gradle_cmd=("/tmp/gradle-8.8/bin/gradle")
elif command -v gradle >/dev/null 2>&1; then
  gradle_cmd=("gradle")
else
  printf 'ERROR: Gradle was not found. Set JAVASPEC_GRADLE_BIN to a Gradle executable.\n' >&2
  exit 1
fi

version="$(python - <<'PY'
import xml.etree.ElementTree as ET
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
root = ET.parse('pom.xml').getroot()
version = root.find('m:version', ns)
print(version.text.strip())
PY
)"

section() { printf '\n==> %s\n' "$1"; }
pass() { printf 'PASS: %s\n' "$1"; }
fail() { printf 'FAIL: %s\n' "$1" >&2; exit 1; }

artifact_files=()

require_file() {
  if [ ! -f "$1" ]; then
    printf 'Artifact directory contents for diagnostics:\n' >&2
    find "$(dirname "$1")" -maxdepth 1 -type f -printf '%f\n' 2>/dev/null | sort >&2 || true
    fail "missing expected artifact: $1"
  fi
  artifact_files+=("$1")
  pass "artifact exists: $1"
}

generate_and_verify_checksums() {
  if ! command -v sha256sum >/dev/null 2>&1; then
    fail "sha256sum is required for release artifact checksum verification"
  fi
  local checksum_file="target/release-dry-run-checksums.sha256"
  section "Generate and verify release artifact checksums"
  : > "$checksum_file"
  local artifact
  for artifact in "${artifact_files[@]}"; do
    sha256sum "$artifact" >> "$checksum_file"
  done
  sha256sum -c "$checksum_file"
  pass "release artifact checksums generated and verified: $checksum_file"
}

package_maven_module() {
  local label="$1"
  local pom="$2"
  section "Package $label release artifacts"
  "$MAVEN_BIN" -q -f "$pom" -Prelease-artifacts -DskipTests package
}

scripts/check-version-alignment.sh
scripts/check-current-docs.sh
scripts/check-api-surface.sh

package_maven_module "core" "pom.xml"
section "Install core release candidate for standalone artifact builds"
"$MAVEN_BIN" -q -DskipTests install
package_maven_module "Maven plugin" "javaspec-maven-plugin/pom.xml"
package_maven_module "JUnit Platform engine" "javaspec-junit-platform-engine/pom.xml"
package_maven_module "bytecode doubles" "javaspec-bytecode-doubles/pom.xml"
package_maven_module "bytecode agent" "javaspec-bytecode-agent/pom.xml"

require_file "target/javaspec-${version}.jar"
require_file "target/javaspec-${version}-sources.jar"
require_file "target/javaspec-${version}-javadoc.jar"
require_file "javaspec-maven-plugin/target/javaspec-maven-plugin-${version}.jar"
require_file "javaspec-maven-plugin/target/javaspec-maven-plugin-${version}-sources.jar"
require_file "javaspec-maven-plugin/target/javaspec-maven-plugin-${version}-javadoc.jar"
require_file "javaspec-junit-platform-engine/target/javaspec-junit-platform-engine-${version}.jar"
require_file "javaspec-junit-platform-engine/target/javaspec-junit-platform-engine-${version}-sources.jar"
require_file "javaspec-junit-platform-engine/target/javaspec-junit-platform-engine-${version}-javadoc.jar"
require_file "javaspec-bytecode-doubles/target/javaspec-bytecode-doubles-${version}.jar"
require_file "javaspec-bytecode-doubles/target/javaspec-bytecode-doubles-${version}-sources.jar"
require_file "javaspec-bytecode-doubles/target/javaspec-bytecode-doubles-${version}-javadoc.jar"
require_file "javaspec-bytecode-agent/target/javaspec-bytecode-agent-${version}.jar"
require_file "javaspec-bytecode-agent/target/javaspec-bytecode-agent-${version}-sources.jar"
require_file "javaspec-bytecode-agent/target/javaspec-bytecode-agent-${version}-javadoc.jar"

manifest="$(mktemp)"
unzip -p "javaspec-bytecode-agent/target/javaspec-bytecode-agent-${version}.jar" META-INF/MANIFEST.MF > "$manifest"
rg -q '^Premain-Class: io.github.jvmspec.doubles.agent.JavaspecBytecodeAgent' "$manifest" || fail "bytecode agent Premain-Class missing"
rg -q '^Agent-Class: io.github.jvmspec.doubles.agent.JavaspecBytecodeAgent' "$manifest" || fail "bytecode agent Agent-Class missing"
rm -f "$manifest"
pass "bytecode agent manifest entries present"

section "Build Gradle plugin release artifacts"
(cd javaspec-gradle-plugin && "${gradle_cmd[@]}" clean test build)
require_file "javaspec-gradle-plugin/build/libs/javaspec-gradle-plugin-${version}.jar"
require_file "javaspec-gradle-plugin/build/libs/javaspec-gradle-plugin-${version}-sources.jar"
require_file "javaspec-gradle-plugin/build/libs/javaspec-gradle-plugin-${version}-javadoc.jar"

generate_and_verify_checksums

section "Run external consumer examples"
scripts/verify-examples.sh

pass "release dry-run completed for ${version}"
