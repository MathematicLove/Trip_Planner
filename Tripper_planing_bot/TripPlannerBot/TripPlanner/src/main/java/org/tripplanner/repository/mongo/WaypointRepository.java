package org.tripplanner.repository.mongo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Reactive MongoDB repository for WaypointEntity.
 * Provides methods to query, save and delete waypoints associated with trips.
 */
@Repository
public interface WaypointRepository extends ReactiveMongoRepository<WaypointEntity, String> {

    /**
     * Find all waypoints belonging to a given trip.
     * Used by TripHelperService, TripHistoryService, SuggestionsService, etc.
     *
     * @param tripId the ID of the TripEntity
     * @return a Flux of all WaypointEntity documents with that tripId
     */
    Flux<WaypointEntity> findByTripId(Long tripId);

    // Note: save, saveAll, findById, findAll(), deleteById, etc.
    // are inherited from ReactiveMongoRepository.
}
