package com.trekmate.backend.service;

import com.trekmate.backend.dto.request.GuideReplyRequest;
import com.trekmate.backend.dto.request.ReviewRequest;
import com.trekmate.backend.dto.response.ReviewResponse;
import com.trekmate.backend.dto.response.ReviewSummaryResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID userId, ReviewRequest request);

    Page<ReviewResponse> getReviewsByTour(UUID tourId, UUID currentUserId, int page, int size, String sortBy);

    Page<ReviewResponse> getReviewsByUser(UUID userId, int page, int size);

    ReviewSummaryResponse getReviewSummary(UUID tourId);

    ReviewResponse toggleHelpful(Long reviewId, UUID userId);

    ReviewResponse replyToReview(Long reviewId, UUID guideUserId, GuideReplyRequest request);

    ReviewResponse approveReview(Long reviewId);

    void deleteReview(Long reviewId, UUID userId);
}
