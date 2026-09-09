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
if [[ -n "${JHV_NOM_TAM_JAR:-}" ]]; then
  if [[ ! -f "$JHV_NOM_TAM_JAR" ]]; then
    echo "Candidate nom-tam JAR not found: $JHV_NOM_TAM_JAR" >&2
    exit 1
  fi
  CP="resources:$JHV_NOM_TAM_JAR"
fi
while IFS= read -r jar; do
  if [[ -n "${JHV_NOM_TAM_JAR:-}" && "${jar##*/}" == nom-tam-fits-*.jar ]]; then
    continue
  fi
  CP="$CP:$jar"
done < <(find lib -type f -name '*.jar' | sort)

if [[ -n "${JHV_NOM_TAM_JAR:-}" ]]; then
  find src -name '*.java' | sort > "$BUILD_DIR/sources.txt"
  javac --release 25 -cp "$CP" -d "$BUILD_DIR" @"$BUILD_DIR/sources.txt"
  CP="$BUILD_DIR:$CP"
fi

javac --release 25 -cp "$CP" -d "$BUILD_DIR" extra/fits/FastRiceVerifier.java
java --enable-native-access=ALL-UNNAMED -Duser.timezone=UTC -Duser.language=en -Duser.country=US -cp "$BUILD_DIR:$CP" FastRiceVerifier "$@"
