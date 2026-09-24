# Copilot instructions

Two Gradle projects side by side. Not one multi-module build. Not `src/` + `tests/unit/`.

- `mci-core`: Java 21, package `dev.hieplp.mci.core`, zero runtime dependencies. Public API is `MyBigNumber.sum` and `sum(String, String, StepListener)` — steps are streamed to the listener, never stored. Operands are non-negative ASCII digit strings. `NumberStrings.requireDigits` rejects null, empty, and non-digits with `IllegalArgumentException`. Log through `AppLogger` (`{0}` placeholders). Do not use `System.out` or `BigInteger` in main code. Tests may use `BigInteger` as an oracle.
- `mci-web`: Spring Boot 3.5, Thymeleaf, Java 21, package `dev.hieplp.mci.web`. One page: `GET /` renders the form, `POST /` computes, `POST /sum/stream` streams steps as SSE. `SumController` → `SumService` → `MyBigNumber`. CSS is `src/main/resources/static/css/app.css`. Core is pulled in by `includeBuild '../mci-core'` in `mci-web/settings.gradle`. Do not publish a jar to make the web module compile.

Do not add a database, ORM, REST API, message bus, or third module. There is no persistence layer. Do not add a runtime dependency to `mci-core`. Do not add a web dependency unless the issue names it. Do not change the `sum(String, String)` signature or store the streamed steps.

Match the existing style: constructor injection, no Lombok, JUnit 5, records for results. Run `./gradlew test` in the module you edit (`gradlew.bat test` on Windows).

Before generating or editing code, read and follow [docs/coding-rules.md](../docs/coding-rules.md), [docs/api-rules.md](../docs/api-rules.md), and [docs/security-rules.md](../docs/security-rules.md). Do not restate those rules here.


Spec: `docs/requirement.md` and `docs/Add2Num_High-level-requirement_v1.8.md`. Behavior: `mci-core/docs/usage.md`. The code wins if a doc disagrees.
