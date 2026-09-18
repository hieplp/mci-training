package dev.hieplp.mci.core;



/**
 * Adds two large non-negative integers represented as decimal strings,
 * using the elementary-school column-addition algorithm.
 */
public class MyBigNumber {

    private static final AppLogger LOG = AppLogger.of(MyBigNumber.class);


    /**
     * Adds {@code stn1} and {@code stn2} digit by digit, right to left,
     * logging each step of the calculation.
     *
     * @param stn1 first operand, ASCII digits only
     * @param stn2 second operand, ASCII digits only
     * @return the sum as a decimal string, no leading zeros
     * @throws IllegalArgumentException if either operand is null, empty,
     *         or contains a non-digit character
     */
    public String sum(String stn1, String stn2) {
        requireDigits(stn1, "stn1");
        requireDigits(stn2, "stn2");

        StringBuilder result = new StringBuilder();
        int carry = 0;
        int step = 0;

        int i = stn1.length() - 1;
        int j = stn2.length() - 1;

        while (i >= 0 || j >= 0 || carry > 0) {
            int d1 = i >= 0 ? stn1.charAt(i) - '0' : 0;
            int d2 = j >= 0 ? stn2.charAt(j) - '0' : 0;

            int prevCarry = carry;
            int total = d1 + d2 + prevCarry;
            int digit = total % 10;
            carry = total / 10;
            result.append(digit);
            step++;

            LOG.info(
                    "Step {0}: {1} + {2} + carry {3} = {4}. Write {5}, carry {6}. Result so far: \"{7}\"",
                    step, d1, d2, prevCarry, total, digit, carry, result
            );

            i--;
            j--;
        }

        String sum = stripLeadingZeros(result.reverse().toString());
        LOG.info("Final: {0} + {1} = {2}", stn1, stn2, sum);
        return sum;
    }

    private static void requireDigits(String s, String name) {
        if (s == null || s.isEmpty()) {
            LOG.warn("Invalid operand {0}: null or empty", name);
            throw new IllegalArgumentException(name + " must not be null or empty");
        }
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c < '0' || c > '9') {
                LOG.warn("Invalid operand {0}: non-digit character ''{1}'' at index {2}", name, c, k);
                throw new IllegalArgumentException(
                        name + " contains non-digit character '" + c + "' at index " + k);
            }
        }
    }

    private static String stripLeadingZeros(String s) {
        int k = 0;
        while (k < s.length() - 1 && s.charAt(k) == '0') {
            k++;
        }
        return s.substring(k);
    }
}
