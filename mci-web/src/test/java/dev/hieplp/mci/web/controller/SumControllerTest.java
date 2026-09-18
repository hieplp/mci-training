package dev.hieplp.mci.web.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.hieplp.mci.core.MyBigNumber.SumResult;
import dev.hieplp.mci.core.MyBigNumber.Step;
import dev.hieplp.mci.web.service.SumService;

@WebMvcTest(SumController.class)
class SumControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SumService sumService;

    @Test
    void noParamsRendersFormWithoutCallingService() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeDoesNotExist("result", "error"));
        verifyNoInteractions(sumService);
    }

    @Test
    void validParamsPutTheResultOnTheModel() throws Exception {
        SumResult result = new SumResult("2131", List.of(
                new Step(1, 4, 7, 0, 11, 1, 1, "1")));
        when(sumService.sum("1234", "897")).thenReturn(result);

        mvc.perform(get("/").param("stn1", "1234").param("stn2", "897"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("stn1", "1234"))
                .andExpect(model().attribute("stn2", "897"))
                .andExpect(model().attribute("result", result));
        verify(sumService).sum("1234", "897");
    }

    @Test
    void invalidParamsPutTheErrorMessageOnTheModel() throws Exception {
        when(sumService.sum("12a3", "1"))
                .thenThrow(new IllegalArgumentException("stn1: non-digit character"));

        mvc.perform(get("/").param("stn1", "12a3").param("stn2", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("error", "stn1: non-digit character"))
                .andExpect(model().attributeDoesNotExist("result"));
    }
}
