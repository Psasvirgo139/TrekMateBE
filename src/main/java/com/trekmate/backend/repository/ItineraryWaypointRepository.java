package com.trekmate.backend.repository;

import com.trekmate.backend.model.ItineraryWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ItineraryWaypointRepository extends JpaRepository<ItineraryWaypoint, UUID> {
    List<ItineraryWaypoint> findByItineraryIdOrderByVisitOrderAsc(UUID itineraryId);
}
