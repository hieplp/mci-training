package dev.hieplp.mci.web.controller;

import dev.hieplp.mci.core.MyBigNumber.SumResult;
import dev.hieplp.mci.web.service.SumService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Serves the index page and handles sum requests.
 *
 * <p>{@code GET /} echoes the two operands back into the form and, when
 * both are present, adds the {@link SumResult} — or the
 * {@link IllegalArgumentException} message on invalid input — to the
 * model for the {@code index} view.</p>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @see SumService
 */
@Controller
public class SumController {

    private final SumService sumService;

    public SumController(SumService sumService) {
        this.sumService = sumService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String stn1,
            @RequestParam(required = false) String stn2,
            Model model
    ) {
        model.addAttribute("stn1", stn1);
        model.addAttribute("stn2", stn2);
        if (stn1 != null && stn2 != null) {
            try {
                SumResult result = sumService.sum(stn1, stn2);
                model.addAttribute("result", result);
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "index";
    }

}
