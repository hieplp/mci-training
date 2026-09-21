# Logging

`mci-core` logs through `AppLogger`, a thin facade over
`java.lang.System.Logger`. With no other logging framework on the
classpath, `System.Logger` routes to **JUL** (`java.util.logging`) — so
everything below is standard JUL configuration.

## What gets logged

| Level | Source |
|---|---|
| INFO | `MyBigNumber.sum` — input, every column-addition step, final result |
| WARNING | `NumberStrings.requireDigits` — rejected operands |
| DEBUG | available on `AppLogger`, unused by the library today |

Logger names are the fully-qualified class names, e.g.
`dev.hieplp.mci.core.MyBigNumber`.

## Quiet the per-step output

Each step logs an immutable partial-result snapshot, so log volume is O(n²)
in operand length, and `sum()` only pays for it while INFO is enabled: with
INFO off it stays O(n) in time and memory. For large inputs, raise the level:

```properties
# logging.properties
handlers=java.util.logging.ConsoleHandler
.level=INFO
dev.hieplp.mci.core.MyBigNumber.level=WARNING
```

```bash
java -Djava.util.logging.config.file=logging.properties -cp mci-core-0.0.1.jar ...
```

Or programmatically:

```java
java.util.logging.Logger.getLogger("dev.hieplp.mci.core.MyBigNumber")
    .setLevel(java.util.logging.Level.WARNING);
```

## Redirect to another backend

`System.Logger` is a facade: whichever `System.LoggerFinder` is on the
class path wins. To route into SLF4J/Logback (e.g. inside a Spring app),
add `org.slf4j:slf4j-jdk-platform-logging` and configure SLF4J as usual —
no changes to `mci-core` call sites needed. That indirection is the
reason `AppLogger` exists.
