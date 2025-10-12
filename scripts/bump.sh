#!/bin/bash
set -euo pipefail

TYPE=${1:-patch}  # patch|minor|major

# ---- functions ----
mvn_get_version() {
  ./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null | tr -d '\r'
}

mvn_set_version() {
  MAVEN_OPTS="--enable-native-access=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED" \
  ./mvnw -q -ntp -B versions:set \
    -DnewVersion="$1" \
    -DgenerateBackupPoms=false \
    -DprocessAllModules=true \
    1>/dev/null 2>/dev/null
}

# ---- logic ----
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
mvn_set_version "$NEXT"

git add pom.xml */pom.xml >/dev/null 2>&1 || true
git commit -m "Bump version: $CURRENT → $NEXT" >/dev/null 2>&1 || true

echo "✅ Bumped version: $CURRENT → $NEXT"
