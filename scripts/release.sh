#!/bin/bash
set -euo pipefail

VERSION=${1:-}
MODULE=${2:-}

if [ -z "$VERSION" ]; then
  echo "❌ VERSION required"
  echo "Usage:"
  echo "  ./scripts/release.sh 0.0.22"
  echo "  ./scripts/release.sh 0.0.22 polyglot-core"
  exit 1
fi

if ! git diff-index --quiet HEAD --; then
  echo "❌ Working directory not clean."
  exit 1
fi

if git show-ref --tags --verify --quiet "refs/tags/v$VERSION"; then
  echo "⚠️ Tag v$VERSION already exists."
  exit 0
fi

export MAVEN_OPTS="--enable-native-access=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED"

echo "🔧 Setting version $VERSION for all modules"

./mvnw -q -ntp -B versions:set \
  -DnewVersion="$VERSION" \
  -DprocessAllModules=true \
  -DprocessParent=true \
  -DgenerateBackupPoms=false

# ------------------------------------------------------------------
# CHANGELOG
# ------------------------------------------------------------------

if [ ! -f CHANGELOG.md ]; then
  echo "📝 Generating initial CHANGELOG.md"
  git cliff --config .git-cliff.toml --output CHANGELOG.md
else
  echo "📝 Updating CHANGELOG.md for $VERSION"
  git cliff --config .git-cliff.toml \
    --unreleased \
    --tag "$VERSION" \
    --prepend CHANGELOG.md
fi

# ------------------------------------------------------------------
# Commit + tag
# ------------------------------------------------------------------

git add pom.xml CHANGELOG.md */pom.xml
git commit -m "Release $VERSION" || true
git tag -a "v$VERSION" -m "Release $VERSION"

git push origin main
git push origin "v$VERSION"

echo "✅ Release $VERSION completed"
