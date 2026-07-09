package com.trekmate.backend.repository;
import com.trekmate.backend.model.DepartureGuide;
import com.trekmate.backend.model.embeddable.DepartureGuideId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.trekmate.backend.model.enums.DepartureStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Repository
public interface DepartureGuideRepository extends JpaRepository<DepartureGuide, DepartureGuideId> {
    List<DepartureGuide> findByDepartureId(UUID departureId);
    List<DepartureGuide> findByGuideId(UUID guideId);

    long countByGuideIdAndDepartureStatus(UUID guideId, DepartureStatus status);
    @Query("SELECT dg FROM DepartureGuide dg JOIN dg.departure td " +
           "WHERE dg.guide.id = :guideId " +
           "AND (:excludeDepartureId IS NULL OR td.id <> :excludeDepartureId) " +
           "AND td.status NOT IN ('CANCELLED','COMPLETED') " +
           "AND td.departureDate <= :endDate AND td.returnDate >= :startDate")
    List<DepartureGuide> findConflicts(@Param("guideId") UUID guideId,
                                       @Param("excludeDepartureId") UUID excludeDepartureId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") java.time.LocalDate endDate);

    @Query("SELECT dg FROM DepartureGuide dg JOIN FETCH dg.departure td JOIN FETCH dg.guide g " +
           "WHERE td.status NOT IN ('CANCELLED') " +
           "AND td.departureDate <= :endDate AND td.returnDate >= :startDate")
    List<DepartureGuide> findAssignmentsInPeriod(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
}
