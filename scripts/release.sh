#!/bin/bash
set -euo pipefail

VERSION=${1:-}
if [ -z "$VERSION" ]; then
  echo "❌ VERSION required: ./release.sh 1.2.3"
  exit 1
fi

if ! git diff-index --quiet HEAD --; then
  echo "❌ Working directory not clean."
  exit 1
fi

if git rev-parse "v$VERSION" >/dev/null 2>&1; then
  echo "⚠️ Tag v$VERSION already exists."
  exit 0
fi

MAVEN_OPTS="--enable-native-access=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED"

MAVEN_OPTS="$MAVEN_OPTS" ./mvnw -q -ntp -B \
  versions:set -DnewVersion="$VERSION" \
  -DgenerateBackupPoms=false -DprocessAllModules=true 2>/dev/null

git add pom.xml */pom.xml
git commit -m "Release $VERSION" >/dev/null 2>&1 || true

git tag -a "v$VERSION" -m "Release $VERSION"
git push origin "v$VERSION" >/dev/null 2>&1
git push origin main >/dev/null 2>&1

echo "✅ Released $VERSION (tag v$VERSION pushed)"
