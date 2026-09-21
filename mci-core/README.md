# mci-core

Core utilities for the MCI training project. Java 21, zero runtime dependencies.

## What's inside

| Class | Purpose |
|---|---|
| `MyBigNumber` | Adds two non-negative integers given as decimal strings (arbitrary length), logging each column-addition step. |
| `NumberStrings` | Validation (`requireDigits`) and normalization (`stripLeadingZeros`) for digit-string operands. |
| `AppLogger` | Thin facade over `java.lang.System.Logger` (JUL by default) so the backend can be swapped without touching call sites. |

Package: `dev.hieplp.mci.core`

## Requirements

- JDK 21+ (Gradle toolchain resolves it automatically via the foojay plugin if not installed).

## Use it in your project

**Option A — build the jar and add it to your classpath:**

```bash
./gradlew jar        # produces build/libs/mci-core-0.0.1.jar
```

**Option B — include it in a multi-project Gradle build:**

```groovy
// settings.gradle of the consuming project
include 'mci-core'
project(':mci-core').projectDir = file('path/to/mci-core')

// build.gradle of the consuming module
dependencies {
    implementation project(':mci-core')
}
```

## Quick start

```java
import dev.hieplp.mci.core.MyBigNumber;

String sum = new MyBigNumber().sum("999", "1");   // "1000"
```

Invalid input (null, empty, non-digit) throws `IllegalArgumentException`.

## Docs

- [docs/usage.md](docs/usage.md) — per-class usage and examples
- [docs/logging.md](docs/logging.md) — how logging works and how to configure/redirect it
- [docs/performance.md](docs/performance.md) — before/after benchmark of the `sum()` hot path

## Build & test

```bash
./gradlew build      # compile + run tests
./gradlew test       # tests only (JUnit 5)
```

Tests live in `src/test/java/dev/hieplp/mci/core/` and cover the spec
example (`1234 + 897 = 2131`), carry propagation, different-length
operands, zeros, 1000-digit numbers, randomized cross-checks against
`BigInteger`, and the step-by-step log records.
