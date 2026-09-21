package dev.hieplp.mci.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-optimization implementation kept as the benchmark baseline; superseded
 * by {@link MyBigNumber}. Do not use in production code.
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 */
public final class LegacyMyBigNumber {

    private static final AppLogger LOG = AppLogger.of(LegacyMyBigNumber.class);

    /** Logs the input, every column and the final result at INFO. */
    public String sum(String stn1, String stn2) {
        return sumWithSteps(stn1, stn2).sum();
    }

    /** Same as {@link #sum(String, String)} but also returns each step. */
    public MyBigNumber.SumResult sumWithSteps(String stn1, String stn2) {
        LOG.info("Input: stn1=\"{0}\", stn2=\"{1}\"", stn1, stn2);

        NumberStrings.requireDigits(stn1, "stn1");
        NumberStrings.requireDigits(stn2, "stn2");
        stn1 = NumberStrings.stripLeadingZeros(stn1);
        stn2 = NumberStrings.stripLeadingZeros(stn2);

        StringBuilder result = new StringBuilder(Math.max(stn1.length(), stn2.length()) + 1);
        int carry = 0;
        int stepIndex = 0;
        List<MyBigNumber.Step> steps = new ArrayList<>();

        int i = stn1.length() - 1;
        int j = stn2.length() - 1;

        while (i >= 0 || j >= 0 || carry > 0) {
            int firstDigit = i >= 0 ? stn1.charAt(i--) - '0' : 0;
            int secondDigit = j >= 0 ? stn2.charAt(j--) - '0' : 0;

            int carryIn = carry;
            int columnTotal = firstDigit + secondDigit + carryIn;
            int resultDigit = columnTotal % 10;
            carry = columnTotal / 10;
            result.append(resultDigit);

            String resultSoFar = new StringBuilder(result).reverse().toString();
            MyBigNumber.Step step = new MyBigNumber.Step(++stepIndex, firstDigit, secondDigit,
                    carryIn, columnTotal, resultDigit, carry, resultSoFar);
            LOG.info("{0}", step);
            steps.add(step);
        }

        String sum = result.reverse().toString();
        LOG.info("Final: {0} + {1} = {2}", stn1, stn2, sum);

        return new MyBigNumber.SumResult(sum, List.copyOf(steps));
    }
}
