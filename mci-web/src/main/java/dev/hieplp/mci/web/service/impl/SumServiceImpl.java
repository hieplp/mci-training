package dev.hieplp.mci.web.service.impl;

import dev.hieplp.mci.core.MyBigNumber;
import dev.hieplp.mci.core.MyBigNumber.StepListener;
import dev.hieplp.mci.web.service.SumService;
import org.springframework.stereotype.Service;

/**
 * {@link SumService} backed by {@link MyBigNumber}.
 *
 * <p>Delegates to {@link MyBigNumber#sum(String, String, StepListener)};
 * validation happens there.</p>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @see MyBigNumber
 */
@Service
public class SumServiceImpl implements SumService {

    private final MyBigNumber bigNumber = new MyBigNumber();

    @Override
    public String sum(String stn1, String stn2, StepListener listener) {
        return bigNumber.sum(stn1, stn2, listener);
    }

}
