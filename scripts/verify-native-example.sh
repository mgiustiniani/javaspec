#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"
MAVEN_BIN="${MAVEN_BIN:-mvn}"
NATIVE_IMAGE_BIN="${NATIVE_IMAGE_BIN:-$(command -v native-image || true)}"

if [ -z "$NATIVE_IMAGE_BIN" ] || [ ! -x "$NATIVE_IMAGE_BIN" ]; then
  printf '%s\n' 'NATIVE TYPED STOP [GRAALVM_NATIVE_IMAGE_REQUIRED]: native-image was not found. Set NATIVE_IMAGE_BIN or install GraalVM Native Image.' >&2
  exit 2
fi

native_image_real="$(readlink -f "$NATIVE_IMAGE_BIN")"
candidate_home="$(cd "$(dirname "$native_image_real")/../../.." && pwd -P)"
if [ -x "$candidate_home/bin/java" ]; then
  export JAVA_HOME="$candidate_home"
  export PATH="$JAVA_HOME/bin:$PATH"
elif [ -n "${GRAALVM_HOME:-}" ] && [ -x "${GRAALVM_HOME}/bin/java" ]; then
  export JAVA_HOME="$GRAALVM_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if ! "$NATIVE_IMAGE_BIN" --version 2>&1 | grep -F 'native-image' >/dev/null; then
  printf 'NATIVE TYPED STOP [GRAALVM_NATIVE_IMAGE_INVALID]: %s does not report a native-image tool.\n' "$NATIVE_IMAGE_BIN" >&2
  exit 2
fi

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/javaspec-native-verify.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT
build_log="$work_dir/native-build.log"
example_dir="$repo_root/examples/native-basic"
executable="$example_dir/target/javaspec-native-basic"

printf 'GRAALVM_NATIVE_IMAGE=%s\n' "$("$NATIVE_IMAGE_BIN" --version | head -n 1)"
printf 'JAVA_RUNTIME=%s\n' "$(java -version 2>&1 | head -n 1)"

(
  cd "$repo_root"
  "$MAVEN_BIN" -q -DskipTests install
  "$MAVEN_BIN" -q -f javaspec-maven-plugin/pom.xml -DskipTests install
  "$MAVEN_BIN" -f examples/native-basic/pom.xml -Pnative clean package
) >"$build_log" 2>&1 || {
  tail -n 120 "$build_log" >&2
  exit 1
}

if [ ! -x "$executable" ]; then
  printf 'ERROR: native example executable was not created: %s\n' "$executable" >&2
  exit 1
fi

launcher="$example_dir/target/generated-test-sources/javaspec-native/io/github/jvmspec/generated/JavaspecNativeMain.java"
reflection="$example_dir/target/test-classes/META-INF/native-image/io.github.jvmspec/javaspec-native/reflect-config.json"
test -f "$launcher"
test -f "$reflection"
if command -v ldd >/dev/null 2>&1 && ldd "$executable" 2>/dev/null | grep -F 'libjvm' >/dev/null; then
  printf 'ERROR: native executable unexpectedly links libjvm.\n' >&2
  exit 1
fi
grep -F 'spec.com.example.CalculatorSpec.class' "$launcher" >/dev/null
grep -F 'com.example.Calculator.class' "$launcher" >/dev/null
grep -F '"name": "spec.com.example.CalculatorSpec"' "$reflection" >/dev/null
grep -F '"name": "com.example.Calculator"' "$reflection" >/dev/null

pretty="$work_dir/pretty.out"
json="$work_dir/json.out"
help="$work_dir/help.out"
unsupported_out="$work_dir/unsupported.out"
unsupported_err="$work_dir/unsupported.err"
runtime_path="$work_dir/no-jvm-path"
mkdir -p "$runtime_path"
run_native=(env -i "PATH=$runtime_path" "HOME=$work_dir" "$executable")

"${run_native[@]}" >"$pretty"
grep -F 'PASSED spec.com.example.CalculatorSpec#it_adds_two_numbers' "$pretty" >/dev/null
grep -F 'PENDING spec.com.example.CalculatorSpec#it_records_pending_examples' "$pretty" >/dev/null
grep -F 'SKIPPED spec.com.example.CalculatorSpec#it_records_skipped_examples' "$pretty" >/dev/null
grep -F 'Examples: 3 total, 1 passed, 0 failed, 0 broken, 1 skipped, 1 pending.' "$pretty" >/dev/null

"${run_native[@]}" --formatter json >"$json"
grep -F '"schemaVersion": 1' "$json" >/dev/null
grep -F '"status": "passed"' "$json" >/dev/null
grep -F '"status": "pending"' "$json" >/dev/null
grep -F '"status": "skipped"' "$json" >/dev/null

"${run_native[@]}" --help >"$help"
grep -F 'linked into this executable at native-image build time' "$help" >/dev/null
grep -F 'No runtime source discovery, compilation, generation' "$help" >/dev/null

set +e
"${run_native[@]}" --compile >"$unsupported_out" 2>"$unsupported_err"
unsupported_status=$?
set -e
if [ "$unsupported_status" -ne 64 ]; then
  printf 'ERROR: unsupported native option returned %s instead of 64.\n' "$unsupported_status" >&2
  exit 1
fi
grep -F 'Unsupported native option: --compile' "$unsupported_err" >/dev/null

printf 'NATIVE_EXECUTABLE=%s\n' "$executable"
printf 'NATIVE_EXECUTABLE_BYTES=%s\n' "$(wc -c < "$executable")"
printf 'NATIVE_EXECUTABLE_SHA256=%s\n' "$(sha256sum "$executable" | awk '{print $1}')"
printf '%s\n' 'PASS: project-specific JavaSpec native executable built and replayed without a JVM process.'
