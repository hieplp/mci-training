# API

Two surfaces. Neither is a CRUD resource, and neither has a database.

## Library

```java
String sum(String stn1, String stn2)
SumResult sumWithSteps(String stn1, String stn2)
```

Package `dev.hieplp.mci.core`. Invalid input throws `IllegalArgumentException`. The message names the operand (`stn1` or `stn2`).

## Page

`GET /`

| Query | Required | Meaning |
|---|---|---|
| `stn1` | no | First operand. Echoed back into the form. |
| `stn2` | no | Second operand. Echoed back into the form. |

When both are present, the page shows the sum and each column step, or the validation message. Missing either parameter renders the empty form. The form submits with `GET`.

Run: [how-to-run.md](how-to-run.md). `./run.sh` serves port 8081. `./gradlew bootRun` serves port 8080.
