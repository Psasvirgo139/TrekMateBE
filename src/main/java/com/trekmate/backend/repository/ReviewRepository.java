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
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByTourIdAndIsApproved(UUID tourId, Boolean isApproved, Pageable pageable);
    Page<Review> findByUserId(UUID userId, Pageable pageable);
    Optional<Review> findByBookingId(UUID bookingId);
    boolean existsByBookingId(UUID bookingId);
    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true")
    Double avgRatingByTour(@Param("tourId") UUID tourId);
}
