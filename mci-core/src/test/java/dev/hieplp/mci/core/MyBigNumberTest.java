package dev.hieplp.mci.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class MyBigNumberTest {

    private final MyBigNumber bigNumber = new MyBigNumber();

    @Test
    void specExample() {
        assertEquals("2131", bigNumber.sum("1234", "897"));
    }

    @Test
    void differentLengths() {
        assertEquals("10000", bigNumber.sum("9999", "1"));
        assertEquals("10000", bigNumber.sum("1", "9999"));
    }

    @Test
    void zeros() {
        assertEquals("0", bigNumber.sum("0", "0"));
        assertEquals("123", bigNumber.sum("0", "123"));
    }

    @Test
    void carryPropagatesThroughAllDigits() {
        assertEquals("1000000000", bigNumber.sum("999999999", "1"));
    }

    @Test
    void veryLargeNumbers() {
        String a = "9".repeat(1000);
        String b = "9".repeat(999) + "8";
        assertEquals(new BigInteger(a).add(new BigInteger(b)).toString(), bigNumber.sum(a, b));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum(null, "1"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("1", null));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("", "1"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("1", ""));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("12a3", "1"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("abc", "1"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("1.5", "1"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("-5", "1"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("1 2", "1"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("١٢٣", "1")); // Arabic-Indic digits
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("1", "2x"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("1", "-5"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("1", "abc"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("abc", "xyz"));
    }

    @Test
    void acceptsLeadingZeros() {
        assertEquals("124", bigNumber.sum("000123", "0001"));
    }

    @Test
    void sumWithStepsReturnsOrderedStepData() {
        MyBigNumber.SumResult r = bigNumber.sumWithSteps("1234", "897");
        assertEquals("2131", r.sum());
        assertEquals(List.of(
                new MyBigNumber.Step(1, 4, 7, 0, 11, 1, 1, "1"),
                new MyBigNumber.Step(2, 3, 9, 1, 13, 3, 1, "31"),
                new MyBigNumber.Step(3, 2, 8, 1, 11, 1, 1, "131"),
                new MyBigNumber.Step(4, 1, 0, 1, 2, 2, 0, "2131")),
                r.steps());
    }

    @Test
    void randomAgainstBigInteger() {
        Random rnd = new Random(42);
        for (int k = 0; k < 200; k++) {
            String a = randomDigits(rnd, 1 + rnd.nextInt(200));
            String b = randomDigits(rnd, 1 + rnd.nextInt(200));
            assertEquals(new BigInteger(a).add(new BigInteger(b)).toString(), bigNumber.sum(a, b));
        }
    }

    private static String randomDigits(Random rnd, int len) {
        StringBuilder sb = new StringBuilder(len);
        sb.append((char) ('1' + rnd.nextInt(9))); // no leading zero
        for (int i = 1; i < len; i++) {
            sb.append((char) ('0' + rnd.nextInt(10)));
        }
        return sb.toString();
    }

    @Test
    void logsEachStepWithThePartialResult() {
        List<LogRecord> records = new ArrayList<>();
        Logger jul = Logger.getLogger(MyBigNumber.class.getName());
        Handler collector = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.INFO.intValue()) {
                    records.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Level level = jul.getLevel();
        boolean parentHandlers = jul.getUseParentHandlers();
        jul.setLevel(Level.ALL);
        jul.setUseParentHandlers(false);
        jul.addHandler(collector);
        try {
            assertEquals("2131", bigNumber.sum("1234", "897"));
        } finally {
            jul.removeHandler(collector);
            jul.setLevel(level);
            jul.setUseParentHandlers(parentHandlers);
        }

        // Formatted only now, after sum() returned: a handler that stores records
        // instead of printing them immediately must still see each step's own state.
        assertEquals(List.of(
                        "Input: stn1=\"1234\", stn2=\"897\"",
                        "Step 1: 4 + 7 + carry 0 = 11. Write 1, carry 1. Result so far: \"1\"",
                        "Step 2: 3 + 9 + carry 1 = 13. Write 3, carry 1. Result so far: \"31\"",
                        "Step 3: 2 + 8 + carry 1 = 11. Write 1, carry 1. Result so far: \"131\"",
                        "Step 4: 1 + 0 + carry 1 = 2. Write 2, carry 0. Result so far: \"2131\"",
                        "Final: 1234 + 897 = 2131"),
                records.stream()
                        .map(r -> MessageFormat.format(r.getMessage(), r.getParameters()))
                        .toList());
    }
}
