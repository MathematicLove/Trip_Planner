package org.tripplanner.module.suggestion;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tripplanner.domain.TripEntity;
import org.tripplanner.repository.jpa.TripRepository;
import org.tripplanner.repository.mongo.WaypointEntity;
import org.tripplanner.repository.mongo.WaypointRepository;
import org.tripplanner.telegram.TelegramBotService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Генерация рекомендаций исключительно по истории путешествий пользователя.
 */
@Service            // ← обязательно!
public class SuggestionsService {

    private final TripRepository     trips;
    private final WaypointRepository waypoints;
    private final TelegramBotService bot;

    public SuggestionsService(TripRepository trips,
                              WaypointRepository waypoints,
                              @Lazy TelegramBotService bot) {
        this.trips     = trips;
        this.waypoints = waypoints;
        this.bot       = bot;
    }

    /** Flux всех подсказок. */
    public Flux<TripSuggestion> suggestBasedOnHistory(Long userId) {
        var userTripIds = trips.findAllByUserId(userId).stream()
                               .map(TripEntity::getId)
                               .collect(Collectors.toSet());

        return waypoints.findAll()
                .filter(w -> w.isVisited() && userTripIds.contains(w.getTripId()))
                .distinct(WaypointEntity::getName)
                .map(w -> new TripSuggestion(
                        "Explore around " + w.getName(),
                        "You visited \"" + w.getName() +
                                "\" — how about more nearby sights?",
                        List.of(w.getName())
                ));
    }

    /** Отправить подсказки пользователю (использует bot.sendMessage). */
    public Mono<Void> sendSuggestions(Long userId) {          // ← public!
        return suggestBasedOnHistory(userId)
                .doOnNext(s -> bot.sendMessage(
                        userId,
                        "🤖 " + s.getTitle() + ": " + s.getDescription()))
                .then();
    }

    /* ─── DTO ─── */
    public record TripSuggestion(String title,
                                 String description,
                                 List<String> relatedPoints) {

        // Java-Bean-аксессоры для старых вызовов
        public String getTitle()              { return title; }
        public String getDescription()        { return description; }
        public List<String> getRelatedPoints(){ return relatedPoints; }
    }
}
