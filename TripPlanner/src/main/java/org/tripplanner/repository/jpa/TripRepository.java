package org.tripplanner.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tripplanner.domain.TripEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * JPA repository for TripEntity.
 * Provides methods to query planned and finished trips by user and date.
 */
@Repository
public interface TripRepository extends JpaRepository<TripEntity, Long> {

    /**
     * Find all trips for a given user whose start date is on or after the given date.
     * Used to list planned (upcoming) trips.
     */
    List<TripEntity> findAllByUserIdAndStartDateGreaterThanEqual(Long userId, LocalDate date);

    /**
     * Find all trips for a given user whose end date is before the given date.
     * Used to list finished (past) trips.
     */
    List<TripEntity> findAllByUserIdAndEndDateBefore(Long userId, LocalDate date);

    /**
     * Find all trips for a given user.
     * Used by suggestion logic to gather history.
     */
    List<TripEntity> findAllByUserId(Long userId);

    /**
     * Find all trips whose start date falls between two dates (inclusive).
     * Used to remind users about trips starting soon.
     */
    List<TripEntity> findAllByStartDateBetween(LocalDate start, LocalDate end);
}
