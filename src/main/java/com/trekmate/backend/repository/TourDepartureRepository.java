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
    long countByTourId(UUID tourId);

    @Query("SELECT MIN(td.pricePerPerson) FROM TourDeparture td WHERE td.tour.id = :tourId AND td.status IN ('OPEN','SCHEDULED')")
    Optional<BigDecimal> findMinPriceByTourId(@Param("tourId") UUID tourId);

    @Query("SELECT COUNT(td) FROM TourDeparture td WHERE td.tour.id = :tourId AND td.status IN ('OPEN','SCHEDULED')")
    long countUpcomingByTourId(@Param("tourId") UUID tourId);

    @Query("SELECT COUNT(td) FROM TourDeparture td WHERE td.status IN ('OPEN','SCHEDULED') AND td.departureDate >= CURRENT_DATE")
    long countUpcoming();

    @Query("SELECT td FROM TourDeparture td WHERE td.tour.id = :tourId " +
           "AND td.status IN :statuses " +
           "AND td.departureDate >= :date " +
           "ORDER BY td.departureDate ASC")
    List<TourDeparture> findUpcomingDepartures(
            @Param("tourId") UUID tourId,
            @Param("statuses") List<DepartureStatus> statuses,
            @Param("date") LocalDate date);

    /**
     * Dùng cho WeatherScheduler: tìm tất cả departure trong khoảng ngày cần dự báo.
     * Status IN ('OPEN','SCHEDULED') để chỉ cập nhật tour sắp diễn ra.
     */
    @Query("SELECT td FROM TourDeparture td JOIN FETCH td.tour t " +
           "WHERE td.departureDate BETWEEN :fromDate AND :toDate " +
           "AND td.status IN ('OPEN', 'SCHEDULED')")
    List<TourDeparture> findDeparturesForWeatherUpdate(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Dùng cho AiRecommendationScheduler: tìm các departure chưa có bản ghi
     * AI recommendation trong bảng departure_ai_recommendations.
     * Chỉ quét departure trong phạm vi ngày chỉ định với status OPEN hoặc SCHEDULED.
     */
    @Query("SELECT td FROM TourDeparture td JOIN FETCH td.tour t " +
           "WHERE td.departureDate BETWEEN :fromDate AND :toDate " +
           "AND td.status IN ('OPEN', 'SCHEDULED') " +
           "AND NOT EXISTS (SELECT 1 FROM DepartureAiRecommendation r WHERE r.departure.id = td.id)")
    List<TourDeparture> findDeparturesForAiRecommendation(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}

