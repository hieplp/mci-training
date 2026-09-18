package dev.hieplp.mci.web;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class MciWebApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        String[] args = {"--server.port=0"};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            MciWebApplication.main(args);
            spring.verify(() -> SpringApplication.run(MciWebApplication.class, args));
        }
    }
}
