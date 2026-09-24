package dev.hieplp.mci.web.service;

import dev.hieplp.mci.core.MyBigNumber.StepListener;

/**
 * Big-number addition with step-by-step column progress.
 *
 * <p>Web-layer facade over {@link dev.hieplp.mci.core.MyBigNumber}:
 * operands arrive as request parameters, the sum is rendered by the
 * view, and each column step is reported to {@code listener} as it is
 * computed.</p>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @see dev.hieplp.mci.core.MyBigNumber
 */
public interface SumService {

    /**
     * Adds two non-negative integers given as digit strings.
     *
     * @param stn1     first operand, ASCII digits only
     * @param stn2     second operand, ASCII digits only
     * @param listener receives each column-addition step, in order;
     *                 may be {@code null}
     * @return sum of the two operands, no leading zeros
     * @throws IllegalArgumentException on null, empty, or non-digit input
     */
    String sum(String stn1, String stn2, StepListener listener);

}
