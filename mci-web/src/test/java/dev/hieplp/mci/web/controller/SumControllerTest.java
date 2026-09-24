package dev.hieplp.mci.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import dev.hieplp.mci.core.MyBigNumber.Step;
import dev.hieplp.mci.core.MyBigNumber.StepListener;
import dev.hieplp.mci.web.service.SumService;

@WebMvcTest(SumController.class)
class SumControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SumService sumService;

    @Test
    void getRendersFormWithoutCallingService() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeDoesNotExist("sum", "error"));
        verifyNoInteractions(sumService);
    }

    @Test
    void validParamsPutTheSumOnTheModel() throws Exception {
        when(sumService.sum(eq("1234"), eq("897"), any())).thenReturn("2131");

        mvc.perform(post("/").param("stn1", "1234").param("stn2", "897"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("stn1", "1234"))
                .andExpect(model().attribute("stn2", "897"))
                .andExpect(model().attribute("sum", "2131"));
        verify(sumService).sum(eq("1234"), eq("897"), any());
    }

    @Test
    void invalidParamsPutTheErrorMessageOnTheModel() throws Exception {
        when(sumService.sum(eq("12a3"), eq("1"), any()))
                .thenThrow(new IllegalArgumentException("stn1: non-digit character"));

        mvc.perform(post("/").param("stn1", "12a3").param("stn2", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("error", "stn1: non-digit character"))
                .andExpect(model().attributeDoesNotExist("sum"));
    }

    @Test
    void streamEmitsStepsThenResult() throws Exception {
        when(sumService.sum(eq("1234"), eq("897"), any()))
                .thenAnswer(invocation -> {
                    StepListener listener = invocation.getArgument(2);
                    listener.onStep(new Step(1, 4, 7, 0, 11, 1, 1));
                    listener.onStep(new Step(2, 3, 9, 1, 13, 3, 1));
                    return "2131";
                });

        MvcResult async = mvc.perform(post("/sum/stream").param("stn1", "1234").param("stn2", "897"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:step")))
                .andExpect(content().string(containsString("\"index\":1")))
                .andExpect(content().string(containsString("\"index\":2")))
                .andExpect(content().string(containsString("event:result")))
                .andExpect(content().string(containsString("\"sum\":\"2131\"")));
    }

    @Test
    void streamEmitsErrorOnInvalidInput() throws Exception {
        when(sumService.sum(eq("x"), eq("2"), any()))
                .thenThrow(new IllegalArgumentException("stn1 contains non-digit character 'x' at index 0"));

        MvcResult async = mvc.perform(post("/sum/stream").param("stn1", "x").param("stn2", "2"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:error")))
                .andExpect(content().string(containsString("non-digit character")));
    }

    @Test
    void streamWithoutParamsEmitsErrorWithoutCallingService() throws Exception {
        MvcResult async = mvc.perform(post("/sum/stream"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:error")))
                .andExpect(content().string(containsString("required")));
        verifyNoInteractions(sumService);
    }
}
