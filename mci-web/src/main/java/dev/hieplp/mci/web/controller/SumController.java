package dev.hieplp.mci.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hieplp.mci.web.service.SumService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serves the index page and handles sum requests.
 *
 * <p>{@code GET /} renders the form. {@code POST /} echoes the two
 * operands back into the form and adds the sum — or the
 * {@link IllegalArgumentException} message on invalid input — to the
 * model for the {@code index} view.</p>
 *
 * <p>{@code POST /sum/stream} streams the same calculation as
 * Server-Sent Events: one {@code step} event per column, then a
 * {@code result} event, or a single {@code error} event on invalid
 * input. Steps are never stored server-side.</p>
 *
 * <p>Operands travel in the request body, not the query string, so
 * large inputs do not hit the HTTP header size limit.</p>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @see SumService
 */
@Controller
public class SumController {

    private final SumService sumService;
    private final ObjectMapper objectMapper;

    public SumController(SumService sumService, ObjectMapper objectMapper) {
        this.sumService = sumService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/")
    public String sum(
            @RequestParam(required = false) String stn1,
            @RequestParam(required = false) String stn2,
            Model model
    ) {
        model.addAttribute("stn1", stn1);
        model.addAttribute("stn2", stn2);
        try {
            model.addAttribute("sum", sumService.sum(stn1, stn2, null));
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "index";
    }

    @PostMapping(path = "/sum/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(
            @RequestParam(required = false) String stn1,
            @RequestParam(required = false) String stn2
    ) {
        StreamingResponseBody body = out -> {
            if (stn1 == null || stn2 == null) {
                send(out, "error", Map.of("message", "stn1 and stn2 are required"));
                return;
            }
            try {
                String sum = sumService.sum(stn1, stn2, step -> send(out, "step", step));
                send(out, "result", Map.of("sum", sum));
            } catch (IllegalArgumentException e) {
                send(out, "error", Map.of("message", e.getMessage()));
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }

    private void send(OutputStream out, String event, Object data) {
        try {
            out.write(("event:" + event + "\ndata:"
                    + objectMapper.writeValueAsString(data) + "\n\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            // client disconnected — abort the calculation
            throw new UncheckedIOException("client disconnected", e);
        }
    }

}
