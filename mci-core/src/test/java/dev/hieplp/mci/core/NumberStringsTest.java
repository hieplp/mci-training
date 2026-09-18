package dev.hieplp.mci.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.Test;

class NumberStringsTest {

    @Test
    void requireDigitsAcceptsDigitStrings() {
        NumberStrings.requireDigits("0", "operand");
        NumberStrings.requireDigits("00", "operand");
        NumberStrings.requireDigits("0123456789", "operand");
        NumberStrings.requireDigits("9".repeat(1000), "operand");
    }

    @Test
    void requireDigitsRejectsNullAndEmpty() {
        IllegalArgumentException nullError =
                assertThrows(IllegalArgumentException.class, () -> NumberStrings.requireDigits(null, "stn1"));
        assertTrue(nullError.getMessage().contains("stn1"), nullError.getMessage());

        assertThrows(IllegalArgumentException.class, () -> NumberStrings.requireDigits("", "stn2"));
    }

    @Test
    void requireDigitsRejectsEveryNonAsciiDigit() {
        for (String invalid : new String[] {"-5", "+1", "1 2", "1.0", "12a3", "a", "١٢٣", "①②", "1_000", "１２３"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> NumberStrings.requireDigits(invalid, "operand"),
                    "accepted: " + invalid);
        }
    }

    @Test
    void requireDigitsReportsTheOffendingCharacterAndIndex() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> NumberStrings.requireDigits("12a3", "stn1"));

        assertTrue(error.getMessage().contains("a"), error.getMessage());
        assertTrue(error.getMessage().contains("index 2"), error.getMessage());
    }

    @Test
    void stripLeadingZerosKeepsASingleZero() {
        assertEquals("1234", NumberStrings.stripLeadingZeros("0001234"));
        assertEquals("0", NumberStrings.stripLeadingZeros("000"));
        assertEquals("0", NumberStrings.stripLeadingZeros("0"));
        assertEquals("1234", NumberStrings.stripLeadingZeros("1234"));
    }

    @Test
    void stripLeadingZerosNormalizesToBigIntegerForm() {
        Random rnd = new Random(7);
        for (int k = 0; k < 100; k++) {
            String digits = randomDigits(rnd, 1 + rnd.nextInt(30));
            String stripped = NumberStrings.stripLeadingZeros(digits);

            assertFalse(stripped.isEmpty(), digits);
            assertTrue(stripped.equals("0") || stripped.charAt(0) != '0', digits);
            assertEquals(new BigInteger(digits).toString(), stripped, digits);
        }
    }

    private static String randomDigits(Random rnd, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('0' + rnd.nextInt(10)));
        }
        return sb.toString();
    }
}
