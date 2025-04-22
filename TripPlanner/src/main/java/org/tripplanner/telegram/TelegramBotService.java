package org.tripplanner.telegram;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import org.tripplanner.module.planned.TripPlannerService;
import org.tripplanner.module.helper.TripHelperService;
import org.tripplanner.module.history.TripHistoryService;
import org.tripplanner.module.suggestions.SuggestionsService;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telegram Bot service: polling, command routing, messaging.
 */
@Service
public class TelegramBotService {

    private final WebClient tgClient;
    private final TripPlannerService planner;
    private final TripHelperService helper;
    private final TripHistoryService history;
    private final SuggestionsService suggestions;

    private long offset = 0;
    private final Map<Long, Long> pendingLocationRequests = new ConcurrentHashMap<>();

    public TelegramBotService(
            WebClient webClient,
            @Value("${telegram.bot.token}") String token,
            @Lazy TripPlannerService planner,
            @Lazy TripHelperService helper,
            @Lazy TripHistoryService history,
            @Lazy SuggestionsService suggestions
    ) {
        this.tgClient = webClient
                .mutate()
                .baseUrl("https://api.telegram.org/bot" + token)
                .build();
        this.planner = planner;
        this.helper = helper;
        this.history = history;
        this.suggestions = suggestions;
    }

    @PostConstruct
    public void startPolling() {
        // 1️⃣ Clear webhook once at startup
        System.out.println("🔄 Deleting existing webhook (if any)...");
        tgClient.post()
                .uri(uri -> uri.path("/deleteWebhook")
                        .queryParam("drop_pending_updates", "true")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> System.out.println("Webhook delete response: " + resp))
                .doOnError(err -> System.err.println("Failed to delete webhook: " + err.getMessage()))
                .block();

        // 2️⃣ Now start polling, but swallow 409 Conflict errors
        System.out.println("Starting getUpdates polling...");
        Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick ->
                        pollUpdates()
                                .onErrorResume(WebClientResponseException.Conflict.class, ex -> {
                                    // ignore the 409, return empty so the stream continues
                                    return Flux.empty();
                                })
                )
                .subscribe(
                        this::handleUpdate,
                        err -> System.err.println("Unexpected polling error: " + err)
                );
    }


    private Flux<Update> pollUpdates() {
        return tgClient.get()
                .uri(uri -> uri
                        .path("/getUpdates")
                        .queryParam("timeout", 30)
                        .queryParam("offset", offset)
                        .build()
                )
                .retrieve()
                .bodyToMono(GetUpdatesResponse.class)
                .flatMapMany(resp -> Flux.fromIterable(resp.result()));
    }

    private void handleUpdate(Update upd) {
        offset = upd.update_id + 1;
        Message msg = upd.message;
        if (msg == null || msg.chat == null) return;
        Long chatId = msg.chat.id;

        // LOCATION
        if (msg.location != null) {
            Long tripId = pendingLocationRequests.remove(chatId);
            if (tripId != null) {
                helper.handleLocation(tripId,
                                msg.location.latitude,
                                msg.location.longitude)
                        .subscribe();
            } else {
                sendMessage(chatId, "❗️No trip awaiting location.");
            }
            return;
        }

        String text = msg.text != null ? msg.text.trim() : "";
        if (!text.startsWith("/")) return;
        String[] parts = text.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "/help" -> sendMessage(chatId,
                    "/planned\n" +
                            "/plan name|YYYY-MM-DD|YYYY-MM-DD|pt@lat,lon;...\n" +
                            "/delete tripId\n" +
                            "/starttrip tripId\n" +
                            "/mark waypointId\n" +
                            "/note waypointId|note\n" +
                            "/history\n" +
                            "/details tripId\n" +
                            "/rate tripId|1-5\n" +
                            "/suggest"
            );
            case "/planned" -> planner.listPlanned(chatId)
                    .forEach(t -> sendMessage(chatId,
                            String.format("ID:%d %s (%s–%s)",
                                    t.getId(), t.getName(),
                                    t.getStartDate(), t.getEndDate()))
                    );
            case "/plan" -> {
                try {
                    String[] a = arg.split("\\|", 4);
                    String name = a[0];
                    LocalDate start = LocalDate.parse(a[1]);
                    LocalDate end   = LocalDate.parse(a[2]);
                    var pts = new ArrayList<TripPlannerService.WaypointData>();
                    if (a.length == 4 && !a[3].isBlank()) {
                        for (String raw : a[3].split(";")) {
                            String[] p = raw.split("@");
                            String nm = p[0];
                            String[] coords = p[1].split(",");
                            pts.add(new TripPlannerService.WaypointData(
                                    nm,
                                    Double.parseDouble(coords[0]),
                                    Double.parseDouble(coords[1])
                            ));
                        }
                    }
                    planner.createTrip(chatId, name, start, end, pts)
                            .subscribe(tp ->
                                    sendMessage(chatId,
                                            "Created trip ID:" + tp.getTrip().getId()
                                                    + " with " + tp.getWaypoints().size() + " points")
                            );
                } catch (Exception e) {
                    sendMessage(chatId, "❗️Usage: /plan name|YYYY-MM-DD|YYYY-MM-DD|pt@lat,lon;...");
                }
            }
            case "/delete" -> {
                try {
                    Long id = Long.parseLong(arg);
                    planner.deleteTrip(id);
                    sendMessage(chatId, "Deleted trip ID:" + id);
                } catch (Exception e) {
                    sendMessage(chatId, "❗️Usage: /delete tripId");
                }
            }
            case "/starttrip" -> {
                try {
                    Long tid = Long.parseLong(arg);
                    helper.onTripStart(tid);
                    pendingLocationRequests.put(chatId, tid);
                } catch (Exception e) {
                    sendMessage(chatId, "❗️Usage: /starttrip tripId");
                }
            }
            case "/mark" -> helper.markPointVisited(arg)
                    .subscribe(wp -> sendMessage(chatId, "Marked: " + wp.getName()));
            case "/note" -> {
                try {
                    var na = arg.split("\\|", 2);
                    helper.addNoteToPoint(na[0], na[1])
                            .subscribe(wp -> sendMessage(chatId, "Note added to " + wp.getName()));
                } catch (Exception e) {
                    sendMessage(chatId, "❗️Usage: /note waypointId|note");
                }
            }
            case "/history" -> history.listFinished(chatId)
                    .forEach(t -> sendMessage(chatId,
                            String.format("ID:%d %s (%s–%s) rated:%s",
                                    t.getId(), t.getName(),
                                    t.getStartDate(), t.getEndDate(),
                                    t.getRating()==null?"n/a":t.getRating()))
                    );
            case "/details" -> {
                try {
                    Long tid = Long.parseLong(arg);
                    history.getTripDetails(chatId, tid)
                            .subscribe(td -> {
                                sendMessage(chatId, "Trip: " + td.getTrip().getName());
                                td.getWaypoints().forEach(wp ->
                                        sendMessage(chatId,
                                                "- " + wp.getName() + (wp.isVisited()?" ✅":""))
                                );
                            });
                } catch (Exception e) {
                    sendMessage(chatId, "❗️Usage: /details tripId");
                }
            }
            case "/rate" -> {
                try {
                    var ra = arg.split("\\|");
                    history.addRating(chatId, Long.parseLong(ra[0]), Integer.parseInt(ra[1]));
                } catch (Exception e) {
                    sendMessage(chatId, "❗️Usage: /rate tripId|1-5");
                }
            }
            case "/suggest" -> suggestions.sendSuggestions(chatId).subscribe();
            default -> sendMessage(chatId, "Unknown command. /help");
        }
    }

    /** Simple sendMessage */
    public void sendMessage(Long chatId, String text) {
        tgClient.post()
                .uri(uri -> uri
                        .path("/sendMessage")
                        .queryParam("chat_id", chatId)
                        .queryParam("text", text)
                        .build()
                )
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();
    }

    /**
     * Prompt for location with custom keyboard.
     */
    public void requestLocation(Long chatId, String prompt) {
        var locationButton = Map.of(
                "text", "Share location",
                "request_location", true
        );
        var keyboard = Map.of(
                "keyboard", List.of(List.of(locationButton)),
                "one_time_keyboard", true,
                "resize_keyboard", true
        );
        var body = Map.of(
                "chat_id", chatId,
                "text", prompt,
                "reply_markup", keyboard
        );
        tgClient.post()
                .uri("/sendMessage")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();
    }

    // DTOs for getUpdates
    public record GetUpdatesResponse(boolean ok, List<Update> result) {}
    public record Update(long update_id, Message message) {}
    public record Message(long message_id, Chat chat, String text, Location location) {}
    public record Chat(long id) {}
    public record Location(double latitude, double longitude) {}
}
