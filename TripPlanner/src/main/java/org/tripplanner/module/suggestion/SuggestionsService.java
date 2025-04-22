package org.tripplanner.module.suggestions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.ApplicationModule;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.tripplanner.domain.TripEntity;
import org.tripplanner.repository.jpa.TripRepository;
import org.tripplanner.repository.mongo.WaypointEntity;
import org.tripplanner.repository.mongo.WaypointRepository;
import org.tripplanner.telegram.TelegramBotService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for trip‑suggestion functionality:
 *  - History‑based suggestions
 *  - LLM‑based recommendations
 *  - Sending both to user
 */
@Service
@ApplicationModule(id = "suggestions")
public class SuggestionsService {

    private final TripRepository tripRepository;
    private final WaypointRepository waypointRepository;
    private final WebClient webClient;
    private final TelegramBotService telegramBotService;
    private final String llmApiUrl;
    private final String llmApiKey;

    public SuggestionsService(
            TripRepository tripRepository,
            WaypointRepository waypointRepository,
            WebClient webClient,
            TelegramBotService telegramBotService,
            @Value("${llm.api.url}") String llmApiUrl,
            @Value("${llm.api.key}") String llmApiKey
    ) {
        this.tripRepository = tripRepository;
        this.waypointRepository = waypointRepository;
        this.webClient = webClient;
        this.telegramBotService = telegramBotService;
        this.llmApiUrl = llmApiUrl;
        this.llmApiKey = llmApiKey;
    }

    /** Based on visited points */
    public Flux<TripSuggestion> suggestBasedOnHistory(Long userId) {
        var tripIds = tripRepository.findAllByUserId(userId)
                .stream()
                .map(TripEntity::getId)
                .collect(Collectors.toList());

        return waypointRepository.findAll()
                .filter(wp -> wp.isVisited() && tripIds.contains(wp.getTripId()))
                .distinct(WaypointEntity::getName)
                .map(wp -> new TripSuggestion(
                        "Explore around " + wp.getName(),
                        "You visited \"" + wp.getName() +
                                "\"—how about more nearby sights?",
                        List.of(wp.getName())
                ));
    }

    /** Via external LLM */
    public Mono<List<TripSuggestion>> suggestViaLLM(Long userId) {
        String prompt = buildLlmPrompt(userId);

        return webClient.post()
                .uri(llmApiUrl)
                .header("Authorization", "Bearer " + llmApiKey)
                .bodyValue(new LlmRequest(prompt))
                .retrieve()
                .bodyToFlux(TripSuggestion.class)
                .collectList();
    }

    /** Send both */
    public Mono<Void> sendSuggestions(Long userId) {
        suggestBasedOnHistory(userId)
                .doOnNext(s -> telegramBotService.sendMessage(
                        userId, "🤖 " + s.getTitle() + ": " + s.getDescription()))
                .subscribe();

        return suggestViaLLM(userId)
                .doOnNext(list -> list.forEach(s ->
                        telegramBotService.sendMessage(
                                userId, "🧠 " + s.getTitle() + ": " + s.getDescription()
                        )
                ))
                .then();
    }

    private String buildLlmPrompt(Long userId) {
        var visited = waypointRepository.findAll()
                .filter(wp -> wp.isVisited() &&
                        tripRepository.findAllByUserId(userId)
                                .stream()
                                .map(TripEntity::getId)
                                .collect(Collectors.toList())
                                .contains(wp.getTripId()))
                .map(WaypointEntity::getName)
                .distinct()
                .collectList()
                .block();

        return "User visited: " + visited +
                ". Suggest three new trips with name and description.";
    }

    /** DTOs */
    public static class TripSuggestion {
        private final String title, description;
        private final List<String> relatedPoints;
        public TripSuggestion(String title, String description, List<String> relatedPoints) {
            this.title = title;
            this.description = description;
            this.relatedPoints = relatedPoints;
        }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public List<String> getRelatedPoints() { return relatedPoints; }
    }

    public static class LlmRequest {
        private final String prompt;
        public LlmRequest(String prompt) { this.prompt = prompt; }
        public String getPrompt() { return prompt; }
    }
}
