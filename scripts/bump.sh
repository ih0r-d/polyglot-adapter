#!/usr/bin/env bash
set -euo pipefail

mvn_get_version() {
  local out err
  out="$(mktemp)"; err="$(mktemp)"
  if ./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout >"$out" 2>"$err"; then
    tr -d '\r' <"$out"
    rm -f "$out" "$err"
  else
    echo "❌ Maven failed to read project.version"
    cat "$err"
    rm -f "$out" "$err"
    exit 1
  fi
}

mvn_set_quiet() {
  local tmp
  tmp="$(mktemp)"
  if ! ./mvnw -q versions:set -DnewVersion="$1" -DgenerateBackupPoms=false 1>/dev/null 2>"$tmp"; then
    echo "❌ Maven failed to set version to $1"
    cat "$tmp"
    rm -f "$tmp"
    exit 1
  fi
  rm -f "$tmp"
}

TYPE=${1:-patch}  # patch|minor|major
CURRENT="$(mvn_get_version)"
BASE="${CURRENT/-SNAPSHOT/}"
IFS='.' read -r MAJOR MINOR PATCH <<< "$BASE"

case "$TYPE" in
  major) MAJOR=$((MAJOR+1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR+1)); PATCH=0 ;;
  patch) PATCH=$((PATCH+1)) ;;
  *) echo "❌ Unknown bump type: $TYPE (use patch|minor|major)"; exit 1 ;;
esac

NEXT="$MAJOR.$MINOR.$PATCH-SNAPSHOT"
mvn_set_quiet "$NEXT"
echo "🔧 Bumped version: $CURRENT → $NEXT"
