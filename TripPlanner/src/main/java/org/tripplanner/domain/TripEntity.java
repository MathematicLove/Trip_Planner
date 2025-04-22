package org.tripplanner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * JPA entity representing a Trip.
 * Covers:
 *  - Planned trips (name, start/end dates)
 *  - Association to a user (userId)
 *  - Optional rating for completed trips
 */
@Entity
@Table(name = "trips")
public class TripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Telegram user ID or application user ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Human‑readable trip name or title */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Inclusive start date of the trip */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Inclusive end date of the trip */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Optional user rating (e.g. 1–5) for completed trips */
    @Column(name = "rating")
    private Integer rating;

    // ----- Constructors -----

    public TripEntity() {
        // Default constructor required by JPA
    }

    public TripEntity(Long userId, String name, LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // ----- Getters & Setters -----

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getRating() {
        return rating;
    }
    /**
     * Set a rating for a completed trip (1–5), or null to clear.
     */
    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
