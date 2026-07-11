#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"
cd "$repo_root"

classes_dir="target/classes"
if [ ! -d "$classes_dir" ]; then
  printf 'ERROR: %s is missing. Run mvn verify/package before this check.\n' "$classes_dir" >&2
  exit 1
fi

if ! command -v javap >/dev/null 2>&1; then
  printf 'ERROR: javap is required to inspect classfile versions.\n' >&2
  exit 1
fi

status=0
max_major=0
class_count=0
while IFS= read -r -d '' class_file; do
  class_count=$((class_count + 1))
  major="$(javap -verbose "$class_file" | awk '/major version/ {print $3; exit}')"
  if [ -z "$major" ]; then
    printf 'FAIL: could not read classfile major version: %s\n' "$class_file" >&2
    status=1
    continue
  fi
  if [ "$major" -gt "$max_major" ]; then
    max_major="$major"
  fi
  if [ "$major" -gt 52 ]; then
    printf 'FAIL: %s has classfile major version %s (> 52 / Java 8)\n' "$class_file" "$major" >&2
    status=1
  fi
done < <(find "$classes_dir" -name '*.class' -print0)

if [ "$class_count" -eq 0 ]; then
  printf 'FAIL: no classfiles found under %s\n' "$classes_dir" >&2
  exit 1
fi

if [ "$status" -eq 0 ]; then
  printf 'PASS: %s core classfiles inspected; max major version %s (Java 8 compatible).\n' "$class_count" "$max_major"
else
  printf 'FAIL: core classfile bytecode compatibility check failed.\n' >&2
fi

exit "$status"
