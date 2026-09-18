package dev.hieplp.mci.web.service;

import dev.hieplp.mci.core.MyBigNumber.SumResult;

/**
 * Big-number addition with step-by-step column history.
 *
 * <p>Web-layer facade over {@link dev.hieplp.mci.core.MyBigNumber}:
 * operands arrive as request parameters and the result is rendered by
 * the view.</p>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @see dev.hieplp.mci.core.MyBigNumber
 */
public interface SumService {

    /**
     * Adds two non-negative integers given as digit strings and returns
     * the sum together with each column-addition step, so callers can
     * render the calculation progress.
     *
     * @param stn1 first operand, ASCII digits only
     * @param stn2 second operand, ASCII digits only
     * @return sum of the two operands plus the ordered
     *         step descriptions of the column addition
     * @throws IllegalArgumentException on null, empty, or non-digit input
     */
    SumResult sum(String stn1, String stn2);

}
