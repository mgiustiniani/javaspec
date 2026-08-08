#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "${script_dir}/.." && pwd -P)"
cd "$repo_root"

python3 - <<'PY'
from pathlib import Path
import re
import sys

files = [Path("README.md"), Path("CHANGELOG.md"), Path("RELEASING.md"), Path("PLAN.md")]
files.extend(sorted(Path("docs").rglob("*.md")))
files.extend(sorted(Path("examples").glob("README.md")))
files.extend(sorted(Path(".").glob("javaspec-*/README.md")))

link_pattern = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")
missing = []
checked = 0

for source in files:
    text = source.read_text(encoding="utf-8")
    for match in link_pattern.finditer(text):
        destination = match.group(1).strip()
        if destination.startswith("<") and destination.endswith(">"):
            destination = destination[1:-1]
        if not destination or destination.startswith(("#", "http://", "https://", "mailto:")):
            continue
        path_text = destination.split("#", 1)[0]
        if not path_text:
            continue
        checked += 1
        target = (source.parent / path_text).resolve()
        if not target.exists():
            line = text.count("\n", 0, match.start()) + 1
            missing.append((source, line, destination))

if missing:
    for source, line, destination in missing:
        print(f"FAIL: {source}:{line}: missing local Markdown target: {destination}")
    print(f"FAIL: {len(missing)} missing local Markdown link target(s).")
    sys.exit(1)

print(f"PASS: {checked} local Markdown link target(s) resolve across {len(files)} documentation files.")
PY
