#!/usr/bin/env bash
set -e

MODULE="${MODULE:-}"
SKIP_TESTS="${SKIP_TESTS:-false}"

export SDKMAN_DIR="$HOME/.sdkman"
source "$SDKMAN_DIR/bin/sdkman-init.sh" >/dev/null 2>&1
sdk env >/dev/null 2>&1

LOG=$(mktemp)

cleanup() {
  rc=$?
  if [ $rc -ne 0 ]; then
    cat "$LOG"
  fi
  rm -f "$LOG"
  exit $rc
}
trap cleanup EXIT

MVN_ARGS=("-q")

[ -n "$MODULE" ] && MVN_ARGS+=("-pl" "$MODULE")
[ "$SKIP_TESTS" = "true" ] && MVN_ARGS+=("-DskipTests")

echo "→ mvn ${MVN_ARGS[*]} install"
./mvnw "${MVN_ARGS[@]}" install >>"$LOG" 2>&1
