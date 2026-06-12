#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"

MAVEN_BIN="${MAVEN_BIN:-mvn}"
gradle_cmd=()

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

resolve_gradle_cmd() {
  if [ "${JAVASPEC_SKIP_GRADLE_EXAMPLE:-0}" = "1" ]; then
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
    printf 'ERROR: Gradle was not found. Set JAVASPEC_GRADLE_BIN to a Gradle executable or set JAVASPEC_SKIP_GRADLE_EXAMPLE=1 to skip the Gradle example.\n' >&2
    exit 1
  fi
}

assert_file_exists() {
  local label="$1"
  local path="$2"
  if [ ! -f "$path" ]; then
    printf 'ERROR: %s was not created: %s\n' "$label" "$path" >&2
    exit 1
  fi
  printf 'PASS: %s exists: %s\n' "$label" "$path"
}

assert_file_contains() {
  local label="$1"
  local path="$2"
  local marker="$3"
  if ! grep -F -- "$marker" "$path" >/dev/null 2>&1; then
    printf 'ERROR: %s is missing marker %q in %s\n' "$label" "$marker" "$path" >&2
    exit 1
  fi
  printf 'PASS: %s contains marker: %s\n' "$label" "$marker"
}

assert_tree_contains() {
  local label="$1"
  local directory="$2"
  local marker="$3"
  if [ ! -d "$directory" ]; then
    printf 'ERROR: %s directory was not created: %s\n' "$label" "$directory" >&2
    exit 1
  fi
  if ! grep -R -F -- "$marker" "$directory" >/dev/null 2>&1; then
    printf 'ERROR: %s is missing marker %q under %s\n' "$label" "$marker" "$directory" >&2
    exit 1
  fi
  printf 'PASS: %s contains marker under %s: %s\n' "$label" "$directory" "$marker"
}

remove_path() {
  local label="$1"
  shift
  section "$label"
  printf '+ cd %q &&' "${repo_root}"
  quote_cmd rm -rf "$@"
  printf '\n'
  (cd "${repo_root}" && rm -rf "$@")
}

run_at_root "Install root core snapshot for examples" "${MAVEN_BIN}" -q -DskipTests install
run_at_root "Install Maven plugin snapshot for examples" "${MAVEN_BIN}" -q -f javaspec-maven-plugin/pom.xml -DskipTests install
run_at_root "Install bytecode doubles adapter snapshot for examples" "${MAVEN_BIN}" -q -f javaspec-bytecode-doubles/pom.xml -DskipTests install
run_at_root "Install JUnit Platform engine snapshot for examples" "${MAVEN_BIN}" -q -f javaspec-junit-platform-engine/pom.xml -DskipTests install

remove_path "Clean previous Maven example reports" examples/maven-basic/target/javaspec
run_at_root "Verify Maven plugin basic example" "${MAVEN_BIN}" -q -f examples/maven-basic/pom.xml verify
maven_json="${repo_root}/examples/maven-basic/target/javaspec/run-report.json"
maven_xml="${repo_root}/examples/maven-basic/target/javaspec/junit-report.xml"
assert_file_exists "Maven JSON report" "$maven_json"
assert_file_exists "Maven JUnit XML report" "$maven_xml"
assert_file_contains "Maven JSON report" "$maven_json" '"schemaVersion": 1'
assert_file_contains "Maven JSON report" "$maven_json" '"metadata": {'
assert_file_contains "Maven JSON report" "$maven_json" '"time": 0'
assert_file_contains "Maven JSON report" "$maven_json" '"javaspec.report.schemaVersion": "1"'
assert_file_contains "Maven JSON report" "$maven_json" '"javaspec.report.tool": "javaspec"'
assert_file_contains "Maven JSON report" "$maven_json" '"stableId": "spec.com.example.CalculatorSpec#it_adds_two_numbers"'
assert_file_contains "Maven JSON report" "$maven_json" '"status": "PASSED"'
assert_file_contains "Maven JSON report" "$maven_json" '"line": 11'
assert_file_contains "Maven JUnit XML report" "$maven_xml" '<testsuite name="javaspec" tests="1" failures="0" errors="0" skipped="0" timestamp="'
assert_file_contains "Maven JUnit XML report" "$maven_xml" '" time="0">'
assert_file_contains "Maven JUnit XML report" "$maven_xml" '<properties>'
assert_file_contains "Maven JUnit XML report" "$maven_xml" '<property name="javaspec.report.schemaVersion" value="1"/>'
assert_file_contains "Maven JUnit XML report" "$maven_xml" '<property name="javaspec.report.tool" value="javaspec"/>'
assert_file_contains "Maven JUnit XML report" "$maven_xml" 'classname="spec.com.example.CalculatorSpec"'
assert_file_contains "Maven JUnit XML report" "$maven_xml" 'name="it_adds_two_numbers"'
assert_file_contains "Maven JUnit XML report" "$maven_xml" 'line="11"'

remove_path "Clean previous prophecy basic example reports" examples/prophecy-basic/target/javaspec
run_at_root "Verify prophecy basic example" "${MAVEN_BIN}" -q -f examples/prophecy-basic/pom.xml verify
prophecy_json="${repo_root}/examples/prophecy-basic/target/javaspec/run-report.json"
prophecy_xml="${repo_root}/examples/prophecy-basic/target/javaspec/junit-report.xml"
assert_file_exists "Prophecy JSON report" "$prophecy_json"
assert_file_exists "Prophecy JUnit XML report" "$prophecy_xml"
assert_file_contains "Prophecy JSON report" "$prophecy_json" '"schemaVersion": 1'
assert_file_contains "Prophecy JSON report" "$prophecy_json" '"status": "PASSED"'
assert_file_contains "Prophecy JSON report" "$prophecy_json" '"stableId": "spec.com.example.MailerSpec'
assert_file_contains "Prophecy JUnit XML report" "$prophecy_xml" '<testsuite name="javaspec"'
assert_file_contains "Prophecy JUnit XML report" "$prophecy_xml" 'classname="spec.com.example.MailerSpec"'

if [ "${JAVASPEC_SKIP_BYTECODE_DOUBLES_EXAMPLE:-0}" = "1" ]; then
  printf '\nWARNING: Skipping bytecode doubles basic example because JAVASPEC_SKIP_BYTECODE_DOUBLES_EXAMPLE=1.\n'
else
  remove_path "Clean previous bytecode doubles example reports" examples/bytecode-doubles-basic/target/javaspec
  run_at_root "Verify bytecode doubles basic example" "${MAVEN_BIN}" -q -f examples/bytecode-doubles-basic/pom.xml verify
  bytecode_json="${repo_root}/examples/bytecode-doubles-basic/target/javaspec/run-report.json"
  bytecode_xml="${repo_root}/examples/bytecode-doubles-basic/target/javaspec/junit-report.xml"
  assert_file_exists "Bytecode doubles JSON report" "$bytecode_json"
  assert_file_exists "Bytecode doubles JUnit XML report" "$bytecode_xml"
  assert_file_contains "Bytecode doubles JSON report" "$bytecode_json" '"schemaVersion": 1'
  assert_file_contains "Bytecode doubles JSON report" "$bytecode_json" '"metadata": {'
  assert_file_contains "Bytecode doubles JSON report" "$bytecode_json" '"stableId": "spec.com.example.DataServiceSpec#it_saves_data_using_the_store"'
  assert_file_contains "Bytecode doubles JSON report" "$bytecode_json" '"stableId": "spec.com.example.DataServiceSpec#it_returns_not_found_when_store_has_no_entry"'
  assert_file_contains "Bytecode doubles JSON report" "$bytecode_json" '"status": "PASSED"'
  assert_file_contains "Bytecode doubles JSON report" "$bytecode_json" '"line": 14'
  assert_file_contains "Bytecode doubles JSON report" "$bytecode_json" '"line": 21'
  assert_file_contains "Bytecode doubles JUnit XML report" "$bytecode_xml" '<testsuite name="javaspec" tests="2" failures="0" errors="0" skipped="0" timestamp="'
  assert_file_contains "Bytecode doubles JUnit XML report" "$bytecode_xml" 'classname="spec.com.example.DataServiceSpec"'
  assert_file_contains "Bytecode doubles JUnit XML report" "$bytecode_xml" 'name="it_saves_data_using_the_store"'
  assert_file_contains "Bytecode doubles JUnit XML report" "$bytecode_xml" 'name="it_returns_not_found_when_store_has_no_entry"'
  assert_file_contains "Bytecode doubles JUnit XML report" "$bytecode_xml" 'line="14"'
  assert_file_contains "Bytecode doubles JUnit XML report" "$bytecode_xml" 'line="21"'
fi

remove_path "Clean previous JUnit Platform example reports" examples/junit-platform-basic/target/surefire-reports
run_at_root "Verify JUnit Platform engine basic example" "${MAVEN_BIN}" -q -f examples/junit-platform-basic/pom.xml test
junit_reports="${repo_root}/examples/junit-platform-basic/target/surefire-reports"
assert_tree_contains "JUnit Platform Surefire reports" "$junit_reports" 'spec.com.example.CalculatorSpec'
assert_tree_contains "JUnit Platform Surefire reports" "$junit_reports" 'it_adds_two_numbers'

if [ "${JAVASPEC_SKIP_GRADLE_EXAMPLE:-0}" = "1" ]; then
  printf '\nWARNING: Skipping Gradle basic example because JAVASPEC_SKIP_GRADLE_EXAMPLE=1.\n'
else
  resolve_gradle_cmd
  remove_path "Clean previous Gradle example reports" examples/gradle-basic/build/reports/javaspec
  run_at_root "Verify Gradle plugin basic example" "${gradle_cmd[@]}" -p examples/gradle-basic clean javaspecRun
  gradle_json="${repo_root}/examples/gradle-basic/build/reports/javaspec/run-report.json"
  gradle_xml="${repo_root}/examples/gradle-basic/build/reports/javaspec/junit-report.xml"
  assert_file_exists "Gradle JSON report" "$gradle_json"
  assert_file_exists "Gradle JUnit XML report" "$gradle_xml"
  assert_file_contains "Gradle JSON report" "$gradle_json" '"schemaVersion": 1'
  assert_file_contains "Gradle JSON report" "$gradle_json" '"metadata": {'
  assert_file_contains "Gradle JSON report" "$gradle_json" '"time": 0'
  assert_file_contains "Gradle JSON report" "$gradle_json" '"javaspec.report.schemaVersion": "1"'
  assert_file_contains "Gradle JSON report" "$gradle_json" '"javaspec.report.tool": "javaspec"'
  assert_file_contains "Gradle JSON report" "$gradle_json" '"stableId": "spec.com.example.CalculatorSpec#it_adds_two_numbers"'
  assert_file_contains "Gradle JSON report" "$gradle_json" '"status": "PASSED"'
  assert_file_contains "Gradle JSON report" "$gradle_json" '"line": 11'
  assert_file_contains "Gradle JUnit XML report" "$gradle_xml" '<testsuite name="javaspec" tests="1" failures="0" errors="0" skipped="0" timestamp="'
  assert_file_contains "Gradle JUnit XML report" "$gradle_xml" '" time="0">'
  assert_file_contains "Gradle JUnit XML report" "$gradle_xml" '<properties>'
  assert_file_contains "Gradle JUnit XML report" "$gradle_xml" '<property name="javaspec.report.schemaVersion" value="1"/>'
  assert_file_contains "Gradle JUnit XML report" "$gradle_xml" '<property name="javaspec.report.tool" value="javaspec"/>'
  assert_file_contains "Gradle JUnit XML report" "$gradle_xml" 'classname="spec.com.example.CalculatorSpec"'
  assert_file_contains "Gradle JUnit XML report" "$gradle_xml" 'name="it_adds_two_numbers"'
  assert_file_contains "Gradle JUnit XML report" "$gradle_xml" 'line="11"'
fi

printf '\nPASS: standalone examples verification completed.\n'
