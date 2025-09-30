#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:-}

if [ -z "$VERSION" ]; then
  echo "❌ VERSION is required. Usage: ./release.sh 1.2.3"
  exit 1
fi

echo "🚀 Starting release $VERSION..."

./mvnw -q versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false > /dev/null 2>&1
git add -- pom.xml **/pom.xml > /dev/null 2>&1
git commit -m "Release $VERSION" > /dev/null 2>&1
git tag -a "v$VERSION" -m "Release $VERSION" > /dev/null 2>&1
git push > /dev/null 2>&1
git push --tags > /dev/null 2>&1

NEXT_VERSION=$(echo "$VERSION" | awk -F. '{printf "%d.%d.%d-SNAPSHOT", $1, $2, $3+1}')
./mvnw -q versions:set -DnewVersion="$NEXT_VERSION" -DgenerateBackupPoms=false > /dev/null 2>&1
git add -- pom.xml **/pom.xml > /dev/null 2>&1
git commit -m "Prepare for next development iteration $NEXT_VERSION" > /dev/null 2>&1
git push > /dev/null 2>&1

echo "✅ Release $VERSION done. Next version: $NEXT_VERSION"
