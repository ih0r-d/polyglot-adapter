#!/usr/bin/env bash
set -euo pipefail

tmp="$(mktemp)"
if ! ./mvnw -q spotless:apply 1>/dev/null 2>"$tmp"; then
  echo "❌ Maven format failed"
  cat "$tmp"
  rm -f "$tmp"
  exit 1
fi
rm -f "$tmp"
echo "✅ Code formatted"
