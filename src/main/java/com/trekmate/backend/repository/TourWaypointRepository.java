package com.trekmate.backend.repository;
import com.trekmate.backend.model.TourWaypoint;
import com.trekmate.backend.model.enums.WaypointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface TourWaypointRepository extends JpaRepository<TourWaypoint, UUID> {
    List<TourWaypoint> findByTourIdOrderBySequenceOrderAsc(UUID tourId);
    List<TourWaypoint> findByTourIdAndWaypointType(UUID tourId, WaypointType type);
    List<TourWaypoint> findByTourIdAndDayNumber(UUID tourId, Short dayNumber);
}
