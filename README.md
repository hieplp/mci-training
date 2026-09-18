# mci-training

MCI training challenge — Software Developer Intern.

## Task 1 — `mci-core`

Core library adding two large numbers represented as strings, using the
elementary-school column-addition algorithm.

- Class: `dev.hieplp.mci.core.MyBigNumber`
- Method: `String sum(String stn1, String stn2)`
- Each calculation step is recorded via `java.lang.System.Logger` (JUL backend).
### Requirements

- JDK 21+ (Gradle toolchain auto-provisions 21 if absent)
- Gradle via the included wrapper — no install needed

### Build

```bash
cd mci-core
./gradlew build
```

Produces `build/libs/mci-core-0.0.1.jar`.

### Run tests

```bash
cd mci-core
./gradlew test
```


Unit tests live in `mci-core/src/test/java/dev/hieplp/mci/core/` and cover the
spec example (`1234 + 897 = 2131`), carry propagation, different-length
operands, zeros, 1000-digit numbers, randomized cross-checks against
`BigInteger`, and the step-by-step log records (replayed from a handler that
formats them only after `sum()` returns).

### Usage

```java
MyBigNumber bn = new MyBigNumber();
String result = bn.sum("1234", "897"); // "2131"
```

## Task 2 — `mci-web`
Spring Boot + Thymeleaf web app (hand-rolled CSS, no framework) that reuses `mci-core` (via a
Gradle composite build — `includeBuild '../mci-core'` in `settings.gradle`)
and shows the column-addition progress on the page.

### Run

```bash
cd mci-web
./run.sh          # serves on http://localhost:8081 (all interfaces)
```

Or `./gradlew bootRun` for the default port 8080.

### Run tests

```bash
cd mci-web
./gradlew test
```
