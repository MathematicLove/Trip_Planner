package org.tripplanner.telegram;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.tripplanner.dto.UserDTO;
import org.tripplanner.module.helper.TripHelperService;
import org.tripplanner.module.history.TripHistoryService;
import org.tripplanner.module.planned.TripPlannerService;
import org.tripplanner.module.suggestion.SuggestionsService;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Telegram-бот (long-polling): команды, рассылка, учёт пользователей.
 */
@Service
public class TelegramBotService {

    /* ──────────── DI ──────────────────────────────────────────────── */
    private final WebClient           tg;
    private final TripPlannerService  planner;
    private final TripHelperService   helper;
    private final TripHistoryService  history;
    private final SuggestionsService  suggestions;

    /* ──────────── runtime ─────────────────────────────────────────── */
    private long offset = 0;
    private final Map<Long, Long>  pendingLocation = new ConcurrentHashMap<>();
    private final Map<Long, Instant> knownUsers    = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DMY =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /* ──────────── ctor ────────────────────────────────────────────── */
    public TelegramBotService(WebClient baseClient,
                              @Value("${telegram.bot.token}") String token,
                              @Lazy TripPlannerService planner,
                              @Lazy TripHelperService helper,
                              @Lazy TripHistoryService history,
                              @Lazy SuggestionsService suggestions) {

        this.tg = baseClient.mutate()
                .baseUrl("https://api.telegram.org/bot" + token)
                .build();

        this.planner     = planner;
        this.helper      = helper;
        this.history     = history;
        this.suggestions = suggestions;
    }

    /* ──────────── long-polling ────────────────────────────────────── */
    @PostConstruct
    public void startPolling() {
        tg.post().uri("/deleteWebhook?drop_pending_updates=true")
          .retrieve().bodyToMono(String.class)
          .onErrorResume(__ -> Mono.empty()).block();

        Flux.interval(Duration.ofSeconds(1))
            .flatMap(t -> poll()
                    .onErrorResume(WebClientResponseException.Conflict.class,
                                   __ -> Flux.empty()))
            .subscribe(this::handleUpdate,
                       err -> System.err.println("Polling error: " + err));
    }

    private Flux<Update> poll() {
        return tg.get().uri(uri -> uri.path("/getUpdates")
                     .queryParam("timeout", 30)
                     .queryParam("offset", offset).build())
                 .retrieve()
                 .bodyToMono(GetUpdates.class)
                 .flatMapMany(r -> Flux.fromIterable(r.result()));
    }

    /* ──────────── Update handler ──────────────────────────────────── */
    private void handleUpdate(Update upd) {
        offset = upd.update_id + 1;
        Message msg = upd.message;
        if (msg == null || msg.chat == null) return;
        long chat = msg.chat.id;

        // регистрируем пользователя
        knownUsers.computeIfAbsent(chat, __ -> Instant.now());

        /* геолокация */
        if (msg.location != null) {
            Long tid = pendingLocation.remove(chat);
            if (tid != null)
                helper.handleLocation(tid,
                        msg.location.latitude, msg.location.longitude).subscribe();
            else send(chat, "❗️Неожиданная геолокация.");
            return;
        }

        /* команды */
        String txt = msg.text == null ? "" : msg.text.trim();
        if (!txt.startsWith("/")) { send(chat, "Неизвестная команда. /help"); return; }

        String cmd = txt.split("\\s+", 2)[0].toLowerCase();
        switch (cmd) {
            case "/help"    -> cmdHelp(chat);
            case "/planned" -> cmdPlanned(chat);
            case "/plan"    -> cmdPlan(chat, txt.substring(5).trim());
            case "/delete"  -> cmdDelete(chat, txt);
            case "/start"   -> cmdStart(chat, txt);
            case "/mark"    -> cmdMark(chat, txt);
            case "/note"    -> cmdNote(chat, txt);
            case "/history" -> cmdHistory(chat);
            case "/details" -> cmdDetails(chat, txt);
            case "/rate"    -> cmdRate(chat, txt);
            case "/suggest" -> suggestions.sendSuggestions(chat).subscribe();
            default         -> send(chat, "Неизвестная команда. /help");
        }
    }

    /* ──────────── команды бота ────────────────────────────────────── */

    private void cmdHelp(long c) {
        send(c, """
                /plan name:<название> start:<YYYY-MM-DD> end:<YYYY-MM-DD>
                     pts:<Название@lat,lon;…>
                  пример:
                    /plan name:Weekend start:2025-06-01 end:2025-06-03 \
pts:Дом@55.75,37.62;Музей@55.74,37.60
                /planned            – предстоящие поездки
                /delete <id>        – удалить поездку
                /start  <id>        – начать (запросит геолокацию)
                /mark   <pointId>   – отметить точку
                /note   <pointId> <текст>
                /history            – завершённые поездки
                /details <id>
                /rate   <id> <1-5>
                /suggest            – рекомендации
                """);
    }

    private void cmdPlanned(long c) {
        planner.listPlanned(c)
               .forEach(t -> send(c, "ID:" + t.getId() + " "
                       + t.getName() + " (" + t.getStartDate() + "—" + t.getEndDate() + ")"));
    }

    /* ---------- /plan ---------- */
    private static final Pattern KV = Pattern.compile("(\\w+):([^\\s]+)");

    private void cmdPlan(long chat, String body) {
        Matcher m = KV.matcher(body);
        Map<String, String> map = new HashMap<>();
        while (m.find()) map.put(m.group(1).toLowerCase(), m.group(2));

        if (!(map.containsKey("name") && map.containsKey("start") && map.containsKey("end"))) {
            send(chat, "❗️Пример: /plan name:Trip start:2025-06-01 end:2025-06-05 "
                       +"pts:Paris@48.85,2.35");
            return;
        }
        try {
            String   name = map.get("name");
            LocalDate from = LocalDate.parse(map.get("start"));
            LocalDate to   = LocalDate.parse(map.get("end"));

            List<TripPlannerService.WaypointData> pts = new ArrayList<>();
            if (map.containsKey("pts")) {
                for (String raw : map.get("pts").split(";")) {
                    String[] p = raw.split("@");
                    String[] xy = p[1].split(",");
                    pts.add(new TripPlannerService.WaypointData(
                            p[0], Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
                }
            }

            planner.createTrip(chat, name, from, to, pts)
                   .subscribe(tp -> send(chat, "Создано. id=" + tp.getTrip().getId()),
                              err -> send(chat, err.getMessage().contains("INVALID_DATE")
                                                   ? "Неверная дата. Сегодня "
                                                     + DMY.format(LocalDate.now(ZoneOffset.UTC))
                                                   : "Ошибка: " + err.getMessage()));
        } catch (Exception ex) {
            send(chat, "❗️Ошибка: " + ex.getMessage());
        }
    }

    private void cmdDelete(long c, String txt) {
        try {
            planner.deleteTrip(Long.parseLong(arg(txt)));
            send(c, "Удалено.");
        } catch (Exception e) { send(c, "/delete <id>"); }
    }

    private void cmdStart(long c, String txt) {
        try {
            long id = Long.parseLong(arg(txt));
            helper.onTripStart(id);
            pendingLocation.put(c, id);
        } catch (Exception e) { send(c, "/start <id>"); }
    }

    private void cmdMark(long c, String txt) {
        try {
            helper.markPointVisited(arg(txt))
                  .subscribe(wp -> send(c, "✅ " + wp.getName()));
        } catch (Exception e) { send(c, "/mark <pointId>"); }
    }

    private void cmdNote(long c, String txt) {
        String[] p = txt.split("\\s+", 3);
        if (p.length < 3) { send(c, "/note <pointId> <текст>"); return; }
        helper.addNoteToPoint(p[1], p[2])
              .subscribe(__ -> send(c, "✍️"));
    }

    private void cmdHistory(long c) {
        history.listFinished(c)
               .forEach(t -> send(c, "ID:"+ t.getId() +" "+ t.getName()
                       +" ★:"+(t.getRating()==null?"—":t.getRating())));
    }

    private void cmdDetails(long c, String txt) {
        try {
            history.getTripDetails(c, Long.parseLong(arg(txt)))
                   .subscribe(td -> {
                       send(c, "Trip "+ td.getTrip().getName());
                       td.getWaypoints().forEach(w ->
                               send(c, (w.isVisited() ? "✅ " : "▫️ ") + w.getName()));
                   });
        } catch (Exception e) { send(c, "/details <id>"); }
    }

    private void cmdRate(long c, String txt) {
        String[] p = txt.split("\\s+");
        if (p.length != 3) { send(c, "/rate <id> <1-5>"); return; }
        try {
            history.addRating(c, Long.parseLong(p[1]), Integer.parseInt(p[2]));
        } catch (Exception e) { send(c, "Ошибка: " + e.getMessage()); }
    }

    /* ──────────── admin-helpers ───────────────────────────────────── */

    /** Снимок всех известных пользователей. */
    public List<UserDTO> snapshotUsers() {
        return knownUsers.entrySet().stream()
                .map(e -> new UserDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(UserDTO::createdAt))
                .toList();
    }

    /** Рассылка сообщения всем пользователям. */
    public int broadcast(String text) {
        int ok = 0;
        for (Long chatId : knownUsers.keySet()) {
            try { send(chatId, text); ok++; } catch (Exception ignored) {}
        }
        return ok;
    }

    /* ──────────── helpers ─────────────────────────────────────────── */
    private static String arg(String s) { return s.split("\\s+", 2)[1]; }

    public void sendMessage(Long chatId, String text) { send(chatId, text); }

    public void requestLocation(Long chatId, String prompt) {
        var btn = Map.of("text", "Share location", "request_location", true);
        var kb  = Map.of("keyboard", List.of(List.of(btn)),
                         "one_time_keyboard", true,
                         "resize_keyboard", true);
        var body = Map.of("chat_id", chatId, "text", prompt, "reply_markup", kb);
        tg.post().uri("/sendMessage").bodyValue(body)
          .retrieve().bodyToMono(Void.class).subscribe();
    }

    private void send(long id, String text) {
        tg.post().uri(uri -> uri.path("/sendMessage")
                .queryParam("chat_id", id)
                .queryParam("text", text).build())
          .retrieve().bodyToMono(Void.class).subscribe();
    }

    /* ─── Telegram DTO ─────────────────────────────────────────────── */
    public record GetUpdates(boolean ok, List<Update> result) {}
    public record Update(long update_id, Message message) {}
    public record Message(long message_id, Chat chat,
                          String text, Location location) {}
    public record Chat(long id) {}
    public record Location(double latitude, double longitude) {}
}
