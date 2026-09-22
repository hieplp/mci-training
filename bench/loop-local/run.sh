#!/bin/sh
# Compare bench/loop-local/old (locals declared in the loop) with
# bench/loop-local/new (same algorithm, locals declared before the loop).
# new/MyBigNumber.java is a copy of
# ~/Projects/training/mci-training/mci-core/src/main/java/dev/hieplp/mci/core/MyBigNumber.java
# Indexes i, j, and resultIndex stay outside in both. That is loop state, not a step local.
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
mkdir -p "$OUT/lib" "$OUT/old" "$OUT/new" "$OUT/bench"

javac -d "$OUT/lib" "$SRC/AppLogger.java" "$SRC/NumberStrings.java"
javac -cp "$OUT/lib" -d "$OUT/old" "$ROOT/bench/loop-local/old/MyBigNumber.java"
javac -cp "$OUT/lib" -d "$OUT/new" "$ROOT/bench/loop-local/new/MyBigNumber.java"
javac -cp "$OUT/lib:$OUT/old" -d "$OUT/bench" "$ROOT/bench/loop-local/HoistBench.java"

echo "===== same sum? ====="
old_sum=$(java -cp "$OUT/old:$OUT/lib:$OUT/bench" HoistBench check 40 1 off)
new_sum=$(java -cp "$OUT/new:$OUT/lib:$OUT/bench" HoistBench check 40 1 off)
echo "old $old_sum"
echo "new $new_sum"
if [ "$old_sum" != "$new_sum" ]; then
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
    -XX:+UseParallelGC -Xms32m -Xmx512m \
    HoistBench "$mode" "$digits" "$calls" "$log" 2>&1 \
    | awk -v v="$variant" '/^mode=/{line=$0} /maximum resident set size/{rss=$1} END{print "variant=" v, line, "maxrss_bytes=" rss}'
}

echo "===== log off, 5 forks ====="
for variant in old new; do
  for digits in 200 1000; do
    calls=10
    if [ "$digits" = 1000 ]; then calls=4; fi
    for fork in 1 2 3 4 5; do
      run_one "$variant" steps "$digits" "$calls" off
    done
  done
done

echo "===== log on, 3 forks (INFO still formats each step) ====="
for variant in old new; do
  for fork in 1 2 3; do
    run_one "$variant" steps 200 4 on
  done
done
