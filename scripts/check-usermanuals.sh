#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"
cd "$repo_root"

status=0
languages=(en it es de fr ch)
pages=(
  docs/usermanual/Home.md
  docs/usermanual/it/Home.md
  docs/usermanual/es/Home.md
  docs/usermanual/de/Home.md
  docs/usermanual/fr/Home.md
  docs/usermanual/ch/Home.md
)
sidebars=(
  docs/usermanual/_Sidebar.md
  docs/usermanual/it/_Sidebar.md
  docs/usermanual/es/_Sidebar.md
  docs/usermanual/de/_Sidebar.md
  docs/usermanual/fr/_Sidebar.md
  docs/usermanual/ch/_Sidebar.md
)
required_tokens=(
  '1.0.0-RC5'
  'io.github.jvmspec'
  'javaspec describe'
  'javaspec run'
  'javaspec prophesize'
  'javaspec list-extensions'
  '--launcher-fingerprint'
  '--generation-report'
  '// javaspec:stub'
  'scripts/verify-all.sh'
  'HTTP 404'
  'javaspec-guided-development-assistant.md'
)

for index in "${!languages[@]}"; do
  language="${languages[$index]}"
  page="${pages[$index]}"
  sidebar="${sidebars[$index]}"

  if [ ! -s "$page" ]; then
    printf 'FAIL: %s user manual is missing or empty: %s\n' "$language" "$page"
    status=1
    continue
  fi
  if [ ! -s "$sidebar" ]; then
    printf 'FAIL: %s user-manual sidebar is missing or empty: %s\n' "$language" "$sidebar"
    status=1
  fi

  for token in "${required_tokens[@]}"; do
    if ! grep -Fq -- "$token" "$page"; then
      printf 'FAIL: %s is missing contract token: %s\n' "$page" "$token"
      status=1
    fi
  done

  if python3 - "$page" "$sidebar" <<'PY'
from pathlib import Path
import sys
for name in sys.argv[1:]:
    Path(name).read_text(encoding="utf-8")
PY
  then
    printf 'PASS: valid UTF-8 user manual: %s\n' "$page"
  else
    printf 'FAIL: invalid UTF-8 user manual or sidebar: %s\n' "$page"
    status=1
  fi
done

if [ ! -s docs/usermanual/README.md ]; then
  printf 'FAIL: multilingual user-manual index is missing\n'
  status=1
elif grep -Fq 'scripts/check-usermanuals.sh' docs/usermanual/README.md; then
  printf 'PASS: multilingual user-manual index is present\n'
else
  printf 'FAIL: multilingual user-manual index does not document its guard\n'
  status=1
fi

if [ "$status" -eq 0 ]; then
  printf 'PASS: multilingual user manuals are complete and aligned.\n'
else
  printf 'FAIL: multilingual user-manual checks failed.\n'
fi

exit "$status"
