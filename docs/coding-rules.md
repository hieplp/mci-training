# Coding rules

Rules for humans and Copilot. The code wins if this file disagrees with it.

## Layout

Two Gradle projects side by side. Not one multi-module build. Not a root `src/` plus `tests/unit/`.

- `mci-core` — Java 21 library, package `dev.hieplp.mci.core`, zero runtime dependencies.
- `mci-web` — Spring Boot 3.5 + Thymeleaf, package `dev.hieplp.mci.web`. Depends on core through `includeBuild '../mci-core'`.

Do not add a database, ORM, REST resource, message bus, or third module. Do not add a runtime dependency to `mci-core`. Do not add a web dependency unless the issue names it.

## Core

- Public API is `MyBigNumber.sum` and `sumWithSteps`. Do not change the `sum(String, String)` signature or drop the step history.
- Operands are non-negative ASCII digit strings. `NumberStrings.requireDigits` rejects null, empty, and non-digits with `IllegalArgumentException`.
- Log through `AppLogger` with `{0}` placeholders. Do not use `System.out`.
- Do not use `BigInteger` in main code. Tests may use it as an oracle.

## Loops

Declare every local a loop body uses before the loop. Assign inside the body. Do not declare those locals in the body. A `for` index may stay in the `for` header.

```java
int digit = 0;
String partial = null;

while (hasMore) {
    digit = nextDigit();
    partial = accumulate(digit);
}
```

## Web

One page: `GET /` with `stn1` and `stn2`. `SumController` → `SumService` → `MyBigNumber`. CSS is `mci-web/src/main/resources/static/css/app.css`. Do not publish a jar to make the web module compile.

## Style

Constructor injection. No Lombok. JUnit 5. Records for results.

## Checks

```bash
cd mci-core && ./gradlew test
cd mci-web && ./gradlew test
```

Windows: `gradlew.bat test` from the module directory.

## Branches

Cut each change from `main`. Do not add commits to an existing feature branch. Details: [CONTRIBUTING.md](../CONTRIBUTING.md).
