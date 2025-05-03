package org.tripplanner.http;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.tripplanner.telegram.TelegramBotService;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TelegramBotService bot;
    public AdminController(TelegramBotService bot) { this.bot = bot; }

    /* ---------- HTML -------------------------------------------------- */

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView panel() {
        return new ModelAndView("admin")
                .addObject("users", bot.snapshotUsers())
                .addObject("sent", null);
    }

    @PostMapping(path = "/broadcast",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView form(@RequestParam String text) {
        int n = broadcast(text);
        return new ModelAndView("admin")
                .addObject("users", bot.snapshotUsers())
                .addObject("sent", n);
    }

    /* ---------- JSON --------------------------------------------------- */

    @PostMapping(path = "/broadcast",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String,Object> rest(@RequestBody Map<String,String> body) {
        return Map.of("delivered", broadcast(body.get("text")));
    }

    /* ---------- helper ------------------------------------------------- */

    private int broadcast(String text) {
        if (!StringUtils.hasText(text)) return 0;
        return bot.broadcast(text);
    }
}