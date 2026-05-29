#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

if [[ "${JHV_SKIP_COMPILE:-0}" != "1" ]]; then
  ant compile
fi

BUILD_DIR="${TMPDIR:-/tmp}/jhv-fits-benchmark"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

CP="bin:resources"
while IFS= read -r jar; do
  CP="$CP:$jar"
done < <(find lib -type f -name '*.jar' | sort)

javac --release 25 -cp "$CP" -d "$BUILD_DIR" extra/fits/FITSLoadBenchmark.java

JAVA_ARGS=()
if [[ -n "${JHV_BENCHMARK_JVMARG:-}" ]]; then
  JAVA_ARGS+=("$JHV_BENCHMARK_JVMARG")
fi
if [[ -n "${JHV_BENCHMARK_JVM_ARGS:-}" ]]; then
  # Intended for simple whitespace-separated JVM options.
  read -r -a EXTRA_JAVA_ARGS <<< "$JHV_BENCHMARK_JVM_ARGS"
  JAVA_ARGS+=("${EXTRA_JAVA_ARGS[@]}")
fi

JAVA_LOG="$BUILD_DIR/java-output.log"
set +e
java "${JAVA_ARGS[@]}" --enable-native-access=ALL-UNNAMED -cp "$BUILD_DIR:$CP" FITSLoadBenchmark "$@" > "$JAVA_LOG" 2>&1
STATUS=$?
set -e

awk '
  /^JProfiler>/ { next }
  /^(finished|stopped)$/ { next }
  /^[0-9]+;[0-9]+;[0-9]+;(true|false)$/ { next }
  { print }
' "$JAVA_LOG"

SNAPSHOT="$(sed -n 's/^JProfiler> Saving snapshot \(.*\) \.\.\.$/\1/p' "$JAVA_LOG" | tail -n 1)"
if [[ -n "$SNAPSHOT" ]]; then
  echo "JPROFILER_SNAPSHOT=$SNAPSHOT" >&2
fi

exit "$STATUS"
