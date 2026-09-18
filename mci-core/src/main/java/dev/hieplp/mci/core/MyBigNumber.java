package dev.hieplp.mci.core;



/**
 * Adds two large non-negative integers represented as decimal strings,
 * using the elementary-school column-addition algorithm.
 *
 * <p>Both operands are scanned right to left; each pair of digits is
 * summed together with the running carry, and every step is recorded
 * through {@link AppLogger} so callers can replay the calculation
 * history (e.g. for a UI progress view). Each record carries an
 * immutable snapshot of the partial result, so a handler that stores
 * records and formats them later still replays correctly.</p>
 *
 * <p>Operands must be non-empty strings of ASCII digits ({@code 0-9});
 * leading zeros are accepted and normalized away in the result.
 * Arbitrary length is supported — the algorithm is O(n) in the length
 * of the longer operand and never converts to a numeric type. Note the
 * per-step records make emitted log volume O(n²); raise the backend
 * level above INFO to suppress them.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * MyBigNumber bn = new MyBigNumber();
 * String result = bn.sum("1234", "897"); // "2131"
 * }</pre>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @version 0.0.1
 * @since 0.0.1
 * @see NumberStrings
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
        NumberStrings.requireDigits(stn1, "stn1");
        NumberStrings.requireDigits(stn2, "stn2");
        LOG.debug("sum() called: stn1 length={0}, stn2 length={1}", stn1.length(), stn2.length());
        LOG.debug("Operands validated: both are non-empty ASCII digit strings");

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

            // Immutable snapshot: LogRecord formatting is deferred, so a live
            // StringBuilder (reversed in place below) would log the final sum.
            String soFar = new StringBuilder(result).reverse().toString();
            LOG.info(
                    "Step {0}: {1} + {2} + carry {3} = {4}. Write {5}, carry {6}. Result so far: \"{7}\"",
                    step, d1, d2, prevCarry, total, digit, carry, soFar
            );

            i--;
            j--;
        }
        LOG.debug("Column loop finished: {0} steps executed, final carry={1}", step, carry);

        String sum = NumberStrings.stripLeadingZeros(result.reverse().toString());
        LOG.info("Final: {0} + {1} = {2}", stn1, stn2, sum);
        LOG.debug("sum() returning \"{0}\" ({1} digits)", sum, sum.length());
        return sum;
    }

}
