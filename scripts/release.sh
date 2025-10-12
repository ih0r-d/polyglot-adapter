#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:-}

if [ -z "$VERSION" ]; then
  echo "❌ VERSION is required. Usage: ./release.sh 1.2.3"
  exit 1
fi

echo "🚀 Starting release $VERSION..."

# ensure clean git state
if ! git diff-index --quiet HEAD --; then
  echo "❌ Working directory not clean. Commit or stash changes first."
  exit 1
fi

# check if tag already exists
if git rev-parse "v$VERSION" >/dev/null 2>&1; then
  echo "⚠️ Tag v$VERSION already exists — skipping release."
  exit 0
fi

# update versions
./mvnw -q versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false

git add pom.xml */pom.xml
git commit -m "Release $VERSION" >/dev/null 2>&1 || true

# tag + push
echo "🏷️ Creating and pushing tag v$VERSION..."
git tag -a "v$VERSION" -m "Release $VERSION"
git push origin main >/dev/null 2>&1
git push origin "v$VERSION" >/dev/null 2>&1

echo "✅ Tag v$VERSION pushed successfully."

# bump snapshot (local only, no push)
NEXT_VERSION=$(echo "$VERSION" | awk -F. '{printf "%d.%d.%d-SNAPSHOT", $1, $2, $3+1}')
echo "⬆️  Preparing next version $NEXT_VERSION (local only)..."
./mvnw -q versions:set -DnewVersion="$NEXT_VERSION" -DgenerateBackupPoms=false

git add pom.xml */pom.xml
git commit -m "Prepare next iteration $NEXT_VERSION" >/dev/null 2>&1 || true

echo "✅ Done."
echo "🚀 Released $VERSION → next development version: $NEXT_VERSION"
echo "💡 Remember: push main later if you want to publish the next snapshot."
