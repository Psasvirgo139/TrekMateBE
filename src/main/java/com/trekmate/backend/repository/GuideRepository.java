package com.trekmate.backend.repository;
import com.trekmate.backend.model.Guide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface GuideRepository extends JpaRepository<Guide, UUID> {
    Optional<Guide> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    Page<Guide> findAllByOrderByAvgRatingDesc(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT dg.departure.id) FROM DepartureGuide dg " +
           "WHERE dg.guide.id = :guideId " +
           "AND dg.departure.status IN ('OPEN','SCHEDULED')")
    long countUpcomingToursByGuideId(@Param("guideId") UUID guideId);
}
