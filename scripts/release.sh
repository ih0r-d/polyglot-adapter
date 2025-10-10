#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:-}

if [ -z "$VERSION" ]; then
  echo "❌ VERSION is required. Usage: ./release.sh 1.2.3"
  exit 1
fi

echo "🚀 Starting release $VERSION..."

# ensure globstar works for recursive ** pattern
shopt -s globstar

# 1️⃣ Set version for all modules
./mvnw -q versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false

# 2️⃣ Add and commit all pom.xml files
git add pom.xml **/pom.xml
git commit -m "🔖 Release $VERSION"

# 3️⃣ Tag and push
git tag -a "v$VERSION" -m "Release $VERSION"
git push
git push --tags

# 4️⃣ Prepare next snapshot version
NEXT_VERSION=$(echo "$VERSION" | awk -F. '{printf "%d.%d.%d-SNAPSHOT", $1, $2, $3+1}')
./mvnw -q versions:set -DnewVersion="$NEXT_VERSION" -DgenerateBackupPoms=false

git add pom.xml **/pom.xml
git commit -m "🔧 Prepare for next development iteration $NEXT_VERSION"
git push

echo "✅ Release $VERSION done. Next version: $NEXT_VERSION"
