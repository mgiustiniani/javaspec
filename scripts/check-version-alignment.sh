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

extract_maven_property() {
  local key="$1"
  local file="$2"
  awk -v key="$key" '
    {
      pattern = "^[[:space:]]*<" key ">[[:space:]]*[^<]+[[:space:]]*</" key ">[[:space:]]*$"
      if ($0 ~ pattern) {
        line = $0
        sub("^[[:space:]]*<" key ">[[:space:]]*", "", line)
        sub("[[:space:]]*</" key ">[[:space:]]*$", "", line)
        print line
        exit
      }
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

extract_gradle_dependency_version() {
  local coordinates_prefix="$1"
  local file="$2"
  awk -v prefix="$coordinates_prefix" '
    index($0, prefix) > 0 {
      line = substr($0, index($0, prefix) + length(prefix))
      version = ""
      for (i = 1; i <= length(line); i++) {
        c = substr(line, i, 1)
        if (c == "\"" || c == "'\''" || c == " " || c == "\t") {
          break
        }
        version = version c
      }
      print version
      exit
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
bytecode_doubles_pom="${repo_root}/javaspec-bytecode-doubles/pom.xml"
bytecode_agent_pom="${repo_root}/javaspec-bytecode-agent/pom.xml"
gradle_build="${repo_root}/javaspec-gradle-plugin/build.gradle"
example_maven_basic_pom="${repo_root}/examples/maven-basic/pom.xml"
example_junit_basic_pom="${repo_root}/examples/junit-platform-basic/pom.xml"
example_bytecode_doubles_pom="${repo_root}/examples/bytecode-doubles-basic/pom.xml"
example_bytecode_agent_pom="${repo_root}/examples/bytecode-agent-basic/pom.xml"
example_prophecy_pom="${repo_root}/examples/prophecy-basic/pom.xml"
example_gradle_build="${repo_root}/examples/gradle-basic/build.gradle"
launcher="${repo_root}/bin/javaspec"

root_version="$(extract_maven_project_version "$root_pom")"
launcher_fingerprint="$("$launcher" --launcher-fingerprint 2>/dev/null || true)"
launcher_fingerprint_version="$(printf '%s\n' "$launcher_fingerprint" | awk -F= '$1 == "javaspec.launcher.version" { print substr($0, index($0, "=") + 1); exit }')"
launcher_fingerprint_jar="$(printf '%s\n' "$launcher_fingerprint" | awk -F= '$1 == "javaspec.launcher.jar" { print substr($0, index($0, "=") + 1); exit }')"
launcher_fingerprint_sha256="$(printf '%s\n' "$launcher_fingerprint" | awk -F= '$1 == "javaspec.launcher.sha256" { print substr($0, index($0, "=") + 1); exit }')"

if [ -z "$root_version" ]; then
  printf 'FAIL: root pom.xml project version was not found\n'
  exit 1
fi

printf 'Version alignment baseline: %s\n' "$root_version"
record_result 'root pom.xml project version' "$root_version" "$root_version"
record_result 'bin/javaspec launcher version' "$("$launcher" --launcher-version 2>/dev/null || true)" "$root_version"
if [ -z "$launcher_fingerprint" ]; then
  printf 'SKIP: bin/javaspec fingerprint is unavailable before the core artifact is built or installed.\n'
else
  record_result 'bin/javaspec fingerprint version' "$launcher_fingerprint_version" "$root_version"
  if [ -z "$launcher_fingerprint_jar" ] || [ ! -f "$launcher_fingerprint_jar" ]; then
    printf 'FAIL: bin/javaspec fingerprint JAR is missing or not a file: %s\n' "$launcher_fingerprint_jar"
    status=1
  else
    printf 'PASS: bin/javaspec fingerprint JAR = %s\n' "$launcher_fingerprint_jar"
    record_result 'bin/javaspec fingerprint SHA-256' "$launcher_fingerprint_sha256" "$(sha256sum "$launcher_fingerprint_jar" | awk '{print $1}')"
  fi
fi
record_result 'javaspec-maven-plugin/pom.xml project version' "$(extract_maven_project_version "$maven_plugin_pom")" "$root_version"
record_result 'javaspec-junit-platform-engine/pom.xml project version' "$(extract_maven_project_version "$junit_engine_pom")" "$root_version"
record_result 'javaspec-bytecode-doubles/pom.xml project version' "$(extract_maven_project_version "$bytecode_doubles_pom")" "$root_version"
record_result 'javaspec-bytecode-doubles/pom.xml javaspec.version' "$(extract_maven_property javaspec.version "$bytecode_doubles_pom")" "$root_version"
record_result 'javaspec-bytecode-agent/pom.xml project version' "$(extract_maven_project_version "$bytecode_agent_pom")" "$root_version"
record_result 'javaspec-bytecode-agent/pom.xml javaspec.version' "$(extract_maven_property javaspec.version "$bytecode_agent_pom")" "$root_version"
record_result 'javaspec-gradle-plugin/build.gradle version' "$(extract_gradle_assignment version "$gradle_build")" "$root_version"
record_result 'javaspec-gradle-plugin/build.gradle javaspecCoreVersion' "$(extract_gradle_assignment javaspecCoreVersion "$gradle_build")" "$root_version"
record_result 'examples/maven-basic/pom.xml project version' "$(extract_maven_project_version "$example_maven_basic_pom")" "$root_version"
record_result 'examples/maven-basic/pom.xml javaspec.version' "$(extract_maven_property javaspec.version "$example_maven_basic_pom")" "$root_version"
record_result 'examples/junit-platform-basic/pom.xml project version' "$(extract_maven_project_version "$example_junit_basic_pom")" "$root_version"
record_result 'examples/junit-platform-basic/pom.xml javaspec.version' "$(extract_maven_property javaspec.version "$example_junit_basic_pom")" "$root_version"
record_result 'examples/bytecode-doubles-basic/pom.xml project version' "$(extract_maven_project_version "$example_bytecode_doubles_pom")" "$root_version"
record_result 'examples/bytecode-doubles-basic/pom.xml javaspec.version' "$(extract_maven_property javaspec.version "$example_bytecode_doubles_pom")" "$root_version"
record_result 'examples/bytecode-agent-basic/pom.xml project version' "$(extract_maven_project_version "$example_bytecode_agent_pom")" "$root_version"
record_result 'examples/bytecode-agent-basic/pom.xml javaspec.version' "$(extract_maven_property javaspec.version "$example_bytecode_agent_pom")" "$root_version"
record_result 'examples/prophecy-basic/pom.xml project version' "$(extract_maven_project_version "$example_prophecy_pom")" "$root_version"
record_result 'examples/prophecy-basic/pom.xml javaspec.version' "$(extract_maven_property javaspec.version "$example_prophecy_pom")" "$root_version"
record_result 'examples/gradle-basic/build.gradle version' "$(extract_gradle_assignment version "$example_gradle_build")" "$root_version"
record_result 'examples/gradle-basic/build.gradle javaspec dependency' "$(extract_gradle_dependency_version 'io.github.jvmspec:javaspec:' "$example_gradle_build")" "$root_version"

if [ "$status" -eq 0 ]; then
  printf 'PASS: all checked project versions are aligned.\n'
else
  printf 'FAIL: project version alignment check failed.\n'
fi

exit "$status"
