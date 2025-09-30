#!/usr/bin/env bash
set -euo pipefail

echo "🧹 Cleaning remote tags not present locally..."
git fetch --tags >/dev/null 2>&1 || { echo "❌ git fetch failed"; exit 1; }

remote_tags=$(git ls-remote --tags origin | awk '{print $2}' | sed 's|refs/tags/||')
for tag in $remote_tags; do
  if ! git rev-parse "$tag" >/dev/null 2>&1; then
    git push origin --delete "$tag" >/dev/null 2>&1 || { echo "❌ Failed to delete remote tag $tag"; exit 1; }
    echo "🗑 deleted remote tag $tag"
  fi
done
echo "✅ Remote tags synced"
