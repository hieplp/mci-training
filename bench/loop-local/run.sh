#!/bin/sh
# JMH 1.37 plus its gc profiler. Three copies, one classpath each.
#   old         this repo's MyBigNumber
#   hoist-only  same method, while-body locals declared before the loop
#   updated     char[] result, locals declared before the loop
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
JMH="$OUT/jmh"
rm -rf "$OUT"
mkdir -p "$OUT/lib" "$OUT/old" "$OUT/hoist-only" "$OUT/updated" "$OUT/bench" "$JMH"

fetch() {
  name=$1
  url=$2
  curl -fsSL -o "$JMH/$name" "$url"
}
fetch jmh-core-1.37.jar https://repo1.maven.org/maven2/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar
fetch jmh-generator-annprocess-1.37.jar https://repo1.maven.org/maven2/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar
fetch jopt-simple-5.0.4.jar https://repo1.maven.org/maven2/net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar
fetch commons-math3-3.6.1.jar https://repo1.maven.org/maven2/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar
JMH_CP="$JMH/jmh-core-1.37.jar:$JMH/jopt-simple-5.0.4.jar:$JMH/commons-math3-3.6.1.jar"
PROC="$JMH/jmh-generator-annprocess-1.37.jar"

javac -d "$OUT/lib" "$SRC/AppLogger.java" "$SRC/NumberStrings.java"
javac -cp "$OUT/lib" -d "$OUT/old" "$ROOT/bench/loop-local/old/MyBigNumber.java"
javac -cp "$OUT/lib" -d "$OUT/hoist-only" "$ROOT/bench/loop-local/hoist-only/MyBigNumber.java"
javac -cp "$OUT/lib" -d "$OUT/updated" "$ROOT/bench/loop-local/updated/MyBigNumber.java"
javac -cp "$OUT/lib:$OUT/old" -d "$OUT/bench" "$ROOT/bench/loop-local/HoistBench.java"
javac -cp "$OUT/lib:$OUT/old:$JMH_CP" -processorpath "$PROC:$JMH_CP" \
  -d "$OUT/bench" "$ROOT/bench/loop-local/AddBench.java"

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

echo "===== JMH 1.37, gc profiler, 1000 digits, logging off ====="
for variant in old hoist-only updated; do
  echo "----- $variant -----"
  java -cp "$OUT/$variant:$OUT/lib:$OUT/bench:$JMH_CP" org.openjdk.jmh.Main -prof gc
done
