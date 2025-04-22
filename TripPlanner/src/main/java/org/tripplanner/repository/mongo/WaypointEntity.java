package org.tripplanner.repository.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representing a waypoint within a trip.
 *
 * Fields:
 *  - id:           document identifier
 *  - tripId:       reference to the TripEntity.id (indexed for lookup)
 *  - name:         human‑readable name of the point
 *  - latitude:     GPS latitude
 *  - longitude:    GPS longitude
 *  - visited:      whether the user has marked (or auto‑detected) this point as visited
 *  - notes:        arbitrary user‑provided notes for this point
 */
@Document(collection = "waypoints")
public class WaypointEntity {

    @Id
    private String id;

    /** Reference to the relational TripEntity.id */
    @Indexed
    private Long tripId;

    /** Name or title of the waypoint */
    private String name;

    /** GPS coordinates */
    private double latitude;
    private double longitude;

    /** Mark if visited */
    private boolean visited = false;

    /** User‑added notes */
    private List<String> notes = new ArrayList<>();

    // ----- Constructors -----

    /** Default constructor for Spring Data */
    public WaypointEntity() { }

    /**
     * Create a new waypoint for a trip.
     *
     * @param tripId    ID of the trip this point belongs to
     * @param name      human‑readable name
     * @param latitude  GPS latitude
     * @param longitude GPS longitude
     */
    public WaypointEntity(Long tripId, String name, double latitude, double longitude) {
        this.tripId = tripId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // ----- Getters & Setters -----

    public String getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }
    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isVisited() {
        return visited;
    }
    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public List<String> getNotes() {
        return notes;
    }
    /** Replace all notes */
    public void setNotes(List<String> notes) {
        this.notes = notes;
    }
    /** Add a single note */
    public void addNote(String note) {
        this.notes.add(note);
    }
}
