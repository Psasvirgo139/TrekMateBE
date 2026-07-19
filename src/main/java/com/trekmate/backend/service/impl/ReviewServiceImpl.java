package com.trekmate.backend.service.impl;

import com.trekmate.backend.dto.request.GuideReplyRequest;
import com.trekmate.backend.dto.request.ReviewRequest;
import com.trekmate.backend.dto.response.ReviewResponse;
import com.trekmate.backend.dto.response.ReviewSummaryResponse;
import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.trekmate.backend.mapper.ReviewMapper;
import com.trekmate.backend.model.*;
import com.trekmate.backend.model.embeddable.ReviewHelpfulId;
import com.trekmate.backend.model.enums.BookingStatus;
import com.trekmate.backend.repository.*;
import com.trekmate.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final GuideRepository guideRepository;
    private final ReviewMapper reviewMapper;
    private final TourRepository tourRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(UUID userId, ReviewRequest request) {
        // 1. Validate booking exists and belongs to user
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "You can only review your own bookings");
        }

        // 2. Validate booking is COMPLETED
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "You can only review completed tours");
        }

        // 3. Check if already reviewed
        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 4. Build and save review
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Review review = Review.builder()
                .user(user)
                .booking(booking)
                .tour(booking.getDeparture().getTour())
                .departure(booking.getDeparture())
                .guide(findPrimaryGuide(booking.getDeparture()))
                .overallRating(request.getOverallRating())
                .guideRating(request.getGuideRating())
                .difficultyRating(request.getDifficultyRating())
                .sceneryRating(request.getSceneryRating())
                .safetyRating(request.getSafetyRating())
                .valueRating(request.getValueRating())
                .equipmentRating(request.getEquipmentRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .photos(request.getPhotos() != null ? request.getPhotos() : new ArrayList<>())
                .isAnonymous(Boolean.TRUE.equals(request.getIsAnonymous()))
                .isApproved(true) // Auto-approve for MVP
                .build();

        review = reviewRepository.save(review);
        log.info("Review created: id={}, bookingId={}, userId={}", review.getId(), request.getBookingId(), userId);

        updateTourStats(review.getTour());
        updateGuideStats(review.getGuide());

        return reviewMapper.toResponse(review, userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByTour(UUID tourId, UUID currentUserId, int page, int size, String sortBy) {
        Sort sort = buildSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Review> reviews = reviewRepository.findByTourIdAndIsApproved(tourId, true, pageable);

        return reviews.map(review -> {
            boolean isHelpful = currentUserId != null &&
                    reviewHelpfulRepository.existsByIdReviewIdAndIdUserId(review.getId(), currentUserId);
            return reviewMapper.toResponse(review, currentUserId, isHelpful);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByUser(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews = reviewRepository.findByUserId(userId, pageable);

        return reviews.map(review -> {
            boolean isHelpful = reviewHelpfulRepository.existsByIdReviewIdAndIdUserId(review.getId(), userId);
            return reviewMapper.toResponse(review, userId, isHelpful);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(UUID tourId) {
        long totalReviews = reviewRepository.countByTourIdAndIsApproved(tourId, true);

        // Rating distribution
        List<Object[]> distribution = reviewRepository.countByTourGroupByRating(tourId);
        Map<Integer, Long> ratingDistribution = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }
        for (Object[] row : distribution) {
            Integer rating = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            ratingDistribution.put(rating, count);
        }

        return ReviewSummaryResponse.builder()
                .avgOverallRating(reviewRepository.avgRatingByTour(tourId))
                .avgGuideRating(reviewRepository.avgGuideRatingByTour(tourId))
                .avgSceneryRating(reviewRepository.avgSceneryRatingByTour(tourId))
                .avgSafetyRating(reviewRepository.avgSafetyRatingByTour(tourId))
                .avgValueRating(reviewRepository.avgValueRatingByTour(tourId))
                .avgDifficultyRating(reviewRepository.avgDifficultyRatingByTour(tourId))
                .avgEquipmentRating(reviewRepository.avgEquipmentRatingByTour(tourId))
                .totalReviews(totalReviews)
                .ratingDistribution(ratingDistribution)
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse toggleHelpful(Long reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        ReviewHelpfulId helpfulId = new ReviewHelpfulId(reviewId, userId);
        boolean exists = reviewHelpfulRepository.existsById(helpfulId);

        if (exists) {
            reviewHelpfulRepository.deleteById(helpfulId);
            review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            ReviewHelpful helpful = ReviewHelpful.builder()
                    .id(helpfulId)
                    .review(review)
                    .user(user)
                    .build();
            reviewHelpfulRepository.save(helpful);
            review.setHelpfulCount(review.getHelpfulCount() + 1);
        }

        review = reviewRepository.save(review);
        return reviewMapper.toResponse(review, userId, !exists);
    }

    @Override
    @Transactional
    public ReviewResponse replyToReview(Long reviewId, UUID guideUserId, GuideReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // Verify user is a guide (simplified — just check guide table)
        Guide guide = guideRepository.findById(guideUserId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_GUIDE_NOT_FOUND,
                        "Only guides can reply to reviews"));

        review.setGuideReply(request.getGuideReply());
        review.setGuideRepliedAt(LocalDateTime.now());
        review = reviewRepository.save(review);

        log.info("Guide {} replied to review {}", guideUserId, reviewId);
        return reviewMapper.toResponse(review, guideUserId, false);
    }

    @Override
    @Transactional
    public ReviewResponse approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        review.setIsApproved(true);
        review = reviewRepository.save(review);

        log.info("Review {} approved", reviewId);
        updateTourStats(review.getTour());
        updateGuideStats(review.getGuide());
        return reviewMapper.toResponse(review, null, false);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // Allow owner or admin (admin check is done at controller level via @PreAuthorize)
        if (!review.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "You can only delete your own reviews");
        }

        Tour tour = review.getTour();
        Guide guide = review.getGuide();

        reviewRepository.delete(review);
        log.info("Review {} deleted by user {}", reviewId, userId);

        updateTourStats(tour);
        updateGuideStats(guide);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Guide findPrimaryGuide(TourDeparture departure) {
        if (departure.getGuideAssignments() != null && !departure.getGuideAssignments().isEmpty()) {
            return departure.getGuideAssignments().get(0).getGuide();
        }
        return null;
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sortBy) {
            case "highest" -> Sort.by(Sort.Direction.DESC, "overallRating");
            case "lowest" -> Sort.by(Sort.Direction.ASC, "overallRating");
            case "helpful" -> Sort.by(Sort.Direction.DESC, "helpfulCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
    private void updateTourStats(Tour tour) {
        if (tour == null) return;
        Double avgRating = reviewRepository.avgRatingByTour(tour.getId());
        long totalReviews = reviewRepository.countByTourIdAndIsApproved(tour.getId(), true);
        tour.setAvgRating(avgRating != null ? java.math.BigDecimal.valueOf(avgRating) : java.math.BigDecimal.ZERO);
        tour.setTotalReviews((int) totalReviews);
        tourRepository.save(tour);
    }

    private void updateGuideStats(Guide guide) {
        if (guide == null) return;
        Double avgRating = reviewRepository.avgRatingByGuide(guide.getId());
        long totalReviews = reviewRepository.countByGuideIdAndIsApproved(guide.getId(), true);
        guide.setAvgRating(avgRating != null ? java.math.BigDecimal.valueOf(avgRating) : java.math.BigDecimal.ZERO);
        guide.setTotalReviews((int) totalReviews);
        guideRepository.save(guide);
    }
}
