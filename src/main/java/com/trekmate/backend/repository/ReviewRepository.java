package com.trekmate.backend.repository;
import com.trekmate.backend.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByTourIdAndIsApproved(UUID tourId, Boolean isApproved, Pageable pageable);
    Page<Review> findByUserId(UUID userId, Pageable pageable);
    Optional<Review> findByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true")
    Double avgRatingByTour(@Param("tourId") UUID tourId);

    @Query("SELECT r.overallRating, COUNT(r) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true GROUP BY r.overallRating")
    List<Object[]> countByTourGroupByRating(@Param("tourId") UUID tourId);

    long countByTourIdAndIsApproved(UUID tourId, Boolean isApproved);

    @Query("SELECT AVG(r.guideRating) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true")
    Double avgGuideRatingByTour(@Param("tourId") UUID tourId);

    @Query("SELECT AVG(r.sceneryRating) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true")
    Double avgSceneryRatingByTour(@Param("tourId") UUID tourId);

    @Query("SELECT AVG(r.safetyRating) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true")
    Double avgSafetyRatingByTour(@Param("tourId") UUID tourId);

    @Query("SELECT AVG(r.valueRating) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true")
    Double avgValueRatingByTour(@Param("tourId") UUID tourId);

    @Query("SELECT AVG(r.difficultyRating) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true")
    Double avgDifficultyRatingByTour(@Param("tourId") UUID tourId);

    @Query("SELECT AVG(r.equipmentRating) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true")
    Double avgEquipmentRatingByTour(@Param("tourId") UUID tourId);
}

