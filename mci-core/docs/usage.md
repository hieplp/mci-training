# Usage

All classes live in `dev.hieplp.mci.core`.

## MyBigNumber

Adds two non-negative integers supplied as decimal strings. Works on
arbitrarily long inputs (no `long`/`BigInteger` overflow concern — it does
column addition digit by digit) and logs each step at INFO.

```java
MyBigNumber bn = new MyBigNumber();

bn.sum("123", "456");        // "579"
bn.sum("999", "1");          // "1000"
bn.sum("000123", "00456");   // "579"  — leading zeros stripped
bn.sum("0", "0");            // "0"
```

Need the step-by-step history (e.g. to show calculation progress in a UI)?
Pass a `StepListener` — each column-addition step is reported as it is
computed, never stored; formatting is the caller's choice:

```java
List<MyBigNumber.Step> steps = new ArrayList<>();
String sum = bn.sum("1234", "897", steps::add);   // "2131"
// steps: [Step 1: 4 + 7 + carry 0 = 11. Write 1, carry 1., ...]
// each Step exposes its fields (index, firstDigit, carryIn, ...)
// and toString() renders the same sentence as the log line
```

The listener runs on the caller's thread before `sum` returns; pass
`null` when no steps are needed.

**Errors** — `IllegalArgumentException` when an operand is `null`, empty,
or contains a non-digit character:

```java
bn.sum("12a", "1");   // IllegalArgumentException: stn1 contains non-digit character 'a' at index 2
bn.sum(null, "1");    // IllegalArgumentException: stn1 must not be null or empty
```

**Cost note:** each addition step is logged at INFO, so log volume is
O(n) in input length. Raise the log level above INFO for large operands
(see [logging.md](logging.md)).

## NumberStrings

Static helpers for digit-string operands — reusable if you build your own
string-arithmetic service.

```java
// Validation: throws IllegalArgumentException on null/empty/non-digit.
// The `name` argument is used in the exception and log messages.
NumberStrings.requireDigits("123", "stn1");   // passes silently

// Normalization: strips leading zeros, never returns empty.
NumberStrings.stripLeadingZeros("000123");    // "123"
NumberStrings.stripLeadingZeros("000");       // "0"
```

`stripLeadingZeros` assumes the input was already validated — call
`requireDigits` first on untrusted input.

## AppLogger

Logging facade over `java.lang.System.Logger`. Use it in your own classes
the same way the library does:

```java
import dev.hieplp.mci.core.AppLogger;

public class MyService {
    private static final AppLogger LOG = AppLogger.of(MyService.class);

    public void run(int count) {
        LOG.info("Processed {0} items", count);   // MessageFormat-style {0} placeholders
        LOG.warn("Rejected input: {0}", input);
        LOG.error("Failed: {0}", reason);
        LOG.debug("detail={0}", detail);          // off at default JUL INFO level
    }
}
```

Instances are cheap wrappers — safe to hold in a `static final` field.
