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

## Recommendation

Keep the rule only as a style rule, if you want one declaration site. Do not cite it as a performance change, and do not refactor `MyBigNumber` to satisfy it for speed. If a loop is slow, look for repeated allocation (`new StringBuilder`, `new String`, `new Step` per column), not for where `int` is written.
