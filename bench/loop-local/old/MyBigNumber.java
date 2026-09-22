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

        if (stn1 == null || stn1.isEmpty()) {
            LOG.warn("Invalid operand {0}: null or empty", "stn1");
            throw new IllegalArgumentException("stn1 must not be null or empty");
        }
        if (stn2 == null || stn2.isEmpty()) {
            LOG.warn("Invalid operand {0}: null or empty", "stn2");
            throw new IllegalArgumentException("stn2 must not be null or empty");
        }
        stn1 = NumberStrings.stripLeadingZeros(stn1);
        stn2 = NumberStrings.stripLeadingZeros(stn2);

        char[] result = new char[Math.max(stn1.length(), stn2.length()) + 1];
        int carry = 0;
        int stepIndex = 0;
        List<Step> steps = new ArrayList<>();

        int i = stn1.length() - 1;
        int j = stn2.length() - 1;
        int resultIndex = result.length - 1;

        while (i >= 0 || j >= 0 || carry > 0) {
            char firstCharacter = '0';
            if (i >= 0) {
                firstCharacter = stn1.charAt(i--);
                if (firstCharacter < '0' || firstCharacter > '9') {
                    LOG.warn("Invalid operand {0}: non-digit character ''{1}'' at index {2}", "stn1", firstCharacter, i + 1);
                    throw new IllegalArgumentException("stn1 contains non-digit character '" + firstCharacter + "' at index " + (i + 1));
                }
            }
            int firstDigit = firstCharacter - '0';

            char secondCharacter = '0';
            if (j >= 0) {
                secondCharacter = stn2.charAt(j--);
                if (secondCharacter < '0' || secondCharacter > '9') {
                    LOG.warn("Invalid operand {0}: non-digit character ''{1}'' at index {2}", "stn2", secondCharacter, j + 1);
                    throw new IllegalArgumentException("stn2 contains non-digit character '" + secondCharacter + "' at index " + (j + 1));
                }
            }
            int secondDigit = secondCharacter - '0';

            int carryIn = carry;
            int columnTotal = firstDigit + secondDigit + carryIn;
            int resultDigit = columnTotal % 10;
            carry = columnTotal / 10;
            result[resultIndex--] = (char) ('0' + resultDigit);

            String resultSoFar = new String(result, resultIndex + 1, result.length - resultIndex - 1);
            Step step = new Step(++stepIndex, firstDigit, secondDigit, carryIn, columnTotal, resultDigit, carry, resultSoFar);
            LOG.info("{0}", step);
            steps.add(step);
        }

        String sum = new String(result, resultIndex + 1, result.length - resultIndex - 1);
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
     * @param resultSoFar result digits written so far, most-significant first
     */
    public record Step(
            int index,
            int firstDigit,
            int secondDigit,
            int carryIn,
            int columnTotal,
            int resultDigit,
            int carryOut,
            String resultSoFar
    ) {

        /** The step rendered as a sentence — same text as the INFO log line. */
        @Override
        public String toString() {
            return MessageFormat.format(
                    "Step {0}: {1} + {2} + carry {3} = {4}. Write {5}, carry {6}. Result so far: \"{7}\"",
                    index, firstDigit, secondDigit, carryIn, columnTotal, resultDigit, carryOut, resultSoFar
            );
        }

    }

}
