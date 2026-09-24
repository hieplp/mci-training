# Step progress without storing steps

Status: shipped in #19 (core `StepListener`) and #20 (SSE endpoint). As built, `/sum/stream` is `POST` via `StreamingResponseBody` and the page uses `fetch`, not `GET`/`SseEmitter`/`EventSource` as written below — operands moved to the request body to avoid the request-header size limit. Replaces the step-history contract in [api-spec.md](api-spec.md), [domain-model.md](domain-model.md), and [coding-rules.md](coding-rules.md) §3.

## Problem

`sumWithSteps` returns `SumResult(sum, List<Step>)` and each `Step` carries `resultSoFar` — a string that grows by one digit per column. For an n-digit sum the result object holds O(n²) characters (~50 MB of text at n = 10 000) and the page renders n list items at once. The bigger the input, the bigger the result — the opposite of what the web page needs.

## Decision

Stream each step to the caller as it is computed. Store nothing.

- Core: `sum(stn1, stn2, listener)` invokes the listener once per column, in order. No step list is built.
- Web: `GET /sum/stream` emits the steps as Server-Sent Events; the page appends them live with `EventSource`.
- `resultSoFar` is removed from `Step`. The client derives it by prepending each `resultDigit` (steps arrive right-to-left), so it never has to be built or sent.

Rejected alternatives:

| Option | Why rejected |
|---|---|
| Keep sync `GET /`, drop `resultSoFar`, derive it in the template | Still stores n `Step` objects per request and renders them in one burst — no visible progress. Kept as fallback if JS must be avoided entirely. |
| Compute steps in browser JS | Duplicates the column-addition algorithm; the core library's steps go unused. |
| Cap the stored steps (first/last k) | Loses steps; still allocates the full `resultSoFar` strings unless `Step` changes anyway. |

## Core API — `mci-core`

```java
String sum(String stn1, String stn2)                       // unchanged signature
String sum(String stn1, String stn2, StepListener listener) // listener may be null

@FunctionalInterface
interface StepListener { void onStep(MyBigNumber.Step step); }
```

- `sum(stn1, stn2)` delegates to `sum(stn1, stn2, null)`.
- `sumWithSteps` and `SumResult` are deleted. `Step` is kept and loses `resultSoFar`:

```java
record Step(int index, int firstDigit, int secondDigit,
            int carryIn, int columnTotal, int resultDigit, int carryOut) {}
```

- The listener is invoked on the caller's thread, once per column, before the method returns. Exceptions from the listener propagate.
- INFO logging is unchanged in shape — the step line drops the `Result so far` clause:
  `Step 1: 4 + 7 + carry 0 = 11. Write 1, carry 1.` The requirement's "record the history, prefer LOGGING" is still satisfied by the log.
- Memory: O(n) for the result `char[]`; O(1) per step.

## Web endpoint — `mci-web`

`GET /sum/stream?stn1=…&stn2=…` → `text/event-stream`, via `SseEmitter` (spring-boot-starter-web, already present; Jackson serializes `Step`).

Events, in order:

| Event | Data | When |
|---|---|---|
| `step` | `{"index":1,"firstDigit":4,"secondDigit":7,"carryIn":0,"columnTotal":11,"resultDigit":1,"carryOut":1}` | once per column |
| `result` | `{"sum":"2131"}` | once, after the last step |
| `error` | `{"message":"stn1 must not be null or empty"}` | on `IllegalArgumentException` — same messages as the page shows today |

- The controller creates the emitter, submits one task that calls `SumService.sum(stn1, stn2, listener)` where the listener is `step -> emitter.send(event("step").data(step, APPLICATION_JSON))`, then sends `result` and calls `complete()`. `IllegalArgumentException` → `error` event + `complete()`; anything else → `completeWithError`.
- `SumService` becomes `String sum(String stn1, String stn2, StepListener listener)` — still a thin delegate to `MyBigNumber`.
- `GET /` is unchanged: it renders the sum only (listener = `null`), so the page still works with JS disabled — it just shows no step list.
- No new query parameters; `stn1`/`stn2` only. No request body, no JSON error document — SSE events are not the RFC 7807 surface api-rules.md §4 governs.

## Page behavior

New file `mci-web/src/main/resources/static/js/sum.js`, loaded by `index.html`:

1. On form submit: `preventDefault`, close any open `EventSource`, clear the step list, open `new EventSource('/sum/stream?stn1=…&stn2=…')`.
2. `step` event: append one `<li>` rendered like the current template line; `resultSoFar` = `resultDigit` prepended to the running string.
3. `result` event: show the sum, `close()` the source (mandatory — `EventSource` auto-reconnects otherwise and would rerun the sum).
4. `error` event: show the message in the error panel, `close()`.
5. `onerror` (connection dropped): show a generic "connection lost" line, `close()`.

## Edge cases

- **Client disconnects mid-stream**: `emitter.send` throws `IOException` inside the listener → propagates out of `sum` → task ends. No server-side accumulation, so nothing to clean up.
- **Reconnect**: prevented by `close()` on `result`/`error`/`onerror`.
- **Huge inputs**: the server stays O(n); the DOM still grows to n nodes with O(n²) total text if every `resultSoFar` is rendered. Optional client cap: keep only the last N `<li>`s plus a "step k of the calculation" counter. Not required for v1.
- **Missing params**: `/sum/stream` without both params sends one `error` event (`stn1`/`stn2` required) and completes.
- **Threads**: one emitter and one listener per request; no shared state. `SseEmitter.send` is safe from the async task thread.

## Touch list

| File | Change |
|---|---|
| `mci-core/…/MyBigNumber.java` | add `StepListener`, 3-arg `sum`; delete `sumWithSteps`, `SumResult`, `Step.resultSoFar`; update step log format |
| `mci-core/…/MyBigNumberTest.java` | collect steps via a listener instead of `SumResult`; update expected log lines |
| `mci-core/docs/usage.md` | replace `sumWithSteps` example with listener example |
| `mci-web/…/SumService.java`, `SumServiceImpl.java` | signature → `sum(stn1, stn2, listener)` returning `String` |
| `mci-web/…/SumController.java` | `GET /` puts `String sum` on the model; add `GET /sum/stream` returning `SseEmitter` |
| `mci-web/…/index.html` | remove `th:each` step rendering; add empty `<ol>` + `<script src>` for `sum.js` |
| `mci-web/…/static/js/sum.js` | new — EventSource client above |
| `mci-web/…/static/css/app.css` | styles for streamed list only if the existing `.steps-list` doesn't cover it |
| `mci-web` tests | controller test: model carries `sum` string; new test for the SSE endpoint's event order; service test: listener pass-through |
| `docs/api-spec.md` | library signature block + `GET /sum/stream` event table |
| `docs/domain-model.md` | `Step` field table loses `resultSoFar`; steps described as streamed, not returned |
| `docs/coding-rules.md` §3 | public API becomes `sum` + `sum(stn1, stn2, listener)`; "step history" = the listener/log, not a returned list |
| `.github/copilot-instructions.md` | same API sentence update |
| `mci-web/README.md` | `SumController` row gains the stream endpoint |
