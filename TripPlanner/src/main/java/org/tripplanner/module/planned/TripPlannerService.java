package org.tripplanner.module.planned;

import org.springframework.modulith.ApplicationModule;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import org.tripplanner.domain.TripEntity;
import org.tripplanner.repository.jpa.TripRepository;
import org.tripplanner.repository.mongo.WaypointEntity;
import org.tripplanner.repository.mongo.WaypointRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ApplicationModule(id = "planned-trips")
public class TripPlannerService {

    private final TripRepository tripRepository;
    private final WaypointRepository waypointRepository;

    public TripPlannerService(TripRepository tripRepository,
                              WaypointRepository waypointRepository) {
        this.tripRepository = tripRepository;
        this.waypointRepository = waypointRepository;
    }

    /**
     * List all trips for the user whose start date is today or later.
     */
    public List<TripEntity> listPlanned(Long userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return tripRepository
                .findAllByUserIdAndStartDateGreaterThanEqual(userId, today)
                .stream()
                .collect(Collectors.toList());
    }

    /**
     * Create a new trip and its waypoints.
     */
    public Mono<TripWithPoints> createTrip(
            Long userId,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            List<WaypointData> points
    ) {
        TripEntity trip = tripRepository.save(
                new TripEntity(userId, name, startDate, endDate)
        );

        var entities = points.stream()
                .map(d -> new WaypointEntity(
                        trip.getId(),
                        d.getName(),
                        d.getLatitude(),
                        d.getLongitude()
                ))
                .collect(Collectors.toList());

        return waypointRepository
                .saveAll(entities)
                .collectList()
                .map(saved -> new TripWithPoints(trip, saved));
    }

    /**
     * Delete a planned trip and all its waypoints.
     */
    public void deleteTrip(Long tripId) {
        tripRepository.deleteById(tripId);
        waypointRepository.findByTripId(tripId)
                .flatMap(wp -> waypointRepository.deleteById(wp.getId()))
                .subscribe();
    }

    // --- DTOs ---

    public static class WaypointData {
        private final String name;
        private final double latitude;
        private final double longitude;
        public WaypointData(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        public String getName() { return name; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }

    public static class TripWithPoints {
        private final TripEntity trip;
        private final List<WaypointEntity> waypoints;
        public TripWithPoints(TripEntity trip, List<WaypointEntity> waypoints) {
            this.trip = trip;
            this.waypoints = waypoints;
        }
        public TripEntity getTrip() { return trip; }
        public List<WaypointEntity> getWaypoints() { return waypoints; }
    }
}
