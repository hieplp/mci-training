#!/bin/sh
# Three copies of MyBigNumber, one classpath each.
#   old         this repo's MyBigNumber
#   hoist-only  same method, while-body locals declared before the loop
#   updated     char[] result, locals declared before the loop
#
# Uses the hand-rolled HoistBench, not JMH. JMH runs the benchmark at max
# sustained rate; sumWithSteps allocates ~1.15 MB/call, so each step list is
# mid-construction when a young GC fires and promotes to Old faster than the
# concurrent mark reclaims it. That OOMs at any heap size. HoistBench's loop
# throttles allocation enough to run cleanly and reports ns, alloc_bytes,
# gc_count, and heap_after_gc directly.
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
old_sum=$(java -cp "$OUT/old:$OUT/lib:$OUT/bench" HoistBench check 999 1 off)
hoist_sum=$(java -cp "$OUT/hoist-only:$OUT/lib:$OUT/bench" HoistBench check 999 1 off)
updated_sum=$(java -cp "$OUT/updated:$OUT/lib:$OUT/bench" HoistBench check 999 1 off)
echo "old $old_sum"
echo "hoist-only $hoist_sum"
echo "updated $updated_sum"
if [ "$old_sum" != "$hoist_sum" ] || [ "$old_sum" != "$updated_sum" ]; then
  echo "sums differ" >&2
  exit 1
fi

# Warm up the JIT once per variant so the timed runs are steady-state.
for variant in old hoist-only updated; do
  java -cp "$OUT/$variant:$OUT/lib:$OUT/bench" -XX:+UseParallelGC -Xms256m -Xmx512m \
    HoistBench steps 999 3000 off > /dev/null
done

echo "Running. 999-digit operands, 1,000 steps per call, logging off."
for variant in old hoist-only updated; do
  for fork in 1 2 3 4 5; do
    java -cp "$OUT/$variant:$OUT/lib:$OUT/bench" -XX:+UseParallelGC -Xms256m -Xmx512m \
      HoistBench steps 999 2000 off > "$OUT/$variant.$fork.txt"
  done
done

python3 - "$OUT" << 'PY'
import re, statistics, sys
from pathlib import Path

out = Path(sys.argv[1])
order = [("old", "old"), ("hoist-only", "only move the variables"), ("updated", "updated")]

def load(key):
    rows = []
    for f in range(1, 6):
        m = dict(re.findall(r"(\w+)=([0-9]+)", (out / f"{key}.{f}.txt").read_text()))
        rows.append({
            "ns": int(m["ns"]),
            "alloc": int(m["alloc_bytes"]),
            "gc": int(m["gc_count"]),
            "heap": int(m["heap_after_gc"]),
        })
    return rows

data = {key: load(key) for key, _ in order}
base = data["old"]

def med(key, field):
    return statistics.median(r[field] for r in data[key])

def ratio_note(key, field):
    if key == "old":
        return ""
    b, v = med("old", field), med(key, field)
    if v < b:
        return f"{b / v:.1f}x less"
    if v > b:
        return f"{v / b:.2f}x more"
    return "same"

label_width = max(len(label) for _, label in order)
print()
print("Compare. Lower is better. Median of 5 forks, 2000 calls each.")
print()
print("Time per call")
for key, label in order:
    us = med(key, "ns") / 2000 / 1000
    print(f"  {label:<{label_width}}  {us:8.1f} us   {ratio_note(key, 'ns')}")
print()
print("Bytes allocated per call")
for key, label in order:
    b = med(key, "alloc") / 2000
    print(f"  {label:<{label_width}}  {b:12,.0f} B   {ratio_note(key, 'alloc')}")
print()
print("GC count (2000 calls)")
for key, label in order:
    print(f"  {label:<{label_width}}  {med(key, 'gc'):8.0f}   {ratio_note(key, 'gc')}")
print()
print("Heap after GC")
for key, label in order:
    print(f"  {label:<{label_width}}  {med(key, 'heap'):12,.0f} B   {ratio_note(key, 'heap')}")
print()
print("Raw per-fork output: bench/loop-local/out/<name>.<fork>.txt")
PY
