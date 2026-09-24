package dev.hieplp.mci.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SumWebTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void indexRendersTheForm() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Big Number Sum")))
                .andExpect(content().string(containsString("name=\"stn1\"")))
                .andExpect(content().string(containsString("name=\"stn2\"")));
    }

    @Test
    void validOperandsShowTheSum() throws Exception {
        mvc.perform(post("/").param("stn1", "1234").param("stn2", "897"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2131")));
    }

    @Test
    void invalidOperandShowsTheErrorMessage() throws Exception {
        mvc.perform(post("/").param("stn1", "12a3").param("stn2", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("non-digit character")));
    }
}
