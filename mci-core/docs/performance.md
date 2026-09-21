# Performance

`MyBigNumber.sum()` no longer builds a step object and a partial-result
snapshot per column when the per-step INFO logging is disabled. This page
records the before/after measurement.

## Environment and method

| | |
|---|---|
| Machine | Apple M4 Pro (arm64), macOS 25.5.0 |
| JVM | Eclipse Temurin 21.0.12.1+1 LTS (the Gradle toolchain), no extra flags |
| Time | `System.nanoTime()` around a single call; median of 7 runs |
| Memory | `com.sun.management.ThreadMXBean#getThreadAllocatedBytes` on the calling thread; **bytes allocated** per call, not live heap |
| Warmup | 3 runs of *every* variant before *any* variant is measured, so the first-measured variant does not pay for JIT compilation the others skip |

Operands are random fixed-length decimal strings (no leading zero); both
variants receive the identical strings.

## INFO disabled (JUL loggers `dev.hieplp.mci.core.MyBigNumber` and `dev.hieplp.mci.core.LegacyMyBigNumber` at WARNING)

The realistic hot path: callers that want the sum, not the commentary.

| n | variant | median time (ms) | allocated bytes/call |
|---:|---|---:|---:|
| 1 000 | old `sum()` | 0.129 | 1,179,392 |
| 1 000 | new `sum()` | 0.004 | 2,128 |
| 1 000 | old `sumWithSteps()` | 0.122 | 1,179,392 |
| 1 000 | new `sumWithSteps()` | 0.106 | 611,344 |
| 2 000 | old `sum()` | 0.424 | 4,361,936 |
| 2 000 | new `sum()` | 0.006 | 4,128 |
| 2 000 | old `sumWithSteps()` | 0.435 | 4,361,936 |
| 2 000 | new `sumWithSteps()` | 0.394 | 2,225,888 |
| 10 000 | old `sum()` | 9.523 | 101,809,336 |
| 10 000 | new `sum()` | 0.029 | 20,128 |
| 10 000 | old `sumWithSteps()` | 9.837 | 101,809,336 |
| 10 000 | new `sumWithSteps()` | 9.045 | 51,129,288 |
| 50 000 | old `sum()` | 261.460 | 2,509,054,464 |
| 50 000 | new `sum()` | 0.145 | 100,128 |
| 50 000 | old `sumWithSteps()` | 250.779 | 2,509,054,464 |
| 50 000 | new `sumWithSteps()` | 227.124 | 1,255,654,416 |
| 100 000 | old `sum()` | 1,080.400 | 10,017,681,320 |
| 100 000 | new `sum()` | 0.290 | 200,128 |
| 100 000 | old `sumWithSteps()` | 1,152.511 | 10,017,681,320 |
| 100 000 | new `sumWithSteps()` | 1,038.372 | 5,010,881,272 |

Ratios (old ÷ new):

| n | `sum()` time | `sum()` bytes | `sumWithSteps()` time | `sumWithSteps()` bytes |
|---:|---:|---:|---:|---:|
| 1 000 | 32× | 554× | 1.2× | 1.9× |
| 2 000 | 71× | 1,057× | 1.1× | 2.0× |
| 10 000 | 328× | 5,058× | 1.1× | 2.0× |
| 50 000 | 1,803× | 25,058× | 1.1× | 2.0× |
| 100 000 | 3,726× | 50,056× | 1.1× | 2.0× |

The scaling is the point: `sum()` time and bytes now grow linearly
(`bytes ≈ 2n + 128`) instead of quadratically (`bytes ≈ n²`), so the gap
widens with every extra digit. At n = 100 000 the new `sum()` allocates
50,000× less and takes about a quarter of a millisecond where the old one
took a second.

`sumWithSteps()` is inherently quadratic — it must hand back n
partial-result strings — so its win is the constant factor: exactly half the
allocated bytes (~1.1× faster), plus a smaller live heap while it runs.

## INFO enabled (n = 2 000, handler formats each record)

This is the logging path itself: `sum()` builds each step sentence inside the
supplier it hands to `AppLogger.info(Supplier)`, and the JUL handler formats
every record with `MessageFormat` (as `SimpleFormatter` does) into a discard
sink. No console I/O is measured.

| variant | median time (ms) | allocated bytes/call |
|---|---:|---:|
| old `sum()` | 8.172 | 38,175,448 |
| new `sum()` | 6.267 | 32,363,144 |
| old `sumWithSteps()` | 5.736 | 38,175,448 |
| new `sumWithSteps()` | 5.593 | 36,039,400 |

With logging on, the O(n²) snapshot and `MessageFormat` dominate both
variants, so this is roughly parity: the new variant allocates 6–18% less
per call, while the timings vary by more than that from run to run. `old
sum()` and `old sumWithSteps()` in the table above are *the same code*, and
still differ by 40% — GC inside the measured window is the noise source, so
read the byte counts here, not the milliseconds.

The point of the lazy log is not a faster log line, it is that the log line is
opt-in. At n = 2 000 the same new `sum()` costs **6.267 ms / 32.4 MB** with
INFO on versus **0.006 ms / 4.1 kB** with INFO off: ~1,000× the time and
~7,800× the allocation for commentary the caller did not ask for.

## What changed and why

| Fix | Effect |
|---|---|
| `sum()` and `sumWithSteps()` share one column-addition core (`addColumns`), with `sum()` passing the non-capturing `logStep` visitor | One loop and one validate/strip prologue instead of two near-identical copies; `sum()` still builds no `Step` records, no `ArrayList` growth, no `Step.toString()` |
| Per-column snapshot + log line moved into `AppLogger.info(Supplier)` — `System.Logger.log(Level, Supplier)` runs the supplier only when INFO is enabled | Same laziness as the explicit guard it replaced, without the guard: removes the O(n) `StringBuilder(result).reverse().toString()` per column from the default path → O(n), not O(n²) |
| Step sentence formatted inside the supplier `AppLogger.info(Supplier)` from `Step.toString()` rather than left to the backend | Keeps the exact `Step.toString()` text and formats it only when the line will actually be logged; the `Step` itself is built lazily inside the supplier, so INFO off allocates nothing per column |
| `result.append((char) ('0' + digit))` instead of `result.append(int)` | Integer-append boxing path replaced by a direct char append |
| Reused a scratch `StringBuilder` for snapshots in `sumWithSteps()` instead of `new StringBuilder(result)` per column | Removes a `StringBuilder` (object + backing array) per column; ~2× less allocated bytes overall |
| `Collections.unmodifiableList(steps)` instead of `List.copyOf(steps)` | Removes a full copy of the step list; the list is locally created and never shared |

No public API changed: `sum`, `sumWithSteps`, `SumResult`, `Step` and the
`MessageFormat` step sentence are untouched. `AppLogger` gained one method,
`info(Supplier<String>)`, which logs a lazily built message.

## Correctness

The benchmark compares against the pre-refactor algorithm preserved verbatim
in `LegacyMyBigNumber` (a top-level class in the test sources) and asserts the
new result equals the old one for 6 sizes × 4 operand shapes (equal length,
all nines, single digit, 1:3 length ratio) plus 50 random operand pairs:

```
sanity: new sum() == old sum() for 6 sizes x 4 shapes + 50 random pairs
```

`./gradlew test` additionally covers the logged line sequence and the
returned step data, unmodified.

## Reproduce

```bash
cd mci-core
./gradlew test                                   # correctness
./gradlew compileTestJava                        # compiles the benchmark
java -cp build/classes/java/main:build/classes/java/test dev.hieplp.mci.core.Bench
```

The benchmark is a `main` method, not a JUnit test, so `gradle test` never
runs it. It needs JDK 21+ (any JDK; the numbers above are Temurin 21) and
takes ~45 s end to end — almost all of it the quadratic baseline at
n = 100 000. To iterate quickly, trim `SIZES` in `Bench.java`.

Timings drift a few percent between runs with INFO off, more (up to ~40%)
with INFO on, where each call allocates tens of megabytes and GC lands
inside the measured window. The byte counts are exact and repeat to within a
few tens of bytes. The ratios are what matter — the asymptotics, not the
third decimal.

## Appendix: variable declaration placement

Claim: declaring the column ints (`firstDigit`, `secondDigit`, `carryIn`,
`columnTotal`, `resultDigit`) inside the `while` body costs something versus
hoisting them above the loop. `VarHoistBench` runs the real
`MyBigNumber.sum()` arithmetic loop both ways on two identical seeded
10,000-digit operands — 2,000 interleaved warmup calls, then 500 interleaved
A,B,A,B measured calls — so drift, JIT and GC hit both variants alike. Each
call records wall time (`System.nanoTime()`) and bytes allocated on the
calling thread (`ThreadMXBean#getThreadAllocatedBytes`).

| variant | n | mean ms | stddev ms | median ms | min ms | max ms | allocated B/call |
|---|---:|---:|---:|---:|---:|---:|---:|
| inside loop | 500 | 0.023955 | 0.004230 | 0.022833 | 0.020541 | 0.075750 | 20,096 |
| outside loop | 500 | 0.024088 | 0.004331 | 0.023125 | 0.020666 | 0.088584 | 20,096 |

Welch two-sample t-test on the nanosecond samples: mean difference
inside − outside = **−0.000132 ms**, `t = −0.489`, `df ≈ 997.4`,
`p = 0.625` → not significant at p < 0.05. The confidence intervals overlap
almost exactly and both variants allocate the identical 20,096 B/call.

Conclusion: placement is statistically indistinguishable, as expected —
`javac` compiles both methods to the same 79-instruction opcode sequence,
differing only in local-variable slot numbering (verified with `javap -c`
over the two methods). Hoisting the declarations buys nothing; keep whichever
reads better.

Reproduce:

```bash
cd mci-core
./gradlew compileTestJava
java -cp build/classes/java/main:build/classes/java/test dev.hieplp.mci.core.VarHoistBench
```
