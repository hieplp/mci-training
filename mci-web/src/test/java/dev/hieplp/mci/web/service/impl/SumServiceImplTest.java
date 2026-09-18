package dev.hieplp.mci.web.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import dev.hieplp.mci.core.MyBigNumber;
import dev.hieplp.mci.core.MyBigNumber.SumResult;
import dev.hieplp.mci.core.MyBigNumber.Step;

class SumServiceImplTest {

    @Test
    void sumDelegatesToBigNumberAndReturnsItsResult() {
        SumResult expected = new SumResult("3", List.of(
                new Step(1, 1, 2, 0, 3, 3, 0, "3")));
        try (MockedConstruction<MyBigNumber> construction = mockConstruction(
                MyBigNumber.class,
                (mock, context) -> when(mock.sumWithSteps("1", "2")).thenReturn(expected))) {
            SumResult actual = new SumServiceImpl().sum("1", "2");
            assertSame(expected, actual);
        }
    }

    @Test
    void sumPropagatesValidationErrors() {
        try (MockedConstruction<MyBigNumber> construction = mockConstruction(
                MyBigNumber.class,
                (mock, context) -> when(mock.sumWithSteps("x", "2"))
                        .thenThrow(new IllegalArgumentException("non-digit")))) {
            assertThrows(IllegalArgumentException.class,
                    () -> new SumServiceImpl().sum("x", "2"));
        }
    }
}
