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

# Changelog via git-cliff
if [ ! -f CHANGELOG.md ]; then
  echo "📝 Generating initial CHANGELOG.md with git-cliff..."
  git cliff --config .git-cliff.toml --output CHANGELOG.md
else
  echo "📝 Updating CHANGELOG.md for $VERSION with git-cliff (prepend)..."
  git cliff --config .git-cliff.toml \
    --unreleased \
    --tag "$VERSION" \
    --prepend CHANGELOG.md
fi

# Stage POMs + changelog
git add pom.xml CHANGELOG.md
git add */pom.xml 2>/dev/null || true

git commit -m "Release $VERSION" >/dev/null 2>&1 || true

git tag -a "v$VERSION" -m "Release $VERSION"

git push origin "v$VERSION" >/dev/null 2>&1
git push origin main >/dev/null 2>&1

echo "✅ Released $VERSION (tag v$VERSION pushed)"
