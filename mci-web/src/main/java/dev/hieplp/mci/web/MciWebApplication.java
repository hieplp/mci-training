package dev.hieplp.mci.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the big-number addition web UI.
 *
 * <p>Serves a single page where two non-negative integers, entered as
 * decimal strings, are added by {@link dev.hieplp.mci.core.MyBigNumber}
 * and rendered together with each column-addition step.</p>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @see dev.hieplp.mci.web.controller.SumController
 */
@SpringBootApplication
public class MciWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(MciWebApplication.class, args);
    }

}
