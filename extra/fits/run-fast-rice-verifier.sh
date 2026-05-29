#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

if [[ "${JHV_SKIP_COMPILE:-0}" != "1" ]]; then
  ant compile
fi

BUILD_DIR="${TMPDIR:-/tmp}/jhv-fast-rice-verifier"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

CP="bin:resources"
while IFS= read -r jar; do
  CP="$CP:$jar"
done < <(find lib -type f -name '*.jar' | sort)

javac --release 25 -cp "$CP" -d "$BUILD_DIR" extra/fits/FastRiceVerifier.java
java -cp "$BUILD_DIR:$CP" FastRiceVerifier "$@"
