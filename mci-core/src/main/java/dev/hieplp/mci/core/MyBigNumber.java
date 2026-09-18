package dev.hieplp.mci.core;



/**
 * Adds two large non-negative integers represented as decimal strings,
 * using the elementary-school column-addition algorithm.
 *
 * <p>Assumption (per spec): inputs contain only valid digits — no
 * validation is performed.</p>
 */
public class MyBigNumber {
    private static final AppLogger LOG = AppLogger.of(MyBigNumber.class);


    /**
     * Adds {@code stn1} and {@code stn2} digit by digit, right to left,
     * logging each step of the calculation.
     *
     * @param stn1 first operand, digits only
     * @param stn2 second operand, digits only
     * @return the sum as a decimal string, no leading zeros
     */
    public String sum(String stn1, String stn2) {
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
                    step, d1, d2, prevCarry, total, digit, carry, result);

            i--;
            j--;
        }

        String sum = result.reverse().toString();
        LOG.info("Final: {0} + {1} = {2}", stn1, stn2, sum);
        return sum;
    }
}
