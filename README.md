# mci-training

MCI training challenge — Software Developer Intern.

## Task 1 — `mci-core`

Core library adding two large numbers represented as strings, using the
elementary-school column-addition algorithm.

- Class: `dev.hieplp.mci.core.MyBigNumber`
- Method: `String sum(String stn1, String stn2)`
- Each calculation step is recorded via `java.lang.System.Logger` (JUL backend).

### Requirements

- JDK 17+
- Maven 3.9+

### Build

```bash
cd mci-core
mvn package
```

Produces `target/mci-core-0.0.1.jar`.

### Run tests

```bash
cd mci-core
mvn test
```

Unit tests live in `mci-core/src/test/java/dev/hieplp/mci/core/` and cover the
spec example (`1234 + 897 = 2131`), carry propagation, different-length
operands, zeros, 1000-digit numbers, and randomized cross-checks against
`BigInteger`.

### Usage

```java
MyBigNumber bn = new MyBigNumber();
String result = bn.sum("1234", "897"); // "2131"
```
