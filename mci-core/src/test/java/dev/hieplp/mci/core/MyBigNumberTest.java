package dev.hieplp.mci.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Random;

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
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("-5", "1"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("1 2", "1"));
        assertThrows(IllegalArgumentException.class, () -> bigNumber.sum("١٢٣", "1")); // Arabic-Indic digits
    }

    @Test
    void acceptsLeadingZeros() {
        assertEquals("124", bigNumber.sum("000123", "0001"));
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
}
