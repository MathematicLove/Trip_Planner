package org.tripplanner.http;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.tripplanner.telegram.TelegramBotService;

@Controller
public class UsersController {

    private final TelegramBotService bot;
    public UsersController(TelegramBotService bot) { this.bot = bot; }

    // HTML
    @GetMapping(path = "/users", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView page() {
        return new ModelAndView("users")
                .addObject("users", bot.snapshotUsers());
    }

    // JSON
    @GetMapping(path = "/users/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object json() { return bot.snapshotUsers(); }
}
