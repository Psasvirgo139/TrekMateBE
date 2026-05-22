package com.trekmate.backend.repository;
import com.trekmate.backend.model.TourDailyItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface TourDailyItineraryRepository extends JpaRepository<TourDailyItinerary, UUID> {
    List<TourDailyItinerary> findByTourIdOrderByDayNumberAsc(UUID tourId);
    Optional<TourDailyItinerary> findByTourIdAndDayNumber(UUID tourId, Short dayNumber);
}
