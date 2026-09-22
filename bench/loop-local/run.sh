#!/bin/sh
# Three copies of MyBigNumber:
#   old         repo original. Locals declared inside the while. StringBuilder snapshots.
#   hoist-only  same algorithm as old. Only the while-body locals are declared before the loop.
#   updated     copy of ~/Projects/training/mci-training/.../MyBigNumber.java
#               (char[] result, checks inside the loop, locals declared before the loop).
# i and j stay outside in all three. They are the loop indexes.
set -eu
cd "$(dirname "$0")/../.."
ROOT=$(pwd)
JDK="$HOME/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.12.1+1/Contents/Home"
if [ ! -x "$JDK/bin/javac" ]; then
  JDK="${JAVA_HOME:-}"
fi
if [ ! -x "$JDK/bin/javac" ]; then
  echo "Need JDK 21. Set JAVA_HOME." >&2
  exit 1
fi
export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"

SRC="$ROOT/mci-core/src/main/java/dev/hieplp/mci/core"
OUT="$ROOT/bench/loop-local/out"
rm -rf "$OUT"
mkdir -p "$OUT/lib" "$OUT/old" "$OUT/hoist-only" "$OUT/updated" "$OUT/bench"

javac -d "$OUT/lib" "$SRC/AppLogger.java" "$SRC/NumberStrings.java"
javac -cp "$OUT/lib" -d "$OUT/old" "$ROOT/bench/loop-local/old/MyBigNumber.java"
javac -cp "$OUT/lib" -d "$OUT/hoist-only" "$ROOT/bench/loop-local/hoist-only/MyBigNumber.java"
javac -cp "$OUT/lib" -d "$OUT/updated" "$ROOT/bench/loop-local/updated/MyBigNumber.java"
javac -cp "$OUT/lib:$OUT/old" -d "$OUT/bench" "$ROOT/bench/loop-local/HoistBench.java"

echo "===== same sum? ====="
old_sum=$(java -cp "$OUT/old:$OUT/lib:$OUT/bench" HoistBench check 40 1 off)
hoist_sum=$(java -cp "$OUT/hoist-only:$OUT/lib:$OUT/bench" HoistBench check 40 1 off)
updated_sum=$(java -cp "$OUT/updated:$OUT/lib:$OUT/bench" HoistBench check 40 1 off)
echo "old $old_sum"
echo "hoist-only $hoist_sum"
echo "updated $updated_sum"
if [ "$old_sum" != "$hoist_sum" ] || [ "$old_sum" != "$updated_sum" ]; then
  echo "sums differ" >&2
  exit 1
fi

run_one() {
  variant=$1
  mode=$2
  digits=$3
  calls=$4
  log=$5
  /usr/bin/time -l java -cp "$OUT/$variant:$OUT/lib:$OUT/bench" \
    -XX:+UseParallelGC -Xms64m -Xmx512m \
    HoistBench "$mode" "$digits" "$calls" "$log" 2>&1 \
    | awk -v v="$variant" '/^mode=/{line=$0} /maximum resident set size/{rss=$1} /instructions retired/{ins=$1} /cycles elapsed/{cyc=$1} /peak memory footprint/{peak=$1} END{print "variant=" v, line, "maxrss_bytes=" rss, "insns=" ins, "cycles=" cyc, "peak_bytes=" peak}'
}

echo "===== 1000 digits, 800 calls, log off, 5 forks ====="
for variant in old hoist-only updated; do
  for fork in 1 2 3 4 5; do
    run_one "$variant" steps 1000 800 off
  done
done
