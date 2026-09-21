package dev.hieplp.mci.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adds two non-negative integers given as decimal strings via
 * column addition, logging each step at INFO.
 *
 * <p>
 * {@link #sum(String, String)} is O(n) in time and memory while the
 * per-step INFO logging is off; the snapshot it logs per column (and
 * {@link #sumWithSteps(String, String)}, which returns those snapshots)
 * makes it O(n²) again when logging is on.
 * </p>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @see NumberStrings
 */
public class MyBigNumber {

    private static final AppLogger LOG = AppLogger.of(MyBigNumber.class);

    /** One {@link MessageFormat} pattern, shared by {@link Step#toString()} and the log line. */
    private static final String STEP_MESSAGE =
            "Step {0}: {1} + {2} + carry {3} = {4}. Write {5}, carry {6}. Result so far: \"{7}\"";

    /**
     * Logs the input, every column and the final result at INFO, and skips
     * building the per-column log message when INFO is disabled — O(n) time
     * and memory without logging, O(n²) with it.
     *
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
        int stepIndex = 0;

        int i = stn1.length() - 1;
        int j = stn2.length() - 1;

        while (i >= 0 || j >= 0 || carry > 0) {
            int firstDigit = i >= 0 ? stn1.charAt(i--) - '0' : 0;
            int secondDigit = j >= 0 ? stn2.charAt(j--) - '0' : 0;

            int carryIn = carry;
            int columnTotal = firstDigit + secondDigit + carryIn;
            int resultDigit = columnTotal % 10;
            carry = columnTotal / 10;
            result.append((char) ('0' + resultDigit));

            // Guarded by the supplier: the snapshot below runs only when INFO is enabled.
            int step = ++stepIndex;
            int carryOut = carry;
            LOG.info(() -> MessageFormat.format(STEP_MESSAGE, step, firstDigit, secondDigit, carryIn,
                    columnTotal, resultDigit, carryOut, new StringBuilder(result).reverse().toString()));
        }

        String sum = result.reverse().toString();
        LOG.info("Final: {0} + {1} = {2}", stn1, stn2, sum);
        return sum;
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
        // Reused scratch, so each snapshot costs only its String, not a whole
        // StringBuilder (object + backing array) per column.
        StringBuilder snapshot = new StringBuilder(result.capacity());
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
            result.append((char) ('0' + resultDigit));

            snapshot.setLength(0);
            snapshot.append(result).reverse();
            String resultSoFar = snapshot.toString();

            Step step = new Step(++stepIndex, firstDigit, secondDigit, carryIn, columnTotal, resultDigit, carry, resultSoFar);
            LOG.info("{0}", step);
            steps.add(step);
        }

        String sum = result.reverse().toString();
        LOG.info("Final: {0} + {1} = {2}", stn1, stn2, sum);

        return new SumResult(sum, Collections.unmodifiableList(steps));
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
            return MessageFormat.format(STEP_MESSAGE,
                    index, firstDigit, secondDigit, carryIn, columnTotal, resultDigit, carryOut, resultSoFar);
        }

    }

}
