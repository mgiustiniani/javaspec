#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"

status=0

extract_maven_project_version() {
  local file="$1"
  awk '
    /^[[:space:]]*<version>[[:space:]]*[^<]+[[:space:]]*<\/version>[[:space:]]*$/ {
      line = $0
      sub(/^[[:space:]]*<version>[[:space:]]*/, "", line)
      sub(/[[:space:]]*<\/version>[[:space:]]*$/, "", line)
      print line
      exit
    }
  ' "$file"
}

extract_gradle_assignment() {
  local key="$1"
  local file="$2"
  awk -v key="$key" '
    {
      line = $0
      sub(/^[[:space:]]+/, "", line)
      if (line ~ "^" key "[[:space:]]*=") {
        sub("^" key "[[:space:]]*=[[:space:]]*", "", line)
        sub(/[[:space:]]*$/, "", line)
        if (substr(line, 1, 1) == "'\''") {
          sub(/^'\''/, "", line)
          sub(/'\''.*/, "", line)
        } else if (substr(line, 1, 1) == "\"") {
          sub(/^"/, "", line)
          sub(/".*/, "", line)
        }
        print line
        exit
      }
    }
  ' "$file"
}

record_result() {
  local label="$1"
  local actual="$2"
  local expected="$3"

  if [ -z "$actual" ]; then
    printf 'FAIL: %s was not found\n' "$label"
    status=1
  elif [ "$actual" = "$expected" ]; then
    printf 'PASS: %s = %s\n' "$label" "$actual"
  else
    printf 'FAIL: %s = %s (expected %s)\n' "$label" "$actual" "$expected"
    status=1
  fi
}

root_pom="${repo_root}/pom.xml"
maven_plugin_pom="${repo_root}/javaspec-maven-plugin/pom.xml"
junit_engine_pom="${repo_root}/javaspec-junit-platform-engine/pom.xml"
gradle_build="${repo_root}/javaspec-gradle-plugin/build.gradle"

root_version="$(extract_maven_project_version "$root_pom")"

if [ -z "$root_version" ]; then
  printf 'FAIL: root pom.xml project version was not found\n'
  exit 1
fi

printf 'Version alignment baseline: %s\n' "$root_version"
record_result 'root pom.xml project version' "$root_version" "$root_version"
record_result 'javaspec-maven-plugin/pom.xml project version' "$(extract_maven_project_version "$maven_plugin_pom")" "$root_version"
record_result 'javaspec-junit-platform-engine/pom.xml project version' "$(extract_maven_project_version "$junit_engine_pom")" "$root_version"
record_result 'javaspec-gradle-plugin/build.gradle version' "$(extract_gradle_assignment version "$gradle_build")" "$root_version"
record_result 'javaspec-gradle-plugin/build.gradle javaspecCoreVersion' "$(extract_gradle_assignment javaspecCoreVersion "$gradle_build")" "$root_version"

if [ "$status" -eq 0 ]; then
  printf 'PASS: all checked project versions are aligned.\n'
else
  printf 'FAIL: project version alignment check failed.\n'
fi

exit "$status"
