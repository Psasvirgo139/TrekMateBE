package com.trekmate.backend.repository;
import com.trekmate.backend.model.ReviewHelpful;
import com.trekmate.backend.model.embeddable.ReviewHelpfulId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
@Repository
public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, ReviewHelpfulId> {
    long countByReviewId(Long reviewId);
}
