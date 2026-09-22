# Loop locals: declaration site is not a speedup

Question: does the coding rule "declare every local before the loop, assign inside" make the loop faster?

Answer: no. On HotSpot 21 the loop body is the same instruction stream. Moving the declaration out adds a few dead stores before the loop and does not change throughput. Reusing an object is a different change, and that one does show up.

No production code changed. `MyBigNumber.sumWithSteps` still declares `firstDigit`, `secondDigit`, `carryIn`, `columnTotal`, `resultDigit`, `resultSoFar`, and `step` inside the `while`. Applying the rule there would not speed it up. The O(n²) cost is the per-step `String` / `Step` snapshot, already noted on the class.

## Bytecode

JDK: Eclipse Temurin 21.0.12.1+1 (`~/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2`), `javac -g:none`, `javap -c -p -v`.

Two methods, same work. One declares `digit`, `other`, `column` before the `while` and assigns inside. The other declares them in the body. The index stays in the header in both, which the rule already allows.

Both methods: `stack=5, locals=8`. Same frame size.

Hoisted form, once per call, before the loop (not per iteration):

```
iconst_0
istore 4    // digit = 0
iconst_0
istore 5    // other = 0
iconst_0
istore 6    // column = 0
```

Loop body, hoisted (`declaredBefore`, back-edge at offset 14):

```
aload_1
iload 7
caload
bipush 48
isub
istore 4
iload 4
iconst_3
imul
iload 7
bipush 7
iand
iadd
istore 5
iload 4
iload 5
iadd
istore 6
lload_2
iload 6
iload 6
bipush 10
idiv
iadd
i2l
ladd
lstore_2
iinc 7, 1
goto 14
```

Loop body, declared inside (`declaredInside`, back-edge at offset 5):

```
aload_1
iload 4
caload
bipush 48
isub
istore 5
iload 5
iconst_3
imul
iload 4
bipush 7
iand
iadd
istore 6
iload 5
iload 6
iadd
istore 7
lload_2
iload 7
iload 7
bipush 10
idiv
iadd
i2l
ladd
lstore_2
iinc 4, 1
goto 5
```

Same opcodes. Only slot numbers differ, because the hoisted form reserved slots 4–6 before the loop and used slot 7 for the index. The JVM allocates `max_locals` slots when the frame is created, not when the source declaration is reached. A declaration is not an instruction.

With `-g`, the `LocalVariableTable` range is longer for the hoisted names. That table is debug info. It is not executed. The `StackMapTable` at the loop head is also wider in the hoisted form (verifier metadata, not a per-iteration cost).

`-XX:+PrintCompilation` on the timing run: both methods are C2 (level 4) and OSR-compiled at the loop head (`declaredBefore @ 14`, `declaredInside @ 5`).

## Timing

Same JDK, `-XX:+UseParallelGC`, one mode per JVM so compile order cannot favor either side. Apple M4 Pro. Not JMH. Five forks, `n=10000`, 5000 calls per sample, 9 samples, report the median.

| mode | fork medians (ns/call) | mean | sd |
| --- | --- | --- | --- |
| declared before | 7769.0, 7862.2, 7847.0, 7875.0, 7705.4 | 7811.7 | 64.7 |
| declared inside | 7765.9, 7818.1, 7882.8, 7852.4, 7776.2 | 7819.1 | 44.4 |

Inside / before = 1.0009 (+0.094%). Ranges overlap. That is noise, not a win. The three extra stores run once per call; at 10_000 iterations they disappear.

## What does move the number

Hoisting a declaration does not hoist the assignment. `partial = new StringBuilder()` inside the body still allocates every iteration. Reusing one builder is a different edit.

Same harness, `n=2000`, 2000 calls, 3 forks. `alloc-inside` constructs a `StringBuilder` each iteration. `alloc-reused` constructs one and calls `setLength(0)`.

| mode | fork medians (ns/call) | mean |
| --- | --- | --- |
| alloc-inside | 10258.6, 5921.3, 6265.6 | 7481.8 |
| alloc-reused | 3437.4, 3442.1, 3497.7 | 3459.1 |

Every reused fork beat every allocating fork. Mean ratio about 2.2×. Variance on the allocating side is GC. That is the kind of change that shows up. The coding rule does not require it.


## RAM

Same JDK, `-XX:+UseParallelGC`, `-Xms64m -Xmx256m` for the microbench. Allocated bytes are `ThreadMXBean.getCurrentThreadAllocatedBytes` on the calling thread. Process RAM is macOS `maximum resident set size` from `/usr/bin/time -l`. One mode per JVM.

### Declaration site does not allocate

Primitive temps, `n=4000`, 200 calls, 3 forks. The counter includes harness overhead. It does not include a `new` in the loop.

| mode | alloc bytes (every fork) | GC count | max RSS |
| --- | --- | --- | --- |
| declared before | 291744 | 0 | 48791552 |
| declared inside | 291744 | 0 | 48889856 |

Identical allocation counter. RSS differs by 98 KB, which is noise next to a 48 MB process.

Reference form of the rule: `StringBuilder partial = null` before the loop, `partial = new StringBuilder()` inside. Versus the same `new` declared in the body. `n=4000`, 200 calls, 3 forks.

| mode | alloc bytes per fork | mean alloc | GC count | max RSS |
| --- | --- | --- | --- | --- |
| ref declared before | 42392392, 41810728, 41808016 | 42003712 | 2 | 65241088 |
| ref declared inside | 42003904, 42017656, 42298504 | 42106688 | 2 | 65290240 |

Inside / before = 1.002. Same two collections. RSS differs by 48 KB. Moving the declaration does not move the `new`, so it does not move the heap.

Frame size does not move either. The primitive pair was `locals=8` both ways. A local slot exists for the frame, not for the source line that names it.

### Hoisting a reference can keep the last object alive

`byte[1_000_000]` assigned each of 4 iterations, then `System.gc()` before the method returns. Allocated bytes were 4292456 in every fork of every mode. Retained heap was not.

| mode | heap after GC (3 forks, identical) |
| --- | --- |
| reference declared before the loop | 2627952 |
| reference declared in the body | 1627928 |
| body local, then an `int` overwrites that slot | 1627936 |

Before minus inside = 1000024 bytes. That is one `byte[1000000]` plus the 24-byte array header. `javap -v` shows why. The hoisted form's stack map at the GC call still lists the `byte[]` local, so it is a GC root until the method returns. The in-body form's stack map at that call does not list the slot, and the collector reclaimed the array. Overwriting the slot with an `int` does the same. This is a retained-heap cost of the rule for references, not a saving. It does not apply to the `int` temps in `sumWithSteps`.

### What actually allocates

One reused `StringBuilder` (`setLength(0)` each iteration), same `n` and call count:

| mode | alloc bytes | GC count | max RSS |
| --- | --- | --- | --- |
| `new` each iteration | ~42000000 | 2 | ~65200000 |
| one builder, reused | 303112 | 0 | 49528832 |

About 139× fewer bytes allocated, no GC, and about 16 MB less max RSS. That is object reuse, which the coding rule does not require.

### `sumWithSteps` on this tree

Real `MyBigNumber`, classpath `mci-core/build/classes/java/main`. `-Xms32m -Xmx512m`. `sum` is `sumWithSteps(...).sum()`, so it allocates the steps and then drops them. JUL default is INFO, and each step calls `Step.toString()` (`MessageFormat`). `mci.log=off` sets that logger to `OFF` before the first call.

Log on:

| call | digits | calls | ns | alloc bytes | heap after GC | max RSS |
| --- | --- | --- | --- | --- | --- | --- |
| steps | 50 | 5 | 23264542 | 5095632 | 2151752 | 69091328 |
| steps | 200 | 5 | 42393042 | 21057232 | 2186984 | 94781440 |
| steps | 1000 | 2 | 62872792 | 53205392 | 3269160 | 105299968 |
| sum | 50 | 5 | 23266708 | 5095656 | 2145528 | 69369856 |
| sum | 200 | 5 | 42497292 | 21057304 | 2147408 | 94011392 |
| sum | 1000 | 2 | 63243458 | 53205464 | 2662568 | 104497152 |

Log off:

| call | digits | calls | ns | alloc bytes | heap after GC | max RSS |
| --- | --- | --- | --- | --- | --- | --- |
| steps | 50 | 5 | 413334 | 66448 | 1467736 | 48168960 |
| steps | 200 | 5 | 989500 | 405528 | 1501472 | 48807936 |
| steps | 1000 | 2 | 889042 | 2406960 | 2061472 | 51806208 |
| sum | 50 | 5 | 420625 | 66448 | 1461456 | 48414720 |
| sum | 200 | 5 | 966625 | 405528 | 1461896 | 48676864 |
| sum | 1000 | 2 | 879250 | 2406960 | 1464304 | 50544640 |

At 200 digits and 5 calls, INFO logging allocated 21057232 bytes and took 42.4 ms. Logging off allocated 405528 bytes and took 0.99 ms. About 52× the bytes and 43× the time, from `MessageFormat` plus the per-step snapshot, not from where `firstDigit` is declared. `sum` and `sumWithSteps` allocate the same amount during the call. The only retained-heap gap is after return: at 1000 digits with logging off, keeping `SumResult` left 2061472 bytes after GC, and keeping only the sum string left 1464304. Compact strings make `resultSoFar` one byte per digit; the retained strings are still O(n²) because step k copies k digits.

## Same algorithm, two stored files

The file under test is a copy of `~/Projects/training/mci-training/mci-core/src/main/java/dev/hieplp/mci/core/MyBigNumber.java`. It already declares the step locals before the `while`. It also uses a `char[]` result, which is a different algorithm from the `StringBuilder` version in `mci-core`. Those two changes are not mixed here.

Stored pair, same algorithm, only the declaration site differs:

- `bench/loop-local/old/MyBigNumber.java` — `firstCharacter`, `firstDigit`, `secondCharacter`, `secondDigit`, `carryIn`, `columnTotal`, `resultDigit`, `resultSoFar`, and `step` are declared inside the loop.
- `bench/loop-local/new/MyBigNumber.java` — exact copy of the file above. Those locals are declared before the loop and assigned inside.

`i`, `j`, and `resultIndex` stay outside in both. They have to. They are the loop indexes.

Rerun:

```bash
./bench/loop-local/run.sh
```

The script compiles both against this repo's `AppLogger` and `NumberStrings`, checks that both sums match `BigInteger`, then measures time, allocated bytes, heap after GC, and macOS max RSS. JDK 21. One JVM per fork. `-XX:+UseParallelGC -Xms32m -Xmx512m`.

Both sides agreed on 40 nines plus 40 ones: sum `11111111111111111111111111111111111111110`, 41 steps. Every timed run also checked the sum against `BigInteger`.

`javap`: old `sumWithSteps` is `stack=10, locals=19`. New is `stack=10, locals=20`. One extra stack slot. That is not heap.

Logging off. Five forks. Bytes allocated were the same number on every fork.

| digits | calls | where declared | median time | bytes allocated, every fork | heap after GC | median max RSS |
| --- | --- | --- | --- | --- | --- | --- |
| 200 | 10 | inside the loop | 0.989 ms | 748552 | 1561504 | 50757632 |
| 200 | 10 | before the loop | 0.962 ms | 748552 | 1561512 | 50675712 |
| 1000 | 4 | inside the loop | 2.238 ms | 2831944 | 2123552 | 53608448 |
| 1000 | 4 | before the loop | 2.177 ms | 2831944 | 2123560 | 53592064 |

200-digit times, all five forks, milliseconds: inside 0.948, 1.010, 0.989, 1.229, 0.953. Before 0.948, 0.976, 0.995, 0.936, 0.962. The ranges overlap. About 2.7% on the median, one slow fork on the inside version. Not a win you can keep.

1000-digit times, milliseconds: inside 2.306, 2.289, 2.238, 2.185, 2.223. Before 2.161, 2.190, 2.177, 2.176, 2.253. Same overlap.

Heap after GC differs by 8 bytes. Process RSS differs by tens of kilobytes on a 50 MB process. The allocated-byte counter did not move by one byte.

Logging on (JUL INFO, so each step still formats a sentence). 200 digits, 4 calls, 3 forks.

| where declared | median time | bytes allocated |
| --- | --- | --- |
| inside the loop | 34.1 ms | 16712120, 16711984, 16712072 |
| before the loop | 33.4 ms | 16712072, 16712072, 16712072 |

Still the same ballpark. Logging, not the declaration line, is why this is about 35× slower and about 22× more bytes than the logging-off run of the same size.


## Recommendation

Keep the rule only as a style rule, if you want one declaration site. It is not a CPU win and not a RAM win. The stored pair in `bench/loop-local` is the check: same algorithm, declaration site only, allocated bytes identical on every fork. For a reference, hoisting can retain the last object until the method returns (measured on a separate bench: one extra `byte[1000000]`, 1000024 bytes after GC). Do not refactor `MyBigNumber` to satisfy the rule for speed or memory. If a loop is slow or fat, look at repeated `new` and at INFO logging of `Step.toString()`, not at where `firstDigit` is written.
