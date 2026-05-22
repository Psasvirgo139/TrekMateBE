package com.trekmate.backend.repository;
import com.trekmate.backend.model.GuideRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface GuideRatingRepository extends JpaRepository<GuideRating, UUID> {
    List<GuideRating> findByGuideIdAndIsApproved(UUID guideId, Boolean isApproved);
}
