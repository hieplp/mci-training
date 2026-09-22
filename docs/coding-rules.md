# Coding rules

Rules for humans and Copilot. The code wins if this file disagrees with it.

Also read [api-rules.md](api-rules.md) and [security-rules.md](security-rules.md).

## 1. Layout

Java 21. Two Gradle projects side by side. Not one multi-module build. Not a root `src/` plus `tests/unit/`.

- `mci-core` — package `dev.hieplp.mci.core`, zero runtime dependencies.
- `mci-web` — Spring Boot 3.5 + Thymeleaf, package `dev.hieplp.mci.web`. Depends on core through `includeBuild '../mci-core'`.

Do not add a database, ORM, REST resource, message bus, or third module. Do not add a runtime dependency to `mci-core`. Do not add a web dependency unless the issue names it.

- **GOOD:** a change stays in `mci-core` or `mci-web` and adds no dependency.
- **BAD:** a new module, or a JDBC driver on `mci-core`.

## 2. Naming

`PascalCase` for classes, `camelCase` for methods and variables, `UPPER_SNAKE_CASE` for constants.

- **GOOD:** `MyBigNumber`, `sumWithSteps`, `private static final AppLogger LOG`.
- **BAD:** `my_big_number`, `Sum_With_Steps`, `private static final AppLogger log`.

## 3. Public API

Public API is `MyBigNumber.sum` and `sumWithSteps`. Do not change `sum(String, String)`. Do not drop the step history.

- **GOOD:** `public String sum(String stn1, String stn2)` still returns the sum, and `sumWithSteps` still returns the steps.
- **BAD:** adding a third parameter, or deleting `SumResult`.

## 4. Operands

Operands are non-negative ASCII digit strings. `NumberStrings.requireDigits` rejects null, empty, and non-digits with `IllegalArgumentException`. The message names the operand (`stn1` or `stn2`).

- **GOOD:** `NumberStrings.requireDigits(stn1, "stn1");`
- **BAD:** treating `"12a"` as zero, or returning null for an empty operand.

## 5. Exceptions

Do not throw generic `Exception` or `RuntimeException`. Do not catch an input error and return a sentinel.

- **GOOD:** `throw new IllegalArgumentException("stn1 must not be null or empty");`
- **BAD:** `throw new RuntimeException("bad");` or `catch (Exception e) { return "0"; }`.

## 6. Logging

Log through `AppLogger` with `{0}` placeholders. Do not use `System.out` or `System.err`. Do not log secrets or PII: passwords, tokens, API keys, emails, phone numbers, connection strings.

- **GOOD:** `LOG.warn("Invalid operand {0}: non-digit at index {1}", "stn1", index);`
- **BAD:** `System.out.println(password);` or `LOG.info("email={0} token={1}", email, token);`

## 7. Loop locals

Declare every local a loop body uses before the loop. Assign inside the body. Do not declare those locals in the body. A `for` index may stay in the `for` header.

**GOOD:**

```java
int digit = 0;
while (hasMore) {
    digit = nextDigit();
}
```

**BAD:**

```java
while (hasMore) {
    int digit = nextDigit();
}
```

## 8. Web surface

One page: `GET /` with `stn1` and `stn2`. `SumController` → `SumService` → `MyBigNumber`. CSS is `mci-web/src/main/resources/static/css/app.css`. Do not publish a jar to make the web module compile.

- **GOOD:** the form submits `stn1` and `stn2` to `GET /`.
- **BAD:** a new `POST /api/workorders` mapping in `mci-web`.

## 9. Injection

Constructor injection. No field `@Autowired`. No Lombok.

**GOOD:**

```java
public SumController(SumService sumService) {
    this.sumService = sumService;
}
```

**BAD:** `@Autowired private SumService sumService;` or `@RequiredArgsConstructor`.

## 10. Shape

4-space indent. Opening brace on the same line. No wildcard imports. Records for results. JUnit 5.

- **GOOD:** `public record SumResult(String sum, List<Step> steps) {}`
- **BAD:** a result bean with setters, or `import java.util.*;` in main code.

## 11. Core purity

Do not use `BigInteger` in main code. Tests may use it as an oracle.

- **GOOD:** column addition on digit characters.
- **BAD:** `new BigInteger(stn1).add(new BigInteger(stn2))` inside `MyBigNumber`.

## 12. Branches

Cut each change from `main`. Do not add commits to an existing feature branch. Details: [CONTRIBUTING.md](../CONTRIBUTING.md).

- **GOOD:** `git checkout main` then a new branch.
- **BAD:** more commits on an old feature branch.

## Checks

```bash
cd mci-core && ./gradlew test
cd mci-web && ./gradlew test
```

Windows: `gradlew.bat test` from the module directory.
