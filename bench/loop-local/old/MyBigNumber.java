package dev.hieplp.mci.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

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
        return sumWithSteps(stn1, stn2).sum();
    }

    /**
     * Same as {@link #sum(String, String)} but also returns each
     * column-addition step as structured data, so callers can render the
     * calculation progress in whatever format they choose (e.g. a web UI).
     *
     * @param stn1 first operand, ASCII digits only
     * @param stn2 second operand, ASCII digits only
     * @return the sum plus one {@link Step} per column addition, in order
     * @throws IllegalArgumentException on null, empty, or non-digit input
     */
    public SumResult sumWithSteps(String stn1, String stn2) {
        LOG.info("Input: stn1=\"{0}\", stn2=\"{1}\"", stn1, stn2);

        NumberStrings.requireDigits(stn1, "stn1");
        NumberStrings.requireDigits(stn2, "stn2");
        stn1 = NumberStrings.stripLeadingZeros(stn1);
        stn2 = NumberStrings.stripLeadingZeros(stn2);

        StringBuilder result = new StringBuilder(Math.max(stn1.length(), stn2.length()) + 1);
        int carry = 0;
        int stepIndex = 0;
        List<Step> steps = new ArrayList<>();

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

            // resultSoFar snapshot removed: O(n^2) memory, dominates the bench.
            Step step = new Step(++stepIndex, firstDigit, secondDigit, carryIn, columnTotal, resultDigit, carry);
            LOG.info("{0}", step);
            steps.add(step);
        }

        String sum = result.reverse().toString();
        LOG.info("Final: {0} + {1} = {2}", stn1, stn2, sum);

        return new SumResult(sum, List.copyOf(steps));
    }

    /**
     * Result of {@link #sumWithSteps}: the sum and the ordered steps of
     * the column addition.
     *
     * @param sum   the sum, no leading zeros
     * @param steps one entry per column-addition step, in order; immutable
     */
    public record SumResult(String sum, List<Step> steps) {
    }

    /**
     * One column-addition step, as raw data — formatting is the caller's
     * choice.
     *
     * @param index       1-based step number
     * @param firstDigit  digit taken from the first operand (0 when exhausted)
     * @param secondDigit digit taken from the second operand (0 when exhausted)
     * @param carryIn     carry brought into this step
     * @param columnTotal {@code firstDigit + secondDigit + carryIn}
     * @param resultDigit digit written to the result ({@code columnTotal % 10})
     * @param carryOut    carry passed to the next step ({@code columnTotal / 10})
     */
    public record Step(
            int index,
            int firstDigit,
            int secondDigit,
            int carryIn,
            int columnTotal,
            int resultDigit,
            int carryOut
    ) {

        /** The step rendered as a sentence — same text as the INFO log line. */
        @Override
        public String toString() {
            return MessageFormat.format(
                    "Step {0}: {1} + {2} + carry {3} = {4}. Write {5}, carry {6}.",
                    index, firstDigit, secondDigit, carryIn, columnTotal, resultDigit, carryOut
            );
        }

    }

}
