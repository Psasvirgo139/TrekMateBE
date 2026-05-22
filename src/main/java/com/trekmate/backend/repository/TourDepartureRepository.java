package com.trekmate.backend.repository;
import com.trekmate.backend.model.TourDeparture;
import com.trekmate.backend.model.enums.DepartureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface TourDepartureRepository extends JpaRepository<TourDeparture, UUID> {
    Page<TourDeparture> findByTourId(UUID tourId, Pageable pageable);
    Page<TourDeparture> findByStatus(DepartureStatus status, Pageable pageable);
    List<TourDeparture> findByTourIdAndStatus(UUID tourId, DepartureStatus status);
    Optional<TourDeparture> findByTourIdAndDepartureDate(UUID tourId, LocalDate departureDate);

    @Query("SELECT MIN(td.pricePerPerson) FROM TourDeparture td WHERE td.tour.id = :tourId AND td.status IN ('OPEN','SCHEDULED')")
    Optional<BigDecimal> findMinPriceByTourId(@Param("tourId") UUID tourId);

    @Query("SELECT COUNT(td) FROM TourDeparture td WHERE td.tour.id = :tourId AND td.status IN ('OPEN','SCHEDULED')")
    long countUpcomingByTourId(@Param("tourId") UUID tourId);

    @Query("SELECT COUNT(td) FROM TourDeparture td WHERE td.status IN ('OPEN','SCHEDULED') AND td.departureDate >= CURRENT_DATE")
    long countUpcoming();
}
