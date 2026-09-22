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

## Three stored files

Rerun: `./bench/loop-local/run.sh`

The script compiles all three against this repo's `AppLogger` and `NumberStrings`. JDK 21. One JVM per fork. `-XX:+UseParallelGC -Xms64m -Xmx512m`. Logging off. Every run checks the sum against `BigInteger`.

| directory | what it is |
| --- | --- |
| `bench/loop-local/old` | This repo's `MyBigNumber`. `StringBuilder` snapshots. Step locals declared inside the `while`. |
| `bench/loop-local/hoist-only` | That same method. The only change is declaring `firstDigit`, `secondDigit`, `carryIn`, `columnTotal`, `resultDigit`, `resultSoFar`, and `step` before the `while` and assigning inside. |
| `bench/loop-local/updated` | Exact copy of `~/Projects/training/mci-training/mci-core/src/main/java/dev/hieplp/mci/core/MyBigNumber.java`. `char[]` result, digit checks inside the loop, and those locals declared before the loop. |

`i` and `j` stay outside in all three. They are the loop indexes. All three agreed on 40 nines plus 40 ones: sum `11111111111111111111111111111111111111110`, 41 steps.

1,000 digits, 800 calls, 5 forks. Medians.

| | old | hoist-only | updated |
| --- | --- | --- | --- |
| Loop time | 122.4 ms | 121.9 ms | 46.9 ms |
| Instructions | 3,709,223,338 | 3,711,457,444 | 1,947,991,701 |
| Cycles | 1,203,596,086 | 1,207,762,413 | 642,632,643 |
| GC | 11 times, 2 ms | 11 times, 2 ms | 8 times, 1 ms |
| Bytes allocated | 954,118,384 | 954,094,304 | 505,759,040 |
| Heap after GC | 5,456,128 | 5,456,144 | 5,406,368 |
| Process RAM | 236,535,808 | 236,503,040 | 188,956,672 |
| Peak footprint | 213,189,544 | 213,320,640 | 165,839,856 |

Hoist-only versus old is noise. Loop time 121.9 ms vs 122.4 ms. Allocated bytes 954,094,304 vs 954,118,384. Same 11 collections. Process RAM matches. Moving the declarations does nothing you can measure.

Updated versus old is the other edits, not the declarations. About 2.6× less loop time (122.4 / 46.9). About half the instructions (3.71 billion vs 1.95 billion). About half the bytes allocated (954 MB vs 506 MB). Three fewer garbage collections. Process RAM 226 MB vs 180 MB. Heap left after GC is almost the same (5.46 MB vs 5.41 MB), because all three keep the same step list. The saving is garbage created during the call: `new StringBuilder(result).reverse()` every column, which the `char[]` version does not do.

The tables below are an earlier check. They compared two copies of the `char[]` algorithm, declaration site only. Those two sources are not what `old/` and `updated/` are now.


Logging off. Five forks. Bytes allocated were the same number on every fork.

| digits | calls | where declared | median time | bytes allocated, every fork | heap after GC | median max RSS |
| --- | --- | --- | --- | --- | --- | --- |
| 200 | 10 | inside the loop | 0.989 ms | 748552 | 1561504 | 50757632 |
| 200 | 10 | before the loop | 0.962 ms | 748552 | 1561512 | 50675712 |
| 1000 | 4 | inside the loop | 2.238 ms | 2831944 | 2123552 | 53608448 |
| 1000 | 4 | before the loop | 2.177 ms | 2831944 | 2123560 | 53592064 |

200-digit times, all five forks, milliseconds: inside 0.948, 1.010, 0.989, 1.229, 0.953. Before 0.948, 0.976, 0.995, 0.936, 0.962. The ranges overlap. About 2.7% on the median, one slow fork on the inside version. Not a win you can keep.

1000-digit times, milliseconds: inside 2.306, 2.289, 2.238, 2.185, 2.223. Before 2.161, 2.190, 2.177, 2.176, 2.253. Same overlap.

Not every RAM number is the same. Three different measurements:

Bytes allocated by the addition are the same on a clean run. A follow-up probe, logging off, 3 calls, 3 forks, `ThreadMXBean.getCurrentThreadAllocatedBytes`:

| digits | inside the loop | before the loop |
| --- | --- | --- |
| 20 | 31568, every fork | 31568, every fork |
| 200 | 168488, every fork | 168488, every fork |
| 1000 | 1951568, every fork | 1951568, every fork |

A longer run (1000 digits, 8 calls) was 5168408 on most forks. One inside fork and one before fork were 5168664, 256 bytes higher. Either side can hit that. It is not "before uses more."

Heap left after `System.gc()` is not the same. Before is always 8 bytes higher, and the gap is already there before any addition:

| digits | heap, nothing kept | heap, result kept | heap, result dropped |
| --- | --- | --- | --- |
| 20, inside | 1429536 | 1286984 | 1284608 |
| 20, before | 1429544 | 1286992 | 1284616 |
| 200, inside | 1429536 | 1324776 | 1284960 |
| 200, before | 1429544 | 1324784 | 1284968 |
| 1000, inside | 1429536 | 1884776 | 1286560 |
| 1000, before | 1429544 | 1884784 | 1286568 |

Every cell is +8 for the before-the-loop class. The step list is the same size. The 8 bytes do not grow with the number of digits, so they are not the loop. The before-the-loop class file is 3358 bytes on disk; the inside-the-loop class file is 3284. One extra stack slot (`locals=20` vs `19`) plus the `""` and `null` initializers. That fixed 8 bytes is the whole retained-heap gap.

Process RSS is not locked to one number. 1000 digits, 8 calls, 3 forks: inside 54329344, 54165504, 54460416. Before 54329344, 54263808, 54329344. They overlap. Spread is about 0.3 MB on a 52 MB process.


Logging on (JUL INFO, so each step still formats a sentence). 200 digits, 4 calls, 3 forks.

| where declared | median time | bytes allocated |
| --- | --- | --- |
| inside the loop | 34.1 ms | 16712120, 16711984, 16712072 |
| before the loop | 33.4 ms | 16712072, 16712072, 16712072 |

Still the same ballpark. Logging, not the declaration line, is why this is about 35× slower and about 22× more bytes than the logging-off run of the same size.

## CPU, RAM, and the other counters

Same stored pair. Logging off. 1,000 digits, 800 calls, 5 fresh JVMs each. `-Xms64m -Xmx512m`. Loop time is `nanoTime` around the calls only. The other counters are the whole process from `/usr/bin/time -l`, so they include JVM startup. Medians.

| | Inside the loop | Before the loop |
| --- | --- | --- |
| Loop time | 46.3 ms | 49.3 ms |
| User CPU | 0.13 s | 0.13 s |
| System CPU | 0.01 s | 0.01 s |
| Instructions retired | 1,941,651,906 | 1,943,997,570 |
| Cycles | 640,105,846 | 642,807,200 |
| GC | 8 collections, 1 ms | 8 collections, 1 ms |
| Bytes allocated | 505,662,720 | 505,518,240 |
| Heap after GC | 5,406,312 | 5,406,328 |
| Max RSS | 189,120,512 | 188,891,136 |
| Peak footprint | 165,987,264 | 165,839,880 |
| Page reclaims | 12,655 | 12,654 |
| Page faults | 0 | 0 |
| Involuntary context switches | 611 | 615 |

Instructions differ by 0.12%. Cycles by 0.42%. Allocated bytes overlap (old 505,229,280–505,807,200, new 505,397,840–505,747,320). RSS overlaps. GC count and GC time match. Page faults were 0 except one inside run that had 3. Context switches sit in the same band.

The loop-time median is 6.6% higher for the before-the-loop copy (46.3 ms vs 49.3 ms). User CPU time, at 0.01 s resolution, is 0.13 s both ways. A 3 ms gap does not show up there. The five loop times overlap at the edge: inside 45.8, 46.0, 46.3, 46.4, 55.1 ms; before 46.1, 48.7, 49.3, 49.4, 53.1 ms. Not a CPU win for moving the declarations out. The instruction count says the work is the same.



## Recommendation

Keep the rule only as a style rule, if you want one declaration site. It is not a CPU win and not a RAM win. The stored pair in `bench/loop-local` is the check: same algorithm, declaration site only, allocated bytes identical on every fork. For a reference, hoisting can retain the last object until the method returns (measured on a separate bench: one extra `byte[1000000]`, 1000024 bytes after GC). Do not refactor `MyBigNumber` to satisfy the rule for speed or memory. If a loop is slow or fat, look at repeated `new` and at INFO logging of `Step.toString()`, not at where `firstDigit` is written.
