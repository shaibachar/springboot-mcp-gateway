#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SPEC_DIR="$ROOT_DIR/spec"

if ! command -v rg >/dev/null 2>&1; then
  echo "ripgrep (rg) is required for the spec coverage check." >&2
  exit 1
fi

missing=()

for spec_file in "$SPEC_DIR"/*.md; do
  name="$(basename "$spec_file")"
  if ! rg --files-with-matches --fixed-strings --glob '!spec/**' "$name" "$ROOT_DIR" > /dev/null; then
    missing+=("$name")
  fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "Spec coverage check failed. The following specs are not referenced outside the spec directory:" >&2
  for file in "${missing[@]}"; do
    echo " - spec/$file" >&2
  done
  exit 1
fi

echo "All spec documents are referenced in the codebase."
