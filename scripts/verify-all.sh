#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"

MAVEN_BIN="${MAVEN_BIN:-mvn}"

section() {
  printf '\n==> %s\n' "$1"
}

quote_cmd() {
  printf ' %q' "$@"
}

run_at_root() {
  section "$1"
  shift
  printf '+ cd %q &&' "${repo_root}"
  quote_cmd "$@"
  printf '\n'
  (cd "${repo_root}" && "$@")
}

run_gradle_plugin() {
  section "$1"
  shift
  printf '+ cd %q &&' "${repo_root}/javaspec-gradle-plugin"
  quote_cmd "${gradle_cmd[@]}" "$@"
  printf '\n'
  (cd "${repo_root}/javaspec-gradle-plugin" && "${gradle_cmd[@]}" "$@")
}

resolve_gradle_cmd() {
  if [ "${JAVASPEC_SKIP_GRADLE:-0}" = "1" ]; then
    return 0
  fi

  if [ -n "${JAVASPEC_GRADLE_BIN:-}" ]; then
    gradle_cmd=("${JAVASPEC_GRADLE_BIN}")
  elif [ -x "${repo_root}/gradlew" ]; then
    gradle_cmd=("${repo_root}/gradlew")
  elif [ -x "/tmp/gradle-8.8/bin/gradle" ]; then
    gradle_cmd=("/tmp/gradle-8.8/bin/gradle")
  elif command -v gradle >/dev/null 2>&1; then
    gradle_cmd=("gradle")
  else
    printf 'ERROR: Gradle was not found. Set JAVASPEC_GRADLE_BIN to a Gradle executable or set JAVASPEC_SKIP_GRADLE=1 to skip Gradle adapter verification.\n' >&2
    exit 1
  fi
}

gradle_cmd=()

run_at_root "Version alignment check" "${repo_root}/scripts/check-version-alignment.sh"
run_at_root "Root core verify" "${MAVEN_BIN}" -q verify
run_at_root "Root runtime dependency tree audit" "${MAVEN_BIN}" dependency:tree -Dscope=runtime
run_at_root "Install root core snapshot" "${MAVEN_BIN}" -q -DskipTests install
run_at_root "Maven plugin verify" "${MAVEN_BIN}" -q -f javaspec-maven-plugin/pom.xml verify
run_at_root "Maven plugin runtime dependency tree audit" "${MAVEN_BIN}" -f javaspec-maven-plugin/pom.xml dependency:tree -Dscope=runtime
run_at_root "JUnit Platform engine verify" "${MAVEN_BIN}" -q -f javaspec-junit-platform-engine/pom.xml verify
run_at_root "JUnit Platform engine runtime dependency tree audit" "${MAVEN_BIN}" -f javaspec-junit-platform-engine/pom.xml dependency:tree -Dscope=runtime

if [ "${JAVASPEC_SKIP_GRADLE:-0}" = "1" ]; then
  printf '\nWARNING: Skipping Gradle adapter verification because JAVASPEC_SKIP_GRADLE=1.\n'
else
  resolve_gradle_cmd
  run_gradle_plugin "Gradle plugin clean test build" clean test build
  run_gradle_plugin "Gradle plugin runtime dependency tree audit" dependencies --configuration runtimeClasspath
fi
