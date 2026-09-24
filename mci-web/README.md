# mci-web

Spring Boot + Thymeleaf web UI for the MCI training project. Java 21.
Reuses `mci-core` via a Gradle composite build (`includeBuild '../mci-core'`
in `settings.gradle`) — no published jar needed.

## What's inside

| Class | Purpose |
|---|---|
| `MciWebApplication` | Spring Boot entry point. |
| `SumController` | `GET /` — renders the form. `POST /` — echoes the operands, adds the sum (or the validation error) to the model. `POST /sum/stream` — SSE stream of `step`/`result`/`error` events. |
| `SumService` / `SumServiceImpl` | Web-layer facade delegating to `MyBigNumber.sum(stn1, stn2, listener)`. |

Package: `dev.hieplp.mci.web`

UI: single `index.html` page (Thymeleaf) + hand-rolled `static/css/app.css`,
showing the column-addition steps streamed live from `POST /sum/stream` via `static/js/sum.js`.

## Requirements

- JDK 21+ (Gradle toolchain resolves it automatically via the foojay plugin if not installed).
- `mci-core` checked out next to this directory (composite build).

## Run

```bash
./run.sh              # serves on http://localhost:8081 (all interfaces)
./gradlew bootRun     # default port 8080
```

## Build & test

```bash
./gradlew build       # compile + run tests
./gradlew test        # tests only (JUnit 5 + Mockito + MockMvc)
```

Tests: `SumWebTest` (full-context MockMvc), `SumControllerTest`
(`@WebMvcTest` + `@MockitoBean`), `SumServiceImplTest` (Mockito
`mockConstruction`), `MciWebApplicationTest` (`mockStatic`).
