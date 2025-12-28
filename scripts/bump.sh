#!/bin/bash
set -euo pipefail

TYPE=${1:-patch}  # patch|minor|major

# ---- functions ----
mvn_get_version() {
  ./mvnw -q help:evaluate \
    -Dexpression=project.version \
    -DforceStdout
}

mvn_set_version() {
  MAVEN_OPTS="--enable-native-access=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED" \
  ./mvnw -q -B -ntp versions:set \
    -DnewVersion="$1" \
    -DprocessAllModules=true \
    -DgenerateBackupPoms=false
}

# ---- logic ----
CURRENT="$(mvn_get_version)"
BASE="${CURRENT/-SNAPSHOT/}"

IFS='.' read -r MAJOR MINOR PATCH <<< "$BASE"

case "$TYPE" in
  major) ((MAJOR++)); MINOR=0; PATCH=0 ;;
  minor) ((MINOR++)); PATCH=0 ;;
  patch) ((PATCH++)) ;;
  *)
    echo "❌ Unknown bump type: $TYPE (use patch|minor|major)"
    exit 1
    ;;
esac

NEXT="$MAJOR.$MINOR.$PATCH-SNAPSHOT"

mvn_set_version "$NEXT"

git add pom.xml **/pom.xml || true
git commit -m "Bump version: $CURRENT → $NEXT" || true

echo "✅ Bumped version: $CURRENT → $NEXT"
