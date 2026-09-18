package dev.hieplp.mci.core;

/**
 * Adds two non-negative integers given as decimal strings via
 * column addition, logging each step at INFO with an immutable
 * partial-result snapshot.
 *
 * <p>
 * Per-step snapshots make both time and log volume O(n²);
 * raise the level above INFO to skip the logging.
 * </p>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @see NumberStrings
 */
public class MyBigNumber {

    private static final AppLogger LOG = AppLogger.of(MyBigNumber.class);

    /**
     * @param stn1 first operand, ASCII digits only
     * @param stn2 second operand, ASCII digits only
     * @return the sum, no leading zeros
     * @throws IllegalArgumentException on null, empty, or non-digit input
     */
    public String sum(String stn1, String stn2) {
        LOG.info("Input: stn1=\"{0}\", stn2=\"{1}\"", stn1, stn2);

        NumberStrings.requireDigits(stn1, "stn1");
        NumberStrings.requireDigits(stn2, "stn2");
        stn1 = NumberStrings.stripLeadingZeros(stn1);
        stn2 = NumberStrings.stripLeadingZeros(stn2);

        StringBuilder result = new StringBuilder(Math.max(stn1.length(), stn2.length()) + 1);
        int carry = 0;
        int step = 0;

        int i = stn1.length() - 1;
        int j = stn2.length() - 1;

        while (i >= 0 || j >= 0 || carry > 0) {
            int d1 = i >= 0 ? stn1.charAt(i--) - '0' : 0;
            int d2 = j >= 0 ? stn2.charAt(j--) - '0' : 0;

            int prevCarry = carry;
            int total = d1 + d2 + prevCarry;
            int digit = total % 10;
            carry = total / 10;
            result.append(digit);

            LOG.info(
                    "Step {0}: {1} + {2} + carry {3} = {4}. Write {5}, carry {6}. Result so far: \"{7}\"",
                    ++step, d1, d2, prevCarry, total, digit, carry, new StringBuilder(result).reverse()
                );
        }

        String sum = result.reverse().toString();
        LOG.info("Final: {0} + {1} = {2}", stn1, stn2, sum);

        return sum;
    }

}
