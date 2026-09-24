# API

Two surfaces. Neither is a CRUD resource, and neither has a database.

## Library

```java
String sum(String stn1, String stn2)
String sum(String stn1, String stn2, StepListener listener) // listener may be null

@FunctionalInterface
interface StepListener { void onStep(MyBigNumber.Step step); }

record Step(int index, int firstDigit, int secondDigit,
            int carryIn, int columnTotal, int resultDigit, int carryOut) {}
```

Package `dev.hieplp.mci.core`. Invalid input throws `IllegalArgumentException`. The message names the operand (`stn1` or `stn2`). The listener is invoked once per column, in order, on the caller's thread; steps are never stored.

## Page

`GET /` renders the empty form. It takes no parameters and computes nothing.

`POST /`

| Form field | Required | Meaning |
|---|---|---|
| `stn1` | yes | First operand. Echoed back into the form. |
| `stn2` | yes | Second operand. Echoed back into the form. |

The page shows the sum, or the validation message. Missing or invalid input renders the form with the `IllegalArgumentException` message.

`POST /sum/stream` — same form fields in the request body (not the query string, so large operands do not hit the request-header size limit). Responds `text/event-stream`:

| Event | Data | When |
|---|---|---|
| `step` | `{"index":1,"firstDigit":4,"secondDigit":7,"carryIn":0,"columnTotal":11,"resultDigit":1,"carryOut":1}` | once per column |
| `result` | `{"sum":"2131"}` | once, after the last step |
| `error` | `{"message":"stn1 and stn2 are required"}` | on missing params or `IllegalArgumentException` |

The page's `static/js/sum.js` consumes the stream with `fetch` (`EventSource` is GET-only) and appends one list item per `step`.

Run: [how-to-run.md](how-to-run.md). `./run.sh` serves port 8081. `./gradlew bootRun` serves port 8080.
