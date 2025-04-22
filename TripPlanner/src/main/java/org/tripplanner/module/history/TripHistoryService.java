package org.tripplanner.module.history;

import org.springframework.modulith.ApplicationModule;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import org.tripplanner.domain.TripEntity;
import org.tripplanner.repository.jpa.TripRepository;
import org.tripplanner.repository.mongo.WaypointEntity;
import org.tripplanner.repository.mongo.WaypointRepository;
import org.tripplanner.telegram.TelegramBotService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for trip‑history functionality:
 *  - List finished trips
 *  - Show details for a specific finished trip
 *  - Add a rating
 */
@Service
@ApplicationModule(id = "trip-history")
public class TripHistoryService {

    private final TripRepository tripRepository;
    private final WaypointRepository waypointRepository;
    private final TelegramBotService telegramBotService;

    public TripHistoryService(TripRepository tripRepository,
                              WaypointRepository waypointRepository,
                              TelegramBotService telegramBotService) {
        this.tripRepository = tripRepository;
        this.waypointRepository = waypointRepository;
        this.telegramBotService = telegramBotService;
    }

    /** List all trips ended before today. */
    public List<TripEntity> listFinished(Long userId) {
        var today = LocalDate.now(ZoneOffset.UTC);
        return tripRepository
                .findAllByUserIdAndEndDateBefore(userId, today)
                .stream()
                .collect(Collectors.toList());
    }

    /**
     * Details for one finished trip.
     */
    public Mono<TripDetails> getTripDetails(Long userId, Long tripId) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        if (!trip.getUserId().equals(userId))
            return Mono.error(new IllegalArgumentException("Access denied"));
        if (!trip.getEndDate().isBefore(LocalDate.now(ZoneOffset.UTC)))
            return Mono.error(new IllegalStateException("Trip not yet finished"));

        return waypointRepository.findByTripId(tripId)
                .collectList()
                .map(wps -> new TripDetails(trip, wps));
    }

    /**
     * Add or update a rating (1–5).
     */
    public TripEntity addRating(Long userId, Long tripId, int rating) {
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("Rating must be 1–5");
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        if (!trip.getUserId().equals(userId))
            throw new IllegalArgumentException("Access denied");
        if (!trip.getEndDate().isBefore(LocalDate.now(ZoneOffset.UTC)))
            throw new IllegalStateException("Cannot rate ongoing trip");

        trip.setRating(rating);
        TripEntity updated = tripRepository.save(trip);
        telegramBotService.sendMessage(
                userId,
                "⭐️ Your trip \"" + updated.getName() +
                        "\" has been rated " + rating + "/5. Thanks!"
        );
        return updated;
    }

    /** DTO for details */
    public static class TripDetails {
        private final TripEntity trip;
        private final List<WaypointEntity> waypoints;
        public TripDetails(TripEntity trip, List<WaypointEntity> waypoints) {
            this.trip = trip;
            this.waypoints = waypoints;
        }
        public TripEntity getTrip() { return trip; }
        public List<WaypointEntity> getWaypoints() { return waypoints; }
    }
}
