package com.trekmate.backend.repository;
import com.trekmate.backend.model.DepartureGuide;
import com.trekmate.backend.model.embeddable.DepartureGuideId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Repository
public interface DepartureGuideRepository extends JpaRepository<DepartureGuide, DepartureGuideId> {
    List<DepartureGuide> findByDepartureId(UUID departureId);
    List<DepartureGuide> findByGuideId(UUID guideId);
    @Query("SELECT dg FROM DepartureGuide dg JOIN dg.departure td " +
           "WHERE dg.guide.id = :guideId " +
           "AND td.status NOT IN ('CANCELLED','COMPLETED') " +
           "AND td.departureDate <= :endDate AND td.returnDate >= :startDate")
    List<DepartureGuide> findConflicts(@Param("guideId") UUID guideId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}
