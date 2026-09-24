package dev.hieplp.mci.web.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import dev.hieplp.mci.core.MyBigNumber;
import dev.hieplp.mci.core.MyBigNumber.StepListener;

class SumServiceImplTest {

    @Test
    void sumDelegatesToBigNumberAndReturnsItsResult() {
        StepListener listener = step -> { };
        try (MockedConstruction<MyBigNumber> construction = mockConstruction(
                MyBigNumber.class,
                (mock, context) -> when(mock.sum("1", "2", listener)).thenReturn("3"))) {
            assertEquals("3", new SumServiceImpl().sum("1", "2", listener));
            verify(construction.constructed().get(0)).sum("1", "2", listener);
        }
    }

    @Test
    void sumPropagatesValidationErrors() {
        try (MockedConstruction<MyBigNumber> construction = mockConstruction(
                MyBigNumber.class,
                (mock, context) -> when(mock.sum("x", "2", null))
                        .thenThrow(new IllegalArgumentException("non-digit")))) {
            assertThrows(IllegalArgumentException.class,
                    () -> new SumServiceImpl().sum("x", "2", null));
        }
    }
}
