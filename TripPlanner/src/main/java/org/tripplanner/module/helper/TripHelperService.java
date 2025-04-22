package org.tripplanner.module.helper;

import org.springframework.context.annotation.Lazy;
import org.springframework.modulith.ApplicationModule;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import org.tripplanner.domain.TripEntity;
import org.tripplanner.repository.jpa.TripRepository;
import org.tripplanner.repository.mongo.WaypointEntity;
import org.tripplanner.repository.mongo.WaypointRepository;
import org.tripplanner.telegram.TelegramBotService;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@ApplicationModule(id = "trip-helper")
public class TripHelperService {

    private final TripRepository tripRepository;
    private final WaypointRepository waypointRepository;
    private final TelegramBotService telegramBotService;

    public TripHelperService(
            TripRepository tripRepository,
            WaypointRepository waypointRepository,
            @Lazy TelegramBotService telegramBotService  // ← mark lazy
    ) {
        this.tripRepository = tripRepository;
        this.waypointRepository = waypointRepository;
        this.telegramBotService = telegramBotService;
    }

    /**
     * Every hour, remind users about trips starting in next 24h.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void remindAboutClosestTrips() {
        var today    = LocalDate.now(ZoneOffset.UTC);
        var tomorrow = today.plusDays(1);
        tripRepository.findAllByStartDateBetween(today, tomorrow)
                .forEach(trip ->
                        telegramBotService.sendMessage(
                                trip.getUserId(),
                                "🔔 Your trip \"" + trip.getName() +
                                        "\" starts on " + trip.getStartDate()
                        )
                );
    }

    /**
     * When a trip starts, prompt user to share location.
     */
    public void onTripStart(Long tripId) {
        TripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        telegramBotService.requestLocation(
                trip.getUserId(),
                "Your trip \"" + trip.getName() + "\" starts now! Please share your location."
        );
    }

    /**
     * Handle location: mark nearby (<100m) waypoints visited.
     */
    public Mono<Void> handleLocation(Long tripId, double lat, double lon) {
        return waypointRepository.findByTripId(tripId)
                .filter(wp -> distanceMeters(
                        wp.getLatitude(), wp.getLongitude(), lat, lon) < 100)
                .flatMap(wp -> {
                    wp.setVisited(true);
                    return waypointRepository.save(wp)
                            .doOnNext(updated ->
                                    telegramBotService.sendMessage(
                                            updated.getTripId(),
                                            "📍 You visited \"" + updated.getName() + "\""
                                    )
                            );
                })
                .then();
    }

    /**
     * Manually mark a waypoint visited.
     */
    public Mono<WaypointEntity> markPointVisited(String waypointId) {
        return waypointRepository.findById(waypointId)
                .flatMap(wp -> {
                    wp.setVisited(true);
                    return waypointRepository.save(wp);
                });
    }

    /**
     * Add note to a waypoint.
     */
    public Mono<WaypointEntity> addNoteToPoint(String waypointId, String note) {
        return waypointRepository.findById(waypointId)
                .flatMap(wp -> {
                    wp.getNotes().add(note);
                    return waypointRepository.save(wp);
                });
    }

    private static double distanceMeters(
            double lat1, double lon1, double lat2, double lon2) {
        final int R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}
