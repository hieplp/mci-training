# Domain model

Adds two non-negative integers given as decimal strings, the way column addition works on paper. No persistence. No customer, order, or account entity.

## Operands

`stn1` and `stn2` are non-empty strings of ASCII digits `0`–`9`. Leading zeros are stripped before addition; `"000"` becomes `"0"`. Null, empty, and any non-digit are invalid.

There is no sign, decimal point, or thousands separator.

## Result

`MyBigNumber.sum` returns the sum as a digit string, most-significant digit first.

`sumWithSteps` returns the same sum plus one step per column, from right to left:

| Field | Meaning |
|---|---|
| `index` | 1-based step number |
| `firstDigit` | Digit from `stn1`, or `0` when that operand is exhausted |
| `secondDigit` | Digit from `stn2`, or `0` when that operand is exhausted |
| `carryIn` | Carry brought into this column |
| `columnTotal` | `firstDigit + secondDigit + carryIn` |
| `resultDigit` | Digit written (`columnTotal % 10`) |
| `carryOut` | Carry passed on (`columnTotal / 10`) |
| `resultSoFar` | Digits written so far, most-significant first |

Example: `sum("1234", "897")` is `"2131"`.

## Where it lives

| Type | Module |
|---|---|
| `MyBigNumber`, `NumberStrings`, `AppLogger` | `mci-core` |
| `SumService`, `SumController`, the page | `mci-web` |

Behavior and logging: [mci-core/docs/usage.md](../mci-core/docs/usage.md), [mci-core/docs/logging.md](../mci-core/docs/logging.md).
