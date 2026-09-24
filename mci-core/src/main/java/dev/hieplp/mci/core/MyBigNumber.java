package dev.hieplp.mci.core;

import java.text.MessageFormat;

/**
 * Adds two non-negative integers given as decimal strings via
 * column addition, logging each step at INFO and reporting it to an
 * optional {@link StepListener}.
 *
 * <p>
 * Steps are streamed to the listener, never collected: memory stays
 * O(n) in the input length regardless of how many steps run.
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
        return sum(stn1, stn2, null);
    }

    /**
     * Same as {@link #sum(String, String)} but reports each
     * column-addition step to {@code listener} as it happens, on the
     * caller's thread, before this method returns. Exceptions thrown by
     * the listener propagate.
     *
     * @param stn1     first operand, ASCII digits only
     * @param stn2     second operand, ASCII digits only
     * @param listener receives one {@link Step} per column, in order;
     *                 may be {@code null}
     * @return the sum, no leading zeros
     * @throws IllegalArgumentException on null, empty, or non-digit input
     */
    public String sum(String stn1, String stn2, StepListener listener) {
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
        StepListener stepListener = listener != null ? listener : step -> { };

        int i = stn1.length() - 1;
        int j = stn2.length() - 1;
        int resultIndex = result.length - 1;
        char firstCharacter = '0';
        char secondCharacter = '0';
        int firstDigit = 0;
        int secondDigit = 0;
        int carryIn = 0;
        int columnTotal = 0;
        int resultDigit = 0;
        Step step = null;

        while (i >= 0 || j >= 0 || carry > 0) {
            firstCharacter = '0';
            if (i >= 0) {
                firstCharacter = stn1.charAt(i--);
                if (firstCharacter < '0' || firstCharacter > '9') {
                    LOG.warn("Invalid operand {0}: non-digit character ''{1}'' at index {2}", "stn1", firstCharacter, i + 1);
                    throw new IllegalArgumentException("stn1 contains non-digit character '" + firstCharacter + "' at index " + (i + 1));
                }
            }
            firstDigit = firstCharacter - '0';

            secondCharacter = '0';
            if (j >= 0) {
                secondCharacter = stn2.charAt(j--);
                if (secondCharacter < '0' || secondCharacter > '9') {
                    LOG.warn("Invalid operand {0}: non-digit character ''{1}'' at index {2}", "stn2", secondCharacter, j + 1);
                    throw new IllegalArgumentException("stn2 contains non-digit character '" + secondCharacter + "' at index " + (j + 1));
                }
            }
            secondDigit = secondCharacter - '0';

            carryIn = carry;
            columnTotal = firstDigit + secondDigit + carryIn;
            resultDigit = columnTotal % 10;
            carry = columnTotal / 10;
            result[resultIndex--] = (char) ('0' + resultDigit);

            step = new Step(++stepIndex, firstDigit, secondDigit, carryIn, columnTotal, resultDigit, carry);
            LOG.info("{0}", step);
            stepListener.onStep(step);
        }

        String sum = new String(result, resultIndex + 1, result.length - resultIndex - 1);
        LOG.info("Final: {0} + {1} = {2}", stn1, stn2, sum);

        return sum;
    }

    /**
     * Receives each column-addition step as {@link #sum} computes it.
     * Steps are delivered in order on the caller's thread; nothing is
     * stored by {@code MyBigNumber}.
     */
    @FunctionalInterface
    public interface StepListener {

        /**
         * @param step the column-addition step just performed
         */
        void onStep(Step step);

    }

    /**
     * One column-addition step, as raw data — formatting is the caller's
     * choice. The digits written so far are derivable by prepending
     * {@link #resultDigit} of each step in order.
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
